"""조선말 조수 Gradio 6 채팅 UI.

【초보자 안내】
  이 파일이 사용자와 직접 대화하는 웹 인터페이스를 만든다.
  python -m src.app.gradio_app 를 실행하면 http://127.0.0.1:8000 에서 열린다.

  UI 구성:
    탭 1 - 자유 대화:  일반 대화 (음식, 운동, 기타)
    탭 2 - 료리 추천:  조선식 료리법 안내
    탭 3 - 운동 계획:  운동 계획 작성

  각 탭에는:
    - 대화 화면 (chatbot)
    - 메시지 입력창
    - 보내기 / 초기화 버튼
    - 샘플링 조정 슬라이더 (온도, Top-K, Top-P 등)

  Windows 포트 주의:
    포트 7787~7886은 Hyper-V/WSL이 예약한 포트이므로 쓸 수 없다.
    이 프로그람은 기본값 8000을 사용한다.

  실행 방법:
    python -m src.app.gradio_app
    python -m src.app.gradio_app --checkpoint checkpoints/ckpt_step005000_final.pt
    python -m src.app.gradio_app --share  (인터넷 공개 링크 생성)
"""

from __future__ import annotations

import argparse
import re
import sys
from pathlib import Path

import gradio as gr

# 프로젝트 루트를 파이썬 경로에 추가
ROOT = Path(__file__).resolve().parents[2]
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))

from src.inference.chat import ChatSession, ChatTurn, load_session_from_checkpoint  # noqa: E402


# 각 탭의 체계 접두사 (역할 지시문)
# 어휘 크기가 8000 이상일 때만 적용됨 (소규모 모형은 지시를 따르지 못함)
SYSTEM_PREFIXES = {
    "free": "당신은 음식과 운동에 대해 안내하는 조선말 조수입니다. 친절하게 대답하세요.",
    "recipe": (
        "당신은 조선식 료리법 안내 조수입니다. "
        "사용자의 요청에 따라 재료, 조리방법을 단계별로 안내해주세요."
    ),
    "workout": (
        "당신은 운동 계획 안내 조수입니다. "
        "사용자의 목표와 수준에 맞춰 안전하고 효과적인 운동 계획을 짜주세요."
    ),
}


def find_default_checkpoint(out_dir: Path) -> Path | None:
    """체크포인트 폴더에서 가장 적합한 체크포인트를 자동으로 찾는다.

    우선순위:
      1. _best 태그 파일 (검증 손실이 가장 낮을 때 저장된 것)
      2. _final 태그 파일 (훈련 완료 시 저장된 것)
      3. 가장 단계 번호가 높은 태그 없는 파일

    인수:
        out_dir: 체크포인트 폴더 경로

    반환값:
        가장 적합한 체크포인트 파일 경로, 없으면 None
    """
    # 1순위: _best 태그 파일
    best = list(out_dir.glob("ckpt_step*_best.pt"))
    if best:
        return max(best, key=lambda p: p.stat().st_mtime)  # 가장 최근 파일

    # 2순위: _final 태그 파일
    final = list(out_dir.glob("ckpt_step*_final.pt"))
    if final:
        return max(final, key=lambda p: p.stat().st_mtime)

    # 3순위: 단계 번호가 가장 높은 태그 없는 파일
    pattern = re.compile(r"^ckpt_step(\d+)\.pt$")
    untagged = []
    for p in out_dir.glob("ckpt_step*.pt"):
        m = pattern.match(p.name)
        if m:
            untagged.append((int(m.group(1)), p))  # (단계 번호, 경로)
    if not untagged:
        return None
    untagged.sort(key=lambda x: x[0])  # 단계 번호 오름차순 정렬
    return untagged[-1][1]              # 가장 큰 단계 번호의 파일 반환


