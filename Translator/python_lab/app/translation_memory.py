"""Exact-match translation memory for reviewed sentence pairs.

The translation memory is deliberately simple: if a sentence has been approved
by a human reviewer, it should be returned before the neural model is called.
"""

from __future__ import annotations

import json
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
MEMORY_PATH = ROOT / "data" / "translation_memory.json"


def lookup_translation(text: str, source_language: str, target_language: str) -> str | None:
    """Return an approved exact translation if one exists."""
    memory = _load_memory()
    key = _make_key(source_language, target_language, text.strip())
    return memory.get(key)


def _load_memory() -> dict[str, str]:
    if not MEMORY_PATH.exists():
        return {}
    return json.loads(MEMORY_PATH.read_text(encoding="utf-8"))


def _make_key(source_language: str, target_language: str, text: str) -> str:
    return f"{source_language}\t{target_language}\t{text}"
