"""Import raw DPRK Korean text files into a normalized sentence table.

How to use:

1. Put `.corpus`, `.md`, `.json`, `.jsonl`, `.tsv`, or `.csv` files under
   `data/corpus/raw/`.
2. Run `python scripts/import_monolingual_corpus.py --language ko_kp`.
3. Review `data/corpus/processed/monolingual_sentences.tsv`.

This script does not decide whether a source is legally usable. Record that in
`data/corpus/source_registry.tsv` before using the data for training.
"""

from __future__ import annotations

import argparse
import csv
import hashlib
import json
import re
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
RAW_ROOT = ROOT / "data" / "corpus" / "raw"
OUTPUT = ROOT / "data" / "corpus" / "processed" / "monolingual_sentences.tsv"
SUPPORTED_SUFFIXES = {".corpus", ".txt", ".md", ".json", ".jsonl", ".tsv", ".csv"}


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--language", default="ko_kp", help="language code for imported raw files")
    parser.add_argument("--min-chars", type=int, default=3)
    parser.add_argument("--output", default=str(OUTPUT))
    args = parser.parse_args()

    rows = []
    seen_texts: set[str] = set()
    for path in sorted(RAW_ROOT.rglob("*")):
        if not path.is_file() or path.suffix.lower() not in SUPPORTED_SUFFIXES:
            continue
        for index, text in enumerate(_extract_texts(path), start=1):
            for sentence in _split_sentences(_normalize_text(text)):
                if len(sentence) < args.min_chars or sentence in seen_texts:
                    continue
                seen_texts.add(sentence)
                rows.append(
                    {
                        "source_id": _source_id(path),
                        "language": args.language,
                        "text": sentence,
                        "path": str(path.relative_to(ROOT)),
                        "record_no": str(index),
                    }
                )

    output = Path(args.output)
    output.parent.mkdir(parents=True, exist_ok=True)
    with output.open("w", encoding="utf-8", newline="") as file:
        writer = csv.DictWriter(file, fieldnames=["source_id", "language", "text", "path", "record_no"], delimiter="\t")
        writer.writeheader()
        writer.writerows(rows)

    print(f"Imported {len(rows)} sentences into {output}")
    return 0


def _extract_texts(path: Path) -> list[str]:
    suffix = path.suffix.lower()
    if suffix in {".corpus", ".txt", ".md"}:
        return [path.read_text(encoding="utf-8", errors="ignore")]
    if suffix == ".jsonl":
        texts = []
        for line in path.read_text(encoding="utf-8", errors="ignore").splitlines():
            if line.strip():
                texts.extend(_texts_from_json(json.loads(line)))
        return texts
    if suffix == ".json":
        return _texts_from_json(json.loads(path.read_text(encoding="utf-8", errors="ignore")))
    if suffix in {".tsv", ".csv"}:
        delimiter = "\t" if suffix == ".tsv" else ","
        with path.open("r", encoding="utf-8", errors="ignore", newline="") as file:
            reader = csv.DictReader(file, delimiter=delimiter)
            rows = list(reader)
            if reader.fieldnames:
                text_columns = [name for name in reader.fieldnames if "text" in name.lower() or "sentence" in name.lower()]
                if text_columns:
                    return [row.get(column, "") for row in rows for column in text_columns]
            return [" ".join(value for value in row.values() if value) for row in rows]
    return []


def _texts_from_json(value: object) -> list[str]:
    if isinstance(value, str):
        return [value]
    if isinstance(value, list):
        texts: list[str] = []
        for item in value:
            texts.extend(_texts_from_json(item))
        return texts
    if isinstance(value, dict):
        preferred = [value.get(key) for key in ("text", "sentence", "content", "body") if isinstance(value.get(key), str)]
        if preferred:
            return [str(item) for item in preferred]
        texts = []
        for item in value.values():
            texts.extend(_texts_from_json(item))
        return texts
    return []


def _normalize_text(text: str) -> str:
    text = text.replace("\ufeff", " ")
    text = re.sub(r"\s+", " ", text)
    return text.strip()


def _split_sentences(text: str) -> list[str]:
    parts = re.split(r"(?<=[.!?。！？\n])\s+|(?<=[다까요죠오네음함])\.\s*", text)
    return [part.strip() for part in parts if part.strip()]


def _source_id(path: Path) -> str:
    digest = hashlib.sha1(str(path.relative_to(RAW_ROOT)).encode("utf-8")).hexdigest()[:10]
    return f"raw_{digest}"


if __name__ == "__main__":
    raise SystemExit(main())
