"""Import text from a URL into the Translator corpus workspace.

This script is the entry point for the workflow:

    URL -> raw corpus file -> source registry -> data pipeline -> training files

It intentionally uses only Python standard-library modules so it works on a
fresh machine. The user is responsible for giving URLs whose content may be
used for the project.
"""

from __future__ import annotations

import argparse
import csv
import hashlib
import html
import re
import sys
from html.parser import HTMLParser
from pathlib import Path
from urllib.error import HTTPError, URLError
from urllib.request import Request, urlopen


ROOT = Path(__file__).resolve().parents[1]
RAW_ROOT = ROOT / "data" / "corpus" / "raw"
REGISTRY = ROOT / "data" / "corpus" / "source_registry.tsv"
USER_AGENT = "TranslatorCorpusBuilder/0.1"
RAW_EXTENSION = ".corpus"


class VisibleTextParser(HTMLParser):
    """Extract visible text from simple HTML documents.

    Inline HTML nodes are joined with spaces, not line breaks. Paragraph-like
    tags create line boundaries. This prevents pages that wrap every word in a
    separate span from producing one raw-corpus line per word.
    """

    BLOCK_TAGS = {
        "address",
        "article",
        "aside",
        "blockquote",
        "br",
        "caption",
        "dd",
        "div",
        "dt",
        "figcaption",
        "footer",
        "h1",
        "h2",
        "h3",
        "h4",
        "h5",
        "h6",
        "header",
        "hr",
        "li",
        "main",
        "nav",
        "p",
        "pre",
        "section",
        "td",
        "th",
        "tr",
    }

    def __init__(self) -> None:
        super().__init__()
        self._skip_depth = 0
        self.parts: list[str] = []

    def handle_starttag(self, tag: str, attrs: list[tuple[str, str | None]]) -> None:
        tag_name = tag.lower()
        if tag_name in {"script", "style", "noscript", "svg"}:
            self._skip_depth += 1
            return
        if not self._skip_depth and tag_name in self.BLOCK_TAGS:
            self._append_break()

    def handle_endtag(self, tag: str) -> None:
        tag_name = tag.lower()
        if tag_name in {"script", "style", "noscript", "svg"} and self._skip_depth:
            self._skip_depth -= 1
            return
        if not self._skip_depth and tag_name in self.BLOCK_TAGS:
            self._append_break()

    def handle_data(self, data: str) -> None:
        if not self._skip_depth and data.strip():
            self.parts.append(_clean_line(data))

    def text(self) -> str:
        return " ".join(self.parts)

    def _append_break(self) -> None:
        if self.parts and self.parts[-1] != "\n":
            self.parts.append("\n")


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("url", nargs="*", help="URL to import")
    parser.add_argument("--url-file", help="UTF-8 text file containing one URL per line")
    parser.add_argument("--language", default="ko_kp", help="language code for the imported text")
    parser.add_argument("--license", default="user-provided")
    parser.add_argument("--permission-status", default="needs-review")
    parser.add_argument("--domain", default="general")
    parser.add_argument("--max-bytes", type=int, default=5_000_000)
    args = parser.parse_args()

    urls = list(args.url)
    if args.url_file:
        urls.extend(_read_url_file(Path(args.url_file)))
    if not urls:
        raise SystemExit("No URL was provided.")

    RAW_ROOT.mkdir(parents=True, exist_ok=True)
    _ensure_registry()

    imported = 0
    for url in urls:
        text, content_type = fetch_url_text(url, args.max_bytes)
        cleaned = clean_extracted_text(text)
        if not cleaned:
            print(f"Skipped empty URL: {url}")
            continue
        source_id = _source_id(url)
        output = RAW_ROOT / f"{source_id}{RAW_EXTENSION}"
        output.write_text(cleaned + "\n", encoding="utf-8")
        _append_registry(
            source_id=source_id,
            title=url,
            url_or_location=url,
            language=args.language,
            license_name=args.license,
            permission_status=args.permission_status,
            domain=args.domain,
            notes=f"content_type={content_type}; imported_by=import_url_corpus.py",
        )
        print(f"Imported {url} -> {output}")
        imported += 1

    print(f"Imported URLs: {imported}")
    return 0


