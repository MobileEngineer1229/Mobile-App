"""Build a simple approved translation memory from a parallel TSV file.

The output is a JSON dictionary that the web service can later load before
calling the neural model. Only exact matches should go into this memory.
"""

from __future__ import annotations

import argparse
import csv
import json
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
DEFAULT_OUTPUT = ROOT / "data" / "translation_memory.json"


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("path", help="parallel TSV with reviewed sentence pairs")
    parser.add_argument("--output", default=str(DEFAULT_OUTPUT))
    args = parser.parse_args()

    memory: dict[str, str] = {}
    with Path(args.path).open("r", encoding="utf-8", newline="") as file:
        reader = csv.DictReader(file, delimiter="\t")
        for row in reader:
            source = row.get("source_text", "").strip()
            target = row.get("target_text", "").strip()
            source_language = row.get("source_language", "").strip()
            target_language = row.get("target_language", "").strip()
            if not source or not target:
                continue
            key = f"{source_language}\t{target_language}\t{source}"
            memory[key] = target

    output = Path(args.output)
    output.write_text(json.dumps(memory, ensure_ascii=False, indent=2), encoding="utf-8")
    print(f"Wrote {len(memory)} entries to {output}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
