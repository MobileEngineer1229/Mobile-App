"""표본추출 기반 글 생성 모듈.

【초보자 안내】
  이 파일은 훈련된 GPT 모형으로 글을 한 글자씩 생성하는 함수들을 담고 있다.

  생성 과정 요약:
    1. 프롬프트 글자렬을 어표 ID 목록으로 변환
    2. 모형에 넣어 다음 글자 확률 계산 (16,384개)
    3. 확률에서 반복 벌점, Top-K, Top-P, 온도 순으로 조정
    4. 최종 확률로 다음 글자 하나를 무작위 선택
    5. 선택된 글자를 입력에 추가하고 2번으로 돌아감
    6. EOS가 나오거나 max_new_tokens에 도달하면 종료

  생성 품질 조절 도구:
    온도 (temperature):    낮으면 더 확실한 글자, 높으면 더 다양한 글자
    Top-K:                 확률 상위 K개 후보만 허용
    Top-P (핵 표본추출):   누적 확률 P 이하의 후보만 허용
    반복 벌점:             이미 나온 글자의 확률을 낮춰 반복 방지
"""

from __future__ import annotations

from typing import Iterator

import torch
import torch.nn.functional as F

from src.tokenizer.tokenizer import BOS_ID, EOS_ID, Tokenizer


def _apply_repetition_penalty(logits: torch.Tensor, generated_ids: torch.Tensor, penalty: float) -> torch.Tensor:
    """이미 생성한 글자의 확률을 낮추어 반복을 억제한다 (CTRL 방식).

    【초보자 안내】
      이 벌점이 없으면 모형이 같은 말을 무한 반복한다.
      예: "김치 김치 김치 김치 김치..."

      CTRL 방식:
        - 이미 생성된 글자 중 logit(점수)이 양수이면 → penalty로 나눔 (낮아짐)
        - logit이 음수이면 → penalty를 곱함 (더 낮아짐, 즉 확률 감소)
        - penalty=1.0: 벌점 없음 (기본 동작)
        - penalty=1.15 (기본값): 약 13% 확률 감소

    인수:
        logits:        현재 단계의 점수 벡토르 (어휘 크기)
        generated_ids: 지금까지 생성된 어표 ID 목록
        penalty:       반복 벌점 계수 (1.0 = 없음, >1.0 = 반복 억제)

    반환값:
        벌점이 적용된 logits
    """
    if penalty == 1.0 or generated_ids.numel() == 0:
        return logits  # 벌점 없음 또는 아직 생성된 어표 없음 → 그대로 반환

    # 이미 생성된 어표들의 logit 값만 추출
    score = logits.gather(-1, generated_ids)
    # 양수는 나누고, 음수는 곱해서 항상 확률이 내려가도록
    score = torch.where(score < 0, score * penalty, score / penalty)
    # 조정된 값을 원래 위치에 다시 넣기
    logits.scatter_(-1, generated_ids, score)
    return logits


