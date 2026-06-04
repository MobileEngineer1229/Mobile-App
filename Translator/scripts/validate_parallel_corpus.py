"""Validate a TSV parallel corpus file before model training.

Expected columns:

    source_language  target_language  source_text  target_text

The validator catches empty rows, missing columns, duplicated pairs, and very
long lines that should probably be split before training.
"""

from __future__ import annotations

import argparse
import csv
from pathlib import Path


REQUIRED_COLUMNS = ["source_language", "target_language", "source_text", "target_text"]


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("path", help="TSV file to validate")
    parser.add_argument("--max-chars", type=int, default=600)
    args = parser.parse_args()

    path = Path(args.path)
    rows = _read_rows(path)
    errors: list[str] = []
    seen: set[tuple[str, str, str, str]] = set()

    for index, row in enumerate(rows, start=2):
        for column in REQUIRED_COLUMNS:
            if not row.get(column, "").strip():
                errors.append(f"line {index}: missing {column}")

        pair = tuple(row.get(column, "").strip() for column in REQUIRED_COLUMNS)
        if pair in seen:
            errors.append(f"line {index}: duplicate pair")
        seen.add(pair)

        if len(row.get("source_text", "")) > args.max_chars or len(row.get("target_text", "")) > args.max_chars:
            errors.append(f"line {index}: sentence is longer than {args.max_chars} characters")

    print(f"Rows: {len(rows)}")
    print(f"Errors: {len(errors)}")
    for error in errors[:50]:
        print(f"- {error}")
    if len(errors) > 50:
        print(f"... {len(errors) - 50} more")

    return 1 if errors else 0


def _read_rows(path: Path) -> list[dict[str, str]]:
    with path.open("r", encoding="utf-8", newline="") as file:
        reader = csv.DictReader(file, delimiter="\t")
        missing = [column for column in REQUIRED_COLUMNS if column not in (reader.fieldnames or [])]
        if missing:
            raise SystemExit(f"Missing columns: {', '.join(missing)}")
        return list(reader)


if __name__ == "__main__":
    raise SystemExit(main())
