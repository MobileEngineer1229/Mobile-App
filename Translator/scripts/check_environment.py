"""Check the local development environment for the Translator project.

This script is intentionally lightweight and safe to run on a fresh machine.
It does not download models. It only reports whether the tools needed for
mobile development and model preparation are visible from the command line.
"""

from __future__ import annotations

import json
import shutil
import subprocess
import sys
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
MANIFEST = ROOT / "models" / "manifest.example.json"


def main() -> int:
    print("Translator environment check")
    print(f"Project root: {ROOT}")
    print()

    checks = [
        ("python", [sys.executable, "--version"]),
        ("git", ["git", "--version"]),
    ]

    for name, command in checks:
        _check_command(name, command)

    print()
    _check_manifest()
    _check_model_folders()
    print("[ok] web UI: python scripts/run_web_service.py")
    return 0


def _check_command(name: str, command: list[str]) -> None:
    executable = command[0]
    if executable != sys.executable and shutil.which(executable) is None:
        print(f"[missing] {name}: command not found")
        return

    try:
        result = subprocess.run(
            command,
            check=False,
            capture_output=True,
            text=True,
            timeout=15,
        )
    except Exception as exc:  # pragma: no cover - diagnostic path
        print(f"[error] {name}: {exc}")
        return

    first_line = (result.stdout or result.stderr).splitlines()
    label = first_line[0] if first_line else "installed"
    print(f"[ok] {name}: {label}")


def _check_manifest() -> None:
    if not MANIFEST.exists():
        print(f"[missing] model manifest: {MANIFEST}")
        return

    data = json.loads(MANIFEST.read_text(encoding="utf-8"))
    model_names = ", ".join(data.get("models", {}).keys())
    print(f"[ok] model manifest: {model_names}")


def _check_model_folders() -> None:
    expected = ["text", "asr", "ocr", "tts"]
    for folder in expected:
        path = ROOT / "models" / folder
        status = "ok" if path.exists() else "missing"
        print(f"[{status}] models/{folder}")


if __name__ == "__main__":
    raise SystemExit(main())