def _top_k_top_p(logits: torch.Tensor, top_k: int, top_p: float) -> torch.Tensor:
    """Top-K 와 Top-P (핵 표본추출) 필터를 적용한다.

    【초보자 안내】
      이 함수가 없으면 아주 낮은 확률의 엉뚱한 글자도 뽑힐 수 있다.

      Top-K 필터:
        상위 K개 후보만 남기고 나머지를 -∞로 만든다.
        예: top_k=50 → 50개 후보 중에서만 선택

      Top-P (핵 표본추출) 필터:
        확률의 누적 합이 p가 될 때까지의 후보만 남긴다.
        예: top_p=0.95 → 합계 95%가 되는 상위 몇 개만 선택
        상황에 따라 후보 수가 달라지는 동적 필터

      두 필터를 순서대로 적용한다 (Top-K 먼저).

    인수:
        logits: 현재 단계의 점수 벡토르
        top_k:  상위 K개 후보 수 (0이면 필터 없음)
        top_p:  누적 확률 임계값 (1.0이면 필터 없음)

    반환값:
        필터가 적용된 logits (-∞로 막힌 후보들 포함)
    """
    if top_k > 0:
        # 어휘 크기보다 크게 설정하면 의미 없으므로 조정
        top_k = min(top_k, logits.size(-1))
        # 상위 top_k번째 값을 찾아 기준값으로 삼음
        kth = torch.topk(logits, top_k).values[..., -1, None]
        # 기준값보다 작은 logit은 모두 -∞ 로 → 소프트막스 후 확률 0
        logits = torch.where(logits < kth, torch.full_like(logits, float("-inf")), logits)

    if 0.0 < top_p < 1.0:
        # 높은 것부터 정렬
        sorted_logits, sorted_idx = torch.sort(logits, descending=True)
        # 소프트막스로 확률 계산 후 누적 합 계산
        probs = F.softmax(sorted_logits, dim=-1)
        cum = probs.cumsum(dim=-1)

        # 누적 확률이 top_p를 초과하는 위치 표시
        remove = cum > top_p
        # 한 칸 오른쪽으로 이동: 경계값은 포함시키기 위해
        remove[..., 1:] = remove[..., :-1].clone()
        remove[..., 0] = False  # 첫 번째는 항상 포함

        # 제거 표시된 위치를 -∞ 로
        sorted_logits = sorted_logits.masked_fill(remove, float("-inf"))
        # 정렬 전 원래 위치로 되돌리기
        logits = torch.empty_like(logits).scatter_(-1, sorted_idx, sorted_logits)

    return logits


@torch.no_grad()
def sample_token(
    model,
    idx: torch.Tensor,
    *,
    temperature: float,
    top_k: int,
    top_p: float,
    repetition_penalty: float,
) -> torch.Tensor:
    """다음 어표 하나를 표본추출한다.

    【초보자 안내】
      이 함수가 "다음 글자 하나를 선택" 하는 핵심 함수다.
      모형을 한 번 실행하고, 필터들을 적용한 뒤, 확률로 선택한다.

      @torch.no_grad(): 기울기 계산을 하지 않음 → 추론 시 기억기 절약

    인수:
        model:             GPT 모형 인스턴스
        idx:               현재까지의 어표 ID 목록, 형태 (B, T)
        temperature:       온도 (낮을수록 더 확실한 선택)
        top_k:             Top-K 후보 수
        top_p:             Top-P 누적 확률 임계값
        repetition_penalty: 반복 벌점 계수

    반환값:
        선택된 다음 어표 ID, 형태 (B, 1)
    """
    # block_size(문맥 길이) 확인: torch.compile 포장을 고려
    block_size = model.cfg.block_size if hasattr(model, "cfg") else model._orig_mod.cfg.block_size

    # 입력이 block_size를 초과하면 가장 최근 block_size 개만 사용
    idx_cond = idx if idx.size(1) <= block_size else idx[:, -block_size:]

    # 모형 실행 → logits: (B, 1, vocab_size)
    logits, _ = model(idx_cond)
    logits = logits[:, -1, :]  # 마지막 위치의 logit만 사용: (B, vocab_size)

    # 1. 반복 벌점 적용 (이미 생성된 어표의 확률을 낮춤)
    logits = _apply_repetition_penalty(logits, idx, repetition_penalty)

    if temperature <= 0.0:
        # 탐욕적 선택: 항상 가장 높은 확률의 글자 선택 (다양성 없음)
        return logits.argmax(dim=-1, keepdim=True)

    # 2. 온도 나누기: 높은 온도 → 확률 분포가 평평해짐 (더 다양)
    #                낮은 온도 → 확률 분포가 뾰족해짐 (더 확실)
    logits = logits / temperature

    # 3. Top-K + Top-P 필터: 엉뚱한 후보 제거
    logits = _top_k_top_p(logits, top_k=top_k, top_p=top_p)

    # 4. 소프트막스로 확률 계산 후 무작위 선택
    probs = F.softmax(logits, dim=-1)
    next_id = torch.multinomial(probs, num_samples=1)  # 확률에 비례한 무작위 선택
    return next_id


