"""Report corpus and training readiness statistics.

The web UI and command line both use this script's logic to show how much data
is ready for translation memory and model training.
"""

from __future__ import annotations

import csv
import json
from collections import Counter, defaultdict
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
MONO = ROOT / "data" / "corpus" / "processed" / "monolingual_sentences.tsv"
PARALLEL = ROOT / "data" / "corpus" / "parallel" / "reviewed_parallel.tsv"
MEMORY = ROOT / "data" / "translation_memory.json"
TERM = ROOT / "data" / "terms" / "term_candidates.tsv"
PHRASE = ROOT / "data" / "terms" / "phrase_candidates.tsv"
PARALLEL_PHRASE = ROOT / "data" / "terms" / "parallel_phrase_candidates.tsv"
CHAR_LM = ROOT / "models" / "text" / "ko_kp_char_lm.json"
RAW_SUFFIXES = {".corpus", ".rawdata"}


def build_status() -> dict[str, object]:
    """Build a JSON-serializable corpus status report."""
    mono_by_language = _count_mono_by_language()
    parallel_by_direction = _count_parallel_by_direction()
    term_by_language = _count_terms_by_language(TERM)
    phrase_by_language = _count_terms_by_language(PHRASE)
    parallel_phrase_by_direction = _count_parallel_phrases_by_direction()
    return {
        "raw_text_files": _raw_file_count(),
        "url_pair_candidate_files": len(list((ROOT / "data" / "corpus" / "url_pairs" / "candidates").glob("*.tsv"))),
        "url_pair_approved_files": len(list((ROOT / "data" / "corpus" / "url_pairs" / "approved").glob("*.tsv"))),
        "translation_memory_entries": _memory_count(),
        "raw_sentences_by_language": mono_by_language,
        "parallel_sentences_by_direction": parallel_by_direction,
        "term_candidates_by_language": term_by_language,
        "phrase_candidates_by_language": phrase_by_language,
        "parallel_phrase_candidates_by_direction": parallel_phrase_by_direction,
        "training_readiness": _training_readiness(parallel_by_direction),
        "ko_kp_char_lm": _char_lm_status(),
        "offline_runtime": True,
    }


def main() -> int:
    print(json.dumps(build_status(), ensure_ascii=False, indent=2))
    return 0


def _raw_file_count() -> int:
    raw_root = ROOT / "data" / "corpus" / "raw"
    return sum(1 for path in raw_root.glob("*") if path.is_file() and path.suffix.lower() in RAW_SUFFIXES)


def _count_mono_by_language() -> dict[str, int]:
    counts: Counter[str] = Counter()
    if MONO.exists():
        with MONO.open("r", encoding="utf-8", newline="") as file:
            for row in csv.DictReader(file, delimiter="\t"):
                if row.get("language") and row.get("text"):
                    counts[row["language"]] += 1
    return dict(sorted(counts.items()))


def _count_parallel_by_direction() -> dict[str, int]:
    counts: Counter[str] = Counter()
    if PARALLEL.exists():
        with PARALLEL.open("r", encoding="utf-8", newline="") as file:
            for row in csv.DictReader(file, delimiter="\t"):
                source_language = row.get("source_language", "")
                target_language = row.get("target_language", "")
                if source_language and target_language and row.get("source_text") and row.get("target_text"):
                    counts[f"{source_language}->{target_language}"] += 1
    return dict(sorted(counts.items()))


def _count_terms_by_language(path: Path) -> dict[str, int]:
    counts: Counter[str] = Counter()
    if path.exists():
        with path.open("r", encoding="utf-8", newline="") as file:
            for row in csv.DictReader(file, delimiter="\t"):
                language = row.get("language", "")
                if language:
                    counts[language] += 1
    return dict(sorted(counts.items()))


def _count_parallel_phrases_by_direction() -> dict[str, int]:
    counts: Counter[str] = Counter()
    if PARALLEL_PHRASE.exists():
        with PARALLEL_PHRASE.open("r", encoding="utf-8", newline="") as file:
            for row in csv.DictReader(file, delimiter="\t"):
                source_language = row.get("source_language", "")
                target_language = row.get("target_language", "")
                if source_language and target_language:
                    counts[f"{source_language}->{target_language}"] += 1
    return dict(sorted(counts.items()))


def _memory_count() -> int:
    if not MEMORY.exists():
        return 0
    return len(json.loads(MEMORY.read_text(encoding="utf-8")))


def _char_lm_status() -> dict[str, object] | None:
    if not CHAR_LM.exists():
        return None
    data = json.loads(CHAR_LM.read_text(encoding="utf-8"))
    return {
        "sentences": data.get("sentences", 0),
        "order": data.get("order"),
        "vocab_size": len(data.get("vocab", [])),
    }


def _training_readiness(parallel_by_direction: dict[str, int]) -> dict[str, dict[str, object]]:
    readiness: dict[str, dict[str, object]] = {}
    for direction, count in sorted(parallel_by_direction.items()):
        readiness[direction] = {
            "sentence_pairs": count,
            "stage": _stage(count),
            "next_target": _next_target(count),
        }
    return readiness


def _stage(count: int) -> str:
    if count >= 500_000:
        return "general-translator-target"
    if count >= 100_000:
        return "strong-domain-training"
    if count >= 30_000:
        return "first-serious-finetune"
    if count >= 5_000:
        return "memory-and-small-experiment"
    if count > 0:
        return "seed-data"
    return "empty"


def _next_target(count: int) -> int:
    for target in [5_000, 30_000, 100_000, 500_000]:
        if count < target:
            return target
    return count


if __name__ == "__main__":
    raise SystemExit(main())
