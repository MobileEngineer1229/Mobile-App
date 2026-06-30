"""다양한 형식의 파일에서 학습용 텍스트를 추출하는 공용 모듈.

【초보자 안내】
  이 파일은 훈련 파이프라인의 "자료 입구" 다.
  다양한 형식의 파일들을 읽어서 글자렬(문자열)로 바꿔준다.
  preprocess.py와 train_tokenizer.py 가 이 모듈을 사용한다.

  iter_texts() 함수 하나로 모든 형식을 처리한다:
    for text in iter_texts(Path("data/raw")):
        # text는 각 문서의 글자렬

지원 형식
---------
    .txt            일반 문자(UTF-8)
    .json           JSON 배열 또는 단일 객체
    .jsonl          JSON 행 형식 (한 줄에 하나의 JSON 객체)
    .pdf            PDF 문서 (텍스트 추출)
    .docx           마이크로소프트 워드 문서
    이미지 파일     .jpg .jpeg .png .bmp .gif .webp .tiff .tif (OCR 문자 인식)

의존 라이브러리 설치 (필요 형식만 설치하면 됨)
--------------------
    pip install pdfplumber     ← PDF 지원
    pip install python-docx    ← DOCX 지원
    pip install easyocr        ← 이미지 OCR 지원 (약 200MB 모형 자동 내려받기)

사용 방법
---------
    from src.data.readers import iter_texts
    for text in iter_texts(Path("data/raw")):
        process(text)
"""

from __future__ import annotations

import json
import sys
from pathlib import Path
from typing import Iterator

# 이미지 지원 확장자 목록
IMAGE_SUFFIXES = {".jpg", ".jpeg", ".png", ".bmp", ".gif", ".webp", ".tiff", ".tif"}

# EasyOCR 인스턴스를 한 번만 생성하여 재사용 (초기화 비용 절감)
_ocr_reader = None


def _get_ocr_reader():
    """EasyOCR 인스턴스를 반환합니다. 처음 호출 시 모형을 내려받습니다."""
    global _ocr_reader
    if _ocr_reader is None:
        try:
            import easyocr
        except ImportError:
            raise ImportError(
                "이미지 파일 처리에 easyocr가 필요합니다.\n"
                "설치 명령: pip install easyocr"
            )
        print(
            "[readers] EasyOCR 문자 인식기 초기화 중...\n"
            "          처음 실행 시 조선말/영어 인식 모형을 내려받습니다 (약 200MB).",
            file=sys.stderr,
        )
        # gpu=True: 그라픽 처리 장치 가속 사용 (없으면 자동으로 cpu 사용)
        _ocr_reader = easyocr.Reader(["ko", "en"], gpu=True)
    return _ocr_reader


# ---------------------------------------------------------------------------
# 형식별 읽기 함수
# ---------------------------------------------------------------------------

def _read_txt(path: Path) -> Iterator[str]:
    """UTF-8 일반 문자 파일을 읽습니다."""
    try:
        content = path.read_text(encoding="utf-8", errors="replace").strip()
        if content:
            yield content
    except Exception as e:
        print(f"[readers] txt 읽기 오류 {path}: {e}", file=sys.stderr)


def _read_jsonl(path: Path) -> Iterator[str]:
    """JSON 행 형식(.jsonl) 파일을 읽습니다.

    지원 형식:
        {"text": "..."}
        {"question": "...", "answer": "..."}
        {"content": "..."}
        {"body": "..."}
    """
    try:
        with open(path, "r", encoding="utf-8", errors="replace") as f:
            for lineno, line in enumerate(f, 1):
                line = line.strip()
                if not line:
                    continue
                try:
                    obj = json.loads(line)
                except json.JSONDecodeError:
                    continue
                text = _extract_text_from_obj(obj)
                if text:
                    yield text
    except Exception as e:
        print(f"[readers] jsonl 읽기 오류 {path}: {e}", file=sys.stderr)


def _read_json(path: Path) -> Iterator[str]:
    """JSON 파일(.json)을 읽습니다.

    지원 구조:
        단일 객체:  {"text": "..."}
        배열:       [{"text": "..."}, ...]
        문자열 배열: ["문장1", "문장2", ...]
    """
    try:
        with open(path, "r", encoding="utf-8", errors="replace") as f:
            data = json.load(f)
    except Exception as e:
        print(f"[readers] json 읽기 오류 {path}: {e}", file=sys.stderr)
        return

    if isinstance(data, list):
        for item in data:
            if isinstance(item, str) and item.strip():
                yield item.strip()
            elif isinstance(item, dict):
                text = _extract_text_from_obj(item)
                if text:
                    yield text
    elif isinstance(data, dict):
        text = _extract_text_from_obj(data)
        if text:
            yield text


