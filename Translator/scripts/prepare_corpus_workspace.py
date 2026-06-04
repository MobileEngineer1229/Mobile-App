"""Create the DPRK Korean corpus workspace.

Run this before collecting or importing data. The script creates the expected
folders and empty registry files without downloading anything.
"""

from __future__ import annotations

import csv
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
CORPUS_ROOT = ROOT / "data" / "corpus"
SOURCE_REGISTRY = CORPUS_ROOT / "source_registry.tsv"


def main() -> int:
    for folder in [
        "raw",
        "processed",
        "parallel",
        "parallel/inbox",
        "speech",
        "speech/audio",
        "image",
        "image/files",
        "url_pairs",
        "url_pairs/inbox",
        "url_pairs/candidates",
        "url_pairs/approved",
        "licenses",
    ]:
        path = CORPUS_ROOT / folder
        path.mkdir(parents=True, exist_ok=True)
        (path / ".gitkeep").touch(exist_ok=True)

    if not SOURCE_REGISTRY.exists():
        with SOURCE_REGISTRY.open("w", encoding="utf-8", newline="") as file:
            writer = csv.writer(file, delimiter="\t")
            writer.writerow(
                [
                    "source_id",
                    "title",
                    "url_or_location",
                    "language",
                    "license",
                    "permission_status",
                    "domain",
                    "notes",
                ]
            )
        print(f"Created: {SOURCE_REGISTRY}")
    else:
        print(f"Already exists: {SOURCE_REGISTRY}")

    return 0


if __name__ == "__main__":
    raise SystemExit(main())