def fetch_url_text(url: str, max_bytes: int) -> tuple[str, str]:
    """Fetch a URL and return readable text plus its content type."""
    request = Request(url, headers={"User-Agent": USER_AGENT})
    try:
        with urlopen(request, timeout=30) as response:
            content_type = response.headers.get("Content-Type", "")
            raw = response.read(max_bytes + 1)
    except (HTTPError, URLError, TimeoutError) as exc:
        raise SystemExit(f"Could not fetch {url}: {exc}") from exc

    if len(raw) > max_bytes:
        raise SystemExit(f"URL is larger than --max-bytes: {url}")

    encoding = _guess_encoding(content_type)
    body = raw.decode(encoding, errors="replace")
    if "html" in content_type.lower() or "<html" in body[:500].lower():
        parser = VisibleTextParser()
        parser.feed(body)
        body = parser.text()
    return body, content_type


def clean_extracted_text(text: str) -> str:
    """Normalize text extracted from HTML or plain files."""
    text = html.unescape(text)
    text = text.replace("\ufeff", " ")
    text = re.sub(r"\r\n?", "\n", text)
    text = re.sub(r"[ \t]+", " ", text)
    lines = [_clean_line(line) for line in text.splitlines()]
    lines = [line for line in lines if line and not _is_page_counter(line)]
    lines = _merge_broken_lines(lines)
    lines = _split_embedded_standalone_lines(lines)
    return "\n".join(lines).strip()


def _clean_line(line: str) -> str:
    return re.sub(r"\s+", " ", line).strip()


def _is_page_counter(line: str) -> bool:
    """Return true for site navigation counters such as `7 / 41`."""
    return re.fullmatch(r"\d+\s*/\s*\d+", line) is not None


def _merge_broken_lines(lines: list[str]) -> list[str]:
    """Join lines that were split inside one sentence by HTML markup."""
    merged: list[str] = []
    buffer = ""
    for line in lines:
        if not buffer:
            buffer = line
            continue
        if _starts_new_line(buffer, line):
            merged.append(buffer)
            buffer = line
        else:
            buffer = f"{buffer} {line}".strip()
    if buffer:
        merged.append(buffer)
    return merged


def _starts_new_line(previous: str, current: str) -> bool:
    """Decide whether current text should stay on a new raw-corpus line."""
    if _is_fixed_standalone(previous) or _is_fixed_standalone(current):
        return True
    if _looks_like_title_before_repeated_marker(previous, current):
        return True
    if _has_unclosed_quote(previous) or _looks_incomplete_phrase(previous):
        return False
    if _looks_like_heading(previous) and (_starts_body_after_heading(current) or len(current) >= 45):
        return True
    if _ends_sentence(previous):
        return True
    if _looks_like_credit_or_source(previous) or _looks_like_credit_or_source(current):
        return True
    return False


def _ends_sentence(text: str) -> bool:
    return re.search(r"[.!?。！？…》〉）)]\s*$", text) is not None


def _is_fixed_standalone(text: str) -> bool:
    """Keep site labels and issue/date lines separate from article text."""
    normalized = text.strip()
    if normalized in {"Rodong Newspaper", "Rodong Sinmun"}:
        return True
    if re.search(r"[0-9０-９]{4}year\s+[0-9０-９]{1,2}month\s+[0-9０-９]{1,2}work", normalized):
        return True
    if re.fullmatch(r"\d+\s*cotton(?:\s*\[[^\]]+\])?", normalized):
        return True
    return False


def _looks_like_credit_or_source(text: str) -> bool:
    normalized = text.strip()
    if normalized.startswith("【") and normalized.endswith("】"):
        return True
    if "Taken" in normalized or normalized.startswith("Headquarters Reporter"):
        return True
    return False


def _looks_like_heading(text: str) -> bool:
    """Heuristic for article titles and subtitles without final punctuation."""
    normalized = text.strip()
    if len(normalized) < 12 or len(normalized) > 90:
        return False
    if (
        _ends_sentence(normalized)
        or _is_fixed_standalone(normalized)
        or _looks_like_credit_or_source(normalized)
        or _has_unclosed_quote(normalized)
        or _looks_incomplete_phrase(normalized)
    ):
        return False
    if any(mark in normalized for mark in ["，", ",", "；", ";"]):
        return False
    return True


