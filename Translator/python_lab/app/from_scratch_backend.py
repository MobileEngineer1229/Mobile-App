"""Inference backend for the project-owned Transformer translator.

The model loaded here is expected to be trained by
`scripts/train_transformer_from_scratch.py`. It does not use pretrained
weights, pretrained tokenizers, or Hugging Face model classes.
"""

from __future__ import annotations

import json
import math
from dataclasses import dataclass
from pathlib import Path


PAD_ID = 0
UNK_ID = 1
BOS_ID = 2
EOS_ID = 3


@dataclass(frozen=True)
class TranslationConfig:
    """Architecture values saved beside a from-scratch checkpoint."""

    vocab_size: int
    d_model: int = 256
    nhead: int = 4
    num_encoder_layers: int = 4
    num_decoder_layers: int = 4
    dim_feedforward: int = 1024
    dropout: float = 0.1
    max_length: int = 256


def translate_from_scratch(
    text: str,
    source_language: str,
    target_language: str,
    *,
    model_dir: Path,
    tokenizer_path: Path,
    device: str = "cpu",
    max_new_tokens: int = 128,
) -> str | None:
    """Translate one sentence with a locally trained from-scratch model."""
    try:
        import sentencepiece as spm
        import torch
    except Exception:
        return None

    checkpoint_path = model_dir / "best.pt"
    config_path = model_dir / "config.json"
    if not checkpoint_path.exists() or not config_path.exists() or not tokenizer_path.exists():
        return None

    sp = spm.SentencePieceProcessor(model_file=str(tokenizer_path))
    saved_config = json.loads(config_path.read_text(encoding="utf-8"))
    config = TranslationConfig(**saved_config["model"])
    model = FromScratchTransformer(config)
    checkpoint = torch.load(checkpoint_path, map_location=device)
    model.load_state_dict(checkpoint["model_state"])
    model.to(device)
    model.eval()

    source_ids = encode_text(sp, text, source_language, max_length=config.max_length)
    source = torch.tensor(source_ids, dtype=torch.long, device=device).unsqueeze(1)
    source_padding_mask = source.transpose(0, 1).eq(PAD_ID)

    generated = [BOS_ID, _language_token_id(sp, target_language)]
    generation_steps = max(1, min(max_new_tokens, config.max_length - len(generated)))
    with torch.no_grad():
        for _ in range(generation_steps):
            target = torch.tensor(generated, dtype=torch.long, device=device).unsqueeze(1)
            target_mask = generate_square_subsequent_mask(target.size(0), device)
            logits = model(source, target, src_key_padding_mask=source_padding_mask, tgt_mask=target_mask)
            next_id = int(logits[-1, 0].argmax().item())
            generated.append(next_id)
            if next_id == EOS_ID:
                break

    return decode_ids(sp, generated)


class PositionalEncoding:
    """Sinusoidal position encoding without any pretrained parameters."""

    def __init__(self, d_model: int, max_length: int) -> None:
        import torch

        position = torch.arange(max_length).unsqueeze(1)
        div_term = torch.exp(torch.arange(0, d_model, 2) * (-math.log(10_000.0) / d_model))
        pe = torch.zeros(max_length, d_model)
        pe[:, 0::2] = torch.sin(position * div_term)
        pe[:, 1::2] = torch.cos(position * div_term)
        self.pe = pe.unsqueeze(1)

    def to(self, device: str):
        self.pe = self.pe.to(device)
        return self

    def __call__(self, x):
        return x + self.pe[: x.size(0)]


class FromScratchTransformer:
    """Small encoder-decoder Transformer for local NMT training."""

    def __new__(cls, config: TranslationConfig):
        import torch.nn as nn

        class _Model(nn.Module):
            def __init__(self, model_config: TranslationConfig) -> None:
                super().__init__()
                self.config = model_config
                self.embedding = nn.Embedding(model_config.vocab_size, model_config.d_model, padding_idx=PAD_ID)
                self.position = PositionalEncoding(model_config.d_model, model_config.max_length)
                self.transformer = nn.Transformer(
                    d_model=model_config.d_model,
                    nhead=model_config.nhead,
                    num_encoder_layers=model_config.num_encoder_layers,
                    num_decoder_layers=model_config.num_decoder_layers,
                    dim_feedforward=model_config.dim_feedforward,
                    dropout=model_config.dropout,
                    batch_first=False,
                )
                self.output = nn.Linear(model_config.d_model, model_config.vocab_size)

            def to(self, *args, **kwargs):
                super().to(*args, **kwargs)
                device = next(self.parameters()).device
                self.position.to(str(device))
                return self

            def forward(self, src, tgt, *, src_key_padding_mask=None, tgt_key_padding_mask=None, tgt_mask=None):
                scale = math.sqrt(self.config.d_model)
                src_emb = self.position(self.embedding(src) * scale)
                tgt_emb = self.position(self.embedding(tgt) * scale)
                output = self.transformer(
                    src_emb,
                    tgt_emb,
                    tgt_mask=tgt_mask,
                    src_key_padding_mask=src_key_padding_mask,
                    tgt_key_padding_mask=tgt_key_padding_mask,
                    memory_key_padding_mask=src_key_padding_mask,
                )
                return self.output(output)

        return _Model(config)


def encode_text(sp, text: str, language: str, *, max_length: int) -> list[int]:
    """Encode text with BOS, language tag, SentencePiece ids, and EOS."""
    ids = [BOS_ID, _language_token_id(sp, language)]
    ids.extend(int(item) for item in sp.encode(text, out_type=int))
    ids.append(EOS_ID)
    return ids[:max_length]


def decode_ids(sp, ids: list[int]) -> str:
    """Decode generated token ids while removing control tokens."""
    control_ids = {PAD_ID, UNK_ID, BOS_ID, EOS_ID}
    pieces = [item for item in ids if item not in control_ids and not _is_language_id(sp, item)]
    return sp.decode(pieces).strip()


def generate_square_subsequent_mask(size: int, device: str):
    import torch

    return torch.triu(torch.full((size, size), float("-inf"), device=device), diagonal=1)


def _language_token_id(sp, language: str) -> int:
    token_id = int(sp.piece_to_id(f"<{language}>"))
    return token_id if token_id >= 0 else UNK_ID


def _is_language_id(sp, token_id: int) -> bool:
    piece = sp.id_to_piece(int(token_id))
    return piece in {"<ko_kp>", "<en>", "<zh>", "<ru>"}
