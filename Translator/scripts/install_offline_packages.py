"""Install training packages from the local wheelhouse.

This script does not contact the internet. It installs packages only from
`third_party/wheels/`.
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

    _run(
        [
            sys.executable,
            "-m",
            "pip",
            "install",
            "--no-index",
            "--find-links",
            args.wheelhouse,
            "-r",
            args.requirements,
        ]
    )
    return 0


def _run(command: list[str]) -> None:
    print(f"$ {' '.join(command)}")
    subprocess.run(command, cwd=ROOT, check=True)


if __name__ == "__main__":
    raise SystemExit(main())

