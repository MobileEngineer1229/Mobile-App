"""Advanced offline RAG store.

The retrieval stack is intentionally local:
- multilingual dense embeddings from `models/embedding`,
- BM25 sparse retrieval,
- HyDE-style query expansion from initial lexical evidence,
- lightweight graph expansion from document term co-occurrence,
- cross-encoder reranking from `models/reranker`.
"""

from __future__ import annotations

import math
import json
import sqlite3
from collections import Counter
from dataclasses import dataclass
from pathlib import Path

from .models import encode_query, generate_grounded_answer, rerank_pairs
from .text import VECTOR_DIM, cosine_similarity, tokenize, vectorize


@dataclass
class SearchResult:
    chunk_id: int
    document_name: str
    chunk_text: str
    score: float
    dense_score: float = 0.0
    bm25_score: float = 0.0
    graph_score: float = 0.0
    rerank_score: float = 0.0


class VectorStore:
    def __init__(self, db_path: str | Path):
        self.db_path = Path(db_path)
        self.db_path.parent.mkdir(parents=True, exist_ok=True)

    def connect(self) -> sqlite3.Connection:
        conn = sqlite3.connect(self.db_path)
        conn.row_factory = sqlite3.Row
        return conn

    def initialize(self, *, reset: bool = False) -> None:
        """Create tables, optionally clearing previous vectors."""
        with self.connect() as conn:
            if reset:
                conn.execute("DROP TABLE IF EXISTS chunks")
                conn.execute("DROP TABLE IF EXISTS documents")
                conn.execute("DROP TABLE IF EXISTS metadata")
            conn.execute(
                """
                CREATE TABLE IF NOT EXISTS documents (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    path TEXT NOT NULL UNIQUE,
                    name TEXT NOT NULL,
                    mtime REAL NOT NULL
                )
                """
            )
            conn.execute(
                """
                CREATE TABLE IF NOT EXISTS chunks (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    document_id INTEGER NOT NULL,
                    chunk_index INTEGER NOT NULL,
                    text TEXT NOT NULL,
                    vector_json TEXT NOT NULL,
                    token_json TEXT NOT NULL,
                    FOREIGN KEY (document_id) REFERENCES documents(id)
                )
                """
            )
            conn.execute(
                """
                CREATE TABLE IF NOT EXISTS metadata (
                    key TEXT PRIMARY KEY,
                    value TEXT NOT NULL
                )
                """
            )
            conn.execute(
                "INSERT OR REPLACE INTO metadata (key, value) VALUES (?, ?)",
                ("vector_dim", str(VECTOR_DIM)),
            )
            conn.execute(
                "INSERT OR REPLACE INTO metadata (key, value) VALUES (?, ?)",
                ("retrieval_stack", "dense+bm25+hyde+graph+cross_encoder"),
            )

    def add_document(self, path: Path, chunks: list[str], vectors: list[list[float]] | None = None) -> int:
        """Insert one document and all of its vectorized chunks."""
        stat = path.stat()
        with self.connect() as conn:
            cur = conn.execute(
                "INSERT INTO documents (path, name, mtime) VALUES (?, ?, ?)",
                (str(path), path.name, stat.st_mtime),
            )
            doc_id = int(cur.lastrowid)
            for index, chunk in enumerate(chunks):
                vector = vectors[index] if vectors else vectorize(chunk)
                tokens = tokenize(chunk)
                conn.execute(
                    """
                    INSERT INTO chunks (document_id, chunk_index, text, vector_json, token_json)
                    VALUES (?, ?, ?, ?, ?)
                    """,
                    (
                        doc_id,
                        index,
                        chunk,
                        json.dumps(vector, separators=(",", ":")),
                        json.dumps(tokens, ensure_ascii=False, separators=(",", ":")),
                    ),
                )
            return doc_id

    def count_chunks(self) -> int:
        with self.connect() as conn:
            row = conn.execute("SELECT COUNT(*) AS count FROM chunks").fetchone()
            return int(row["count"])

    def search(self, query: str, *, limit: int = 4) -> list[SearchResult]:
        """Return top matching chunks using hybrid retrieval and reranking."""
        with self.connect() as conn:
            rows = conn.execute(
                """
                SELECT chunks.id, documents.name, chunks.text, chunks.vector_json, chunks.token_json
                FROM chunks
                JOIN documents ON documents.id = chunks.document_id
                """
            ).fetchall()

        if not rows:
            return []

        docs = [_row_to_doc(row) for row in rows]
        query_terms = tokenize(query)
        idf = _build_idf([doc["tokens"] for doc in docs])
        avg_len = sum(len(doc["tokens"]) for doc in docs) / max(1, len(docs))

        initial_bm25 = [
            _bm25_score(query_terms, doc["tokens"], idf, avg_len)
            for doc in docs
        ]
        pseudo_context = _make_pseudo_context(docs, initial_bm25)
        query_vector = encode_query(query, pseudo_context) or vectorize(f"{query}\n{pseudo_context}")

        expanded_terms = _expand_terms_from_graph(query_terms, docs)
        dense_scores: list[float] = []
        graph_scores: list[float] = []
        late_scores: list[float] = []
        for doc in docs:
            dense_scores.append(cosine_similarity(query_vector, doc["vector"]))
            graph_scores.append(_graph_score(expanded_terms, doc["tokens"]))
            late_scores.append(_late_interaction_score(query_terms, doc["tokens"], idf))

        bm25_norm = _minmax(initial_bm25)
        dense_norm = _minmax(dense_scores)
        graph_norm = _minmax(graph_scores)
        late_norm = _minmax(late_scores)

        candidates: list[SearchResult] = []
        for index, doc in enumerate(docs):
            candidate_score = (
                0.46 * dense_norm[index]
                + 0.30 * bm25_norm[index]
                + 0.14 * graph_norm[index]
                + 0.10 * late_norm[index]
            )
            if candidate_score <= 0:
                continue
            candidates.append(
                SearchResult(
                    chunk_id=doc["id"],
                    document_name=doc["name"],
                    chunk_text=doc["text"],
                    score=float(candidate_score),
                    dense_score=float(dense_scores[index]),
                    bm25_score=float(initial_bm25[index]),
                    graph_score=float(graph_scores[index]),
                )
            )

        candidates.sort(key=lambda item: item.score, reverse=True)
        candidates = candidates[: max(limit * 6, 24)]
        rerank_scores = rerank_pairs(query, [item.chunk_text for item in candidates])
        if rerank_scores:
            rerank_norm = _minmax(rerank_scores)
            for item, raw_rerank, norm_rerank in zip(candidates, rerank_scores, rerank_norm):
                item.rerank_score = raw_rerank
                item.score = 0.58 * norm_rerank + 0.42 * item.score
            candidates.sort(key=lambda item: item.score, reverse=True)

        return candidates[:limit]


