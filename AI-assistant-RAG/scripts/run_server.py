r"""Run the AI-assistant-RAG local web server.

Purpose:
    Start the offline RAG backend and serve the frontend UI.

Before running:
    1. Create/install the project virtual environment.
    2. Download local models with scripts/download_models.py.
    3. Build the vector database with scripts/ingest_documents.py.

Usage:
    .\.venv\Scripts\python.exe scripts\run_server.py
"""

from __future__ import annotations

import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))

from app.main import main  # noqa: E402


if __name__ == "__main__":
    raise SystemExit(main())
