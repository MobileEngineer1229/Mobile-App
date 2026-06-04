"""Create a local editable model manifest from the example manifest.

The generated file is ignored by git so each developer can point to local
offline model files without changing shared project configuration.
"""

from __future__ import annotations

import argparse
import shutil
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
EXAMPLE = ROOT / "models" / "manifest.example.json"
LOCAL = ROOT / "models" / "manifest.local.json"


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument(
        "--force",
        action="store_true",
        help="overwrite models/manifest.local.json if it already exists",
    )
    args = parser.parse_args()

    if LOCAL.exists() and not args.force:
        print(f"Already exists: {LOCAL}")
        print("Use --force if you want to recreate it from the example.")
        return 0

    shutil.copyfile(EXAMPLE, LOCAL)
    print(f"Created: {LOCAL}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
