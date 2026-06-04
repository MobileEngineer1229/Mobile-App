"""Optional offline translation model backends.

The service always tries translation memory first. If no exact match exists,
this module can load a local model backend. Backends are optional so the web UI
still runs on a fresh machine.
"""

from __future__ import annotations

import json
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
MODEL_ROOT = ROOT / "models" / "text"
CONFIG_PATH = MODEL_ROOT / "active_backend.json"


def translate_with_backend(text: str, source_language: str, target_language: str) -> tuple[str, str] | None:
    """Translate with the configured local backend, if available."""
    config = _load_config()
    backend = config.get("backend")
    if backend == "from_scratch":
        return _translate_from_scratch(text, source_language, target_language, config)
    if backend in {"ctranslate2", "transformers"} and not config.get("allow_legacy_pretrained_backend"):
        return None
    if backend == "ctranslate2":
        return _translate_ctranslate2(text, source_language, target_language, config)
    if backend == "transformers":
        return _translate_transformers(text, source_language, target_language, config)
    return None


def _load_config() -> dict[str, object]:
    if not CONFIG_PATH.exists():
        return {}
    return json.loads(CONFIG_PATH.read_text(encoding="utf-8"))


def _translate_ctranslate2(
    text: str,
    source_language: str,
    target_language: str,
    config: dict[str, object],
) -> tuple[str, str] | None:
    try:
        import ctranslate2
        import sentencepiece as spm
    except Exception:
        return None

    direction = f"{source_language}__{target_language}"
    model_dir = Path(str(config.get("model_dir", MODEL_ROOT / "ctranslate2" / direction)))
    tokenizer_path = Path(str(config.get("tokenizer", model_dir / "sentencepiece.model")))
    if not model_dir.exists() or not tokenizer_path.exists():
        return None

    sp = spm.SentencePieceProcessor(model_file=str(tokenizer_path))
    translator = ctranslate2.Translator(str(model_dir), device=str(config.get("device", "cpu")))
    tokens = sp.encode(text, out_type=str)
    results = translator.translate_batch([tokens], beam_size=int(config.get("beam_size", 4)))
    output_tokens = results[0].hypotheses[0]
    return sp.decode(output_tokens), "ctranslate2"


def _translate_from_scratch(
    text: str,
    source_language: str,
    target_language: str,
    config: dict[str, object],
) -> tuple[str, str] | None:
    """Translate with the project-owned Transformer trained from zero."""
    try:
        from app.from_scratch_backend import translate_from_scratch
    except Exception:
        return None

    direction = f"{source_language}__{target_language}"
    model_dir = _resolve_path(config.get("model_dir", MODEL_ROOT / "from_scratch" / direction))
    tokenizer_path = _resolve_path(config.get("tokenizer", MODEL_ROOT / "from_scratch" / "tokenizer" / "tokenizer.model"))
    result = translate_from_scratch(
        text,
        source_language,
        target_language,
        model_dir=model_dir,
        tokenizer_path=tokenizer_path,
        device=str(config.get("device", "cpu")),
        max_new_tokens=int(config.get("max_new_tokens", 128)),
    )
    if result is None:
        return None
    return result, "from-scratch-transformer"


def _resolve_path(value: object) -> Path:
    path = Path(str(value))
    if path.is_absolute():
        return path
    return ROOT / path


def _translate_transformers(
    text: str,
    source_language: str,
    target_language: str,
    config: dict[str, object],
) -> tuple[str, str] | None:
    try:
        from transformers import AutoModelForSeq2SeqLM, AutoTokenizer
    except Exception:
        return None

    direction = f"{source_language}__{target_language}"
    model_dir = Path(str(config.get("model_dir", MODEL_ROOT / "neural" / direction / "final")))
    if not model_dir.exists():
        return None

    tokenizer = AutoTokenizer.from_pretrained(str(model_dir))
    model = AutoModelForSeq2SeqLM.from_pretrained(str(model_dir))
    inputs = tokenizer(text, return_tensors="pt", truncation=True)
    output = model.generate(**inputs, max_new_tokens=int(config.get("max_new_tokens", 256)))
    return tokenizer.decode(output[0], skip_special_tokens=True), "transformers"
