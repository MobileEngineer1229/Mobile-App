"""조선말 SentencePiece BPE 어표 분석기를 학습합니다.

【초보자 안내】
  이 파일은 조선말 자료(텍스트, JSON, PDF 등)를 읽어
  SentencePiece BPE 어표 분석기를 처음부터 훈련한다.

  BPE (Byte Pair Encoding) 란:
    1. 처음에는 모든 글자를 개별 단위로 취급
    2. 가장 자주 함께 나오는 두 단위를 하나로 합침
    3. 이 과정을 어휘 크기(vocab_size)에 도달할 때까지 반복
    결과: 자주 나오는 단어는 통째로, 드문 단어는 조각으로 처리

  지원 자료 형식: .txt .json .jsonl .pdf .docx 이미지(.jpg .png 등)

  출력 파일:
    checkpoints/tokenizer/dprk_sp.model  ← 훈련된 어표 분석기 (사용 시 필요)
    checkpoints/tokenizer/dprk_sp.vocab  ← 어휘 목록 (참고용)

  실행 방법:
    python -m src.tokenizer.train_tokenizer --config config/model_config.yaml
"""

from __future__ import annotations

import argparse
import sys
import tempfile
import unicodedata
from pathlib import Path

import sentencepiece as spm

# 프로젝트 루트를 파이썬 경로에 추가 (어디서 실행해도 불러오기 가능하도록)
ROOT = Path(__file__).resolve().parents[2]
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))

from src.data.readers import iter_texts   # noqa: E402  자료 읽기 함수
from src.model.config import load_config  # noqa: E402  설정 불러오기 함수


def normalize(text: str) -> str:
    """글자렬을 정규화한다.

    처리 내용:
        1. Unicode NFC 정규화: 같은 글자의 다른 표현을 통일
           (예: 한글 자모를 조합하여 완성형 글자로)
        2. 공백 정규화: 여러 개의 공백, 탭, 줄바꿈을 하나의 공백으로

    훈련 자료가 다양한 출처에서 올 때 형식을 통일하는 데 필요하다.
    """
    text = unicodedata.normalize("NFC", text)  # Unicode NFC 정규화
    return " ".join(text.split())              # 공백 정규화 (탭, 줄바꿈 포함)


