"""HTTP backend for AI-assistant-RAG.

The server exposes a small RAG API and serves the static frontend:
- GET  /                 -> chat UI
- POST /api/chat         -> retrieve context and return a grounded answer
- POST /api/search       -> retrieve matching document chunks only
- GET  /api/health       -> database status

The backend itself uses the standard HTTP library, while retrieval uses local
models installed inside this project.
"""

from __future__ import annotations

import json
import mimetypes
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path
from urllib.parse import unquote

from app.rag.models import embedding_available, generator_available, reranker_available
from app.rag.store import VectorStore, build_answer


ROOT = Path(__file__).resolve().parents[1]
FRONTEND_DIR = ROOT / "frontend"
DB_PATH = ROOT / "storage" / "vector_store.db"
DEFAULT_HOST = "127.0.0.1"
DEFAULT_PORT = 8010


class RAGRequestHandler(BaseHTTPRequestHandler):
    server_version = "AI-assistant-RAG/0.1"

    def do_GET(self) -> None:
        if self.path == "/api/health":
            self._handle_health()
            return
        self._serve_static()

    def do_POST(self) -> None:
        if self.path == "/api/chat":
            self._handle_chat()
            return
        if self.path == "/api/search":
            self._handle_search()
            return
        self._json_response({"error": "Not found"}, status=404)

    def _handle_health(self) -> None:
        store = VectorStore(DB_PATH)
        ready = DB_PATH.exists()
        chunks = store.count_chunks() if ready else 0
        self._json_response(
            {
                "ok": True,
                "ready": ready,
                "chunks": chunks,
                "embedding": embedding_available(),
                "reranker": reranker_available(),
                "generator": generator_available(),
                "retrieval": "dense+bm25+hyde+graph+cross_encoder",
            }
        )

    def _handle_chat(self) -> None:
        payload = self._read_json_body()
        question = str(payload.get("question", "")).strip()
        if not question:
            self._json_response({"error": "Question is required."}, status=400)
            return

        store = VectorStore(DB_PATH)
        if not DB_PATH.exists() or store.count_chunks() == 0:
            self._json_response(
                {
                    "error": "Vector database is empty. Run scripts/ingest_documents.py first.",
                },
                status=503,
            )
            return

        results = store.search(question, limit=int(payload.get("limit", 4)))
        answer = build_answer(question, results)
        self._json_response({"question": question, **answer})

    def _handle_search(self) -> None:
        payload = self._read_json_body()
        query = str(payload.get("query", "")).strip()
        if not query:
            self._json_response({"error": "Query is required."}, status=400)
            return

        store = VectorStore(DB_PATH)
        results = store.search(query, limit=int(payload.get("limit", 5)))
        self._json_response(
            {
                "query": query,
                "results": [
                    {
                        "document": item.document_name,
                        "score": round(item.score, 4),
                        "dense": round(item.dense_score, 4),
                        "bm25": round(item.bm25_score, 4),
                        "graph": round(item.graph_score, 4),
                        "rerank": round(item.rerank_score, 4),
                        "text": item.chunk_text,
                    }
                    for item in results
                ],
            }
        )

    def _read_json_body(self) -> dict:
        length = int(self.headers.get("content-length", "0") or "0")
        raw = self.rfile.read(length).decode("utf-8") if length else "{}"
        try:
            data = json.loads(raw)
        except json.JSONDecodeError:
            return {}
        return data if isinstance(data, dict) else {}

    def _serve_static(self) -> None:
        request_path = unquote(self.path.split("?", 1)[0])
        if request_path == "/":
            relative = "index.html"
        else:
            relative = request_path.lstrip("/")

        target = (FRONTEND_DIR / relative).resolve()
        frontend_root = FRONTEND_DIR.resolve()
        if frontend_root not in target.parents and target != frontend_root:
            self.send_error(403)
            return
        if not target.exists() or not target.is_file():
            self.send_error(404)
            return

        content_type, _ = mimetypes.guess_type(str(target))
        data = target.read_bytes()
        self.send_response(200)
        self.send_header("Content-Type", content_type or "application/octet-stream")
        self.send_header("Content-Length", str(len(data)))
        self.end_headers()
        self.wfile.write(data)

    def _json_response(self, payload: dict, *, status: int = 200) -> None:
        data = json.dumps(payload, ensure_ascii=False).encode("utf-8")
        self.send_response(status)
        self.send_header("Content-Type", "application/json; charset=utf-8")
        self.send_header("Content-Length", str(len(data)))
        self.end_headers()
        self.wfile.write(data)

    def log_message(self, format: str, *args) -> None:
        print(f"[server] {self.address_string()} - {format % args}")


def main() -> int:
    server = ThreadingHTTPServer((DEFAULT_HOST, DEFAULT_PORT), RAGRequestHandler)
    print(f"AI-assistant-RAG running at http://{DEFAULT_HOST}:{DEFAULT_PORT}")
    print(f"Vector database: {DB_PATH}")
    try:
        server.serve_forever()
    except KeyboardInterrupt:
        print("\nStopping server...")
    finally:
        server.server_close()
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
