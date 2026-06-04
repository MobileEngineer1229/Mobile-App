"""Print recommended offline model candidates and their target folders.

Large AI models should be downloaded deliberately, not as a surprise side
effect of setup. This script gives one consistent place to document download
choices before conversion into mobile-friendly formats.
"""

from __future__ import annotations

from dataclasses import dataclass


@dataclass(frozen=True)
class Candidate:
    feature: str
    name: str
    url: str
    target_folder: str
    note: str


CANDIDATES = [
    Candidate(
        feature="Fast offline service",
        name="Argos Translate language packages",
        url="https://argos-translate.readthedocs.io/",
        target_folder="models/text/",
        note="Fastest path when exact language-pair packages are available.",
    ),
    Candidate(
        feature="Text translation",
        name="facebook/nllb-200-distilled-600M + CTranslate2",
        url="https://huggingface.co/facebook/nllb-200-distilled-600M",
        target_folder="models/text/",
        note="Better multilingual baseline for Korean, English, Chinese, and Russian.",
    ),
    Candidate(
        feature="Speech recognition",
        name="whisper.cpp models",
        url="https://github.com/ggml-org/whisper.cpp",
        target_folder="models/asr/",
        note="Use a quantized model for phone CPU performance.",
    ),
    Candidate(
        feature="OCR",
        name="PaddleOCR inference models",
        url="https://github.com/PaddlePaddle/PaddleOCR",
        target_folder="models/ocr/",
        note="Prepare Korean, English, Chinese, and Russian recognition assets.",
    ),
    Candidate(
        feature="Speech synthesis",
        name="Piper voices",
        url="https://github.com/rhasspy/piper",
        target_folder="models/tts/",
        note="Use only languages where acceptable local voices are available.",
    ),
]


def main() -> int:
    print("Offline model candidates")
    print()
    for item in CANDIDATES:
        print(f"- {item.feature}: {item.name}")
        print(f"  URL: {item.url}")
        print(f"  Target: {item.target_folder}")
        print(f"  Note: {item.note}")
        print()
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
