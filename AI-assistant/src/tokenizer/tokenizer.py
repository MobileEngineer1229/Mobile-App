"""SentencePiece tokenizer wrapper.

We use SentencePiece BPE because:
- Korean is agglutinative; BPE handles morphological variation without explicit segmentation.
- SentencePiece operates on raw text (no pretokenization required).
- It produces a self-contained `.model` file that loads in milliseconds.
"""

from __future__ import annotations

from pathlib import Path

import sentencepiece as spm


# Special token ids — these MUST match the order passed to SentencePieceTrainer.
PAD_ID = 0
BOS_ID = 1
EOS_ID = 2
UNK_ID = 3


class Tokenizer:
    def __init__(self, model_path: str | Path):
        self.sp = spm.SentencePieceProcessor()
        self.sp.Load(str(model_path))
        self.model_path = str(model_path)

    @property
    def vocab_size(self) -> int:
        return self.sp.GetPieceSize()

    def encode(self, text: str, add_bos: bool = False, add_eos: bool = False) -> list[int]:
        ids = self.sp.EncodeAsIds(text)
        if add_bos:
            ids = [BOS_ID] + ids
        if add_eos:
            ids = ids + [EOS_ID]
        return ids

    def decode(self, ids: list[int]) -> str:
        # Strip all special tokens including UNK from output.
        clean = [i for i in ids if i not in (PAD_ID, BOS_ID, EOS_ID, UNK_ID)]
        text = self.sp.DecodeIds(clean)
        # Remove the UNK surface string in case SentencePiece emits it for OOV pieces.
        return text.replace("⁇", "").replace(" ⁇ ", " ").replace("⁇", "").strip()

    def __repr__(self) -> str:
        return f"Tokenizer(vocab_size={self.vocab_size}, model={self.model_path})"


def load_tokenizer(model_path: str | Path) -> Tokenizer:
    p = Path(model_path)
    if not p.exists():
        raise FileNotFoundError(
            f"Tokenizer model not found at {p}. "
            f"Run scripts/01_train_tokenizer.ps1 first."
        )
    return Tokenizer(p)
