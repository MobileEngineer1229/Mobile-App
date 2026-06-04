"""Train a project-owned SentencePiece tokenizer from local corpus data.

This script does not download or reuse any pretrained tokenizer. It builds a
new BPE vocabulary only from the text already collected inside this project.
Use it before `scripts/train_transformer_from_scratch.py`.
"""

from __future__ import annotations

import argparse
import csv
import json
import tempfile
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
NEURAL_ROOT = ROOT / "data" / "training" / "neural"
TRANSLATION_SPLITS = ROOT / "data" / "training" / "translation"
MONO = ROOT / "data" / "corpus" / "processed" / "monolingual_sentences.tsv"
DEFAULT_OUTPUT = ROOT / "models" / "text" / "from_scratch" / "tokenizer"


def main() -> int:
    parser = argparse.ArgumentParser(description="Train an offline DPRK translator tokenizer from scratch.")
    parser.add_argument("--direction", help="Optional direction such as ko_kp__en. Omit to use all directions.")
    parser.add_argument("--output", default=str(DEFAULT_OUTPUT), help="Output directory for tokenizer.model.")
    parser.add_argument("--vocab-size", type=int, default=16_000, help="Requested SentencePiece vocabulary size.")
    parser.add_argument("--character-coverage", type=float, default=1.0, help="Keep all observed characters by default.")
    parser.add_argument("--model-type", choices=["bpe", "unigram"], default="bpe")
    args = parser.parse_args()

    try:
        import sentencepiece as spm
    except Exception as exc:  # pragma: no cover - environment guard
        raise SystemExit("sentencepiece is required. Install the offline wheel first.") from exc

    output_dir = Path(args.output).resolve()
    output_dir.mkdir(parents=True, exist_ok=True)

    rows = list(_iter_training_text(args.direction))
    if not rows:
        raise SystemExit("No local corpus text was found. Build training data before tokenizer training.")

    model_prefix = output_dir / "tokenizer"
    manifest = {
        "trained_by": "scripts/train_tokenizer.py",
        "pretrained": False,
        "model_type": args.model_type,
        "direction": args.direction or "all",
        "requested_vocab_size": args.vocab_size,
        "training_lines": len(rows),
        "source": "Translator/data only",
        "model_file": _display_path(output_dir / "tokenizer.model"),
        "vocab_file": _display_path(output_dir / "tokenizer.vocab"),
    }

    # SentencePiece needs a real text file. A temporary file keeps the corpus
    # build reproducible without leaving large intermediate files in git.
    with tempfile.NamedTemporaryFile("w", encoding="utf-8", suffix=".txt", delete=False) as file:
        training_text_path = Path(file.name)
        for text in rows:
            file.write(text.replace("\n", " ").strip() + "\n")

    try:
        spm.SentencePieceTrainer.Train(
            input=str(training_text_path),
            model_prefix=str(model_prefix),
            vocab_size=args.vocab_size,
            model_type=args.model_type,
            character_coverage=args.character_coverage,
            pad_id=0,
            unk_id=1,
            bos_id=2,
            eos_id=3,
            pad_piece="<pad>",
            unk_piece="<unk>",
            bos_piece="<bos>",
            eos_piece="<eos>",
            user_defined_symbols=["<ko_kp>", "<en>", "<zh>", "<ru>"],
            hard_vocab_limit=False,
            train_extremely_large_corpus=False,
        )
    finally:
        training_text_path.unlink(missing_ok=True)

    manifest["actual_vocab_size"] = _count_vocab(output_dir / "tokenizer.vocab")
    (output_dir / "tokenizer_manifest.json").write_text(json.dumps(manifest, ensure_ascii=False, indent=2), encoding="utf-8")
    print(json.dumps(manifest, ensure_ascii=False, indent=2))
    return 0


def _iter_training_text(direction: str | None) -> list[str]:
    texts: list[str] = []
    texts.extend(_read_neural_jsonl(direction))
    texts.extend(_read_translation_splits(direction))
    if direction is None:
        texts.extend(_read_monolingual())
    return _deduplicate(texts)


def _read_neural_jsonl(direction: str | None) -> list[str]:
    texts: list[str] = []
    direction_dirs = [NEURAL_ROOT / direction] if direction else sorted(path for path in NEURAL_ROOT.glob("*__*") if path.is_dir())
    for direction_dir in direction_dirs:
        for split in ["train", "dev", "test"]:
            path = direction_dir / f"{split}.jsonl"
            if not path.exists():
                continue
            for line in path.read_text(encoding="utf-8").splitlines():
                if not line.strip():
                    continue
                row = json.loads(line)
                texts.append(str(row.get("source_text", "")).strip())
                texts.append(str(row.get("target_text", "")).strip())
    return texts


def _read_translation_splits(direction: str | None) -> list[str]:
    texts: list[str] = []
    for split in ["train", "dev", "test"]:
        path = TRANSLATION_SPLITS / f"{split}.tsv"
        if not path.exists():
            continue
        with path.open("r", encoding="utf-8", newline="") as file:
            for row in csv.DictReader(file, delimiter="\t"):
                row_direction = f"{row.get('source_language', '')}__{row.get('target_language', '')}"
                if direction and row_direction != direction:
                    continue
                texts.append(row.get("source_text", "").strip())
                texts.append(row.get("target_text", "").strip())
    return texts


def _read_monolingual() -> list[str]:
    if not MONO.exists():
        return []
    texts: list[str] = []
    with MONO.open("r", encoding="utf-8", newline="") as file:
        for row in csv.DictReader(file, delimiter="\t"):
            texts.append(row.get("text", "").strip())
    return texts


def _deduplicate(texts: list[str]) -> list[str]:
    seen: set[str] = set()
    unique: list[str] = []
    for text in texts:
        if not text or text in seen:
            continue
        seen.add(text)
        unique.append(text)
    return unique


def _count_vocab(path: Path) -> int:
    if not path.exists():
        return 0
    return sum(1 for line in path.read_text(encoding="utf-8").splitlines() if line.strip())


def _display_path(path: Path) -> str:
    resolved = path.resolve()
    try:
        return str(resolved.relative_to(ROOT))
    except ValueError:
        return str(resolved)


if __name__ == "__main__":
    raise SystemExit(main())
