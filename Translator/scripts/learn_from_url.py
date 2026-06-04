"""Import a URL and rebuild the local learning artifacts in one command.

Example:

    python scripts/learn_from_url.py "https://example.com/page" --language ko_kp

This command is meant for the user's daily workflow. It fetches the URL, stores
the text in the corpus, rebuilds processed data, updates translation memory, and
trains the lightweight local DPRK Korean language model.
"""

from __future__ import annotations

import argparse
import subprocess
import sys
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("url", help="URL to learn from")
    parser.add_argument("--language", default="ko_kp")
    parser.add_argument("--license", default="user-provided")
    parser.add_argument("--permission-status", default="needs-review")
    parser.add_argument("--domain", default="general")
    args = parser.parse_args()

    _run(
        [
            sys.executable,
            "scripts/import_url_corpus.py",
            args.url,
            "--language",
            args.language,
            "--license",
            args.license,
            "--permission-status",
            args.permission_status,
            "--domain",
            args.domain,
        ]
    )
    _run([sys.executable, "scripts/bootstrap_data_pipeline.py"])
    return 0


def _run(command: list[str]) -> None:
    print(f"\n$ {' '.join(command)}")
    subprocess.run(command, cwd=ROOT, check=True)


if __name__ == "__main__":
    raise SystemExit(main())
