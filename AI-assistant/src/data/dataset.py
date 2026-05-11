"""Memmap-backed token dataset for training.

We load `train.bin` / `val.bin` once via numpy.memmap and sample random offsets
into them per step. This is the nanoGPT pattern — much faster than building a
PyTorch DataLoader for next-token prediction, since we don't need shuffling
across epochs (every batch is a fresh random sample) and we avoid worker
processes that would hold their own memmap copies.
"""

from __future__ import annotations

from pathlib import Path
from typing import Iterator

import numpy as np
import torch


class TokenDataset:
    def __init__(self, bin_path: str | Path, block_size: int):
        self.bin_path = Path(bin_path)
        if not self.bin_path.exists():
            raise FileNotFoundError(
                f"Token shard not found: {self.bin_path}. "
                f"Run scripts/02_preprocess_data.ps1 first."
            )
        self.block_size = block_size
        # Read uint16 (matches preprocess.py)
        self.data = np.memmap(self.bin_path, dtype=np.uint16, mode="r")
        if len(self.data) <= block_size + 1:
            raise ValueError(
                f"Dataset has {len(self.data)} tokens but block_size is {block_size}. "
                f"Need at least block_size + 2 tokens. Add more training data."
            )

    def __len__(self) -> int:
        return max(0, len(self.data) - self.block_size - 1)

    def sample(self, batch_size: int, device: torch.device | str = "cpu") -> tuple[torch.Tensor, torch.Tensor]:
        """Sample `batch_size` random sequences of length block_size for next-token prediction."""
        ix = np.random.randint(0, len(self.data) - self.block_size - 1, size=batch_size)
        # x: tokens [t : t+block_size], y: tokens [t+1 : t+block_size+1]
        x = np.stack([self.data[i : i + self.block_size].astype(np.int64) for i in ix])
        y = np.stack([self.data[i + 1 : i + 1 + self.block_size].astype(np.int64) for i in ix])
        x_t = torch.from_numpy(x)
        y_t = torch.from_numpy(y)
        if str(device).startswith("cuda"):
            x_t = x_t.pin_memory().to(device, non_blocking=True)
            y_t = y_t.pin_memory().to(device, non_blocking=True)
        else:
            x_t = x_t.to(device)
            y_t = y_t.to(device)
        return x_t, y_t


def get_batch_iterator(
    dataset: TokenDataset, batch_size: int, device: torch.device | str
) -> Iterator[tuple[torch.Tensor, torch.Tensor]]:
    """Infinite iterator yielding (x, y) batches."""
    while True:
        yield dataset.sample(batch_size, device=device)
