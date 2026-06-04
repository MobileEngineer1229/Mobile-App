"""Repair excessive line breaks in already collected raw URL text.

The URL importer used to keep one HTML text node per line. Some pages split
honorific names and sentence parts into many spans, which produced raw text
like:

    경애하는
    김정은
    동지께서는

This script rewrites existing `data/corpus/raw/*.corpus` files with the improved
normalizer from `import_url_corpus.py`, then optionally runs the data pipeline.
"""

from __future__ import annotations

import argparse
import subprocess
import sys
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
RAW_ROOT = ROOT / "data" / "corpus" / "raw"
sys.path.insert(0, str(ROOT / "scripts"))

from import_url_corpus import clean_extracted_text  # noqa: E402


RAW_SUFFIXES = {".corpus", ".txt"}


def main() -> int:
    parser = argparse.ArgumentParser(description="Repair line breaks in raw corpus text files.")
    parser.add_argument("--dry-run", action="store_true", help="show changes without writing files")
    parser.add_argument("--rebuild", action="store_true", help="run bootstrap_data_pipeline.py after repair")
    args = parser.parse_args()

    changed = 0
    for path in sorted(RAW_ROOT.glob("*")):
        if not path.is_file() or path.suffix.lower() not in RAW_SUFFIXES:
            continue
        original = path.read_text(encoding="utf-8", errors="replace")
        repaired = clean_extracted_text(original)
        if repaired:
            repaired += "\n"
        if repaired == original:
            print(f"unchanged: {path.relative_to(ROOT)}")
            continue
        changed += 1
        before_lines = _line_count(original)
        after_lines = _line_count(repaired)
        print(f"repair: {path.relative_to(ROOT)} lines {before_lines} -> {after_lines}")
        if not args.dry_run:
            path.write_text(repaired, encoding="utf-8")

    print(f"Changed files: {changed}")
    if args.rebuild and not args.dry_run:
        _run([sys.executable, "scripts/bootstrap_data_pipeline.py"])
    return 0


def _line_count(text: str) -> int:
    return len([line for line in text.splitlines() if line.strip()])


def _run(command: list[str]) -> None:
    result = subprocess.run(command, cwd=ROOT, check=False)
    if result.returncode != 0:
        raise subprocess.CalledProcessError(result.returncode, command)


if __name__ == "__main__":
    if hasattr(sys.stdout, "reconfigure"):
        sys.stdout.reconfigure(encoding="utf-8")
    raise SystemExit(main())
