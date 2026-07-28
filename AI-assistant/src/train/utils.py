"""Training helper functions — Learning rate planning, Save checkpoint/load.

【Beginner's Guide】
  This file is train.pyIt is a collection of tool functions that are used repeatedly in.

  Main features:
    cosine_lr()            : Adjust learning rate according to steps
    save_checkpoint()      : Save training status as file
    cleanup_old_checkpoints(): Clean up old checkpoint files
    load_checkpoint()      : Resume training from a saved checkpoint
"""

from __future__ import annotations

import math
import re
from pathlib import Path

import torch


def cosine_lr(step: int, *, warmup: int, max_steps: int, peak_lr: float, min_lr: float) -> float:
    """Cosine damped learning rate scheme (Includes linear warm-up).

    【Beginner's Guide】
      What is learning rate? "How much to adjust the weight at a time" is the size of.
      If it's too big, it diverges.(training failure), Too small and you'll be slow or stuck in a false minimum..

      This function adjusts the learning rate in three intervals.:

      Section 1 (warm up, 0 ~ warmup step):
        learning rate from 0 peak_lrLinearly up to.
        Prevent unstable training in the beginning.

        yes: warmup=100, peak_lr=0.0003
          step 0: learning rate = 0.000003  (1/100)
          Step 50: learning rate = 0.00015  (50/100)
          step 100: learning rate = 0.0003  (peak)

      Section 2 (cosine attenuation, warmup ~ max_steps):
        Following the cosine function peak_lrin min_lrgently decreases until.
        It decreases more slowly at the end than in a straight line, making it more stable..

      Section 3 (max_steps After):
        min_lrfixed to.

    argument:
        step:      Current training phase number (0starting from)
        warmup:    Number of warm-up steps
        max_steps: Total number of training steps
        peak_lr:   highest learning rate
        min_lr:    lowest learning rate

    return value:
        Learning rate of current step
    """
    if step < warmup:
        # Warm-up section: 0in peak_lrlinear increase until
        # +1 Reason for adding: step=0When , start from the minimum value other than 0.
        return peak_lr * (step + 1) / max(1, warmup)

    if step >= max_steps:
        # After training: min_lr fixed
        return min_lr

    # Cosine decay section
    # progress: Progress after warm-up (0.0 ~ 1.0)
    progress = (step - warmup) / max(1, max_steps - warmup)
    # cosine function: 0in πuntil → 1.0in -1.0
    # 0.5 × (1 + cos(π × progress)): 1.0from 0.0gently decreases to
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
    """Save training state as checkpoint file.

    【Beginner's Guide】
      A checkpoint is a way to store the state during training..
      Even if the power turns off or an error occurs, you can start again from here..

      Save contents:
        - step: Current training phase number
        - model_state: All weights in the model
        - optimizer_state: AdamWmoving average of(moment) values
        - config: Model structure settings (To restore the correct structure upon reopening.)

      file name format:
        tag None: ckpt_step002500.pt
        tag Yes: ckpt_step002500_best.pt

    argument:
        out_dir:   Folder to save
        step:      Current step number
        model:     GPT model instance
        optimizer: AdamW optimizer instance
        cfg_dict:  settings dictionary (FullConfig.__dict__ form)
        tag:       Tags to add to the end of the file name ("best", "final" etc.)

    return value:
        Path to saved file
    """
    out_dir.mkdir(parents=True, exist_ok=True)

    # Generate file name: ckpt_step002500.pt or ckpt_step002500_best.pt
    name = f"ckpt_step{step:06d}{('_' + tag) if tag else ''}.pt"
    path = out_dir / name

    # Save checkpoint (torch.save: Python Serialize and store objects)
    torch.save(
        {
            "step": step,                              # current stage
            "model_state": model.state_dict(),         # all weight values
            "optimizer_state": optimizer.state_dict(), # AdamW internal state
            "config": cfg_dict,                        # Model structure settings
        },
        path,
    )
    return path


def cleanup_old_checkpoints(out_dir: Path, keep_last: int) -> None:
    """Free up disk space by deleting old checkpoint files.

    【Beginner's Guide】
      As training progresses, checkpoint files accumulate..
      Each file has hundreds MB So if you keep them all, the disk will be full..

      Deletion Rules:
        - File without tags: most recent keep_last Delete leaving only dogs
          yes: keep_last=3 → Keep only the most recent 3
        - file with tags (_best, _final): always keep (Do not delete)

    argument:
        out_dir:    checkpoint folder
        keep_last:  Number of recent files to keep
    """
    # file name pattern: ckpt_stepnumbers[_tag].pt
    pattern = re.compile(r"^ckpt_step(\d+)(?:_([a-zA-Z0-9]+))?\.pt$")
    untagged: list[tuple[int, Path]] = []  # (step number, file path) list

    for p in out_dir.glob("ckpt_step*.pt"):
        m = pattern.match(p.name)
        if not m:
            continue                        # Skip files that do not match the pattern
        if m.group(2):                      # files with tags (_best etc.)
            continue                        # Always keep tagged files
        untagged.append((int(m.group(1)), p))  # Save step number and route

    # Sort ascending by step number (small number = old files)
    untagged.sort(key=lambda x: x[0])

    # recently keep_last Delete everything except the dog
    for _, p in untagged[:-keep_last] if keep_last > 0 else untagged:
        try:
            p.unlink()  # Delete File
        except OSError:
            pass        # Even if deletion fails, training continues


def load_checkpoint(path: str | Path, model: torch.nn.Module, optimizer=None, map_location="cpu"):
    """Restore model and optimizer states from checkpoint files.

    【Beginner's Guide】
      Using this function, you can continue training in a previously saved training state..
      Alternatively, you can load a trained model and use it for inference..

    argument:
        path:         Checkpoint file path (.pt file)
        model:        to restore GPT model instance (Structure must match)
        optimizer:    Optimizer instance to restore (NoneIf so, do not restore)
        map_location: device for lifting weights ("cpu", "cuda" etc.)

    return value:
        Full Checkpoint Dictionary (step, model_state, optimizer_state, config)
    """
    # weights_only=False: config Required to load the dictionary as well
    ckpt = torch.load(str(path), map_location=map_location, weights_only=False)

    # Restoring model weights
    model.load_state_dict(ckpt["model_state"])

    # Restore optimizer state (AdamWmoving average values of)
    # optimizergo NoneIf so, do not restore (When loaded for inference only)
    if optimizer is not None and "optimizer_state" in ckpt:
        optimizer.load_state_dict(ckpt["optimizer_state"])

    return ckpt  # the caller step Return the entire information so that other information can be retrieved and used.
