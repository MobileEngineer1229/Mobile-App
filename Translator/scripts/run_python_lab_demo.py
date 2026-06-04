"""Run a tiny local text-translation demo through the DPRK post-edit layer.

This does not claim to be a real neural translator. It exists so glossary and
style rules can be tested before the mobile native model bridge is connected.
"""

from __future__ import annotations

import argparse
import sys
from pathlib import Path


if hasattr(sys.stdout, "reconfigure"):
    sys.stdout.reconfigure(encoding="utf-8")

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / "python_lab"))

from app.translator_service import translate_for_demo  # noqa: E402


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--source", default="en")
    parser.add_argument("--target", default="ko_kp")
    parser.add_argument("--text", required=True)
    args = parser.parse_args()

    print(translate_for_demo(args.text, args.source, args.target))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