def _read_pdf(path: Path) -> Iterator[str]:
    """PDF 파일에서 텍스트를 추출합니다.

    pdfplumber 라이브러리를 사용합니다.
    설치: pip install pdfplumber
    """
    try:
        import pdfplumber
    except ImportError:
        print(
            f"[readers] PDF 처리에 pdfplumber가 필요합니다.\n"
            f"          설치 명령: pip install pdfplumber\n"
            f"          건너뜀: {path}",
            file=sys.stderr,
        )
        return

    try:
        with pdfplumber.open(str(path)) as pdf:
            for page_num, page in enumerate(pdf.pages, 1):
                try:
                    text = page.extract_text()
                    if text and text.strip():
                        yield text.strip()
                except Exception as e:
                    print(f"[readers] PDF {path} {page_num}쪽 오류: {e}", file=sys.stderr)
    except Exception as e:
        print(f"[readers] PDF 읽기 오류 {path}: {e}", file=sys.stderr)


def _read_docx(path: Path) -> Iterator[str]:
    """마이크로소프트 워드 문서(.docx)에서 텍스트를 추출합니다.

    python-docx 라이브러리를 사용합니다.
    설치: pip install python-docx
    """
    try:
        from docx import Document
    except ImportError:
        print(
            f"[readers] DOCX 처리에 python-docx가 필요합니다.\n"
            f"          설치 명령: pip install python-docx\n"
            f"          건너뜀: {path}",
            file=sys.stderr,
        )
        return

    try:
        doc = Document(str(path))
        paragraphs = [p.text.strip() for p in doc.paragraphs if p.text.strip()]
        if paragraphs:
            yield "\n".join(paragraphs)
        # 표 안의 텍스트도 추출
        for table in doc.tables:
            for row in table.rows:
                for cell in row.cells:
                    text = cell.text.strip()
                    if text:
                        yield text
    except Exception as e:
        print(f"[readers] DOCX 읽기 오류 {path}: {e}", file=sys.stderr)


def _read_image(path: Path) -> Iterator[str]:
    """이미지 파일에서 OCR로 텍스트를 인식합니다.

    EasyOCR 라이브러리를 사용합니다 (조선말 + 영어 지원).
    설치: pip install easyocr

    주의: 처음 실행 시 인식 모형을 자동으로 내려받습니다 (약 200MB).
    """
    try:
        reader = _get_ocr_reader()
    except ImportError as e:
        print(f"[readers] {e}\n          건너뜀: {path}", file=sys.stderr)
        return

    try:
        # detail=0: 글자만 반환 (좌표 없음), paragraph=True: 줄 합치기
        results = reader.readtext(str(path), detail=0, paragraph=True)
        text = "\n".join(r for r in results if isinstance(r, str) and r.strip())
        if text:
            yield text
    except Exception as e:
        print(f"[readers] 이미지 OCR 오류 {path}: {e}", file=sys.stderr)


# ---------------------------------------------------------------------------
# 공용 JSON 객체 텍스트 추출
# ---------------------------------------------------------------------------

def _extract_text_from_obj(obj: dict) -> str:
    """JSON 객체에서 텍스트를 추출합니다.

    인식하는 키: text, question+answer, content, body, passage, context
    """
    if not isinstance(obj, dict):
        return ""

    # 일반 텍스트 필드
    for key in ("text", "content", "body", "passage", "context", "sentence"):
        if key in obj and isinstance(obj[key], str) and obj[key].strip():
            return obj[key].strip()

    # 문답 형식
    if "question" in obj and "answer" in obj:
        q = str(obj["question"]).strip()
        a = str(obj["answer"]).strip()
        if q and a:
            return f"질문: {q}\n대답: {a}"

    # 제목 + 본문 형식
    if "title" in obj and "content" in obj:
        title = str(obj.get("title", "")).strip()
        content = str(obj.get("content", "")).strip()
        if content:
            return f"{title}\n{content}" if title else content

    return ""


# ---------------------------------------------------------------------------
# 주 진입점
# ---------------------------------------------------------------------------

def iter_texts(raw_dir: Path) -> Iterator[str]:
    """raw_dir 아래의 모든 지원 형식 파일에서 텍스트를 추출합니다.

    파일은 하위 폴더까지 재귀적으로 탐색합니다.
    중복 제거는 호출자(preprocess.py)에서 처리합니다.
    """
    for path in sorted(raw_dir.rglob("*")):
        if not path.is_file():
            continue
        suffix = path.suffix.lower()

        if suffix == ".txt":
            yield from _read_txt(path)
        elif suffix == ".jsonl":
            yield from _read_jsonl(path)
        elif suffix == ".json":
            yield from _read_json(path)
        elif suffix == ".pdf":
            yield from _read_pdf(path)
        elif suffix == ".docx":
            yield from _read_docx(path)
        elif suffix in IMAGE_SUFFIXES:
            yield from _read_image(path)
        # 그 외 형식(예: .doc, .xlsx 등)은 조용히 건너뜁니다
