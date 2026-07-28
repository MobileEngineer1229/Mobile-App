"""Extract word and phrase candidates from collected corpus data.

The output is not treated as a verified glossary. It is a candidate list for
human review and future terminology work.
"""

from __future__ import annotations

import csv
import re
from collections import Counter, defaultdict
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
MONO = ROOT / "data" / "corpus" / "processed" / "monolingual_sentences.tsv"
PARALLEL = ROOT / "data" / "corpus" / "parallel" / "reviewed_parallel.tsv"
OUT_DIR = ROOT / "data" / "terms"
TERM_OUT = OUT_DIR / "term_candidates.tsv"
PHRASE_OUT = OUT_DIR / "phrase_candidates.tsv"
PAIR_OUT = OUT_DIR / "parallel_phrase_candidates.tsv"


def main() -> int:
    OUT_DIR.mkdir(parents=True, exist_ok=True)
    sentences_by_language = _read_sentences()
    _write_term_candidates(sentences_by_language)
    _write_phrase_candidates(sentences_by_language)
    _write_parallel_phrase_candidates()
    print(f"Wrote {TERM_OUT}")
    print(f"Wrote {PHRASE_OUT}")
    print(f"Wrote {PAIR_OUT}")
    return 0


def _read_sentences() -> dict[str, list[str]]:
    data: dict[str, list[str]] = defaultdict(list)
    if MONO.exists():
        with MONO.open("r", encoding="utf-8", newline="") as file:
            for row in csv.DictReader(file, delimiter="\t"):
                if row.get("language") and row.get("text"):
                    data[row["language"]].append(row["text"])
    if PARALLEL.exists():
        with PARALLEL.open("r", encoding="utf-8", newline="") as file:
            for row in csv.DictReader(file, delimiter="\t"):
                if row.get("source_language") and row.get("source_text"):
                    data[row["source_language"]].append(row["source_text"])
                if row.get("target_language") and row.get("target_text"):
                    data[row["target_language"]].append(row["target_text"])
    return data


def _write_term_candidates(sentences_by_language: dict[str, list[str]]) -> None:
    with TERM_OUT.open("w", encoding="utf-8", newline="") as file:
        writer = csv.writer(file, delimiter="\t")
        writer.writerow(["language", "term", "count"])
        for language, sentences in sorted(sentences_by_language.items()):
            counts = Counter()
            for sentence in sentences:
                counts.update(_tokens(sentence))
            for term, count in counts.most_common(500):
                if _useful_token(term):
                    writer.writerow([language, term, count])


def _write_phrase_candidates(sentences_by_language: dict[str, list[str]]) -> None:
    with PHRASE_OUT.open("w", encoding="utf-8", newline="") as file:
        writer = csv.writer(file, delimiter="\t")
        writer.writerow(["language", "phrase", "ngram_size", "count"])
        for language, sentences in sorted(sentences_by_language.items()):
            counts = Counter()
            for sentence in sentences:
                tokens = [token for token in _tokens(sentence) if _useful_token(token)]
                for size in [2, 3, 4]:
                    for index in range(0, max(len(tokens) - size + 1, 0)):
                        counts[" ".join(tokens[index : index + size])] += 1
            for phrase, count in counts.most_common(500):
                if count >= 1:
                    writer.writerow([language, phrase, len(phrase.split()), count])


def _write_parallel_phrase_candidates() -> None:
    with PAIR_OUT.open("w", encoding="utf-8", newline="") as file:
        writer = csv.writer(file, delimiter="\t")
        writer.writerow(["source_language", "target_language", "source_phrase", "target_phrase", "source_text", "target_text"])
        if not PARALLEL.exists():
            return
        with PARALLEL.open("r", encoding="utf-8", newline="") as source:
            for row in csv.DictReader(source, delimiter="\t"):
                source_phrase = _headline_phrase(row.get("source_text", ""))
                target_phrase = _headline_phrase(row.get("target_text", ""))
                if source_phrase and target_phrase:
                    writer.writerow(
                        [
                            row.get("source_language", ""),
                            row.get("target_language", ""),
                            source_phrase,
                            target_phrase,
                            row.get("source_text", ""),
                            row.get("target_text", ""),
                        ]
                    )


def _tokens(text: str) -> list[str]:
    return re.findall(r"[\wgo-HeheА-Яа-я]+", text, flags=re.UNICODE)


def _useful_token(token: str) -> bool:
    return len(token) >= 2 and not token.isdigit()


def _headline_phrase(text: str) -> str:
    tokens = [token for token in _tokens(text) if _useful_token(token)]
    return " ".join(tokens[: min(5, len(tokens))])


if __name__ == "__main__":
    raise SystemExit(main())
