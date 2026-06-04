"""HTTP backend for AI-assistant-workout.

Endpoints:
    GET  /                  Frontend UI
    GET  /api/health        Database/service status
    GET  /api/options       Form option values
    POST /api/routine       Create personalized workout routine
    POST /api/feedback      Save user feedback for future tuning

The server uses Python's standard library so it can run easily during early
mobile-app development. A future mobile app can call the same JSON APIs.
"""

from __future__ import annotations

import json
import mimetypes
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path
from urllib.parse import unquote

from app.services.recommender import build_routine, database_ready, feedback_summary, plan_model_ready, save_feedback


ROOT = Path(__file__).resolve().parents[1]
FRONTEND_DIR = ROOT / "frontend"
DEFAULT_HOST = "127.0.0.1"
DEFAULT_PORT = 8020


class WorkoutHandler(BaseHTTPRequestHandler):
    server_version = "AI-assistant-workout/0.1"

    def do_GET(self) -> None:
        if self.path == "/api/health":
            self._json(
                {
                    "ok": True,
                    "database_ready": database_ready(),
                    "plan_model_ready": plan_model_ready(),
                    "feedback": feedback_summary(),
                }
            )
            return
        if self.path == "/api/options":
            self._json(options_payload())
            return
        self._serve_static()

    def do_POST(self) -> None:
        if self.path == "/api/routine":
            self._handle_routine()
            return
        if self.path == "/api/feedback":
            self._handle_feedback()
            return
        self._json({"error": "Not found"}, status=404)

    def _handle_routine(self) -> None:
        if not database_ready():
            self._json({"error": "Database is not ready. Run scripts/ingest_datasets.py first."}, status=503)
            return
        payload = self._read_json()
        try:
            routine = build_routine(payload, save=True)
        except Exception as exc:
            self._json({"error": f"Could not build routine: {exc}"}, status=400)
            return
        self._json(routine)

    def _handle_feedback(self) -> None:
        payload = self._read_json()
        try:
            result = save_feedback(payload)
        except Exception as exc:
            self._json({"error": f"Could not save feedback: {exc}"}, status=400)
            return
        self._json(result)

    def _read_json(self) -> dict:
        length = int(self.headers.get("content-length", "0") or "0")
        raw = self.rfile.read(length).decode("utf-8") if length else "{}"
        try:
            data = json.loads(raw)
        except json.JSONDecodeError:
            return {}
        return data if isinstance(data, dict) else {}

    def _serve_static(self) -> None:
        request_path = unquote(self.path.split("?", 1)[0])
        relative = "index.html" if request_path == "/" else request_path.lstrip("/")
        target = (FRONTEND_DIR / relative).resolve()
        root = FRONTEND_DIR.resolve()
        if root not in target.parents and target != root:
            self.send_error(403)
            return
        if not target.exists() or not target.is_file():
            self.send_error(404)
            return
        data = target.read_bytes()
        content_type, _ = mimetypes.guess_type(str(target))
        self.send_response(200)
        self.send_header("Content-Type", content_type or "application/octet-stream")
        self.send_header("Content-Length", str(len(data)))
        self.end_headers()
        self.wfile.write(data)

    def _json(self, payload: dict, *, status: int = 200) -> None:
        data = json.dumps(payload, ensure_ascii=False).encode("utf-8")
        self.send_response(status)
        self.send_header("Content-Type", "application/json; charset=utf-8")
        self.send_header("Content-Length", str(len(data)))
        self.end_headers()
        self.wfile.write(data)

    def log_message(self, format: str, *args) -> None:
        print(f"[server] {self.address_string()} - {format % args}")


def options_payload() -> dict:
    return {
        "goals": [
            {"value": "fat_loss", "label": "Fat loss"},
            {"value": "muscle_gain", "label": "Muscle gain"},
            {"value": "endurance", "label": "Endurance"},
            {"value": "mobility", "label": "Mobility"},
            {"value": "general", "label": "General fitness"},
        ],
        "fitness_levels": ["Beginner", "Intermediate", "Advanced"],
        "equipment": [
            {"value": "bodyweight", "label": "Bodyweight/home"},
            {"value": "dumbbells", "label": "Dumbbells"},
            {"value": "gym", "label": "Gym"},
        ],
    }


def main() -> int:
    server = ThreadingHTTPServer((DEFAULT_HOST, DEFAULT_PORT), WorkoutHandler)
    print(f"AI-assistant-workout running at http://{DEFAULT_HOST}:{DEFAULT_PORT}")
    try:
        server.serve_forever()
    except KeyboardInterrupt:
        print("\nStopping server...")
    finally:
        server.server_close()
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
