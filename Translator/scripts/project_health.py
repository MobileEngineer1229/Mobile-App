"""Run the main project checks in one place."""

from __future__ import annotations

import subprocess
import sys
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]


def main() -> int:
    commands = [
        [sys.executable, "-m", "compileall", "-q", "scripts", "python_lab", "web_service"],
        [sys.executable, "scripts/check_training_dependencies.py"],
        [sys.executable, "scripts/corpus_status.py"],
        [sys.executable, "scripts/train_tokenizer.py", "--help"],
        [sys.executable, "scripts/train_transformer_from_scratch.py", "--help"],
        [sys.executable, "scripts/evaluate_translation_quality.py"],
    ]
    failures = 0
    for command in commands:
        print(f"\n$ {' '.join(command)}")
        result = subprocess.run(command, cwd=ROOT, check=False)
        if result.returncode != 0:
            failures += 1
            print(f"FAILED: {' '.join(command)}")
    return 1 if failures else 0


if __name__ == "__main__":
    raise SystemExit(main())
