"""다중 차례 대화 세션 관리자.

【초보자 안내】
  이 파일은 GPT 모형을 챗봇처럼 쓸 수 있도록 감싸는 클라스들을 담고 있다.

  기본 모형은 단순한 언어 모형이다 — "다음 글자를 예측" 할 뿐이다.
  이 파일은 대화 형식의 프롬프트를 만들어 챗봇처럼 동작하게 한다.

  대화 형식:
    사용자: <사용자가 입력한 글>
    조수: <모형이 생성한 응답>
    사용자: <다음 사용자 입력>
    조수: <모형이 계속 생성>

  역사(history) 관리:
    대화 기록을 매 차례 프롬프트에 이어붙인다.
    기록이 너무 길어지면 가장 오래된 차례부터 잘라낸다.
    (모형이 한 번에 볼 수 있는 최대 글자 수 = block_size = 1024)

  체계 접두사(system_prefix):
    대화 시작 전에 모형에게 역할을 알려주는 지시문.
    예: "당신은 조선식 료리법 안내 조수입니다."
    어휘 크기가 8000 이상일 때만 적용한다.
    (작은 어휘 모형에서는 지시를 따르는 능력이 부족하므로)
"""

from __future__ import annotations

from dataclasses import dataclass, field
from pathlib import Path
from typing import Iterator

import torch

ROOT = Path(__file__).resolve().parents[2]  # 프로젝트 루트 경로

from src.inference.generate import generate_stream  # 스트리밍 글 생성 함수
from src.model.config import FullConfig, ModelConfig, load_config
from src.model.transformer import GPT
from src.tokenizer.tokenizer import Tokenizer, load_tokenizer


# 대화 형식 태그 — 모형이 이 태그를 보고 누구의 말인지 구분함
USER_TAG      = "사용자: "  # 사용자 말 앞에 붙이는 태그
ASSISTANT_TAG = "조수: "    # 조수(모형) 말 앞에 붙이는 태그


@dataclass
class ChatTurn:
    """대화 한 차례를 나타내는 자료 구조.

    인수:
        role: "user" (사용자) 또는 "assistant" (조수)
        text: 해당 차례의 글 내용
    """
    role: str  # "user" 또는 "assistant"
    text: str


