"""Korean tide Gradio 6 chat UI.

【Beginner's Guide】
  This file creates a web interface that interacts directly with the user..
  python -m src.app.gradio_app If you run http://127.0.0.1:8000 opens in.

  UI composition:
    tab 1 - free conversation:  normal conversation (food, exercise, guitar)
    tab 2 - Ryori Recommendation:  Guide to Joseon-style cooking methods
    tab 3 - exercise plan:  Create a workout plan

  In each tab:
    - conversation screen (chatbot)
    - Message input window
    - send / Reset button
    - Sampling adjustment slider (temperature, Top-K, Top-P etc.)

  Windows port caution:
    port 7787~7886silver Hyper-V/WSLThis port is reserved and cannot be used..
    This program uses the default value of 8000..

  How to run:
    python -m src.app.gradio_app
    python -m src.app.gradio_app --checkpoint checkpoints/ckpt_step005000_final.pt
    python -m src.app.gradio_app --share  (Create an Internet public link)
"""

from __future__ import annotations

import argparse
import re
import sys
from pathlib import Path

import gradio as gr

# Add project root to python path
ROOT = Path(__file__).resolve().parents[2]
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))

from src.inference.chat import ChatSession, ChatTurn, load_session_from_checkpoint  # noqa: E402


# Scheme prefix for each tab (role directive)
# Applies only when vocabulary size is 8000 or greater (Small scale model cannot follow instructions)
SYSTEM_PREFIXES = {
    "free": "You are my Korean assistant who guides you through food and exercise.. Please respond kindly.",
    "recipe": (
        "You are a guide to Joseon-style cooking methods.. "
        "Material according to user's request, Please guide me step by step on how to cook.."
    ),
    "workout": (
        "You are the workout plan guide assistant. "
        "Create a safe and effective exercise plan tailored to your goals and level.."
    ),
}


def find_default_checkpoint(out_dir: Path) -> Path | None:
    """Automatically finds the most appropriate checkpoint in the checkpoint folder.

    priority:
      1. _best tag file (What is stored when verification loss is lowest)
      2. _final tag file (What is saved when training is completed)
      3. Untagged file with highest level number

    argument:
        out_dir: Checkpoint folder path

    return value:
        Most suitable checkpoint file path, If there is no None
    """
    # 1ranking: _best tag file
    best = list(out_dir.glob("ckpt_step*_best.pt"))
    if best:
        return max(best, key=lambda p: p.stat().st_mtime)  # most recent file

    # 2ranking: _final tag file
    final = list(out_dir.glob("ckpt_step*_final.pt"))
    if final:
        return max(final, key=lambda p: p.stat().st_mtime)

    # 3ranking: Untagged file with highest level number
    pattern = re.compile(r"^ckpt_step(\d+)\.pt$")
    untagged = []
    for p in out_dir.glob("ckpt_step*.pt"):
        m = pattern.match(p.name)
        if m:
            untagged.append((int(m.group(1)), p))  # (step number, path)
    if not untagged:
        return None
    untagged.sort(key=lambda x: x[0])  # Sort by step number ascending
    return untagged[-1][1]              # Returns the file with the largest step number


def build_chat_tab(session: ChatSession, label: str, system_prefix: str, placeholder: str):
    """conversation tab UIConfigure and set up event connections.

    【Beginner's Guide】
      Gradio 6of UI place the elements, Connect a function that responds to user input.
      respond() The function takes user input and streams a model response..

    argument:
        session:       ChatSession instance (model, Includes tag analyzer)
        label:         tab title string
        system_prefix: Role directives on this tab
        placeholder:   Guide text in input window
    """
    # Gradio 6: Manage conversation history as a state
    # format: [{"role": "user", "content": "..."}, {"role": "assistant", "content": "..."}]
    history_state = gr.State([])

    with gr.Column():
        gr.Markdown(f"### {label}")

        # conversation screen: A conversation between you and your assistant is displayed
        chatbot = gr.Chatbot(height=480, show_label=False)

        # Message input window
        msg = gr.Textbox(placeholder=placeholder, show_label=False, lines=2, autofocus=True)

        with gr.Row():
            send_btn  = gr.Button("send", variant="primary")  # send message
            clear_btn = gr.Button("reset")                      # Delete conversation history

        # Sampling Adjustment Accordion (Closed by default)
        with gr.Accordion("Sampling Adjustment", open=False):
            temperature    = gr.Slider(0.1, 1.5,
                value=session.cfg.inference.default_temperature,        step=0.05,
                label="Temperature (temperature: The lower the more consistent, The higher the more creative)")
            top_k          = gr.Slider(1,   200,
                value=session.cfg.inference.default_top_k,              step=1,
                label="Top-k (top KOnly dog candidates allowed)")
            top_p          = gr.Slider(0.1, 1.0,
                value=session.cfg.inference.default_top_p,              step=0.01,
                label="Top-p (cumulative probability POnly candidates up to)")
            rep_penalty    = gr.Slider(1.0, 2.0,
                value=session.cfg.inference.default_repetition_penalty, step=0.05,
                label="Repetition penalty (repetition penalty: The higher the number, the more repetitions are suppressed.)")
            max_new_tokens = gr.Slider(16, 1024,
                value=session.cfg.inference.default_max_new_tokens,     step=16,
                label="Max new tokens (Maximum number of generated notes)")

    def respond(user_message, history, t, k, p, rp, mn):
        """A callback function that receives user input and generates a streaming model response..

        argument:
            user_message: Post entered by user
            history:      Gradio List of conversation history in format
            t:            temperature
            k:            Top-K
            p:            Top-P
            rp:           repetition penalty
            mn:           Maximum number of generated notes

        generated value:
            (chatbot, history_state, msg) tuple — At every streaming stage
        """
        if not user_message.strip():
            yield history, history, ""  # Empty input is ignored
            return

        # Gradio 6 message dictionary format ChatTurn convert to list
        # Gradio: [{"role": "user", "content": "..."}]
        # ChatSession: [ChatTurn(role="user", text="...")]
        session.history = []
        for m in history:
            role = m.get("role", "")
            text = m.get("content", "") or ""
            if role in ("user", "assistant"):
                session.history.append(ChatTurn(role=role, text=text))

        # Apply scheme prefixes only when vocabulary size is sufficient
        # Models trained on small corpora lack the ability to follow instructions
        session.system_prefix = system_prefix if session.tokenizer.vocab_size >= 8000 else ""

        # Gradio Add current turn to conversation history (The assistant content is left blank.)
        history = history + [
            {"role": "user",      "content": user_message},
            {"role": "assistant", "content": ""},
        ]

        # Generate streaming response
        partial = ""
        for delta in session.chat_stream(
            user_message,
            temperature=t, top_k=int(k), top_p=p,
            repetition_penalty=rp, max_new_tokens=int(mn),
        ):
            partial += delta                        # stack new letters
            history[-1]["content"] = partial        # Tide speech bubble real-time update
            yield history, history, ""             # UIreflected in

    def clear():
        """Delete all conversation history."""
        return [], []  # chatbot, history_state All to an empty list

    # event connection: Send button or Enter key → respond run
    send_btn.click(
        respond,
        inputs=[msg, history_state, temperature, top_k, top_p, rep_penalty, max_new_tokens],
        outputs=[chatbot, history_state, msg],
    )
    msg.submit(  # Enter Can also be sent by key
        respond,
        inputs=[msg, history_state, temperature, top_k, top_p, rep_penalty, max_new_tokens],
        outputs=[chatbot, history_state, msg],
    )
    # Reset button → clear run
    clear_btn.click(clear, outputs=[chatbot, history_state])


