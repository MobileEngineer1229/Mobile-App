"""Rebuild raw URL text files from `source_registry.tsv`.

Use this when registry rows exist but raw files were removed, or when the URL
extractor was improved and the raw files should be fetched again with the new
normalizer.
"""

from __future__ import annotations

import argparse
import csv
import sys
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
RAW_ROOT = ROOT / "data" / "corpus" / "raw"
REGISTRY = ROOT / "data" / "corpus" / "source_registry.tsv"
sys.path.insert(0, str(ROOT / "scripts"))

from import_url_corpus import clean_extracted_text, fetch_url_text  # noqa: E402


RAW_EXTENSION = ".corpus"


def main() -> int:
    parser = argparse.ArgumentParser(description="Re-fetch raw URL files listed in source_registry.tsv.")
    parser.add_argument("--force", action="store_true", help="overwrite raw files that already exist")
    parser.add_argument("--max-bytes", type=int, default=5_000_000)
    args = parser.parse_args()

    RAW_ROOT.mkdir(parents=True, exist_ok=True)
    rows = _read_registry()
    rebuilt = 0
    skipped = 0
    for row in rows:
        source_id = row.get("source_id", "").strip()
        url = row.get("url_or_location", "").strip()
        if not source_id or not url.startswith(("http://", "https://")):
            skipped += 1
            continue
        output = RAW_ROOT / f"{source_id}{RAW_EXTENSION}"
        if output.exists() and not args.force:
            skipped += 1
            continue
        text, content_type = fetch_url_text(url, args.max_bytes)
        cleaned = clean_extracted_text(text)
        if not cleaned:
            print(f"empty: {url}")
            skipped += 1
            continue
        output.write_text(cleaned + "\n", encoding="utf-8")
        print(f"rebuilt: {output.relative_to(ROOT)} ({content_type})")
        rebuilt += 1

    print(f"Rebuilt raw files: {rebuilt}")
    print(f"Skipped rows: {skipped}")
    return 0


def _read_registry() -> list[dict[str, str]]:
    if not REGISTRY.exists():
        return []
    with REGISTRY.open("r", encoding="utf-8", newline="") as file:
        return list(csv.DictReader(file, delimiter="\t"))


if __name__ == "__main__":
    if hasattr(sys.stdout, "reconfigure"):
        sys.stdout.reconfigure(encoding="utf-8")
    raise SystemExit(main())
