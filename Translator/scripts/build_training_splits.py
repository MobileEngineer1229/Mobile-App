"""Build deterministic train/dev/test splits for DPRK model experiments.

Outputs:

- `data/training/mlm/*.txt` for DPRK-BERT-style masked language modeling.
- `data/training/translation/*.tsv` for seq2seq translation fine-tuning.
"""

from __future__ import annotations

import argparse
import csv
import random
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
MONO = ROOT / "data" / "corpus" / "processed" / "monolingual_sentences.tsv"
PARALLEL = ROOT / "data" / "corpus" / "parallel" / "reviewed_parallel.tsv"
OUT_ROOT = ROOT / "data" / "training"


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--seed", type=int, default=20260529)
    parser.add_argument("--dev-ratio", type=float, default=0.1)
    parser.add_argument("--test-ratio", type=float, default=0.1)
    args = parser.parse_args()

    rng = random.Random(args.seed)
    _build_mlm_splits(rng, args.dev_ratio, args.test_ratio)
    _build_translation_splits(rng, args.dev_ratio, args.test_ratio)
    return 0


def _build_mlm_splits(rng: random.Random, dev_ratio: float, test_ratio: float) -> None:
    if not MONO.exists():
        print(f"Skipped MLM splits, missing {MONO}")
        return
    with MONO.open("r", encoding="utf-8", newline="") as file:
        rows = list(csv.DictReader(file, delimiter="\t"))
    texts = [row["text"] for row in rows if row.get("language") == "ko_kp" and row.get("text")]
    rng.shuffle(texts)
    train, dev, test = _split(texts, dev_ratio, test_ratio)
    out = OUT_ROOT / "mlm"
    out.mkdir(parents=True, exist_ok=True)
    _write_lines(out / "train.txt", train)
    _write_lines(out / "dev.txt", dev)
    _write_lines(out / "test.txt", test)
    print(f"MLM splits: train={len(train)}, dev={len(dev)}, test={len(test)}")


def _build_translation_splits(rng: random.Random, dev_ratio: float, test_ratio: float) -> None:
    if not PARALLEL.exists():
        print(f"Skipped translation splits, missing {PARALLEL}")
        return
    with PARALLEL.open("r", encoding="utf-8", newline="") as file:
        rows = list(csv.DictReader(file, delimiter="\t"))
    rows = [row for row in rows if row.get("source_text") and row.get("target_text")]
    rng.shuffle(rows)
    train, dev, test = _split(rows, dev_ratio, test_ratio)
    out = OUT_ROOT / "translation"
    out.mkdir(parents=True, exist_ok=True)
    _write_parallel(out / "train.tsv", train)
    _write_parallel(out / "dev.tsv", dev)
    _write_parallel(out / "test.tsv", test)
    print(f"Translation splits: train={len(train)}, dev={len(dev)}, test={len(test)}")


def _split(items: list, dev_ratio: float, test_ratio: float) -> tuple[list, list, list]:
    test_size = int(len(items) * test_ratio)
    dev_size = int(len(items) * dev_ratio)
    test = items[:test_size]
    dev = items[test_size : test_size + dev_size]
    train = items[test_size + dev_size :]
    return train, dev, test


def _write_lines(path: Path, lines: list[str]) -> None:
    path.write_text("\n".join(lines) + ("\n" if lines else ""), encoding="utf-8")


def _write_parallel(path: Path, rows: list[dict[str, str]]) -> None:
    with path.open("w", encoding="utf-8", newline="") as file:
        writer = csv.DictWriter(
            file,
            fieldnames=["source_language", "target_language", "source_text", "target_text"],
            delimiter="\t",
        )
        writer.writeheader()
        for row in rows:
            writer.writerow({key: row.get(key, "") for key in writer.fieldnames or []})


if __name__ == "__main__":
    raise SystemExit(main())