def build_app(session: ChatSession) -> gr.Blocks:
    """3Consisting of tabs Gradio Blocks create an app.

    Tab Configuration:
      free conversation: general purpose conversation
      Ryori Recommendation: Specialized in providing guidance on Joseon-style cooking methods
      exercise plan: Specialized in exercise plan creation

    argument:
        session: ChatSession instance

    return value:
        gr.Blocks app instance
    """
    with gr.Blocks(title="DPRK assistant (Food & Workout)") as demo:
        gr.Markdown(
            "# Korean tide — food · exercise\n"
            "From-scratch GPT trained on DPRK Korean food/workout corpus."
        )
        with gr.Tabs():
            with gr.Tab("free conversation"):
                build_chat_tab(session, "free conversation",  SYSTEM_PREFIXES["free"],
                               "what would you like to ask?")
            with gr.Tab("Ryori Recommendation"):
                build_chat_tab(session, "Ryori Recommendation",  SYSTEM_PREFIXES["recipe"],
                               "yes: Please tell me how to make soybean paste soup for lunch.")
            with gr.Tab("exercise plan"):
                build_chat_tab(session, "exercise plan",  SYSTEM_PREFIXES["workout"],
                               "yes: 30Please make a chest exercise plan.")
    return demo


def main() -> int:
    """Program starting point — handle the arguments Gradio Start the server.

    Processing order:
      1. Command line argument processing
      2. Auto-discovery of checkpoint files (or use specified file)
      3. ChatSession load (model + ticket analyzer)
      4. Gradio App configuration
      5. Server startup (port 8000)

    return value:
        0: success
        1: Checkpoint not found
    """
    # Command line argument processing
    parser = argparse.ArgumentParser(description="Korean assistant chat UI.")
    parser.add_argument("--checkpoint",   type=str, default=None,
                        help="Checkpoint file path to use (Auto-discovery if not specified)")
    parser.add_argument("--config",       type=str, default="config/model_config.yaml",
                        help="Configuration file path")
    parser.add_argument("--device",       type=str, default="auto",
                        help="execution device: auto, cpu, cuda")
    parser.add_argument("--share",        action="store_true",
                        help="Create an Internet public link (Gradio Use of shared server)")
    parser.add_argument("--server-port",  type=int, default=8000,
                        help="server port (default: 8000; Windows 7787~7886 Not available)")
    args = parser.parse_args()

    out_dir = ROOT / "checkpoints"

    # Determine checkpoint path
    if args.checkpoint:
        ckpt_path = Path(args.checkpoint)  # Using files specified by command line
    else:
        # Automatic selection of the most suitable files from checkpoint folders
        ckpt_path = find_default_checkpoint(out_dir)
        if ckpt_path is None:
            print(
                f"Checkpoint not found: {out_dir}\n"
                f"scripts/03_train_model.ps1 Run first.",
                file=sys.stderr,
            )
            return 1
        print(f"[app] Use checkpoints: {ckpt_path}")

    # Load model and ChatSession create
    session = load_session_from_checkpoint(
        checkpoint_path=ckpt_path,
        config_path=ROOT / args.config,
        device=args.device,
    )
    print(f"[app] Session ready, device: {session.device}")

    # Gradio App configuration
    demo = build_app(session)
    demo.queue()  # Enable queues for streaming support

    port = args.server_port
    print(f"[app] Open in browser: http://127.0.0.1:{port}")

    # Gradio Server startup
    demo.launch(
        share=args.share,            # --share poetry public URL create
        server_name="127.0.0.1",    # Local only (security)
        server_port=port,           # port 8000
        show_error=True,            # Display error message
        theme=gr.themes.Soft(),     # Apply soft theme
    )
    return 0


if __name__ == "__main__":
    sys.exit(main())
