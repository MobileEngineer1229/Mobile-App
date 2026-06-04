"""Import reviewed parallel sentence pairs into the project corpus.

Drop TSV or CSV files into `data/corpus/parallel/inbox/` with one of these
column sets:

- source_language, target_language, source_text, target_text
- src_lang, tgt_lang, source, target

Then run:

    python scripts/import_parallel_corpus.py
"""

from __future__ import annotations

import argparse
import csv
import re
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
INBOX = ROOT / "data" / "corpus" / "parallel" / "inbox"
URL_APPROVED = ROOT / "data" / "corpus" / "url_pairs" / "approved"
OUTPUT = ROOT / "data" / "corpus" / "parallel" / "reviewed_parallel.tsv"
SUPPORTED_SUFFIXES = {".tsv", ".csv"}


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--input-dir", default=str(INBOX))
    parser.add_argument("--output", default=str(OUTPUT))
    args = parser.parse_args()

    rows = []
    seen: set[tuple[str, str, str, str]] = set()
    input_paths = list(sorted(Path(args.input_dir).rglob("*")))
    if URL_APPROVED.exists():
        input_paths.extend(sorted(URL_APPROVED.rglob("*")))

    for path in input_paths:
        if not path.is_file() or path.suffix.lower() not in SUPPORTED_SUFFIXES:
            continue
        for row in _read_parallel_rows(path):
            key = (
                row["source_language"],
                row["target_language"],
                row["source_text"],
                row["target_text"],
            )
            if key in seen:
                continue
            seen.add(key)
            rows.append(row)

    output = Path(args.output)
    output.parent.mkdir(parents=True, exist_ok=True)
    with output.open("w", encoding="utf-8", newline="") as file:
        writer = csv.DictWriter(
            file,
            fieldnames=["source_language", "target_language", "source_text", "target_text", "source_file"],
            delimiter="\t",
        )
        writer.writeheader()
        writer.writerows(rows)

    print(f"Imported {len(rows)} parallel pairs into {output}")
    return 0


def _read_parallel_rows(path: Path) -> list[dict[str, str]]:
    delimiter = "\t" if path.suffix.lower() == ".tsv" else ","
    with path.open("r", encoding="utf-8", errors="ignore", newline="") as file:
        reader = csv.DictReader(file, delimiter=delimiter)
        if not reader.fieldnames:
            return []
        columns = set(reader.fieldnames)
        if {"source_language", "target_language", "source_text", "target_text"}.issubset(columns):
            mapping = {
                "source_language": "source_language",
                "target_language": "target_language",
                "source_text": "source_text",
                "target_text": "target_text",
            }
        elif {"src_lang", "tgt_lang", "source", "target"}.issubset(columns):
            mapping = {
                "source_language": "src_lang",
                "target_language": "tgt_lang",
                "source_text": "source",
                "target_text": "target",
            }
        else:
            raise SystemExit(f"Unsupported columns in {path}: {', '.join(reader.fieldnames)}")

        rows = []
        for record in reader:
            source_text = _normalize_text(record.get(mapping["source_text"], ""))
            target_text = _normalize_text(record.get(mapping["target_text"], ""))
            if not source_text or not target_text:
                continue
            rows.append(
                {
                    "source_language": record.get(mapping["source_language"], "").strip(),
                    "target_language": record.get(mapping["target_language"], "").strip(),
                    "source_text": source_text,
                    "target_text": target_text,
                    "source_file": str(path.relative_to(ROOT)),
                }
            )
        return rows


def _normalize_text(text: str) -> str:
    text = text.replace("\ufeff", " ")
    return re.sub(r"\s+", " ", text).strip()


if __name__ == "__main__":
    raise SystemExit(main())
