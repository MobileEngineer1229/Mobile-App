"""원본 자료를 학습용 이진 어표 파일로 변환합니다.

【초보자 안내】
  이 파일은 훈련 파이프라인의 2번째 단계다.
  사람이 읽을 수 있는 텍스트를 모형이 읽을 수 있는 숫자 배열로 바꾼다.

  처리 흐름:
    원본 파일들 (.txt, .json, .pdf 등)
      ↓ iter_texts() 로 텍스트 추출
    모든 문서의 글자렬 목록
      ↓ SHA-1 해시로 중복 제거
    중복 없는 문서 목록
      ↓ 무작위 섞기 후 95%/5% 분할
    훈련 문서 / 검증 문서
      ↓ tok.encode() 로 어표화 + BOS/EOS 감싸기
    정수 번호 목록
      ↓ numpy uint16 배열로 저장
    data/processed/train.bin, val.bin, meta.json

  출력 파일:
    data/processed/train.bin   uint16 어표 ID 배열 (학습용)
    data/processed/val.bin     uint16 어표 ID 배열 (검증용)
    data/processed/meta.json   어휘 크기, 어표 수 등 통계

  각 문서 감싸기:
    [BOS_ID] + 어표1 + 어표2 + ... + [EOS_ID]
    → 모형이 문서 경계를 학습할 수 있도록 함

  지원 형식: .txt .json .jsonl .pdf .docx 이미지(.jpg .png 등)

  실행 방법:
    python -m src.data.preprocess --config config/model_config.yaml
"""

from __future__ import annotations

import argparse
import hashlib
import json
import random
import sys
import unicodedata
from pathlib import Path

import numpy as np
from tqdm import tqdm

# 프로젝트 루트를 파이썬 경로에 추가
ROOT = Path(__file__).resolve().parents[2]
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))

from src.data.readers import iter_texts          # noqa: E402  다양한 형식 파일 읽기
from src.model.config import load_config         # noqa: E402  설정 불러오기
from src.tokenizer.tokenizer import BOS_ID, EOS_ID, load_tokenizer  # noqa: E402  어표 분석기


def normalize(text: str) -> str:
    """글자렬을 정규화한다.

    처리 내용:
        1. Unicode NFC 정규화: 한글 자모 조합 등 통일
        2. 각 줄의 공백 정규화: 탭, 연속 공백을 하나로
        3. 빈 줄 제거

    인수:
        text: 정규화할 글자렬

    반환값:
        정규화된 글자렬
    """
    text = unicodedata.normalize("NFC", text)  # Unicode NFC 정규화
    # 각 줄을 정규화하고 빈 줄 제거
    lines = [" ".join(line.split()) for line in text.splitlines()]
    return "\n".join(line for line in lines if line)


