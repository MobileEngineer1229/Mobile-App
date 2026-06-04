"""Local quality helpers loaded by the translation service."""

from __future__ import annotations

import json
import math
from collections import Counter
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
CHAR_LM_PATH = ROOT / "models" / "text" / "ko_kp_char_lm.json"


def score_ko_kp_text(text: str) -> float | None:
    """Return a DPRK Korean character-LM score when a local model exists."""
    if not CHAR_LM_PATH.exists() or not text.strip():
        return None
    model = json.loads(CHAR_LM_PATH.read_text(encoding="utf-8"))
    return _score_text(text, model)


def _score_text(text: str, model: dict[str, object]) -> float:
    order = int(model["order"])
    alpha = float(model["alpha"])
    vocab = list(model["vocab"])
    vocab_size = max(len(vocab), 1)
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