@dataclass
class ChatSession:
    """대화 세션 — 모형, 어표 분석기, 대화 역사를 관리.

    【초보자 안내】
      이 클라스가 GPT 모형과 사용자 사이의 "대화 창구" 역할을 한다.
      chat() 또는 chat_stream() 을 호출하면 응답을 생성한다.

    필드:
        model:         GPT 모형 인스턴스
        tokenizer:     어표 분석기 인스턴스
        cfg:           전체 설정 (FullConfig)
        device:        모형이 올라간 장치 (cuda 또는 cpu)
        history:       지금까지의 대화 기록 (ChatTurn 목록)
        system_prefix: 대화 시작 전 지시문 (선택)
    """
    model:         GPT
    tokenizer:     Tokenizer
    cfg:           FullConfig
    device:        torch.device
    history:       list[ChatTurn] = field(default_factory=list)
    system_prefix: str = ""  # 선택적 체계 지시문

    def reset(self) -> None:
        """대화 역사를 모두 지운다 (새 대화 시작)."""
        self.history.clear()

    def _format_prompt(self, user_message: str) -> str:
        """사용자 메시지와 대화 역사를 프롬프트 글자렬로 조합.

        출력 형식 례:
            당신은 료리법 안내 조수입니다.   ← 체계 접두사 (있을 때만)
            사용자: 안녕하세요              ← 역사: 이전 차례들
            조수: 안녕하세요! 무엇을 도와드릴까요?
            사용자: 김치 담그는 방법이요     ← 현재 사용자 입력
            조수:                           ← 모형이 여기서부터 생성

        인수:
            user_message: 이번 차례 사용자의 입력 글

        반환값:
            모형에 넣을 전체 프롬프트 글자렬
        """
        parts: list[str] = []

        # 체계 접두사가 있으면 맨 앞에 추가
        if self.system_prefix:
            parts.append(self.system_prefix.rstrip() + "\n")

        # 이전 대화 기록을 차례대로 추가
        for turn in self.history:
            tag = USER_TAG if turn.role == "user" else ASSISTANT_TAG
            parts.append(f"{tag}{turn.text}")

        # 이번 차례 사용자 입력 추가
        parts.append(f"{USER_TAG}{user_message}")

        # "조수: " 태그만 추가하고 내용은 비워둠 → 모형이 여기서부터 생성
        parts.append(ASSISTANT_TAG.rstrip())

        return "\n".join(parts)

    def _truncate_history_to_fit(self, user_message: str, max_new_tokens: int) -> None:
        """프롬프트가 문맥 길이를 초과하면 오래된 대화 기록을 잘라낸다.

        【초보자 안내】
          모형은 한 번에 최대 block_size(1024) 개의 어표만 처리할 수 있다.
          대화가 길어지면 프롬프트가 이 한계를 초과한다.
          그럴 때 가장 오래된 사용자-조수 차례 쌍부터 제거한다.

        예산 계산:
          budget = block_size(1024) - max_new_tokens(256) - 8(여유)
                 = 760 어표

          프롬프트가 760 어표 이하가 될 때까지 오래된 기록 제거.

        인수:
            user_message:   현재 사용자 입력
            max_new_tokens: 생성할 최대 어표 수
        """
        # 안전 여유 8을 뺀 프롬프트 최대 허용 어표 수
        budget = self.cfg.model.block_size - max_new_tokens - 8

        while self.history:
            prompt = self._format_prompt(user_message)
            # 현재 프롬프트의 어표 수 계산
            n = len(self.tokenizer.encode(prompt, add_bos=True))
            if n <= budget:
                return  # 예산 안에 들어오면 종료

            # 가장 오래된 사용자 차례 제거
            self.history.pop(0)
            # 그 다음이 조수 차례이면 함께 제거 (사용자-조수 쌍으로 제거)
            if self.history and self.history[0].role == "assistant":
                self.history.pop(0)

    def chat_stream(
        self,
        user_message: str,
        *,
        max_new_tokens: int | None = None,
        temperature: float | None = None,
        top_k: int | None = None,
        top_p: float | None = None,
        repetition_penalty: float | None = None,
    ) -> Iterator[str]:
        """사용자 입력에 대한 응답을 스트리밍으로 생성.

        【초보자 안내】
          이 함수는 응답 글자를 한꺼번에 반환하지 않고,
          생성되는 즉시 조금씩 내보낸다 (스트리밍).
          Gradio UI에서 글자가 하나씩 나타나는 효과가 이것 때문이다.

          응답 중단 조건:
            1. EOS 어표가 생성됨 (generate_stream 내부에서 처리)
            2. 모형이 "사용자:" 태그를 생성함 → 스스로 다음 사람 역할을 시작
               이 경우 "사용자:" 앞까지만 출력하고 중단

          생성이 끝나면 이번 차례를 history 에 추가한다.

        인수:
            user_message:      사용자 입력 글
            max_new_tokens:    생성할 최대 어표 수 (None = 설정 기본값)
            temperature:       온도 (None = 설정 기본값)
            top_k:             Top-K (None = 설정 기본값)
            top_p:             Top-P (None = 설정 기본값)
            repetition_penalty: 반복 벌점 (None = 설정 기본값)

        생성값:
            응답 글의 조각 문자열 (스트리밍)
        """
        ic = self.cfg.inference  # 추론 설정 단축 참조
        max_new = max_new_tokens or ic.default_max_new_tokens

        # 프롬프트가 문맥 길이를 초과하면 오래된 기록 잘라내기
        self._truncate_history_to_fit(user_message, max_new)

        # 최종 프롬프트 구성
        prompt = self._format_prompt(user_message)

        produced_chunks: list[str] = []  # 지금까지 생성된 글 조각들

        # 스트리밍 생성 시작
        for delta in generate_stream(
            self.model, self.tokenizer, prompt,
            max_new_tokens=max_new,
            temperature=temperature if temperature is not None else ic.default_temperature,
            top_k=top_k          if top_k          is not None else ic.default_top_k,
            top_p=top_p          if top_p          is not None else ic.default_top_p,
            repetition_penalty=repetition_penalty if repetition_penalty is not None else ic.default_repetition_penalty,
            device=self.device,
        ):
            # 지금까지 생성된 전체 글 (이전 조각들 + 새 조각)
            running = "".join(produced_chunks) + delta

            # 모형이 스스로 "사용자:" 태그를 쓰기 시작하면 중단
            # (모형이 대화를 혼자 계속하려는 현상 방지)
            if "\n사용자:" in running or running.startswith("사용자:"):
                cut = running.find("사용자:")
                trimmed = running[:cut].rstrip()  # "사용자:" 앞까지만 유효
                # 이미 출력한 부분을 제외한 나머지만 내보냄
                already = "".join(produced_chunks)
                if len(trimmed) > len(already):
                    yield trimmed[len(already):]
                produced_chunks = [trimmed]
                break  # 생성 중단

            produced_chunks.append(delta)
            yield delta  # 새 글 조각을 즉시 내보냄

        # 생성된 전체 응답을 대화 역사에 추가
        full_response = "".join(produced_chunks).strip()
        self.history.append(ChatTurn(role="user",      text=user_message))
        self.history.append(ChatTurn(role="assistant", text=full_response))

    def chat(self, user_message: str, **kwargs) -> str:
        """사용자 입력에 대한 전체 응답을 한꺼번에 반환.

        chat_stream() 의 비스트리밍 버전.
        응답이 완전히 생성된 후 하나의 글자렬로 반환한다.
        """
        return "".join(self.chat_stream(user_message, **kwargs))


