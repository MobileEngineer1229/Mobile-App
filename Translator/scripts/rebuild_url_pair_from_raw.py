"""Rebuild URL-pair candidate files from already downloaded raw text.

Use this when a URL was imported once, but the extraction or sentence-splitting
rules were improved later. The script does not contact the internet. It reads
the raw text paths from `data/corpus/url_pairs/url_pair_registry.tsv`, cleans
the stored text again, and regenerates candidate TSV files.
"""

from __future__ import annotations

import argparse
import csv
import sys
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / "scripts"))

from import_url_corpus import clean_extracted_text  # noqa: E402
from import_url_pair_corpus import _align_by_order, _split_sentences, _write_candidate_file, _write_parallel_file, UrlPair  # noqa: E402


REGISTRY = ROOT / "data" / "corpus" / "url_pairs" / "url_pair_registry.tsv"
CANDIDATES = ROOT / "data" / "corpus" / "url_pairs" / "candidates"
APPROVED = ROOT / "data" / "corpus" / "url_pairs" / "approved"


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("pair_id", help="pair id to rebuild")
    parser.add_argument(
        "--update-approved",
        action="store_true",
        help="rewrite approved TSV too if an approved TSV already exists",
    )
    args = parser.parse_args()

    row = _find_pair(args.pair_id)
    source_path = ROOT / row["source_raw_path"]
    target_path = ROOT / row["target_raw_path"]
    source_clean = clean_extracted_text(source_path.read_text(encoding="utf-8", errors="ignore"))
    target_clean = clean_extracted_text(target_path.read_text(encoding="utf-8", errors="ignore"))
    source_path.write_text(source_clean + "\n", encoding="utf-8")
    target_path.write_text(target_clean + "\n", encoding="utf-8")

    pair = UrlPair(
        pair_id=row["pair_id"],
        source_language=row["source_language"],
        target_language=row["target_language"],
        source_url=row["source_url"],
        target_url=row["target_url"],
        note=row.get("note", ""),
    )
    rows = _align_by_order(pair, _split_sentences(source_clean), _split_sentences(target_clean))
    candidate_path = CANDIDATES / f"{pair.pair_id}.tsv"
    _write_candidate_file(candidate_path, rows)

    approved_path = APPROVED / f"{pair.pair_id}.tsv"
    if args.update_approved and approved_path.exists():
        _write_parallel_file(approved_path, rows)

    print(f"Rebuilt {candidate_path} with {len(rows)} candidate pairs")
    if args.update_approved and approved_path.exists():
        print(f"Rebuilt {approved_path}")
    return 0


def _find_pair(pair_id: str) -> dict[str, str]:
    if not REGISTRY.exists():
        raise SystemExit(f"Missing registry: {REGISTRY}")
    with REGISTRY.open("r", encoding="utf-8", newline="") as file:
        for row in csv.DictReader(file, delimiter="\t"):
            if row.get("pair_id") == pair_id:
                return row
    raise SystemExit(f"Pair id not found: {pair_id}")


if __name__ == "__main__":
    if hasattr(sys.stdout, "reconfigure"):
        sys.stdout.reconfigure(encoding="utf-8")
    raise SystemExit(main())
