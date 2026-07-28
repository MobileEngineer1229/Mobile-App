"""Import paired source/target URLs as candidate parallel training data.

Use this when the user has a DPRK Korean URL and the corresponding English URL.
The script downloads both pages, extracts readable text, splits them into
sentences, and writes an alignment candidate TSV for human review.

The candidate alignment is intentionally conservative and simple. A human
should review the generated file and copy good pairs into:

    data/corpus/url_pairs/approved/

Approved files are consumed by `scripts/import_parallel_corpus.py`.
"""

from __future__ import annotations

import argparse
import csv
import hashlib
import re
import sys
from dataclasses import dataclass
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / "scripts"))

from import_url_corpus import clean_extracted_text, fetch_url_text  # noqa: E402


PAIR_REGISTRY = ROOT / "data" / "corpus" / "url_pairs" / "url_pair_registry.tsv"
INBOX = ROOT / "data" / "corpus" / "url_pairs" / "inbox"
CANDIDATES = ROOT / "data" / "corpus" / "url_pairs" / "candidates"
APPROVED = ROOT / "data" / "corpus" / "url_pairs" / "approved"
RAW_ROOT = ROOT / "data" / "corpus" / "raw"
RAW_EXTENSION = ".corpus"


@dataclass(frozen=True)
class UrlPair:
    pair_id: str
    source_language: str
    target_language: str
    source_url: str
    target_url: str
    note: str = ""


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--source-url", help="source URL, for example DPRK Korean page")
    parser.add_argument("--target-url", help="target URL, for example matching English page")
    parser.add_argument("--source-language", default="ko_kp")
    parser.add_argument("--target-language", default="en")
    parser.add_argument("--pair-id", help="stable id for this URL pair")
    parser.add_argument("--pairs-file", help="TSV with pair_id, source_language, target_language, source_url, target_url")
    parser.add_argument("--approve-exact-order", action="store_true", help="also write candidate pairs into approved/")
    parser.add_argument("--max-bytes", type=int, default=5_000_000)
    args = parser.parse_args()

    _prepare_folders()
    pairs = _load_pairs(args)
    if not pairs:
        raise SystemExit("No URL pair was provided.")

    imported = 0
    for pair in pairs:
        _import_pair(pair, max_bytes=args.max_bytes, approve_exact_order=args.approve_exact_order)
        imported += 1
    print(f"Imported URL pairs: {imported}")
    return 0


def _load_pairs(args: argparse.Namespace) -> list[UrlPair]:
    if args.pairs_file:
        return _read_pairs_file(Path(args.pairs_file))
    if args.source_url and args.target_url:
        pair_id = args.pair_id or _pair_id(args.source_url, args.target_url)
        return [
            UrlPair(
                pair_id=pair_id,
                source_language=args.source_language,
                target_language=args.target_language,
                source_url=args.source_url,
                target_url=args.target_url,
            )
        ]
    return []


def _read_pairs_file(path: Path) -> list[UrlPair]:
    with path.open("r", encoding="utf-8", newline="") as file:
        reader = csv.DictReader(file, delimiter="\t")
        required = {"pair_id", "source_language", "target_language", "source_url", "target_url"}
        missing = required - set(reader.fieldnames or [])
        if missing:
            raise SystemExit(f"Missing columns in {path}: {', '.join(sorted(missing))}")
        return [
            UrlPair(
                pair_id=row["pair_id"].strip() or _pair_id(row["source_url"], row["target_url"]),
                source_language=row["source_language"].strip(),
                target_language=row["target_language"].strip(),
                source_url=row["source_url"].strip(),
                target_url=row["target_url"].strip(),
                note=row.get("note", "").strip(),
            )
            for row in reader
            if row.get("source_url", "").strip() and row.get("target_url", "").strip()
        ]


