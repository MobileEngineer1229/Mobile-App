"""Export reviewed translation data into direction-specific JSONL files.

Seq2seq training tools usually expect a clear source/target pair per row. This
script groups the project TSV splits by language direction so a future neural
training command can consume them directly.
"""

from __future__ import annotations

import csv
import json
import shutil
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
SPLIT_ROOT = ROOT / "data" / "training" / "translation"
OUT_ROOT = ROOT / "data" / "training" / "neural"


def main() -> int:
    if OUT_ROOT.exists():
        _assert_inside_root(OUT_ROOT)
        shutil.rmtree(OUT_ROOT)
    OUT_ROOT.mkdir(parents=True, exist_ok=True)
    exported = 0
    for split in ["train", "dev", "test"]:
        path = SPLIT_ROOT / f"{split}.tsv"
        if not path.exists():
            continue
        for row in _read_rows(path):
            direction = f"{row['source_language']}__{row['target_language']}"
            out_dir = OUT_ROOT / direction
            out_dir.mkdir(parents=True, exist_ok=True)
            out_path = out_dir / f"{split}.jsonl"
            with out_path.open("a", encoding="utf-8") as file:
                file.write(json.dumps(row, ensure_ascii=False) + "\n")
            exported += 1
    print(f"Exported neural training rows: {exported}")
    print(f"Output: {OUT_ROOT}")
    return 0


def _assert_inside_root(path: Path) -> None:
    resolved_root = ROOT.resolve()
    resolved_path = path.resolve()
    if resolved_root not in resolved_path.parents:
        raise SystemExit(f"Refusing to delete outside project root: {resolved_path}")


def _read_rows(path: Path) -> list[dict[str, str]]:
    with path.open("r", encoding="utf-8", newline="") as file:
        reader = csv.DictReader(file, delimiter="\t")
        rows = []
        for row in reader:
            source_language = row.get("source_language", "").strip()
            target_language = row.get("target_language", "").strip()
            source_text = row.get("source_text", "").strip()
            target_text = row.get("target_text", "").strip()
            if source_language and target_language and source_text and target_text:
                rows.append(
                    {
                        "source_language": source_language,
                        "target_language": target_language,
                        "source_text": source_text,
                        "target_text": target_text,
                    }
                )
        return rows


if __name__ == "__main__":
    raise SystemExit(main())
