"""Check whether local training dependencies are importable."""

from __future__ import annotations

import importlib.metadata as metadata
import importlib.util


CHECKS = {
    "torch": "torch",
    "transformers": "transformers",
    "sentencepiece": "sentencepiece",
    "datasets": "datasets",
    "accelerate": "accelerate",
    "sacrebleu": "sacrebleu",
    "protobuf": "google.protobuf",
}


def main() -> int:
    missing = []
    for package_name, import_name in CHECKS.items():
        spec = importlib.util.find_spec(import_name)
        if spec is None:
            print(f"[missing] {package_name}")
            missing.append(package_name)
            continue
        version = _version(package_name)
        print(f"[ok] {package_name}: {version}")
    return 1 if missing else 0


def _version(package_name: str) -> str:
    try:
        return metadata.version(package_name)
    except metadata.PackageNotFoundError:
        return "installed"


if __name__ == "__main__":
    raise SystemExit(main())

