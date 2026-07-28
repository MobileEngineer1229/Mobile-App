"""Text processing helpers for the local RAG pipeline.

This module intentionally avoids heavyweight dependencies. It provides:
- plain text extraction helpers for simple document formats,
- chunking that keeps nearby sentences together,
- hashed bag-of-words vectors for local similarity search.

The vector representation is simple, deterministic, and good enough for a first
offline RAG prototype. A later version can replace this file with transformer
embeddings without changing the API shape of the vector store.
"""

from __future__ import annotations

import hashlib
import json
import math
import re
from pathlib import Path
from typing import Iterable


VECTOR_DIM = 512
SUPPORTED_SUFFIXES = {".txt", ".md", ".json", ".jsonl"}
TOKEN_RE = re.compile(r"[\wgo-Hehe]+", re.UNICODE)
SENTENCE_RE = re.compile(r"(?<=[.!?。！？])\s+|\n{2,}")


def normalize_text(text: str) -> str:
    """Collapse noisy whitespace while preserving paragraph boundaries."""
    lines = [" ".join(line.split()) for line in text.replace("\r\n", "\n").split("\n")]
    paragraphs = [line for line in lines if line]
    return "\n".join(paragraphs)


def tokenize(text: str) -> list[str]:
    """Return lowercase tokens plus Korean character n-grams.

    Korean particles and endings often attach directly to nouns/verbs, so exact
    word matching is brittle. Character bigrams/trigrams make queries such as
    "The document is" still match document text containing "document".
    """
    out: list[str] = []
    for match in TOKEN_RE.finditer(text):
        token = match.group(0).lower()
        out.append(token)
        if any("go" <= char <= "Hehe" for char in token):
            for size in (2, 3):
                for start in range(0, max(0, len(token) - size + 1)):
                    out.append(token[start : start + size])
    return out


def vectorize(text: str, dim: int = VECTOR_DIM) -> list[float]:
    """Create a normalized hashed bag-of-words vector.

    Each token is hashed into a fixed-size vector. Counts are log-scaled and the
    final vector is L2-normalized so dot product equals cosine similarity.
    """
    values = [0.0] * dim
    for token in tokenize(text):
        digest = hashlib.blake2b(token.encode("utf-8"), digest_size=8).digest()
        bucket = int.from_bytes(digest[:4], "big") % dim
        values[bucket] += 1.0

    for i, value in enumerate(values):
        if value > 0:
            values[i] = math.log1p(value)
        elif value < 0:
            values[i] = -math.log1p(abs(value))

    norm = math.sqrt(sum(v * v for v in values))
    if norm == 0:
        return values
    return [v / norm for v in values]


def cosine_similarity(left: list[float], right: list[float]) -> float:
    """Return cosine similarity for normalized vectors."""
    return sum(a * b for a, b in zip(left, right))


def split_into_chunks(text: str, *, max_tokens: int = 180) -> list[str]:
    """Split text into retrieval chunks while preserving original text only."""
    clean = normalize_text(text)
    if not clean:
        return []

    pieces = [p.strip() for p in SENTENCE_RE.split(clean) if p.strip()]
    chunks: list[str] = []
    current: list[str] = []
    current_tokens = 0

    for piece in pieces:
        piece_tokens = len(tokenize(piece))
        if current and current_tokens + piece_tokens > max_tokens:
            chunk = " ".join(current).strip()
            chunks.append(chunk)
            current = []
            current_tokens = 0
        current.append(piece)
        current_tokens += piece_tokens

    if current:
        chunk = " ".join(current).strip()
        if chunk:
            chunks.append(chunk)
    return chunks


def read_document(path: Path) -> str:
    """Read supported documents as text.

    JSON and JSONL files support common fields such as text, content, body,
    question, and answer. Unknown JSON structures are serialized as readable
    text rather than failing.
    """
    suffix = path.suffix.lower()
    if suffix in {".txt", ".md"}:
        return path.read_text(encoding="utf-8", errors="replace")
    if suffix == ".json":
        data = json.loads(path.read_text(encoding="utf-8", errors="replace"))
        return "\n".join(_extract_json_text(data))
    if suffix == ".jsonl":
        rows: list[str] = []
        with path.open("r", encoding="utf-8", errors="replace") as handle:
            for line in handle:
                line = line.strip()
                if not line:
                    continue
                try:
                    rows.extend(_extract_json_text(json.loads(line)))
                except json.JSONDecodeError:
                    continue
        return "\n".join(rows)
    return ""


def iter_document_paths(documents_dir: Path) -> Iterable[Path]:
    """Yield supported document paths in stable order."""
    for path in sorted(documents_dir.rglob("*")):
        if path.is_file() and path.suffix.lower() in SUPPORTED_SUFFIXES:
            yield path


def _extract_json_text(data) -> list[str]:
    """Extract useful text from JSON-like data."""
    if isinstance(data, str):
        return [data]
    if isinstance(data, list):
        out: list[str] = []
        for item in data:
            out.extend(_extract_json_text(item))
        return out
    if isinstance(data, dict):
        if "question" in data and "answer" in data:
            return [f"Question: {data['question']}\nAnswer: {data['answer']}"]
        for key in ("text", "content", "body", "passage", "context", "description"):
            value = data.get(key)
            if isinstance(value, str) and value.strip():
                return [value]
        return [json.dumps(data, ensure_ascii=False)]
    return [str(data)]
