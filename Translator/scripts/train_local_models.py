"""Train lightweight local models from the current corpus.

This is not the final neural translator. It gives the project an immediate
learning step that works offline:

- translation memory from reviewed parallel pairs
- a character n-gram language model from DPRK Korean text

The neural model fine-tuning step can be added later without changing the daily
workflow.
"""

from __future__ import annotations

import argparse
import csv
import json
import math
from collections import Counter
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
MONO = ROOT / "data" / "corpus" / "processed" / "monolingual_sentences.tsv"
PARALLEL = ROOT / "data" / "corpus" / "parallel" / "reviewed_parallel.tsv"
MEMORY = ROOT / "data" / "translation_memory.json"
MODEL_ROOT = ROOT / "models" / "text"
CHAR_LM = MODEL_ROOT / "ko_kp_char_lm.json"
MODEL_CARD = MODEL_ROOT / "local_model_card.json"


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--order", type=int, default=4, help="character n-gram order")
    parser.add_argument("--alpha", type=float, default=0.1, help="additive smoothing")
    args = parser.parse_args()

    MODEL_ROOT.mkdir(parents=True, exist_ok=True)
    memory_count = _build_translation_memory()
    lm_stats = _train_char_lm(order=args.order, alpha=args.alpha)
    _write_model_card(memory_count, lm_stats)
    print(f"Translation memory entries: {memory_count}")
    print(f"Character LM sentences: {lm_stats['sentences']}")
    print(f"Character LM vocabulary: {lm_stats['vocab_size']}")
    return 0


def _build_translation_memory() -> int:
    memory: dict[str, str] = {}
    if PARALLEL.exists():
        with PARALLEL.open("r", encoding="utf-8", newline="") as file:
            reader = csv.DictReader(file, delimiter="\t")
            for row in reader:
                source_language = row.get("source_language", "").strip()
                target_language = row.get("target_language", "").strip()
                source_text = row.get("source_text", "").strip()
                target_text = row.get("target_text", "").strip()
                if not source_language or not target_language or not source_text or not target_text:
                    continue
                memory[f"{source_language}\t{target_language}\t{source_text}"] = target_text
    MEMORY.write_text(json.dumps(memory, ensure_ascii=False, indent=2), encoding="utf-8")
    return len(memory)


def _train_char_lm(order: int, alpha: float) -> dict[str, int | float]:
    sentences = _read_ko_kp_sentences()
    ngrams: Counter[str] = Counter()
    contexts: Counter[str] = Counter()
    vocab: set[str] = set()

    for sentence in sentences:
        padded = "^" * (order - 1) + sentence + "$"
        vocab.update(sentence)
        for index in range(order - 1, len(padded)):
            context = padded[index - order + 1 : index]
            char = padded[index]
            contexts[context] += 1
            ngrams[context + char] += 1

    model = {
        "language": "ko_kp",
        "type": "character_ngram_lm",
        "order": order,
        "alpha": alpha,
        "vocab": sorted(vocab | {"$"}),
        "contexts": dict(contexts),
        "ngrams": dict(ngrams),
        "sentences": len(sentences),
    }
    CHAR_LM.write_text(json.dumps(model, ensure_ascii=False, indent=2), encoding="utf-8")
    return {
        "sentences": len(sentences),
        "vocab_size": len(model["vocab"]),
        "contexts": len(contexts),
        "ngrams": len(ngrams),
    }


def _read_ko_kp_sentences() -> list[str]:
    sentences: list[str] = []
    if MONO.exists():
        with MONO.open("r", encoding="utf-8", newline="") as file:
            reader = csv.DictReader(file, delimiter="\t")
            for row in reader:
                if row.get("language") == "ko_kp" and row.get("text", "").strip():
                    sentences.append(row["text"].strip())
    if PARALLEL.exists():
        with PARALLEL.open("r", encoding="utf-8", newline="") as file:
            reader = csv.DictReader(file, delimiter="\t")
            for row in reader:
                if row.get("source_language") == "ko_kp" and row.get("source_text", "").strip():
                    sentences.append(row["source_text"].strip())
                if row.get("target_language") == "ko_kp" and row.get("target_text", "").strip():
                    sentences.append(row["target_text"].strip())
    return sorted(set(sentences))


def _write_model_card(memory_count: int, lm_stats: dict[str, int | float]) -> None:
    card = {
        "trained_by": "scripts/train_local_models.py",
        "offline": True,
        "translation_memory_entries": memory_count,
        "ko_kp_char_lm": {
            "path": str(CHAR_LM.relative_to(ROOT)),
            **lm_stats,
        },
        "next_neural_step": "Fine-tune a seq2seq translation model with data/training/translation/*.tsv.",
    }
    MODEL_CARD.write_text(json.dumps(card, ensure_ascii=False, indent=2), encoding="utf-8")


def score_text_with_model(text: str, model: dict[str, object]) -> float:
    """Return average log probability for a text under a trained char LM."""
    order = int(model["order"])
    alpha = float(model["alpha"])
    vocab = list(model["vocab"])
    vocab_size = len(vocab)
    contexts = Counter(model["contexts"])
    ngrams = Counter(model["ngrams"])
    padded = "^" * (order - 1) + text + "$"
    total = 0.0
    count = 0
    for index in range(order - 1, len(padded)):
        context = padded[index - order + 1 : index]
        char = padded[index]
        numerator = ngrams[context + char] + alpha
        denominator = contexts[context] + alpha * vocab_size
        total += math.log(numerator / denominator)
        count += 1
    return total / max(count, 1)


if __name__ == "__main__":
    raise SystemExit(main())
