"""Start the dependency-free local web UI for the Translator service.

Run this script from the project root:

    python scripts/run_web_service.py

The service is offline. It uses the current demo engine until a neural
translation model is downloaded and connected.
"""

from __future__ import annotations

import argparse
import sys
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT))

from web_service.server import run  # noqa: E402


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--host", default="127.0.0.1")
    parser.add_argument("--port", type=int, default=8765)
    args = parser.parse_args()

    run(args.host, args.port)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
