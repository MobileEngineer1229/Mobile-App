"""DPRK Korean glossary and post-edit helpers for experiments.

This module is intentionally small. It lets us test terminology and style rules
before moving the same logic into the mobile app.
"""

from __future__ import annotations

from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
GLOSSARY_PATH = ROOT / "data" / "glossary" / "dprk_glossary.tsv"
RULES_PATH = ROOT / "data" / "rules" / "dprk_postedit_rules.tsv"


def apply_postedit(text: str, source_language: str, target_language: str) -> str:
    """Apply glossary and post-edit rules to translated text."""
    output = text
    for row in _read_tsv(GLOSSARY_PATH):
        if row.get("source_language") == source_language and row.get("target_language") == target_language:
            source_term = row.get("source_term", "")
            if source_term:
                output = output.replace(source_term, row.get("target_term", ""))
    for row in _read_tsv(RULES_PATH):
        if row.get("target_language") == target_language:
            find_text = row.get("find", "")
            if find_text:
                output = output.replace(find_text, row.get("replace", ""))
    return output


def _read_tsv(path: Path) -> list[dict[str, str]]:
    if not path.exists():
        return []
    lines = path.read_text(encoding="utf-8").splitlines()
    if not lines:
        return []
    headers = lines[0].split("\t")
    rows = []
    for line in lines[1:]:
        if not line.strip():
            continue
        values = line.split("\t")
        rows.append({header: values[index] if index < len(values) else "" for index, header in enumerate(headers)})
    return rows