def build_answer(question: str, results: list[SearchResult]) -> dict:
    """Create a short grounded answer from retrieved chunks.

    This is deliberately extractive. It avoids pretending to know more than the
    indexed documents contain, and it returns sources so the frontend can show
    where the answer came from.
    """
    if not results:
        return {
            "answer": "색인된 문서에서 이 질문에 답할 충분한 근거를 찾지 못했습니다.",
            "sources": [],
        }

    best = results[0]
    supporting = results[1:3]
    generated = generate_grounded_answer(question, [result.chunk_text for result in results])
    answer_parts = [
        "색인된 문서에 따르면:",
        generated or _shorten(best.chunk_text, 520),
    ]
    if supporting:
        answer_parts.append("관련 근거: " + " ".join(_shorten(r.chunk_text, 180) for r in supporting))

    return {
        "answer": "\n\n".join(answer_parts),
        "sources": [
            {
                "document": result.document_name,
                "score": round(result.score, 4),
                "dense": round(result.dense_score, 4),
                "bm25": round(result.bm25_score, 4),
                "graph": round(result.graph_score, 4),
                "rerank": round(result.rerank_score, 4),
                "preview": _shorten(result.chunk_text, 220),
            }
            for result in results
        ],
    }


def _shorten(text: str, max_chars: int) -> str:
    text = " ".join(text.split())
    if len(text) <= max_chars:
        return text
    return text[: max_chars - 3].rstrip() + "..."


