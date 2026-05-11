"""원본 자료를 학습용 이진 어표 파일로 변환합니다.

지원 형식: .txt .json .jsonl .pdf .docx 이미지(.jpg .png 등)

출력:
    data/processed/train.bin   uint16 어표 ID 배열
    data/processed/val.bin     uint16 어표 ID 배열
    data/processed/meta.json   통계 및 어표 분석기 경로

각 문서는 <bos> 어표1 어표2 ... <eos> 형태로 포장됩니다.
이를 통해 모형이 문서 경계를 정확히 학습합니다.

사용:
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

ROOT = Path(__file__).resolve().parents[2]
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))

from src.data.readers import iter_texts          # noqa: E402
from src.model.config import load_config         # noqa: E402
from src.tokenizer.tokenizer import BOS_ID, EOS_ID, load_tokenizer  # noqa: E402


def normalize(text: str) -> str:
    text = unicodedata.normalize("NFC", text)
    lines = [" ".join(line.split()) for line in text.splitlines()]
    return "\n".join(line for line in lines if line)


def main() -> int:
    parser = argparse.ArgumentParser(description="전처리: 원본 자료 → 이진 어표 파일")
    parser.add_argument("--config", type=str, default="config/model_config.yaml")
    parser.add_argument("--seed", type=int, default=42)
    args = parser.parse_args()

    cfg = load_config(args.config)
    rng = random.Random(args.seed)

    raw_dir = ROOT / cfg.data.raw_dir
    out_dir = ROOT / cfg.data.processed_dir
    out_dir.mkdir(parents=True, exist_ok=True)

    tokenizer_path = ROOT / "checkpoints" / f"{cfg.tokenizer.output_prefix}.model"
    tok = load_tokenizer(tokenizer_path)

    if tok.vocab_size > 65535:
        print(f"어휘 크기 {tok.vocab_size} > 65535; preprocess.py에서 dtype을 uint32로 변경하십시오.", file=sys.stderr)
        return 1

    # 해시 기반 중복 제거 (같은 내용의 문서가 학습을 지배하지 않도록)
    seen: set[str] = set()
    docs: list[str] = []
    for doc in iter_texts(raw_dir):
        norm = normalize(doc)
        if not norm:
            continue
        h = hashlib.sha1(norm.encode("utf-8")).hexdigest()
        if h in seen:
            continue
        seen.add(h)
        docs.append(norm)

    if not docs:
        print(f"{raw_dir}에서 문서를 찾지 못했습니다.", file=sys.stderr)
        print("지원 형식: .txt .json .jsonl .pdf .docx .jpg .png 등", file=sys.stderr)
        return 1

    rng.shuffle(docs)
    n_train = max(1, int(len(docs) * cfg.data.train_split))
    train_docs = docs[:n_train]
    val_docs = docs[n_train:] or [docs[-1]]

    def encode_batch(batch: list[str]) -> np.ndarray:
        all_ids: list[int] = []
        for d in tqdm(batch, desc="어표화"):
            ids = tok.encode(d)
            all_ids.append(BOS_ID)
            all_ids.extend(ids)
            all_ids.append(EOS_ID)
        return np.asarray(all_ids, dtype=np.uint16)

    print(f"학습 문서 {len(train_docs):,}개 어표화 중...")
    train_arr = encode_batch(train_docs)
    print(f"검증 문서 {len(val_docs):,}개 어표화 중...")
    val_arr = encode_batch(val_docs)

    train_path = out_dir / "train.bin"
    val_path = out_dir / "val.bin"
    train_arr.tofile(train_path)
    val_arr.tofile(val_path)

    meta = {
        "tokenizer_model": str(tokenizer_path),
        "vocab_size": tok.vocab_size,
        "train_tokens": int(train_arr.size),
        "val_tokens": int(val_arr.size),
        "n_train_docs": len(train_docs),
        "n_val_docs": len(val_docs),
        "dtype": "uint16",
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