def _starts_body_after_heading(text: str) -> bool:
    normalized = text.strip()
    return normalized.startswith(
        (
            "dear",
            "great",
            "Korean Workers' Party",
            "party ",
            "us ",
            "today",
            "socialism",
        )
    )


def _split_embedded_standalone_lines(lines: list[str]) -> list[str]:
    """Split issue/date labels that got glued to the next sentence."""
    fixed: list[str] = []
    for line in lines:
        fixed.extend(_split_leading_date_label(line))
    return fixed


def _split_leading_date_label(line: str) -> list[str]:
    match = re.match(
        r"^([0-9０-９]{4}year\s+[0-9０-９]{1,2}month\s+[0-9０-９]{1,2}work)(\s+)(dear|great|Korean Workers' Party|us\s+|party\s+)(.+)$",
        line,
    )
    if not match:
        return [line]
    date = match.group(1)
    rest = (match.group(3) + match.group(4)).strip()
    return [date, rest]


def _has_unclosed_quote(text: str) -> bool:
    return text.count("《") > text.count("》") or text.count("“") > text.count("”")


def _looks_incomplete_phrase(text: str) -> bool:
    """Return true for fragments that are clearly waiting for the next line."""
    normalized = text.strip()
    if normalized.startswith(("dear", "great")) and not _ends_sentence(normalized):
        return True
    incomplete_endings = (
        "of",
        "silver",
        "is",
        "This",
        "go",
        "to",
        "to",
        "You",
        "You are",
        "To",
        "in",
        "to",
        "by",
        "Go",
        "And",
        "So",
        "And",
        "did",
        "You put forward",
        "given",
        "They say",
        "But",
        "period",
    )
    return normalized.endswith(incomplete_endings)


def _looks_like_title_before_repeated_marker(previous: str, current: str) -> bool:
    """Keep a title separate when the body starts with the same honorific marker."""
    prev = previous.strip()
    cur = current.strip()
    if len(prev) < 12 or len(prev) > 90:
        return False
    return (prev.startswith("great") and cur.startswith("great")) or (
        prev.startswith("dear") and cur.startswith("dear")
    )


def _guess_encoding(content_type: str) -> str:
    match = re.search(r"charset=([\w-]+)", content_type, flags=re.I)
    return match.group(1) if match else "utf-8"


def _read_url_file(path: Path) -> list[str]:
    return [line.strip() for line in path.read_text(encoding="utf-8").splitlines() if line.strip()]


def _ensure_registry() -> None:
    if REGISTRY.exists():
        return
    REGISTRY.parent.mkdir(parents=True, exist_ok=True)
    with REGISTRY.open("w", encoding="utf-8", newline="") as file:
        writer = csv.writer(file, delimiter="\t")
        writer.writerow(
            [
                "source_id",
                "title",
                "url_or_location",
                "language",
                "license",
                "permission_status",
                "domain",
                "notes",
            ]
        )


def _append_registry(
    *,
    source_id: str,
    title: str,
    url_or_location: str,
    language: str,
    license_name: str,
    permission_status: str,
    domain: str,
    notes: str,
) -> None:
    existing_ids = _read_existing_source_ids()
    if source_id in existing_ids:
        return
    with REGISTRY.open("a", encoding="utf-8", newline="") as file:
        writer = csv.writer(file, delimiter="\t")
        writer.writerow([source_id, title, url_or_location, language, license_name, permission_status, domain, notes])


def _read_existing_source_ids() -> set[str]:
    if not REGISTRY.exists():
        return set()
    with REGISTRY.open("r", encoding="utf-8", newline="") as file:
        return {row.get("source_id", "") for row in csv.DictReader(file, delimiter="\t")}


def _source_id(url: str) -> str:
    return "url_" + hashlib.sha1(url.encode("utf-8")).hexdigest()[:12]


if __name__ == "__main__":
    if hasattr(sys.stdout, "reconfigure"):
        sys.stdout.reconfigure(encoding="utf-8")
    raise SystemExit(main())
