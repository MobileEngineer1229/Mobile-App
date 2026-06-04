"""Train a project-owned Transformer translation model from zero.

This script intentionally avoids pretrained models and pretrained tokenizers.
It consumes the tokenizer trained by `scripts/train_tokenizer.py` and the local
JSONL sentence pairs exported under `data/training/neural/<direction>/`.
"""

from __future__ import annotations

import argparse
import json
import math
import sys
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / "python_lab"))

from app.from_scratch_backend import (  # noqa: E402
    EOS_ID,
    PAD_ID,
    FromScratchTransformer,
    TranslationConfig,
    encode_text,
    generate_square_subsequent_mask,
)


NEURAL_ROOT = ROOT / "data" / "training" / "neural"
DEFAULT_TOKENIZER = ROOT / "models" / "text" / "from_scratch" / "tokenizer" / "tokenizer.model"
DEFAULT_OUTPUT = ROOT / "models" / "text" / "from_scratch"


def main() -> int:
    parser = argparse.ArgumentParser(description="Train the DPRK translator Transformer from scratch.")
    parser.add_argument("--direction", required=True, help="Language direction, for example ko_kp__en.")
    parser.add_argument("--tokenizer", default=str(DEFAULT_TOKENIZER))
    parser.add_argument("--output", default=str(DEFAULT_OUTPUT))
    parser.add_argument("--epochs", type=int, default=20)
    parser.add_argument("--batch-size", type=int, default=8)
    parser.add_argument("--max-length", type=int, default=256)
    parser.add_argument("--d-model", type=int, default=256)
    parser.add_argument("--nhead", type=int, default=4)
    parser.add_argument("--encoder-layers", type=int, default=4)
    parser.add_argument("--decoder-layers", type=int, default=4)
    parser.add_argument("--ffn-dim", type=int, default=1024)
    parser.add_argument("--dropout", type=float, default=0.1)
    parser.add_argument("--learning-rate", type=float, default=3e-4)
    parser.add_argument("--device", default="cuda", help="Use cuda when available, otherwise cpu.")
    args = parser.parse_args()

    try:
        import sentencepiece as spm
        import torch
        from torch.utils.data import DataLoader
    except Exception as exc:  # pragma: no cover - environment guard
        raise SystemExit("torch and sentencepiece are required for from-scratch training.") from exc

    if args.device == "cuda" and not torch.cuda.is_available():
        args.device = "cpu"

    direction_dir = NEURAL_ROOT / args.direction
    tokenizer_path = Path(args.tokenizer)
    if not direction_dir.exists():
        raise SystemExit(f"Missing training direction: {direction_dir}")
    if not tokenizer_path.exists():
        raise SystemExit(f"Missing tokenizer. Run scripts/train_tokenizer.py first: {tokenizer_path}")

    source_language, target_language = args.direction.split("__", 1)
    sp = spm.SentencePieceProcessor(model_file=str(tokenizer_path))
    train_rows = _read_jsonl(direction_dir / "train.jsonl")
    dev_rows = _read_jsonl(direction_dir / "dev.jsonl")
    if not train_rows:
        raise SystemExit(f"No training rows found for {args.direction}")

    train_dataset = TranslationDataset(train_rows, sp, source_language, target_language, args.max_length)
    dev_dataset = TranslationDataset(dev_rows, sp, source_language, target_language, args.max_length) if dev_rows else None
    train_loader = DataLoader(train_dataset, batch_size=args.batch_size, shuffle=True, collate_fn=collate_batch)
    dev_loader = DataLoader(dev_dataset, batch_size=args.batch_size, shuffle=False, collate_fn=collate_batch) if dev_dataset else None

    config = TranslationConfig(
        vocab_size=sp.get_piece_size(),
        d_model=args.d_model,
        nhead=args.nhead,
        num_encoder_layers=args.encoder_layers,
        num_decoder_layers=args.decoder_layers,
        dim_feedforward=args.ffn_dim,
        dropout=args.dropout,
        max_length=args.max_length,
    )
    model = FromScratchTransformer(config).to(args.device)
    optimizer = torch.optim.AdamW(model.parameters(), lr=args.learning_rate)
    criterion = torch.nn.CrossEntropyLoss(ignore_index=PAD_ID)

    output_dir = Path(args.output) / args.direction
    output_dir.mkdir(parents=True, exist_ok=True)
    _write_config(output_dir, args, config, tokenizer_path, len(train_dataset), len(dev_dataset) if dev_dataset else 0)

    best_loss = math.inf
    for epoch in range(1, args.epochs + 1):
        train_loss = _run_epoch(model, train_loader, optimizer, criterion, args.device, train=True)
        dev_loss = _run_epoch(model, dev_loader, optimizer, criterion, args.device, train=False) if dev_loader else train_loss
        print(json.dumps({"epoch": epoch, "train_loss": train_loss, "dev_loss": dev_loss}, ensure_ascii=False))

        checkpoint = {
            "model_state": model.state_dict(),
            "config": config.__dict__,
            "epoch": epoch,
            "train_loss": train_loss,
            "dev_loss": dev_loss,
            "pretrained": False,
        }
        torch.save(checkpoint, output_dir / "last.pt")
        if dev_loss <= best_loss:
            best_loss = dev_loss
            torch.save(checkpoint, output_dir / "best.pt")

    _write_active_backend(output_dir, tokenizer_path, args.device)
    print(f"Saved from-scratch model: {output_dir}")
    return 0