@torch.no_grad()
def generate_stream(
    model,
    tokenizer: Tokenizer,
    prompt: str,
    *,
    max_new_tokens: int = 256,
    temperature: float = 0.9,
    top_k: int = 50,
    top_p: float = 0.95,
    repetition_penalty: float = 1.15,
    device: torch.device | str = "cuda",
    stop_on_eos: bool = True,
) -> Iterator[str]:
    """새로 생성된 글자를 한 조각씩 내보내는 스트리밍 생성기.

    【초보자 안내】
      이 함수는 Gradio UI에서 글자가 실시간으로 나타나는 효과를 만든다.
      완성된 글을 한꺼번에 반환하지 않고, 새 글자가 생길 때마다 바로 내보낸다.

      "델타(delta)" 방식:
        각 단계에서 새로 추가된 부분만 내보낸다 (누적이 아님).
        SentencePiece BPE는 여러 어표를 합쳐서 한 글자를 만들 수 있으므로
        전체를 재해석하고 이전에 내보낸 부분을 빼서 새 부분만 계산한다.

    인수:
        model:             GPT 모형 인스턴스
        tokenizer:         어표 분석기 인스턴스
        prompt:            생성을 시작할 프롬프트 글자렬
        max_new_tokens:    생성할 최대 어표 수
        temperature:       온도
        top_k:             Top-K 후보 수
        top_p:             Top-P 임계값
        repetition_penalty: 반복 벌점
        device:            실행 장치 ("cuda" 또는 "cpu")
        stop_on_eos:       EOS 어표 생성 시 중단 여부

    생성값:
        새로 생성된 글 조각 (델타 문자열)
    """
    model.eval()  # 추론 모드: 드롭아웃 비활성화

    # 프롬프트를 어표 ID로 변환 (BOS 추가)
    ids = tokenizer.encode(prompt, add_bos=True)
    idx = torch.tensor([ids], dtype=torch.long, device=device)  # (1, T)

    produced: list[int] = []  # 새로 생성된 어표 ID 목록 (프롬프트 제외)
    last_text = ""             # 마지막으로 내보낸 글자렬 (델타 계산용)

    for _ in range(max_new_tokens):
        # 다음 어표 선택
        nxt = sample_token(
            model, idx,
            temperature=temperature, top_k=top_k, top_p=top_p,
            repetition_penalty=repetition_penalty,
        )
        token_id = int(nxt.item())

        # EOS 어표가 나오면 생성 종료
        if stop_on_eos and token_id == EOS_ID:
            break

        produced.append(token_id)  # 생성된 어표 기록
        idx = torch.cat([idx, nxt], dim=1)  # 입력에 새 어표 추가 (T가 1 증가)

        # 전체 생성된 어표를 글자렬로 변환 (BPE 합치기 고려)
        # SentencePiece BPE는 여러 어표가 합쳐져야 하나의 글자를 이룰 수 있음
        # 따라서 매 단계마다 전체를 재해석해야 정확한 글자가 나옴
        text = tokenizer.decode(produced)
        if len(text) > len(last_text):
            yield text[len(last_text):]  # 새로 추가된 부분(델타)만 내보냄
            last_text = text


@torch.no_grad()
def generate(
    model,
    tokenizer: Tokenizer,
    prompt: str,
    *,
    max_new_tokens: int = 256,
    temperature: float = 0.9,
    top_k: int = 50,
    top_p: float = 0.95,
    repetition_penalty: float = 1.15,
    device: torch.device | str = "cuda",
    stop_on_eos: bool = True,
) -> str:
    """비스트리밍 글 생성 — 전체 완성된 글자렬을 한꺼번에 반환.

    【초보자 안내】
      generate_stream() 의 비스트리밍 버전이다.
      모든 글자가 생성된 후 프롬프트 + 완성된 응답을 하나의 문자열로 반환한다.
      훈련 중 sample_text() 함수에서 생성 예시를 볼 때 사용한다.

    인수:
        (generate_stream과 동일)

    반환값:
        프롬프트 + 생성된 글자렬 (완성본)
    """
    # generate_stream에서 모든 델타를 모아 이어붙임
    chunks = list(
        generate_stream(
            model, tokenizer, prompt,
            max_new_tokens=max_new_tokens,
            temperature=temperature, top_k=top_k, top_p=top_p,
            repetition_penalty=repetition_penalty,
            device=device, stop_on_eos=stop_on_eos,
        )
    )
    return prompt + "".join(chunks)  # 프롬프트 + 생성된 내용 합쳐서 반환