def main() -> int:
    """전처리 파이프라인 주 함수.

    처리 순서:
      1. 설정 파일 읽기
      2. 어표 분석기 불러오기
      3. 원본 자료 읽기 + 정규화 + SHA-1 중복 제거
      4. 무작위 섞기 후 훈련/검증 분할 (95%/5%)
      5. 어표화: 각 문서를 [BOS] + 어표들 + [EOS] 로 변환
      6. uint16 이진 파일로 저장
      7. 통계 정보 meta.json 저장

    반환값:
        0: 성공
        1: 오류
    """
    # 명령줄 인수 처리
    parser = argparse.ArgumentParser(description="전처리: 원본 자료 → 이진 어표 파일")
    parser.add_argument("--config", type=str, default="config/model_config.yaml")
    parser.add_argument("--seed", type=int, default=42)  # 무작위 섞기 재현성
    args = parser.parse_args()

    cfg = load_config(args.config)
    rng = random.Random(args.seed)  # 고정 씨앗값으로 결과 재현 가능

    # 경로 설정
    raw_dir = ROOT / cfg.data.raw_dir          # 원본 자료 폴더
    out_dir = ROOT / cfg.data.processed_dir    # 출력 폴더
    out_dir.mkdir(parents=True, exist_ok=True) # 출력 폴더가 없으면 생성

    # 어표 분석기 불러오기
    tokenizer_path = ROOT / "checkpoints" / f"{cfg.tokenizer.output_prefix}.model"
    tok = load_tokenizer(tokenizer_path)

    # uint16 형식의 최대값 = 65535 → 어휘 크기가 이를 초과하면 저장 불가
    if tok.vocab_size > 65535:
        print(
            f"어휘 크기 {tok.vocab_size} > 65535; "
            f"preprocess.py에서 dtype을 uint32로 변경하십시오.",
            file=sys.stderr,
        )
        return 1

    # SHA-1 해시 기반 중복 제거
    # 같은 내용의 문서가 여러 번 나오면 그 내용이 학습을 지배하여 과적합 발생
    seen: set[str] = set()   # 이미 본 문서의 해시 값 집합
    docs: list[str] = []     # 중복 없는 문서 목록

    for doc in iter_texts(raw_dir):          # 각 파일에서 텍스트 읽기
        norm = normalize(doc)                # 정규화
        if not norm:                         # 빈 문서 건너뜀
            continue
        # SHA-1 해시로 문서 지문 계산
        h = hashlib.sha1(norm.encode("utf-8")).hexdigest()
        if h in seen:                        # 이미 본 문서는 건너뜀
            continue
        seen.add(h)                          # 해시 기록
        docs.append(norm)                    # 문서 추가

    if not docs:
        print(f"{raw_dir}에서 문서를 찾지 못했습니다.", file=sys.stderr)
        print("지원 형식: .txt .json .jsonl .pdf .docx .jpg .png 등", file=sys.stderr)
        return 1

    # 무작위 섞기 (씨앗값 고정으로 재현 가능)
    rng.shuffle(docs)

    # 훈련/검증 분할 (95%/5%)
    n_train = max(1, int(len(docs) * cfg.data.train_split))
    train_docs = docs[:n_train]
    # 검증 문서가 없으면 마지막 훈련 문서를 검증에도 사용
    val_docs = docs[n_train:] or [docs[-1]]

    def encode_batch(batch: list[str]) -> np.ndarray:
        """문서 목록을 어표 ID 배열로 변환.

        각 문서를 [BOS] + 어표들 + [EOS] 로 감싸고
        모든 문서를 하나의 긴 배열로 이어붙인다.
        """
        all_ids: list[int] = []
        for d in tqdm(batch, desc="어표화"):    # 진행률 표시
            ids = tok.encode(d)                 # 문서를 어표 ID로 변환
            all_ids.append(BOS_ID)              # 문서 시작 표시 추가
            all_ids.extend(ids)                 # 어표 ID들 추가
            all_ids.append(EOS_ID)              # 문서 끝 표시 추가
        # uint16: 0~65535 범위 정수 (어휘 크기 16,384에 충분)
        return np.asarray(all_ids, dtype=np.uint16)

    print(f"학습 문서 {len(train_docs):,}개 어표화 중...")
    train_arr = encode_batch(train_docs)    # 학습 문서 어표화

    print(f"검증 문서 {len(val_docs):,}개 어표화 중...")
    val_arr = encode_batch(val_docs)        # 검증 문서 어표화

    # 이진 파일로 저장 (tofile: numpy 배열을 원시 이진 형식으로 저장)
    train_path = out_dir / "train.bin"
    val_path   = out_dir / "val.bin"
    train_arr.tofile(train_path)
    val_arr.tofile(val_path)

    # 통계 정보 저장 (train.py가 이 파일을 읽어 vocab_size 등을 확인함)
    meta = {
        "tokenizer_model": str(tokenizer_path),  # 어표 분석기 경로
        "vocab_size": tok.vocab_size,             # 실제 어휘 크기 (설정값과 다를 수 있음)
        "train_tokens": int(train_arr.size),      # 학습 어표 수
        "val_tokens": int(val_arr.size),          # 검증 어표 수
        "n_train_docs": len(train_docs),          # 학습 문서 수
        "n_val_docs": len(val_docs),              # 검증 문서 수
        "dtype": "uint16",                        # 이진 파일의 자료형
    }
    with open(out_dir / "meta.json", "w", encoding="utf-8") as f:
        json.dump(meta, f, indent=2, ensure_ascii=False)

    print(f"\n완료.")
    print(f"  학습: {train_arr.size:,} 어표 → {train_path}")
    print(f"  검증: {val_arr.size:,} 어표 → {val_path}")
    print(f"  통계: {out_dir / 'meta.json'}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
