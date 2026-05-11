"""Sampling-based text generation.

Implements:
- Temperature
- Top-k filtering
- Top-p (nucleus) filtering
- Repetition penalty (CTRL-style)
- Streaming via token iterator
"""

from __future__ import annotations

from typing import Iterator

import torch
import torch.nn.functional as F

from src.tokenizer.tokenizer import BOS_ID, EOS_ID, Tokenizer


def _apply_repetition_penalty(logits: torch.Tensor, generated_ids: torch.Tensor, penalty: float) -> torch.Tensor:
    """CTRL-style: divide already-generated tokens' logits by `penalty` if positive,
    multiply if negative. Penalty=1.0 is a no-op."""
    if penalty == 1.0 or generated_ids.numel() == 0:
        return logits
    score = logits.gather(-1, generated_ids)
    score = torch.where(score < 0, score * penalty, score / penalty)
    logits.scatter_(-1, generated_ids, score)
    return logits


def _top_k_top_p(logits: torch.Tensor, top_k: int, top_p: float) -> torch.Tensor:
    """Filter logits to top-k then top-p (nucleus). Returns logits with filtered entries set to -inf."""
    if top_k > 0:
        top_k = min(top_k, logits.size(-1))
        # Keep top-k by setting all others to -inf
        kth = torch.topk(logits, top_k).values[..., -1, None]
        logits = torch.where(logits < kth, torch.full_like(logits, float("-inf")), logits)

    if 0.0 < top_p < 1.0:
        sorted_logits, sorted_idx = torch.sort(logits, descending=True)
        probs = F.softmax(sorted_logits, dim=-1)
        cum = probs.cumsum(dim=-1)
        # Mask tokens beyond the top-p cumulative threshold
        remove = cum > top_p
        # Shift right so the first token above the threshold is still kept
        remove[..., 1:] = remove[..., :-1].clone()
        remove[..., 0] = False
        sorted_logits = sorted_logits.masked_fill(remove, float("-inf"))
        # Scatter back to original positions
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
    """Sample one next token given context `idx`. Returns shape (B, 1)."""
    block_size = model.cfg.block_size if hasattr(model, "cfg") else model._orig_mod.cfg.block_size
    idx_cond = idx if idx.size(1) <= block_size else idx[:, -block_size:]
    logits, _ = model(idx_cond)
    logits = logits[:, -1, :]  # (B, vocab)

    logits = _apply_repetition_penalty(logits, idx, repetition_penalty)

    if temperature <= 0.0:
        # Greedy
        return logits.argmax(dim=-1, keepdim=True)

    logits = logits / temperature
    logits = _top_k_top_p(logits, top_k=top_k, top_p=top_p)
    probs = F.softmax(logits, dim=-1)
    next_id = torch.multinomial(probs, num_samples=1)
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
    """Yield decoded text incrementally as tokens are produced.

    Yields the *delta* string each step (not the cumulative text), suitable for
    streaming UIs.
    """
    model.eval()
    ids = tokenizer.encode(prompt, add_bos=True)
    idx = torch.tensor([ids], dtype=torch.long, device=device)

    produced: list[int] = []
    last_text = ""

    for _ in range(max_new_tokens):
        nxt = sample_token(
            model, idx,
            temperature=temperature, top_k=top_k, top_p=top_p,
            repetition_penalty=repetition_penalty,
        )
        token_id = int(nxt.item())
        if stop_on_eos and token_id == EOS_ID:
            break
        produced.append(token_id)
        idx = torch.cat([idx, nxt], dim=1)

        # Decode incrementally; SentencePiece BPE may merge across steps so
        # we re-decode the full produced list and yield only the delta.
        text = tokenizer.decode(produced)
        if len(text) > len(last_text):
            yield text[len(last_text):]
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
    """Non-streaming generate. Returns prompt + completion."""
    chunks = list(
        generate_stream(
            model, tokenizer, prompt,
            max_new_tokens=max_new_tokens,
            temperature=temperature, top_k=top_k, top_p=top_p,
            repetition_penalty=repetition_penalty,
            device=device, stop_on_eos=stop_on_eos,
        )
    )
    return prompt + "".join(chunks)
