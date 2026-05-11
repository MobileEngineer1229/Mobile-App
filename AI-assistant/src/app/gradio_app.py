"""Gradio 6 chat UI for the DPRK Assistant.

Usage:
    python -m src.app.gradio_app
    python -m src.app.gradio_app --checkpoint checkpoints/ckpt_step005000_final.pt
    python -m src.app.gradio_app --share
"""

from __future__ import annotations

import argparse
import re
import sys
from pathlib import Path

import gradio as gr

ROOT = Path(__file__).resolve().parents[2]
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))

from src.inference.chat import ChatSession, ChatTurn, load_session_from_checkpoint  # noqa: E402


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
    best = list(out_dir.glob("ckpt_step*_best.pt"))
    if best:
        return max(best, key=lambda p: p.stat().st_mtime)
    final = list(out_dir.glob("ckpt_step*_final.pt"))
    if final:
        return max(final, key=lambda p: p.stat().st_mtime)
    pattern = re.compile(r"^ckpt_step(\d+)\.pt$")
    untagged = []
    for p in out_dir.glob("ckpt_step*.pt"):
        m = pattern.match(p.name)
        if m:
            untagged.append((int(m.group(1)), p))
    if not untagged:
        return None
    untagged.sort(key=lambda x: x[0])
    return untagged[-1][1]


def build_chat_tab(session: ChatSession, label: str, system_prefix: str, placeholder: str):
    # Gradio 6: history is list of {"role": "user"/"assistant", "content": str}
    history_state = gr.State([])

    with gr.Column():
        gr.Markdown(f"### {label}")
        chatbot = gr.Chatbot(height=480, show_label=False)
        msg = gr.Textbox(placeholder=placeholder, show_label=False, lines=2, autofocus=True)
        with gr.Row():
            send_btn = gr.Button("보내기", variant="primary")
            clear_btn = gr.Button("초기화")
        with gr.Accordion("샘플링 조정", open=False):
            temperature    = gr.Slider(0.1, 1.5, value=session.cfg.inference.default_temperature,        step=0.05, label="Temperature")
            top_k          = gr.Slider(1,   200, value=session.cfg.inference.default_top_k,              step=1,    label="Top-k")
            top_p          = gr.Slider(0.1, 1.0, value=session.cfg.inference.default_top_p,              step=0.01, label="Top-p")
            rep_penalty    = gr.Slider(1.0, 2.0, value=session.cfg.inference.default_repetition_penalty, step=0.05, label="Repetition penalty")
            max_new_tokens = gr.Slider(16, 1024, value=session.cfg.inference.default_max_new_tokens,     step=16,   label="Max new tokens")

    def respond(user_message, history, t, k, p, rp, mn):
        if not user_message.strip():
            yield history, history, ""
            return

        # Rebuild session history from Gradio 6 message dicts
        session.history = []
        for m in history:
            role = m.get("role", "")
            text = m.get("content", "") or ""
            if role in ("user", "assistant"):
                session.history.append(ChatTurn(role=role, text=text))
        # Only use system prefix once the model is properly trained (vocab ≥ 8k).
        session.system_prefix = system_prefix if session.tokenizer.vocab_size >= 8000 else ""

        # Append user turn and a blank assistant turn
        history = history + [
            {"role": "user",      "content": user_message},
            {"role": "assistant", "content": ""},
        ]

        partial = ""
        for delta in session.chat_stream(
            user_message,
            temperature=t, top_k=int(k), top_p=p,
            repetition_penalty=rp, max_new_tokens=int(mn),
        ):
            partial += delta
            history[-1]["content"] = partial
            yield history, history, ""

    def clear():
        return [], []

    send_btn.click(
        respond,
        inputs=[msg, history_state, temperature, top_k, top_p, rep_penalty, max_new_tokens],
        outputs=[chatbot, history_state, msg],
    )
    msg.submit(
        respond,
        inputs=[msg, history_state, temperature, top_k, top_p, rep_penalty, max_new_tokens],
        outputs=[chatbot, history_state, msg],
    )
    clear_btn.click(clear, outputs=[chatbot, history_state])


def build_app(session: ChatSession) -> gr.Blocks:
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
    parser = argparse.ArgumentParser(description="Gradio chat UI for DPRK Assistant.")
    parser.add_argument("--checkpoint",   type=str, default=None)
    parser.add_argument("--config",       type=str, default="config/model_config.yaml")
    parser.add_argument("--device",       type=str, default="auto")
    parser.add_argument("--share",        action="store_true")
    parser.add_argument("--server-port",  type=int, default=8000)
    args = parser.parse_args()

    out_dir = ROOT / "checkpoints"
    if args.checkpoint:
        ckpt_path = Path(args.checkpoint)
    else:
        ckpt_path = find_default_checkpoint(out_dir)
        if ckpt_path is None:
            print(f"No checkpoint found in {out_dir}. Run scripts/03_train_model.ps1 first.",
                  file=sys.stderr)
            return 1
        print(f"[app] using checkpoint: {ckpt_path}")

    session = load_session_from_checkpoint(
        checkpoint_path=ckpt_path,
        config_path=ROOT / args.config,
        device=args.device,
    )
    print(f"[app] session ready on {session.device}")

    demo = build_app(session)
    demo.queue()

    port = args.server_port
    print(f"[app] open  http://127.0.0.1:{port}  in your browser")
    demo.launch(
        share=args.share,
        server_name="127.0.0.1",
        server_port=port,
        show_error=True,
        theme=gr.themes.Soft(),
    )
    return 0


if __name__ == "__main__":
    sys.exit(main())
