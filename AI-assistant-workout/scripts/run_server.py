r"""Run the AI-assistant-workout backend and frontend.

Purpose:
    Start the local HTTP server for testing the workout routine service.

Usage:
    python scripts\run_server.py

Before running:
    Run `python scripts\ingest_datasets.py` at least once so storage/workout.db
    contains the dataset-derived recommendation tables.
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
