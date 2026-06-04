"""Build the offline advanced RAG database from project documents.

Purpose:
    Read supported files from the `documents/` folder, split them into chunks,
    embed each chunk with the local multilingual model in `models/embedding`,
    and save vectors plus lexical tokens in `storage/vector_store.db`.

Inputs:
    documents/*.txt, *.md, *.json, *.jsonl

Outputs:
    storage/vector_store.db

Notes:
    Keep this script in `scripts/` because it is an operational utility. It is
    safe to rerun whenever documents change; the database is rebuilt from
    scratch to avoid stale vectors.
"""

from __future__ import annotations

import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))

from app.rag.models import encode_passages, embedding_available  # noqa: E402
from app.rag.store import VectorStore  # noqa: E402
from app.rag.text import iter_document_paths, read_document, split_into_chunks  # noqa: E402


DOCUMENTS_DIR = ROOT / "documents"
DB_PATH = ROOT / "storage" / "vector_store.db"


def main() -> int:
    if not embedding_available():
        print("[error] Missing local embedding model at models/embedding.", file=sys.stderr)
        print("        Run: .\\.venv\\Scripts\\python.exe scripts\\download_models.py", file=sys.stderr)
        return 1

    store = VectorStore(DB_PATH)
    store.initialize(reset=True)

    total_docs = 0
    total_chunks = 0
    indexed: list[tuple[Path, list[str]]] = []

    for path in iter_document_paths(DOCUMENTS_DIR):
        text = read_document(path)
        chunks = split_into_chunks(text)
        if not chunks:
            print(f"[skip] {path.relative_to(ROOT)} has no readable text")
            continue
        indexed.append((path, chunks))
        total_docs += 1
        total_chunks += len(chunks)

    if total_chunks == 0:
        print("[error] No chunks were indexed. Add documents and rerun this script.", file=sys.stderr)
        return 1

    all_chunks = [chunk for _, chunks in indexed for chunk in chunks]
    print(f"[embed] encoding {len(all_chunks)} chunk(s) with local embedding model...")
    all_vectors = encode_passages(all_chunks)
    if len(all_vectors) != len(all_chunks):
        print("[error] Embedding failed. Check models/embedding.", file=sys.stderr)
        return 1

    offset = 0
    for path, chunks in indexed:
        vectors = all_vectors[offset : offset + len(chunks)]
        store.add_document(path, chunks, vectors=vectors)
        offset += len(chunks)
        print(f"[ok] {path.relative_to(ROOT)} -> {len(chunks)} chunk(s)")

    print()
    print(f"Indexed documents : {total_docs}")
    print(f"Indexed chunks    : {total_chunks}")
    print("Retrieval stack   : dense + BM25 + HyDE + graph + reranker")
    print(f"Vector database   : {DB_PATH}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
