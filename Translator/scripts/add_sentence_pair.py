"""Add one reviewed sentence pair to the parallel training inbox.

This is useful when the user wants to type a sentence pair directly instead of
importing paired URLs.
"""

from __future__ import annotations

import argparse
import csv
from datetime import datetime, timezone
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
INBOX = ROOT / "data" / "corpus" / "parallel" / "inbox"


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--source-language", required=True)
    parser.add_argument("--target-language", required=True)
    parser.add_argument("--source-text", required=True)
    parser.add_argument("--target-text", required=True)
    parser.add_argument("--note", default="manual-web-entry")
    args = parser.parse_args()

    INBOX.mkdir(parents=True, exist_ok=True)
    path = INBOX / "manual_pairs.tsv"
    exists = path.exists()
    with path.open("a", encoding="utf-8", newline="") as file:
        fieldnames = ["source_language", "target_language", "source_text", "target_text", "note", "created_at"]
        writer = csv.DictWriter(file, fieldnames=fieldnames, delimiter="\t")
        if not exists:
            writer.writeheader()
        writer.writerow(
            {
                "source_language": args.source_language.strip(),
                "target_language": args.target_language.strip(),
                "source_text": _clean(args.source_text),
                "target_text": _clean(args.target_text),
                "note": args.note,
                "created_at": datetime.now(timezone.utc).isoformat(),
            }
        )
    print(f"Added sentence pair to {path}")
    return 0


def _clean(text: str) -> str:
    return " ".join(text.replace("\ufeff", " ").split())


if __name__ == "__main__":
    raise SystemExit(main())
