"""Run the full local data-preparation pipeline after dropping in new data.

This is the command to use during daily corpus building:

    python scripts/bootstrap_data_pipeline.py

It prepares folders, imports raw monolingual text, imports reviewed parallel
pairs, validates the parallel corpus when present, builds translation memory,
and creates train/dev/test splits.
"""

from __future__ import annotations

import subprocess
import sys
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
PARALLEL = ROOT / "data" / "corpus" / "parallel" / "reviewed_parallel.tsv"


def main() -> int:
    commands = [
        [sys.executable, "scripts/prepare_corpus_workspace.py"],
        [sys.executable, "scripts/import_monolingual_corpus.py", "--language", "ko_kp"],
        [sys.executable, "scripts/import_parallel_corpus.py"],
    ]

    for command in commands:
        _run(command)

    if PARALLEL.exists():
        _run([sys.executable, "scripts/validate_parallel_corpus.py", str(PARALLEL)], check=False)
        _run([sys.executable, "scripts/build_translation_memory.py", str(PARALLEL)])

    _run([sys.executable, "scripts/build_training_splits.py"])
    _run([sys.executable, "scripts/train_local_models.py"])
    _run([sys.executable, "scripts/export_neural_training_data.py"])
    _run([sys.executable, "scripts/extract_terms_and_phrases.py"])
    _run([sys.executable, "scripts/evaluate_translation_quality.py"], check=False)
    return 0


def _run(command: list[str], *, check: bool = True) -> None:
    print(f"\n$ {' '.join(command)}")
    result = subprocess.run(command, cwd=ROOT, check=False)
    if check and result.returncode != 0:
        raise subprocess.CalledProcessError(result.returncode, command)
    if not check and result.returncode != 0:
        print(f"Warning: command exited with {result.returncode}, continuing.")


if __name__ == "__main__":
    raise SystemExit(main())
