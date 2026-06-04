"""Evaluate the current offline translation pipeline on phrase pairs.

The script uses the same service code as the web UI. If `sacrebleu` is
available, it reports chrF and BLEU. It always writes a detailed TSV report.
"""

from __future__ import annotations

import argparse
import csv
import sys
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / "python_lab"))

from app.translator_service import translate  # noqa: E402


DEFAULT_EVAL = ROOT / "data" / "evaluation" / "phrase_pairs.tsv"
REPORT = ROOT / "data" / "evaluation" / "translation_eval_report.tsv"


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--input", default=str(DEFAULT_EVAL))
    parser.add_argument("--output", default=str(REPORT))
    args = parser.parse_args()

    rows = _read_eval_rows(Path(args.input))
    predictions = []
    references = []
    report_rows = []
    for row in rows:
        result = translate(row["source_text"], row["source_language"], row["target_language"])
        prediction = str(result["translated_text"])
        reference = row["expected_text"]
        predictions.append(prediction)
        references.append(reference)
        report_rows.append(
            {
                **row,
                "prediction": prediction,
                "engine": str(result["engine"]),
                "exact_match": str(prediction.strip() == reference.strip()).lower(),
            }
        )

    _write_report(Path(args.output), report_rows)
    print(f"Rows: {len(rows)}")
    print(f"Exact matches: {sum(row['exact_match'] == 'true' for row in report_rows)}")
    _print_sacrebleu(predictions, references)
    print(f"Report: {args.output}")
    return 0


def _read_eval_rows(path: Path) -> list[dict[str, str]]:
    if not path.exists():
        return []
    with path.open("r", encoding="utf-8", newline="") as file:
        reader = csv.DictReader(file, delimiter="\t")
        rows = []
        for row in reader:
            expected = row.get("expected_text") or row.get("expected_meaning") or row.get("target_text") or ""
            if row.get("source_language") and row.get("target_language") and row.get("source_text") and expected:
                rows.append(
                    {
                        "source_language": row["source_language"],
                        "target_language": row["target_language"],
                        "source_text": row["source_text"],
                        "expected_text": expected,
                    }
                )
        return rows


def _write_report(path: Path, rows: list[dict[str, str]]) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    fieldnames = ["source_language", "target_language", "source_text", "expected_text", "prediction", "engine", "exact_match"]
    with path.open("w", encoding="utf-8", newline="") as file:
        writer = csv.DictWriter(file, fieldnames=fieldnames, delimiter="\t")
        writer.writeheader()
        writer.writerows(rows)


def _print_sacrebleu(predictions: list[str], references: list[str]) -> None:
    if not predictions:
        return
    try:
        import sacrebleu
    except Exception:
        print("sacrebleu: not available")
        return
    print(f"chrF: {sacrebleu.corpus_chrf(predictions, [references]).score:.2f}")
    print(f"BLEU: {sacrebleu.corpus_bleu(predictions, [references]).score:.2f}")


if __name__ == "__main__":
    raise SystemExit(main())