def _import_pair(pair: UrlPair, *, max_bytes: int, approve_exact_order: bool) -> None:
    source_text, source_content_type = fetch_url_text(pair.source_url, max_bytes)
    target_text, target_content_type = fetch_url_text(pair.target_url, max_bytes)
    source_clean = clean_extracted_text(source_text)
    target_clean = clean_extracted_text(target_text)

    source_raw = RAW_ROOT / f"{pair.pair_id}_{pair.source_language}{RAW_EXTENSION}"
    target_raw = RAW_ROOT / f"{pair.pair_id}_{pair.target_language}{RAW_EXTENSION}"
    source_raw.write_text(source_clean + "\n", encoding="utf-8")
    target_raw.write_text(target_clean + "\n", encoding="utf-8")

    source_sentences = _split_sentences(source_clean)
    target_sentences = _split_sentences(target_clean)
    candidate_rows = _align_by_order(pair, source_sentences, target_sentences)

    candidate_path = CANDIDATES / f"{pair.pair_id}.tsv"
    _write_candidate_file(candidate_path, candidate_rows)
    if approve_exact_order:
        approved_path = APPROVED / f"{pair.pair_id}.tsv"
        _write_parallel_file(approved_path, candidate_rows)

    _append_pair_registry(pair, source_raw, target_raw, source_content_type, target_content_type)
    print(f"Pair {pair.pair_id}: {len(source_sentences)} source sentences, {len(target_sentences)} target sentences")
    print(f"Candidate file: {candidate_path}")


def _align_by_order(pair: UrlPair, source_sentences: list[str], target_sentences: list[str]) -> list[dict[str, str]]:
    rows = []
    source_sentences = _filter_boilerplate(source_sentences)
    target_sentences = _filter_boilerplate(target_sentences)
    alignments = _monotonic_length_align(source_sentences, target_sentences)
    for source_index, target_index, score in alignments:
        source_text = source_sentences[source_index]
        target_text = target_sentences[target_index]
        rows.append(
            {
                "pair_id": pair.pair_id,
                "alignment_status": "candidate",
                "confidence": _confidence(score),
                "alignment_score": f"{score:.3f}",
                "source_language": pair.source_language,
                "target_language": pair.target_language,
                "source_text": source_text,
                "target_text": target_text,
                "source_url": pair.source_url,
                "target_url": pair.target_url,
            }
        )
    return rows


def _monotonic_length_align(source_sentences: list[str], target_sentences: list[str]) -> list[tuple[int, int, float]]:
    """Align sentences monotonically using a lightweight offline score.

    This is not as strong as LaBSE/Vecalign, but it is deterministic, offline,
    dependency-free, and safer than blindly pairing by index.
    """
    rows: list[tuple[int, int, float]] = []
    target_index = 0
    for source_index, source_text in enumerate(source_sentences):
        if target_index >= len(target_sentences):
            break
        window = range(target_index, min(target_index + 3, len(target_sentences)))
        best_index = max(window, key=lambda index: _alignment_score(source_text, target_sentences[index]))
        score = _alignment_score(source_text, target_sentences[best_index])
        rows.append((source_index, best_index, score))
        target_index = best_index + 1
    return rows


def _alignment_score(source_text: str, target_text: str) -> float:
    length_score = _length_score(source_text, target_text)
    number_score = _number_score(source_text, target_text)
    punctuation_score = _punctuation_score(source_text, target_text)
    return round((length_score * 0.65) + (number_score * 0.25) + (punctuation_score * 0.10), 4)


def _length_score(source_text: str, target_text: str) -> float:
    source_len = max(len(source_text), 1)
    target_len = max(len(target_text), 1)
    return min(source_len, target_len) / max(source_len, target_len)


def _number_score(source_text: str, target_text: str) -> float:
    source_numbers = set(re.findall(r"\d+", source_text))
    target_numbers = set(re.findall(r"\d+", target_text))
    if not source_numbers and not target_numbers:
        return 1.0
    if not source_numbers or not target_numbers:
        return 0.0
    return len(source_numbers & target_numbers) / len(source_numbers | target_numbers)


def _punctuation_score(source_text: str, target_text: str) -> float:
    source_marks = set(re.findall(r"[!?]", source_text))
    target_marks = set(re.findall(r"[!?]", target_text))
    return 1.0 if source_marks == target_marks else 0.5


def _confidence(score: float) -> str:
    if score >= 0.72:
        return "high"
    if score >= 0.48:
        return "medium"
    return "low"


