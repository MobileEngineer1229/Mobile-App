"""Import a paired URL set and rebuild training artifacts.

Example:

    python scripts/learn_from_url_pair.py --source-url "..." --target-url "..."

By default the script creates an alignment candidate file that must be reviewed
before it becomes training data. Use `--approve-exact-order` only when both
pages are known to have matching sentence order.
"""

from __future__ import annotations

import argparse
import subprocess
import sys
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--source-url", required=True)
    parser.add_argument("--target-url", required=True)
    parser.add_argument("--source-language", default="ko_kp")
    parser.add_argument("--target-language", default="en")
    parser.add_argument("--pair-id")
    parser.add_argument("--approve-exact-order", action="store_true")
    args = parser.parse_args()

    command = [
        sys.executable,
        "scripts/import_url_pair_corpus.py",
        "--source-url",
        args.source_url,
        "--target-url",
        args.target_url,
        "--source-language",
        args.source_language,
        "--target-language",
        args.target_language,
    ]
    if args.pair_id:
        command.extend(["--pair-id", args.pair_id])
    if args.approve_exact_order:
        command.append("--approve-exact-order")

    _run(command)
    _run([sys.executable, "scripts/bootstrap_data_pipeline.py"])
    return 0


def _run(command: list[str]) -> None:
    print(f"\n$ {' '.join(command)}")
    subprocess.run(command, cwd=ROOT, check=True)


if __name__ == "__main__":
    raise SystemExit(main())
