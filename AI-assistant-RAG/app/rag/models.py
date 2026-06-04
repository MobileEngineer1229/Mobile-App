"""Local neural model loading for offline RAG.

Models are loaded only from the project `models/` folder. The loader sets
Hugging Face offline flags before importing model classes so runtime requests do
not reach the network.
"""

from __future__ import annotations

import os
from pathlib import Path
from typing import Iterable


ROOT = Path(__file__).resolve().parents[2]
MODELS_DIR = ROOT / "models"
EMBEDDING_PATH = MODELS_DIR / "embedding"
RERANKER_PATH = MODELS_DIR / "reranker"
GENERATOR_PATH = MODELS_DIR / "generator"

_embedding_model = None
_reranker_model = None
_generator_model = None
_generator_tokenizer = None


def _enable_offline_mode() -> None:
    os.environ.setdefault("HF_HUB_OFFLINE", "1")
    os.environ.setdefault("TRANSFORMERS_OFFLINE", "1")
    os.environ.setdefault("TOKENIZERS_PARALLELISM", "false")


def embedding_available() -> bool:
    return EMBEDDING_PATH.exists() and (EMBEDDING_PATH / "config.json").exists()


def reranker_available() -> bool:
    return RERANKER_PATH.exists() and (RERANKER_PATH / "config.json").exists()


def generator_available() -> bool:
    return GENERATOR_PATH.exists() and (GENERATOR_PATH / "config.json").exists()


def load_embedding_model():
    """Load the local multilingual embedding model, or return None."""
    global _embedding_model
    if _embedding_model is not None:
        return _embedding_model
    if not embedding_available():
        return None
    _enable_offline_mode()
    from sentence_transformers import SentenceTransformer

    _embedding_model = SentenceTransformer(str(EMBEDDING_PATH), device="cpu")
    return _embedding_model


def load_reranker_model():
    """Load the local cross-encoder reranker, or return None."""
    global _reranker_model
    if _reranker_model is not None:
        return _reranker_model
    if not reranker_available():
        return None
    _enable_offline_mode()
    from sentence_transformers import CrossEncoder

    _reranker_model = CrossEncoder(str(RERANKER_PATH), device="cpu")
    return _reranker_model


def encode_passages(texts: Iterable[str]) -> list[list[float]]:
    """Encode passages with E5-style prefixes."""
    model = load_embedding_model()
    if model is None:
        return []
    prepared = [f"passage: {text}" for text in texts]
    vectors = model.encode(
        prepared,
        batch_size=16,
        normalize_embeddings=True,
        show_progress_bar=False,
    )
    return [vector.astype(float).tolist() for vector in vectors]


def encode_query(question: str, pseudo_context: str = "") -> list[float] | None:
    """Encode a user query, optionally enriched with HyDE-style pseudo context."""
    model = load_embedding_model()
    if model is None:
        return None
    text = question if not pseudo_context else f"{question}\n{pseudo_context}"
    vector = model.encode(
        f"query: {text}",
        normalize_embeddings=True,
        show_progress_bar=False,
    )
    return vector.astype(float).tolist()


def rerank_pairs(question: str, passages: list[str]) -> list[float]:
    """Return cross-encoder scores for question/passage pairs."""
    model = load_reranker_model()
    if model is None or not passages:
        return []
    scores = model.predict([(question, passage) for passage in passages], show_progress_bar=False)
    return [float(score) for score in scores]


def generate_grounded_answer(question: str, contexts: list[str]) -> str | None:
    """Generate a concise answer from retrieved contexts with a local LLM."""
    if not generator_available() or not contexts:
        return None
    tokenizer, model = _load_generator()
    context_text = "\n\n".join(f"[{idx + 1}] {text}" for idx, text in enumerate(contexts[:4]))
    messages = [
        {
            "role": "system",
            "content": (
                "You are an offline RAG assistant. Answer only from the provided context. "
                "If the context is insufficient, say that the indexed documents do not contain enough evidence. "
                "Do not invent examples, commands, files, or steps. "
                "Keep the answer in 2-5 sentences and include source numbers like [1]."
            ),
        },
        {
            "role": "user",
            "content": f"Question:\n{question}\n\nContext:\n{context_text}\n\nAnswer in the user's language.",
        },
    ]
    prompt = tokenizer.apply_chat_template(messages, tokenize=False, add_generation_prompt=True)
    inputs = tokenizer(prompt, return_tensors="pt")
    output = model.generate(
        **inputs,
        max_new_tokens=150,
        do_sample=False,
        repetition_penalty=1.08,
        pad_token_id=tokenizer.eos_token_id,
    )
    generated = output[0][inputs["input_ids"].shape[-1] :]
    text = tokenizer.decode(generated, skip_special_tokens=True).strip()
    if not text or not _looks_grounded(text):
        return None
    return text


def _load_generator():
    global _generator_model, _generator_tokenizer
    if _generator_model is not None and _generator_tokenizer is not None:
        return _generator_tokenizer, _generator_model
    _enable_offline_mode()
    from transformers import AutoModelForCausalLM, AutoTokenizer

    _generator_tokenizer = AutoTokenizer.from_pretrained(str(GENERATOR_PATH), local_files_only=True)
    _generator_model = AutoModelForCausalLM.from_pretrained(str(GENERATOR_PATH), local_files_only=True)
    _generator_model.eval()
    return _generator_tokenizer, _generator_model


def _looks_grounded(answer: str) -> bool:
    """Reject local LLM answers that ignore citation instructions."""
    return "[1]" in answer or "[2]" in answer or "[3]" in answer or "[4]" in answer
