"""Multi-turn chat wrapper.

The base model is a plain causal LM with no built-in chat template. We bolt on
a simple turn-based prompt format:

    사용자: <user message>
    조수: <assistant message>
    사용자: <next user message>
    조수:

History is concatenated each turn. If the prompt would exceed the model's
context window, we drop the oldest turns until it fits.
"""

from __future__ import annotations

from dataclasses import dataclass, field
from pathlib import Path
from typing import Iterator

import torch

ROOT = Path(__file__).resolve().parents[2]

from src.inference.generate import generate_stream
from src.model.config import FullConfig, ModelConfig, load_config
from src.model.transformer import GPT
from src.tokenizer.tokenizer import Tokenizer, load_tokenizer


USER_TAG = "사용자: "
ASSISTANT_TAG = "조수: "


@dataclass
class ChatTurn:
    role: str  # "user" or "assistant"
    text: str


@dataclass
class ChatSession:
    model: GPT
    tokenizer: Tokenizer
    cfg: FullConfig
    device: torch.device
    history: list[ChatTurn] = field(default_factory=list)
    system_prefix: str = ""  # optional content prepended once per call

    def reset(self) -> None:
        self.history.clear()

    def _format_prompt(self, user_message: str) -> str:
        parts: list[str] = []
        if self.system_prefix:
            parts.append(self.system_prefix.rstrip() + "\n")
        for turn in self.history:
            tag = USER_TAG if turn.role == "user" else ASSISTANT_TAG
            parts.append(f"{tag}{turn.text}")
        parts.append(f"{USER_TAG}{user_message}")
        parts.append(ASSISTANT_TAG.rstrip())  # leave model to continue right after the tag
        return "\n".join(parts)

    def _truncate_history_to_fit(self, user_message: str, max_new_tokens: int) -> None:
        """Drop oldest history turns until the encoded prompt fits in the context window."""
        budget = self.cfg.model.block_size - max_new_tokens - 8  # safety margin
        while self.history:
            prompt = self._format_prompt(user_message)
            n = len(self.tokenizer.encode(prompt, add_bos=True))
            if n <= budget:
                return
            # Drop oldest turn pair (user + assistant)
            self.history.pop(0)
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
        ic = self.cfg.inference
        max_new = max_new_tokens or ic.default_max_new_tokens
        self._truncate_history_to_fit(user_message, max_new)
        prompt = self._format_prompt(user_message)

        produced_chunks: list[str] = []
        for delta in generate_stream(
            self.model, self.tokenizer, prompt,
            max_new_tokens=max_new,
            temperature=temperature if temperature is not None else ic.default_temperature,
            top_k=top_k if top_k is not None else ic.default_top_k,
            top_p=top_p if top_p is not None else ic.default_top_p,
            repetition_penalty=repetition_penalty if repetition_penalty is not None else ic.default_repetition_penalty,
            device=self.device,
        ):
            # Stop if the model started a new user turn on its own.
            running = "".join(produced_chunks) + delta
            if "\n사용자:" in running or running.startswith("사용자:"):
                cut = running.find("사용자:")
                trimmed = running[:cut].rstrip()
                # Yield only the delta portion that's still usable.
                already = "".join(produced_chunks)
                if len(trimmed) > len(already):
                    yield trimmed[len(already):]
                produced_chunks = [trimmed]
                break
            produced_chunks.append(delta)
            yield delta

        full_response = "".join(produced_chunks).strip()
        self.history.append(ChatTurn(role="user", text=user_message))
        self.history.append(ChatTurn(role="assistant", text=full_response))

    def chat(self, user_message: str, **kwargs) -> str:
        return "".join(self.chat_stream(user_message, **kwargs))


def load_session_from_checkpoint(
    checkpoint_path: str | Path,
    config_path: str | Path = "config/model_config.yaml",
    device: str | torch.device = "auto",
    system_prefix: str = "",
) -> ChatSession:
    cfg = load_config(config_path)

    if str(device) == "auto":
        device_t = torch.device("cuda" if torch.cuda.is_available() else "cpu")
    else:
        device_t = torch.device(device)

    ckpt = torch.load(str(checkpoint_path), map_location=device_t, weights_only=False)
    saved_model_cfg = ckpt.get("config", {}).get("model")
    if saved_model_cfg:
        cfg.model = ModelConfig(
            **{k: v for k, v in saved_model_cfg.items() if k in ModelConfig.__dataclass_fields__}
        )

    model = GPT(cfg.model).to(device_t)

    state = ckpt["model_state"]
    # Tolerate `_orig_mod.` prefix from torch.compile()
    if any(k.startswith("_orig_mod.") for k in state.keys()):
        state = {k.replace("_orig_mod.", "", 1): v for k, v in state.items()}
    model.load_state_dict(state)
    model.eval()

    tokenizer = load_tokenizer(ROOT / "checkpoints" / f"{cfg.tokenizer.output_prefix}.model")

    return ChatSession(
        model=model,
        tokenizer=tokenizer,
        cfg=cfg,
        device=device_t,
        system_prefix=system_prefix,
    )