def load_session_from_checkpoint(
    checkpoint_path: str | Path,
    config_path: str | Path = "config/model_config.yaml",
    device: str | torch.device = "auto",
    system_prefix: str = "",
) -> ChatSession:
    """체크포인트 파일에서 ChatSession을 불러온다.

    【초보자 안내】
      이 함수가 훈련된 모형을 불러와 챗봇으로 쓸 준비를 하는 관문이다.
      gradio_app.py가 시작할 때 이 함수를 호출한다.

    처리 순서:
      1. 설정 파일 읽기
      2. 장치 결정 (GPU가 있으면 자동으로 GPU)
      3. 체크포인트 파일에서 모형 가중치와 설정 불러오기
      4. 체크포인트의 설정으로 ModelConfig 복원 (저장 시 설정 우선)
      5. GPT 모형 인스턴스 생성 후 가중치 불러오기
      6. torch.compile 포장 제거 (필요 시)
      7. 어표 분석기 불러오기
      8. ChatSession 생성 후 반환

    인수:
        checkpoint_path: 훈련된 체크포인트 파일 경로 (.pt)
        config_path:     설정 파일 경로 (.yaml)
        device:          장치 ("auto", "cpu", "cuda")
        system_prefix:   체계 지시문 글자렬

    반환값:
        사용 준비가 된 ChatSession 인스턴스
    """
    cfg = load_config(config_path)  # 설정 파일 읽기

    # 장치 결정
    if str(device) == "auto":
        # GPU가 있으면 GPU, 없으면 CPU
        device_t = torch.device("cuda" if torch.cuda.is_available() else "cpu")
    else:
        device_t = torch.device(device)

    # 체크포인트 파일 불러오기
    # weights_only=False: config 사전도 함께 불러오기 위해
    ckpt = torch.load(str(checkpoint_path), map_location=device_t, weights_only=False)

    # 체크포인트에 저장된 모형 설정으로 복원
    # (훈련 중 block_size 등이 자동 조정됐을 수 있으므로 yaml보다 우선)
    saved_model_cfg = ckpt.get("config", {}).get("model")
    if saved_model_cfg:
        cfg.model = ModelConfig(
            **{k: v for k, v in saved_model_cfg.items() if k in ModelConfig.__dataclass_fields__}
        )

    # 복원된 설정으로 GPT 모형 생성 후 지정 장치로 이동
    model = GPT(cfg.model).to(device_t)

    # 가중치 불러오기
    state = ckpt["model_state"]
    # torch.compile 을 쓴 경우 가중치 이름에 "_orig_mod." 접두사가 붙음
    # 이를 제거해야 일반 GPT 모형에 불러올 수 있음
    if any(k.startswith("_orig_mod.") for k in state.keys()):
        state = {k.replace("_orig_mod.", "", 1): v for k, v in state.items()}
    model.load_state_dict(state)
    model.eval()  # 추론 모드 설정 (드롭아웃 비활성화 등)

    # 어표 분석기 불러오기
    tokenizer = load_tokenizer(
        ROOT / "checkpoints" / f"{cfg.tokenizer.output_prefix}.model"
    )

    # ChatSession 생성 후 반환
    return ChatSession(
        model=model,
        tokenizer=tokenizer,
        cfg=cfg,
        device=device_t,
        system_prefix=system_prefix,
    )