class TranslationDataset:
    """Simple in-memory dataset for local JSONL sentence pairs."""

    def __init__(self, rows: list[dict[str, str]], sp, source_language: str, target_language: str, max_length: int) -> None:
        self.items: list[tuple[list[int], list[int]]] = []
        for row in rows:
            source = encode_text(sp, row["source_text"], source_language, max_length=max_length)
            target = encode_text(sp, row["target_text"], target_language, max_length=max_length)
            if len(source) >= 3 and len(target) >= 3:
                self.items.append((source, target))

    def __len__(self) -> int:
        return len(self.items)

    def __getitem__(self, index: int) -> tuple[list[int], list[int]]:
        return self.items[index]


def collate_batch(batch: list[tuple[list[int], list[int]]]):
    import torch

    src_max = max(len(source) for source, _ in batch)
    tgt_max = max(len(target) for _, target in batch)
    sources = []
    decoder_inputs = []
    labels = []
    for source, target in batch:
        src = source + [PAD_ID] * (src_max - len(source))
        decoder_input = target[:-1] + [PAD_ID] * (tgt_max - len(target))
        label = target[1:] + [PAD_ID] * (tgt_max - len(target))
        sources.append(src)
        decoder_inputs.append(decoder_input)
        labels.append(label)
    return (
        torch.tensor(sources, dtype=torch.long).transpose(0, 1),
        torch.tensor(decoder_inputs, dtype=torch.long).transpose(0, 1),
        torch.tensor(labels, dtype=torch.long).transpose(0, 1),
    )


def _run_epoch(model, loader, optimizer, criterion, device: str, *, train: bool) -> float:
    import torch

    if loader is None:
        return 0.0
    model.train(train)
    total_loss = 0.0
    total_tokens = 0
    for source, decoder_input, labels in loader:
        source = source.to(device)
        decoder_input = decoder_input.to(device)
        labels = labels.to(device)
        source_padding_mask = source.transpose(0, 1).eq(PAD_ID)
        target_padding_mask = decoder_input.transpose(0, 1).eq(PAD_ID)
        target_mask = generate_square_subsequent_mask(decoder_input.size(0), device)

        if train:
            optimizer.zero_grad(set_to_none=True)
        with torch.set_grad_enabled(train):
            logits = model(
                source,
                decoder_input,
                src_key_padding_mask=source_padding_mask,
                tgt_key_padding_mask=target_padding_mask,
                tgt_mask=target_mask,
            )
            loss = criterion(logits.reshape(-1, logits.size(-1)), labels.reshape(-1))
            if train:
                loss.backward()
                torch.nn.utils.clip_grad_norm_(model.parameters(), 1.0)
                optimizer.step()
        token_count = labels.ne(PAD_ID).sum().item()
        total_loss += float(loss.item()) * max(token_count, 1)
        total_tokens += max(token_count, 1)
    return total_loss / max(total_tokens, 1)


def _read_jsonl(path: Path) -> list[dict[str, str]]:
    if not path.exists():
        return []
    rows: list[dict[str, str]] = []
    for line in path.read_text(encoding="utf-8").splitlines():
        if not line.strip():
            continue
        row = json.loads(line)
        if row.get("source_text") and row.get("target_text"):
            rows.append(row)
    return rows


def _write_config(output_dir: Path, args: argparse.Namespace, config: TranslationConfig, tokenizer_path: Path, train_rows: int, dev_rows: int) -> None:
    payload = {
        "trained_by": "scripts/train_transformer_from_scratch.py",
        "pretrained": False,
        "direction": args.direction,
        "source": "Translator/data/training/neural only",
        "tokenizer": str(tokenizer_path.resolve()),
        "train_rows": train_rows,
        "dev_rows": dev_rows,
        "model": config.__dict__,
        "training": {
            "epochs": args.epochs,
            "batch_size": args.batch_size,
            "learning_rate": args.learning_rate,
            "device": args.device,
        },
    }
    (output_dir / "config.json").write_text(json.dumps(payload, ensure_ascii=False, indent=2), encoding="utf-8")


def _write_active_backend(output_dir: Path, tokenizer_path: Path, device: str) -> None:
    active_backend = {
        "backend": "from_scratch",
        "model_dir": str(output_dir),
        "tokenizer": str(tokenizer_path),
        "device": device,
        "max_new_tokens": 128,
    }
    active_path = ROOT / "models" / "text" / "active_backend.json"
    active_path.write_text(json.dumps(active_backend, ensure_ascii=False, indent=2), encoding="utf-8")


if __name__ == "__main__":
    raise SystemExit(main())
