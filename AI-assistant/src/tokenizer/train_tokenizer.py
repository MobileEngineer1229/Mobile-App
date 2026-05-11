"""조선말 SentencePiece BPE 어표 분석기를 학습합니다.

지원 자료 형식: .txt .json .jsonl .pdf .docx 이미지(.jpg .png 등)

출력:
    checkpoints/tokenizer/dprk_sp.model
    checkpoints/tokenizer/dprk_sp.vocab

사용:
    python -m src.tokenizer.train_tokenizer --config config/model_config.yaml
"""

from __future__ import annotations

import argparse
import sys
import tempfile
import unicodedata
from pathlib import Path

import sentencepiece as spm

ROOT = Path(__file__).resolve().parents[2]
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))

from src.data.readers import iter_texts   # noqa: E402
from src.model.config import load_config  # noqa: E402


def normalize(text: str) -> str:
    text = unicodedata.normalize("NFC", text)
    return " ".join(text.split())


def main() -> int:
    parser = argparse.ArgumentParser(description="SentencePiece 어표 분석기 학습")
    parser.add_argument("--config", type=str, default="config/model_config.yaml")
    args = parser.parse_args()

    cfg = load_config(args.config)

    raw_dir = ROOT / cfg.data.raw_dir
    out_prefix = ROOT / "checkpoints" / cfg.tokenizer.output_prefix
    out_prefix.parent.mkdir(parents=True, exist_ok=True)

    if not raw_dir.exists() or not any(raw_dir.iterdir()):
        print(
            f"{raw_dir}에 자료가 없습니다.\n"
            f"지원 형식: .txt .json .jsonl .pdf .docx .jpg .png 등\n"
            f"자료를 넣은 후 다시 실행하십시오.",
            file=sys.stderr,
        )
        return 1

    # SentencePiece는 파일 경로를 입력받으므로 임시 파일에 정규화된 말뭉치를 씁니다.
    n_lines = 0
    n_chars = 0
    with tempfile.NamedTemporaryFile(
        mode="w", encoding="utf-8", suffix=".txt", delete=False
    ) as tmp:
        tmp_path = Path(tmp.name)
        for doc in iter_texts(raw_dir):
            for line in doc.splitlines():
                line = normalize(line)
                if line:
                    tmp.write(line + "\n")
                    n_lines += 1
                    n_chars += len(line)

    if n_lines == 0:
        print(f"{raw_dir}의 파일에서 텍스트를 추출하지 못했습니다.", file=sys.stderr)
        tmp_path.unlink(missing_ok=True)
        return 1

    # 소규모 말뭉치에서 어휘 크기가 너무 크면 오류 발생 → 자동 조정
    unique_chars = len(set(open(tmp_path, encoding="utf-8").read()))
    max_feasible = max(100, unique_chars * 6)
    vocab_size = min(cfg.tokenizer.vocab_size, max_feasible)
    if vocab_size < cfg.tokenizer.vocab_size:
        print(
            f"[어표 분석기] 자료가 적어 어휘 크기를 {cfg.tokenizer.vocab_size} → {vocab_size}로 조정합니다.\n"
            f"              자료를 더 추가하면 더 큰 어휘를 사용할 수 있습니다."
        )

    print(f"어표 분석기 학습 중: {n_lines:,}줄 / {n_chars:,}글자 (자료: {raw_dir})")
    print(f"출력 경로: {out_prefix}")

    spm.SentencePieceTrainer.Train(
        input=str(tmp_path),
        model_prefix=str(out_prefix),
        vocab_size=vocab_size,
        model_type=cfg.tokenizer.model_type,
        character_coverage=cfg.tokenizer.character_coverage,
        # 특수 어표 순서는 src/tokenizer/tokenizer.py의 상수와 일치해야 합니다.
        pad_id=0, bos_id=1, eos_id=2, unk_id=3,
        pad_piece="<pad>", bos_piece="<bos>", eos_piece="<eos>", unk_piece="<unk>",
        normalization_rule_name="nfkc",
        input_sentence_size=1_000_000,
        shuffle_input_sentence=True,
        num_threads=8,
    )

    tmp_path.unlink(missing_ok=True)

    print(f"\n완료. 어표 분석기 저장 경로:")
    print(f"  {out_prefix}.model")
    print(f"  {out_prefix}.vocab")
    return 0


if __name__ == "__main__":
    sys.exit(main())