def main() -> int:
    """어표 분석기 훈련의 전체 흐름을 처리하는 주 함수.

    처리 순서:
      1. 설정 파일 읽기
      2. 원본 자료 폴더에서 모든 텍스트 추출
      3. 임시 파일에 정규화된 말뭉치 저장
      4. 자료 크기에 따라 어휘 크기 자동 조정 (필요 시)
      5. SentencePiece 훈련 실행
      6. 임시 파일 삭제

    반환값:
        0: 성공
        1: 오류 (자료 없음 또는 텍스트 추출 실패)
    """
    # 명령줄 인수 처리
    parser = argparse.ArgumentParser(description="SentencePiece 어표 분석기 학습")
    parser.add_argument("--config", type=str, default="config/model_config.yaml")
    args = parser.parse_args()

    # 설정 파일 불러오기
    cfg = load_config(args.config)

    # 경로 설정
    raw_dir = ROOT / cfg.data.raw_dir                               # 원본 자료 폴더
    out_prefix = ROOT / "checkpoints" / cfg.tokenizer.output_prefix # 출력 파일 경로
    out_prefix.parent.mkdir(parents=True, exist_ok=True)            # 폴더가 없으면 생성

    # 원본 자료 폴더가 비어있는지 확인
    if not raw_dir.exists() or not any(raw_dir.iterdir()):
        print(
            f"{raw_dir}에 자료가 없습니다.\n"
            f"지원 형식: .txt .json .jsonl .pdf .docx .jpg .png 등\n"
            f"자료를 넣은 후 다시 실행하십시오.",
            file=sys.stderr,
        )
        return 1

    # SentencePiece는 파일 경로를 입력받으므로
    # 정규화된 말뭉치를 임시 파일에 먼저 저장한다
    n_lines = 0  # 총 줄 수 (진행 상황 표시용)
    n_chars = 0  # 총 글자 수 (진행 상황 표시용)
    with tempfile.NamedTemporaryFile(
        mode="w", encoding="utf-8", suffix=".txt", delete=False
    ) as tmp:
        tmp_path = Path(tmp.name)  # 임시 파일 경로 저장 (나중에 삭제하기 위해)
        for doc in iter_texts(raw_dir):       # 각 문서를 글자렬로 받음
            for line in doc.splitlines():     # 문서를 줄 단위로 나눔
                line = normalize(line)        # 정규화
                if line:                      # 빈 줄은 건너뜀
                    tmp.write(line + "\n")    # 임시 파일에 저장
                    n_lines += 1
                    n_chars += len(line)

    # 텍스트가 하나도 없으면 오류
    if n_lines == 0:
        print(f"{raw_dir}의 파일에서 텍스트를 추출하지 못했습니다.", file=sys.stderr)
        tmp_path.unlink(missing_ok=True)  # 빈 임시 파일 삭제
        return 1

    # 자동 어휘 크기 조정:
    # 자료가 적으면 SentencePiece가 원하는 크기만큼 어휘를 만들지 못한다.
    # 고유 글자 수 × 6 을 실현 가능한 최대 어휘 크기로 추정한다.
    unique_chars = len(set(open(tmp_path, encoding="utf-8").read()))  # 고유 글자 종류 수
    max_feasible = max(100, unique_chars * 6)  # 최소 100개는 보장
    vocab_size = min(cfg.tokenizer.vocab_size, max_feasible)  # 설정값과 비교해 작은 것 선택
    if vocab_size < cfg.tokenizer.vocab_size:
        # 설정값보다 줄었으면 사용자에게 알림
        print(
            f"[어표 분석기] 자료가 적어 어휘 크기를 {cfg.tokenizer.vocab_size} → {vocab_size}로 조정합니다.\n"
            f"              자료를 더 추가하면 더 큰 어휘를 사용할 수 있습니다."
        )

    print(f"어표 분석기 학습 중: {n_lines:,}줄 / {n_chars:,}글자 (자료: {raw_dir})")
    print(f"출력 경로: {out_prefix}")

    # SentencePiece 훈련 실행
    spm.SentencePieceTrainer.Train(
        input=str(tmp_path),               # 임시 말뭉치 파일 경로
        model_prefix=str(out_prefix),      # 출력 파일 경로 접두사
        vocab_size=vocab_size,             # 어휘 크기 (자동 조정된 값)
        model_type=cfg.tokenizer.model_type,             # "bpe"
        character_coverage=cfg.tokenizer.character_coverage,  # 0.9995

        # 특수 어표 ID — tokenizer.py의 상수와 반드시 같아야 함
        # 순서: PAD=0, BOS=1, EOS=2, UNK=3
        pad_id=0, bos_id=1, eos_id=2, unk_id=3,
        pad_piece="<pad>", bos_piece="<bos>", eos_piece="<eos>", unk_piece="<unk>",

        normalization_rule_name="nfkc",   # Unicode NFKC 정규화 적용
        input_sentence_size=1_000_000,    # 최대 100만 줄 (너무 크면 자동 표본추출)
        shuffle_input_sentence=True,      # 입력 줄을 무작위 섞기 (학습 안정성)
        num_threads=8,                    # 병렬 처리 스레드 수
    )

    # 훈련이 끝나면 임시 파일 삭제
    tmp_path.unlink(missing_ok=True)

    print(f"\n완료. 어표 분석기 저장 경로:")
    print(f"  {out_prefix}.model")
    print(f"  {out_prefix}.vocab")
    return 0


if __name__ == "__main__":
    sys.exit(main())
