"""Prepare a local wheelhouse for offline package installation.

The project uses `third_party/wheels/` as its local package cache. Run this
script while internet access is available, then future offline machines can
install with:

    python -m pip install --no-index --find-links third_party/wheels -r requirements-training-lite.txt

Heavy CUDA packages such as torch should be handled separately because their
wheels are large and often depend on the exact Python/CUDA setup.
"""

from __future__ import annotations

import argparse
import subprocess
import sys
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
WHEELHOUSE = ROOT / "third_party" / "wheels"
LITE_REQUIREMENTS = ROOT / "requirements-training-lite.txt"


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--requirements", default=str(LITE_REQUIREMENTS))
    parser.add_argument("--wheelhouse", default=str(WHEELHOUSE))
    args = parser.parse_args()

    wheelhouse = Path(args.wheelhouse)
    wheelhouse.mkdir(parents=True, exist_ok=True)
    _run(
        [
            sys.executable,
            "-m",
            "pip",
            "download",
            "--dest",
            str(wheelhouse),
            "-r",
            args.requirements,
        ]
    )
    print(f"Offline wheelhouse prepared: {wheelhouse}")
    return 0


def _run(command: list[str]) -> None:
    print(f"$ {' '.join(command)}")
    subprocess.run(command, cwd=ROOT, check=True)


if __name__ == "__main__":
    raise SystemExit(main())