def _split_sentences(text: str) -> list[str]:
    sentences: list[str] = []
    for line in text.splitlines():
        line = re.sub(r"\s+", " ", line).strip()
        if not line:
            continue
        parts = re.split(r"(?<=[.!?。！？])\s+", line)
        sentences.extend(part.strip() for part in parts if len(part.strip()) >= 2)
    return sentences


def _filter_boilerplate(sentences: list[str]) -> list[str]:
    return [sentence for sentence in sentences if not _is_boilerplate_sentence(sentence)]


def _is_boilerplate_sentence(sentence: str) -> bool:
    text = sentence.strip()
    if not text:
        return True
    lowered = text.lower()
    boilerplate = {
        "rodong sinmun",
        "Rodong Newspaper",
        "【Korean Central News Agency】",
    }
    if text in boilerplate or lowered in boilerplate:
        return True
    if re.fullmatch(r"\d+\s*/\s*\d+", text):
        return True
    return False


def _write_candidate_file(path: Path, rows: list[dict[str, str]]) -> None:
    fieldnames = [
        "pair_id",
        "alignment_status",
        "confidence",
        "alignment_score",
        "source_language",
        "target_language",
        "source_text",
        "target_text",
        "source_url",
        "target_url",
    ]
    with path.open("w", encoding="utf-8", newline="") as file:
        writer = csv.DictWriter(file, fieldnames=fieldnames, delimiter="\t")
        writer.writeheader()
        writer.writerows(rows)


def _write_parallel_file(path: Path, rows: list[dict[str, str]]) -> None:
    fieldnames = ["source_language", "target_language", "source_text", "target_text"]
    with path.open("w", encoding="utf-8", newline="") as file:
        writer = csv.DictWriter(file, fieldnames=fieldnames, delimiter="\t")
        writer.writeheader()
        for row in rows:
            writer.writerow({field: row[field] for field in fieldnames})


def _append_pair_registry(
    pair: UrlPair,
    source_raw: Path,
    target_raw: Path,
    source_content_type: str,
    target_content_type: str,
) -> None:
    _ensure_pair_registry()
    existing = _existing_pair_ids()
    if pair.pair_id in existing:
        return
    with PAIR_REGISTRY.open("a", encoding="utf-8", newline="") as file:
        writer = csv.writer(file, delimiter="\t")
        writer.writerow(
            [
                pair.pair_id,
                pair.source_language,
                pair.target_language,
                pair.source_url,
                pair.target_url,
                str(source_raw.relative_to(ROOT)),
                str(target_raw.relative_to(ROOT)),
                source_content_type,
                target_content_type,
                pair.note,
            ]
        )


def _ensure_pair_registry() -> None:
    if PAIR_REGISTRY.exists():
        return
    with PAIR_REGISTRY.open("w", encoding="utf-8", newline="") as file:
        writer = csv.writer(file, delimiter="\t")
        writer.writerow(
            [
                "pair_id",
                "source_language",
                "target_language",
                "source_url",
                "target_url",
                "source_raw_path",
                "target_raw_path",
                "source_content_type",
                "target_content_type",
                "note",
            ]
        )


def _existing_pair_ids() -> set[str]:
    if not PAIR_REGISTRY.exists():
        return set()
    with PAIR_REGISTRY.open("r", encoding="utf-8", newline="") as file:
        return {row.get("pair_id", "") for row in csv.DictReader(file, delimiter="\t")}


def _prepare_folders() -> None:
    for folder in [INBOX, CANDIDATES, APPROVED, RAW_ROOT]:
        folder.mkdir(parents=True, exist_ok=True)
        (folder / ".gitkeep").touch(exist_ok=True)


def _pair_id(source_url: str, target_url: str) -> str:
    digest = hashlib.sha1(f"{source_url}\n{target_url}".encode("utf-8")).hexdigest()[:12]
    return f"pair_{digest}"


if __name__ == "__main__":
    if hasattr(sys.stdout, "reconfigure"):
        sys.stdout.reconfigure(encoding="utf-8")
    raise SystemExit(main())
