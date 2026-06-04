"""Print the recommended DPRK Korean model training plan.

This script does not train a model yet. It keeps the training method explicit
so the project does not jump into expensive experiments before the corpus is
licensed, cleaned, and validated.
"""

from __future__ import annotations


def main() -> int:
    print("DPRK Korean model training plan")
    print()
    print("1. Build and license-check data/corpus/source_registry.tsv")
    print("2. Clean monolingual DPRK Korean text into data/corpus/processed/")
    print("3. Validate parallel TSV files with scripts/validate_parallel_corpus.py")
    print("4. Build exact translation memory with scripts/build_translation_memory.py")
    print("5. Fine-tune ko_kr <-> ko_kp first, then add en/zh/ru")
    print("6. Export the best model for offline web_service inference")
    print()
    print("Recommended first model path:")
    print("- ko_kr <-> ko_kp: fine-tune a Korean seq2seq base model")
    print("- en/zh/ru <-> ko_kp: NLLB base + DPRK converter, then direct fine-tuning later")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
