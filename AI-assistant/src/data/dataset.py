"""Memory map-based word tag data set — For training.

【Beginner's Guide】
  This file is responsible for quickly reading training data and feeding it to the model..

  memory map(memmap)iran:
    Without loading the entire file into memory, A method of reading only the necessary parts at a time.
    train.bin The number of files GB You can still access it without worrying about memory..

  (x, y) What is a pair?:
    x = current letters [t, t+1, ..., t+block_size-1]
    y = the next letters [t+1, t+2, ..., t+block_size]
    The model is xlooking at yis trained to predict.

    example:
      full data: [BOS, 342, 156, 891, 782, 201, EOS, ...]
      t=1 in block_size=4 Sampling by:
        x = [342, 156, 891, 782]  ← "How to make kimchi"
        y = [156, 891, 782, 201]  ← "How to soak what"  (move one space)
      model target: x[0]→y[0], x[1]→y[1], ... That is, predict the next letter

  nanoGPT Reasons for using patterns:
    - Pick a random location every step → No traditional epoch concept
    - DataLoader directly without numpy Sample from Array → faster
    - multiple workers(worker) Each process memmap No need to have a copy
"""

from __future__ import annotations

from pathlib import Path
from typing import Iterator

import numpy as np
import torch


class TokenDataset:
    """A dataset class that samples a set of word tags from a memory-mapped file..

    【Beginner's Guide】
      When creating this class train.bin or val.bin Link files.
      sample() At a random location each time you call the method block_size of length
      extract a series of letters (x, y) returns in pairs.
    """

    def __init__(self, bin_path: str | Path, block_size: int):
        """Dataset initialization.

        argument:
            bin_path:   Tag binary file path (train.bin or val.bin)
            block_size: Maximum number of characters to process at once (Context length of the model)
        """
        self.bin_path = Path(bin_path)
        if not self.bin_path.exists():
            raise FileNotFoundError(
                f"The tag file cannot be found: {self.bin_path}\n"
                f"scripts/02_preprocess_data.ps1 Run first."
            )
        self.block_size = block_size

        # uint16 Open memory map with format (preprocess.pymatches format with)
        # uint16: 0~65535 integer in range → vocabulary size 16,384enough for a dog
        # mode="r": read only (Do not modify files during training)
        self.data = np.memmap(self.bin_path, dtype=np.uint16, mode="r")

        # If there is too little data (x, y) Can't create a single pair
        # minimum block_size + 2 I need two stamps
        if len(self.data) <= block_size + 1:
            raise ValueError(
                f"There is a word mark in the data set. {len(self.data)}There are only dogs. "
                f"block_size={block_size}When the minimum {block_size + 2}I need a dog.\n"
                f"Add more training materials."
            )

    def __len__(self) -> int:
        """Returns the number of valid sample starting positions.

        From the total number of tickets block_size+1 minus is the number of sample starting positions..
        (in last position block_size If you read that much, you will reach the end of the file.)
        """
        return max(0, len(self.data) - self.block_size - 1)

    def sample(self, batch_size: int, device: torch.device | str = "cpu") -> tuple[torch.Tensor, torch.Tensor]:
        """in a random location batch_size sample a series of.

        argument:
            batch_size: Number of series to include in one bundle
            device:     device to raise the tensor ("cpu" or "cuda")

        return value:
            (x, y) pair:
                x: input wordmark, form (batch_size, block_size)
                y: target tag, form (batch_size, block_size)
                   y[i][j] = x[i][j+1] (next letter)
        """
        # random starting position batch_size dog selection
        # at each location block_size+1 Because you need to be able to read dogs.
        # effective range: [0, len-block_size-1)
        ix = np.random.randint(0, len(self.data) - self.block_size - 1, size=batch_size)

        # x: at each location block_size dog reading (current letters)
        # y: Moved one space block_size dog reading (the next letters)
        # astype(np.int64): PyTorchgo int64(Long) requires a tensor
        x = np.stack([self.data[i : i + self.block_size].astype(np.int64) for i in ix])
        y = np.stack([self.data[i + 1 : i + 1 + self.block_size].astype(np.int64) for i in ix])

        # numpy array PyTorch convert to tensor
        x_t = torch.from_numpy(x)
        y_t = torch.from_numpy(y)

        # GPUtransfer to (CUDA If the device)
        if str(device).startswith("cuda"):
            # pin_memory(): CPU Put it in fixed memory GPU Increased transfer speed
            # non_blocking=True: GPU Transmit and perform other tasks simultaneously
            x_t = x_t.pin_memory().to(device, non_blocking=True)
            y_t = y_t.pin_memory().to(device, non_blocking=True)
        else:
            x_t = x_t.to(device)
            y_t = y_t.to(device)

        return x_t, y_t


def get_batch_iterator(
    dataset: TokenDataset, batch_size: int, device: torch.device | str
) -> Iterator[tuple[torch.Tensor, torch.Tensor]]:
    """Infinitely Repeated Bundle Generator.

    【Beginner's Guide】
      In the training loop: `next(iterator)` Every time you call
      Provides new random bundles.
      `while True` Because it repeats infinitely, there is no stopping due to insufficient data..

    argument:
        dataset:    TokenDataset instance
        batch_size: bundle size
        device:     device to raise the tensor

    generated value:
        (x, y) bundle — continue indefinitely
    """
    while True:
        yield dataset.sample(batch_size, device=device)  # Pull out the bundle, return it, and repeat again.
