"""Reset collected corpus data and generated training artifacts.

Use this when the corpus must be rebuilt from the beginning. The script keeps
project code, documentation, folder structure, glossary, and post-edit rules.
It clears collected raw files, URL-pair candidates, approved parallel data,
training splits, translation memory, extracted term/phrase candidates, and
generated local model files.

The command is intentionally guarded by `--yes` so it is not run by accident:

    python scripts/reset_training_data.py --yes
"""

from __future__ import annotations

import argparse
import json
import shutil
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]


TSV_HEADERS = {
    ROOT / "data" / "corpus" / "source_registry.tsv": "source_id\ttitle\turl_or_location\tlanguage\tlicense\tpermission_status\tdomain\tnotes\n",
    ROOT / "data" / "corpus" / "processed" / "monolingual_sentences.tsv": "source_id\tlanguage\ttext\tpath\trecord_no\n",
    ROOT / "data" / "corpus" / "parallel" / "reviewed_parallel.tsv": "source_language\ttarget_language\tsource_text\ttarget_text\tsource_file\n",
    ROOT / "data" / "corpus" / "url_pairs" / "url_pair_registry.tsv": (
        "pair_id\tsource_language\ttarget_language\tsource_url\ttarget_url\t"
        "source_raw_path\ttarget_raw_path\tsource_content_type\ttarget_content_type\tnote\n"
    ),
    ROOT / "data" / "terms" / "term_candidates.tsv": "language\tterm\tcount\n",
    ROOT / "data" / "terms" / "phrase_candidates.tsv": "language\tphrase\tngram_size\tcount\n",
    ROOT / "data" / "terms" / "parallel_phrase_candidates.tsv": (
        "source_language\ttarget_language\tsource_phrase\ttarget_phrase\tsource_text\ttarget_text\n"
    ),
    ROOT / "data" / "evaluation" / "phrase_pairs.tsv": "source_language\ttarget_language\tsource_text\texpected_meaning\n",
    ROOT / "data" / "evaluation" / "translation_eval_report.tsv": (
        "source_language\ttarget_language\tsource_text\texpected_text\tprediction\tengine\texact_match\n"
    ),
    ROOT / "data" / "training" / "translation" / "train.tsv": "source_language\ttarget_language\tsource_text\ttarget_text\n",
    ROOT / "data" / "training" / "translation" / "dev.tsv": "source_language\ttarget_language\tsource_text\ttarget_text\n",
    ROOT / "data" / "training" / "translation" / "test.tsv": "source_language\ttarget_language\tsource_text\ttarget_text\n",
}


DIRECTORIES_TO_CLEAR = [
    ROOT / "data" / "corpus" / "raw",
    ROOT / "data" / "corpus" / "parallel" / "inbox",
    ROOT / "data" / "corpus" / "url_pairs" / "approved",
    ROOT / "data" / "corpus" / "url_pairs" / "candidates",
    ROOT / "data" / "corpus" / "url_pairs" / "inbox",
    ROOT / "data" / "corpus" / "image" / "files",
    ROOT / "data" / "corpus" / "speech" / "audio",
    ROOT / "data" / "training" / "mlm",
    ROOT / "data" / "training" / "neural",
]


MODEL_TEXT_ROOT = ROOT / "models" / "text"


def main() -> int:
    parser = argparse.ArgumentParser(description="Reset corpus and generated training data.")
    parser.add_argument("--yes", action="store_true", help="confirm destructive reset")
    args = parser.parse_args()

    if not args.yes:
        raise SystemExit("Refusing to reset data without --yes.")

    summary: list[str] = []
    for directory in DIRECTORIES_TO_CLEAR:
        deleted = _clear_directory(directory)
        summary.append(f"cleared {deleted} item(s): {_display(directory)}")

    for path, header in TSV_HEADERS.items():
        _assert_inside_root(path)
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(header, encoding="utf-8")
        summary.append(f"reset TSV: {_display(path)}")

    memory_path = ROOT / "data" / "translation_memory.json"
    _assert_inside_root(memory_path)
    memory_path.parent.mkdir(parents=True, exist_ok=True)
    memory_path.write_text(json.dumps({}, ensure_ascii=False, indent=2), encoding="utf-8")
    summary.append(f"reset JSON: {_display(memory_path)}")

    deleted_models = _clear_model_text_outputs()
    summary.append(f"cleared {deleted_models} generated model item(s): {_display(MODEL_TEXT_ROOT)}")

    print("Training data reset complete.")
    for line in summary:
        print(f"- {line}")
    return 0


def _clear_directory(directory: Path) -> int:
    """Delete all files/folders inside a project directory except .gitkeep."""
    _assert_inside_root(directory)
    directory.mkdir(parents=True, exist_ok=True)
    deleted = 0
    for item in directory.iterdir():
        if item.name == ".gitkeep":
            continue
        _delete_path(item)
        deleted += 1
    gitkeep = directory / ".gitkeep"
    if not gitkeep.exists():
        gitkeep.write_text("", encoding="utf-8")
    return deleted


def _clear_model_text_outputs() -> int:
    """Remove generated text-model artifacts while preserving .gitkeep."""
    _assert_inside_root(MODEL_TEXT_ROOT)
    MODEL_TEXT_ROOT.mkdir(parents=True, exist_ok=True)
    deleted = 0
    for item in MODEL_TEXT_ROOT.iterdir():
        if item.name == ".gitkeep":
            continue
        _delete_path(item)
        deleted += 1
    gitkeep = MODEL_TEXT_ROOT / ".gitkeep"
    if not gitkeep.exists():
        gitkeep.write_text("", encoding="utf-8")
    return deleted


def _delete_path(path: Path) -> None:
    _assert_inside_root(path)
    if path.is_dir():
        shutil.rmtree(path)
    else:
        path.unlink(missing_ok=True)


def _assert_inside_root(path: Path) -> None:
    """Guard every destructive path so reset cannot escape the project."""
    resolved_root = ROOT.resolve()
    resolved_path = path.resolve()
    if resolved_path != resolved_root and resolved_root not in resolved_path.parents:
        raise SystemExit(f"Refusing to touch outside project root: {resolved_path}")


def _display(path: Path) -> str:
    try:
        return str(path.resolve().relative_to(ROOT.resolve()))
    except ValueError:
        return str(path.resolve())


if __name__ == "__main__":
    raise SystemExit(main())