def build_chat_tab(session: ChatSession, label: str, system_prefix: str, placeholder: str):
    """대화 탭 UI를 구성하고 이벤트 연결을 설정한다.

    【초보자 안내】
      Gradio 6의 UI 요소들을 배치하고, 사용자 입력에 반응하는 함수를 연결한다.
      respond() 함수가 사용자 입력을 받아 모형 응답을 스트리밍으로 내보낸다.

    인수:
        session:       ChatSession 인스턴스 (모형, 어표 분석기 포함)
        label:         탭 제목 문자열
        system_prefix: 이 탭의 역할 지시문
        placeholder:   입력창의 안내 글자
    """
    # Gradio 6: 대화 역사를 상태로 관리
    # 형식: [{"role": "user", "content": "..."}, {"role": "assistant", "content": "..."}]
    history_state = gr.State([])

    with gr.Column():
        gr.Markdown(f"### {label}")

        # 대화 화면: 사용자와 조수의 대화가 표시됨
        chatbot = gr.Chatbot(height=480, show_label=False)

        # 메시지 입력창
        msg = gr.Textbox(placeholder=placeholder, show_label=False, lines=2, autofocus=True)

        with gr.Row():
            send_btn  = gr.Button("보내기", variant="primary")  # 메시지 전송
            clear_btn = gr.Button("초기화")                      # 대화 기록 삭제

        # 샘플링 조정 아코디언 (기본값으로 닫혀 있음)
        with gr.Accordion("샘플링 조정", open=False):
            temperature    = gr.Slider(0.1, 1.5,
                value=session.cfg.inference.default_temperature,        step=0.05,
                label="Temperature (온도: 낮을수록 일관적, 높을수록 창의적)")
            top_k          = gr.Slider(1,   200,
                value=session.cfg.inference.default_top_k,              step=1,
                label="Top-k (상위 K개 후보만 허용)")
            top_p          = gr.Slider(0.1, 1.0,
                value=session.cfg.inference.default_top_p,              step=0.01,
                label="Top-p (누적 확률 P까지의 후보만 허용)")
            rep_penalty    = gr.Slider(1.0, 2.0,
                value=session.cfg.inference.default_repetition_penalty, step=0.05,
                label="Repetition penalty (반복 벌점: 높을수록 반복 억제)")
            max_new_tokens = gr.Slider(16, 1024,
                value=session.cfg.inference.default_max_new_tokens,     step=16,
                label="Max new tokens (최대 생성 어표 수)")

    def respond(user_message, history, t, k, p, rp, mn):
        """사용자 입력을 받아 모형 응답을 스트리밍으로 생성하는 콜백 함수.

        인수:
            user_message: 사용자가 입력한 글
            history:      Gradio 형식의 대화 역사 목록
            t:            온도
            k:            Top-K
            p:            Top-P
            rp:           반복 벌점
            mn:           최대 생성 어표 수

        생성값:
            (chatbot, history_state, msg) 튜플 — 매 스트리밍 단계마다
        """
        if not user_message.strip():
            yield history, history, ""  # 빈 입력은 무시
            return

        # Gradio 6 메시지 사전 형식을 ChatTurn 목록으로 변환
        # Gradio: [{"role": "user", "content": "..."}]
        # ChatSession: [ChatTurn(role="user", text="...")]
        session.history = []
        for m in history:
            role = m.get("role", "")
            text = m.get("content", "") or ""
            if role in ("user", "assistant"):
                session.history.append(ChatTurn(role=role, text=text))

        # 어휘 크기가 충분할 때만 체계 접두사 적용
        # 작은 말뭉치에서 훈련한 모형은 지시를 따르는 능력이 부족함
        session.system_prefix = system_prefix if session.tokenizer.vocab_size >= 8000 else ""

        # Gradio 대화 역사에 현재 차례 추가 (조수 내용은 일단 빈 칸)
        history = history + [
            {"role": "user",      "content": user_message},
            {"role": "assistant", "content": ""},
        ]

        # 스트리밍 응답 생성
        partial = ""
        for delta in session.chat_stream(
            user_message,
            temperature=t, top_k=int(k), top_p=p,
            repetition_penalty=rp, max_new_tokens=int(mn),
        ):
            partial += delta                        # 새 글자를 누적
            history[-1]["content"] = partial        # 조수 말풍선 실시간 업데이트
            yield history, history, ""             # UI에 반영

    def clear():
        """대화 기록을 모두 지운다."""
        return [], []  # chatbot, history_state 모두 빈 목록으로

    # 이벤트 연결: 보내기 버튼 또는 Enter 키 → respond 실행
    send_btn.click(
        respond,
        inputs=[msg, history_state, temperature, top_k, top_p, rep_penalty, max_new_tokens],
        outputs=[chatbot, history_state, msg],
    )
    msg.submit(  # Enter 키로도 전송 가능
        respond,
        inputs=[msg, history_state, temperature, top_k, top_p, rep_penalty, max_new_tokens],
        outputs=[chatbot, history_state, msg],
    )
    # 초기화 버튼 → clear 실행
    clear_btn.click(clear, outputs=[chatbot, history_state])


