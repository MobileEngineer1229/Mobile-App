"""Placeholder for converting translation models into mobile runtime format.

The final commands depend on the selected runtime:
- CTranslate2 for fast CPU inference in a Python/server prototype.
- ONNX Runtime Mobile or MLC/llama.cpp-style runtimes for native mobile.

Keep this script as the single entry point so future conversion steps are
repeatable and documented.
"""

from __future__ import annotations

import argparse
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--source", required=True, help="downloaded model directory")
    parser.add_argument("--output", default=str(ROOT / "models" / "text"), help="converted output directory")
    parser.add_argument("--runtime", default="ctranslate2", choices=["ctranslate2", "onnx", "native"])
    args = parser.parse_args()

    source = Path(args.source).resolve()
    output = Path(args.output).resolve()
    print("Translation model conversion plan")
    print(f"Source: {source}")
    print(f"Output: {output}")
    print(f"Runtime: {args.runtime}")
    print()
    print("Conversion command is not executed yet. Add the runtime-specific command here after model choice.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
