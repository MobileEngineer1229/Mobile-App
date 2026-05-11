"""Training utilities: LR schedule, checkpointing."""

from __future__ import annotations

import math
import re
from pathlib import Path

import torch


def cosine_lr(step: int, *, warmup: int, max_steps: int, peak_lr: float, min_lr: float) -> float:
    """Linear warmup -> cosine decay to min_lr."""
    if step < warmup:
        return peak_lr * (step + 1) / max(1, warmup)
    if step >= max_steps:
        return min_lr
    progress = (step - warmup) / max(1, max_steps - warmup)
    coeff = 0.5 * (1.0 + math.cos(math.pi * progress))
    return min_lr + coeff * (peak_lr - min_lr)


def save_checkpoint(
    out_dir: Path,
    step: int,
    model: torch.nn.Module,
    optimizer: torch.optim.Optimizer,
    cfg_dict: dict,
    tag: str = "",
) -> Path:
    out_dir.mkdir(parents=True, exist_ok=True)
    name = f"ckpt_step{step:06d}{('_' + tag) if tag else ''}.pt"
    path = out_dir / name
    torch.save(
        {
            "step": step,
            "model_state": model.state_dict(),
            "optimizer_state": optimizer.state_dict(),
            "config": cfg_dict,
        },
        path,
    )
    return path


def cleanup_old_checkpoints(out_dir: Path, keep_last: int) -> None:
    """Remove old `ckpt_step*.pt` files, keeping the newest `keep_last` and any tagged ones."""
    pattern = re.compile(r"^ckpt_step(\d+)(?:_([a-zA-Z0-9]+))?\.pt$")
    untagged: list[tuple[int, Path]] = []
    for p in out_dir.glob("ckpt_step*.pt"):
        m = pattern.match(p.name)
        if not m:
            continue
        if m.group(2):  # tagged checkpoints (e.g. _best) are preserved
            continue
        untagged.append((int(m.group(1)), p))
    untagged.sort(key=lambda x: x[0])
    for _, p in untagged[:-keep_last] if keep_last > 0 else untagged:
        try:
            p.unlink()
        except OSError:
            pass


def load_checkpoint(path: str | Path, model: torch.nn.Module, optimizer=None, map_location="cpu"):
    ckpt = torch.load(str(path), map_location=map_location, weights_only=False)
    model.load_state_dict(ckpt["model_state"])
    if optimizer is not None and "optimizer_state" in ckpt:
        optimizer.load_state_dict(ckpt["optimizer_state"])
    return ckpt
