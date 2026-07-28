"""Small Python lab service for testing the translation pipeline shape.

The real translator will use an offline neural model. This module keeps the
same input/output flow and lets us validate DPRK glossary post-editing today.
"""

from __future__ import annotations

from app.local_quality import score_ko_kp_text
from app.model_backends import translate_with_backend
from app.postedit import apply_postedit
from app.translation_memory import lookup_translation


def translate(text: str, source_language: str, target_language: str) -> dict[str, str | bool | float | None]:
    """Translate text through the current offline pipeline."""
    memory_result = lookup_translation(text, source_language, target_language)
    if memory_result is not None:
        return {
            "translated_text": memory_result,
            "engine": "translation-memory",
            "offline": True,
            "ko_kp_score": score_ko_kp_text(memory_result) if target_language == "ko_kp" else None,
        }

    backend_result = translate_with_backend(text, source_language, target_language)
    if backend_result is not None:
        raw_result, engine = backend_result
        translated = apply_postedit(raw_result, source_language, target_language)
        return {
            "translated_text": translated,
            "engine": engine,
            "offline": True,
            "ko_kp_score": score_ko_kp_text(translated) if target_language == "ko_kp" else None,
        }

    raw_result = _demo_dictionary_translate(text, source_language, target_language)
    translated = apply_postedit(raw_result, source_language, target_language)
    return {
        "translated_text": translated,
        "engine": "demo-memory-postedit",
        "offline": True,
        "ko_kp_score": score_ko_kp_text(translated) if target_language == "ko_kp" else None,
    }


def translate_for_demo(text: str, source_language: str, target_language: str) -> str:
    """Return a deterministic demo translation result for pipeline testing."""
    return str(translate(text, source_language, target_language)["translated_text"])


def _demo_dictionary_translate(text: str, source_language: str, target_language: str) -> str:
    phrase_map = {
        ("en", "ko_kp", "hello"): "hello",
        ("en", "ko_kp", "korean"): "Korean",
        ("ko_kp", "en", "Joseon language"): "DPRK Korean",
    }
    key = (source_language, target_language, text.strip().lower())
    return phrase_map.get(key, text)
