"""Prepare OCR asset folders for offline image translation.

OCR usually needs a detector, recognizer, and character dictionary. This script
creates the expected folder layout and explains where each model file belongs.
"""

from __future__ import annotations

from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
OCR_ROOT = ROOT / "models" / "ocr"


def main() -> int:
    folders = [
        OCR_ROOT / "det",
        OCR_ROOT / "rec" / "ko",
        OCR_ROOT / "rec" / "en",
        OCR_ROOT / "rec" / "zh",
        OCR_ROOT / "rec" / "ru",
        OCR_ROOT / "dict",
    ]
    for folder in folders:
        folder.mkdir(parents=True, exist_ok=True)
        keep = folder / ".gitkeep"
        keep.touch(exist_ok=True)

    print(f"Prepared OCR folders under {OCR_ROOT}")
    print("Place detector files in models/ocr/det/")
    print("Place recognizer files in models/ocr/rec/<language>/")
    print("Place OCR dictionaries in models/ocr/dict/")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