def _row_to_doc(row: sqlite3.Row) -> dict:
    return {
        "id": int(row["id"]),
        "name": str(row["name"]),
        "text": str(row["text"]),
        "vector": json.loads(row["vector_json"]),
        "tokens": json.loads(row["token_json"]),
    }


def _build_idf(corpus_tokens: list[list[str]]) -> dict[str, float]:
    total_docs = max(1, len(corpus_tokens))
    df: Counter[str] = Counter()
    for tokens in corpus_tokens:
        df.update(set(tokens))
    return {
        term: math.log(1 + (total_docs - freq + 0.5) / (freq + 0.5))
        for term, freq in df.items()
    }


def _bm25_score(query_terms: list[str], doc_terms: list[str], idf: dict[str, float], avg_len: float) -> float:
    if not query_terms or not doc_terms:
        return 0.0
    counts = Counter(doc_terms)
    doc_len = len(doc_terms)
    k1 = 1.5
    b = 0.75
    score = 0.0
    for term in query_terms:
        tf = counts.get(term, 0)
        if tf == 0:
            continue
        denom = tf + k1 * (1 - b + b * doc_len / max(avg_len, 1.0))
        score += idf.get(term, 0.0) * (tf * (k1 + 1)) / denom
    return score


def _make_pseudo_context(docs: list[dict], scores: list[float], *, max_chars: int = 800) -> str:
    ranked = sorted(zip(scores, docs), key=lambda item: item[0], reverse=True)
    snippets = [doc["text"] for score, doc in ranked[:3] if score > 0]
    return _shorten(" ".join(snippets), max_chars)


def _expand_terms_from_graph(query_terms: list[str], docs: list[dict]) -> set[str]:
    """Approximate GraphRAG expansion via chunk-level term co-occurrence."""
    seeds = set(query_terms)
    related: Counter[str] = Counter()
    for doc in docs:
        tokens = doc["tokens"]
        token_set = set(tokens)
        if not seeds.intersection(token_set):
            continue
        related.update(token for token in token_set if len(token) > 1 and token not in seeds)
    return set(seeds).union(term for term, _ in related.most_common(24))


def _graph_score(expanded_terms: set[str], doc_terms: list[str]) -> float:
    if not expanded_terms or not doc_terms:
        return 0.0
    overlap = expanded_terms.intersection(set(doc_terms))
    return len(overlap) / math.sqrt(len(expanded_terms) * max(1, len(set(doc_terms))))


def _late_interaction_score(query_terms: list[str], doc_terms: list[str], idf: dict[str, float]) -> float:
    """Token-level max-sim score inspired by late-interaction retrieval."""
    if not query_terms or not doc_terms:
        return 0.0
    doc_set = set(doc_terms)
    score = 0.0
    for term in query_terms:
        if term in doc_set:
            score += idf.get(term, 0.0)
            continue
        if len(term) <= 2:
            continue
        score += 0.25 * max(
            (idf.get(doc_term, 0.0) for doc_term in doc_set if term in doc_term or doc_term in term),
            default=0.0,
        )
    return score


def _minmax(values: list[float]) -> list[float]:
    if not values:
        return []
    low = min(values)
    high = max(values)
    if high <= low:
        return [1.0 if value > 0 else 0.0 for value in values]
    return [(value - low) / (high - low) for value in values]