def build_app(session: ChatSession) -> gr.Blocks:
    """3개 탭으로 구성된 Gradio Blocks 앱을 만든다.

    탭 구성:
      자유 대화: 일반 목적 대화
      료리 추천: 조선식 료리법 안내에 특화
      운동 계획: 운동 계획 작성에 특화

    인수:
        session: ChatSession 인스턴스

    반환값:
        gr.Blocks 앱 인스턴스
    """
    with gr.Blocks(title="DPRK 조수 (Food & Workout)") as demo:
        gr.Markdown(
            "# 조선말 조수 — 음식 · 운동\n"
            "From-scratch GPT trained on DPRK Korean food/workout corpus."
        )
        with gr.Tabs():
            with gr.Tab("자유 대화"):
                build_chat_tab(session, "자유 대화",  SYSTEM_PREFIXES["free"],
                               "무엇을 물어보시겠습니까?")
            with gr.Tab("료리 추천"):
                build_chat_tab(session, "료리 추천",  SYSTEM_PREFIXES["recipe"],
                               "예: 점심으로 된장국 끓이는 방법을 알려주세요")
            with gr.Tab("운동 계획"):
                build_chat_tab(session, "운동 계획",  SYSTEM_PREFIXES["workout"],
                               "예: 30분 가슴운동 계획을 짜주세요")
    return demo


def main() -> int:
    """프로그람 시작점 — 인수를 처리하고 Gradio 서버를 기동한다.

    처리 순서:
      1. 명령줄 인수 처리
      2. 체크포인트 파일 자동 탐색 (또는 지정된 파일 사용)
      3. ChatSession 불러오기 (모형 + 어표 분석기)
      4. Gradio 앱 구성
      5. 서버 기동 (포트 8000)

    반환값:
        0: 성공
        1: 체크포인트를 찾지 못함
    """
    # 명령줄 인수 처리
    parser = argparse.ArgumentParser(description="조선말 조수 채팅 UI.")
    parser.add_argument("--checkpoint",   type=str, default=None,
                        help="사용할 체크포인트 파일 경로 (지정하지 않으면 자동 탐색)")
    parser.add_argument("--config",       type=str, default="config/model_config.yaml",
                        help="설정 파일 경로")
    parser.add_argument("--device",       type=str, default="auto",
                        help="실행 장치: auto, cpu, cuda")
    parser.add_argument("--share",        action="store_true",
                        help="인터넷 공개 링크 생성 (Gradio 공유 서버 이용)")
    parser.add_argument("--server-port",  type=int, default=8000,
                        help="서버 포트 (기본값: 8000; Windows 7787~7886 사용 불가)")
    args = parser.parse_args()

    out_dir = ROOT / "checkpoints"

    # 체크포인트 경로 결정
    if args.checkpoint:
        ckpt_path = Path(args.checkpoint)  # 명령줄로 지정된 파일 사용
    else:
        # 체크포인트 폴더에서 가장 적합한 파일 자동 선택
        ckpt_path = find_default_checkpoint(out_dir)
        if ckpt_path is None:
            print(
                f"체크포인트를 찾지 못했습니다: {out_dir}\n"
                f"scripts/03_train_model.ps1 을 먼저 실행하십시오.",
                file=sys.stderr,
            )
            return 1
        print(f"[app] 체크포인트 사용: {ckpt_path}")

    # 모형 불러오기 및 ChatSession 생성
    session = load_session_from_checkpoint(
        checkpoint_path=ckpt_path,
        config_path=ROOT / args.config,
        device=args.device,
    )
    print(f"[app] 세션 준비 완료, 장치: {session.device}")

    # Gradio 앱 구성
    demo = build_app(session)
    demo.queue()  # 스트리밍 지원을 위해 큐 활성화

    port = args.server_port
    print(f"[app] 브라우저에서 열기: http://127.0.0.1:{port}")

    # Gradio 서버 기동
    demo.launch(
        share=args.share,            # --share 시 공개 URL 생성
        server_name="127.0.0.1",    # 로컬 전용 (보안)
        server_port=port,           # 포트 8000
        show_error=True,            # 오류 메시지 표시
        theme=gr.themes.Soft(),     # 부드러운 테마 적용
    )
    return 0


if __name__ == "__main__":
    sys.exit(main())
