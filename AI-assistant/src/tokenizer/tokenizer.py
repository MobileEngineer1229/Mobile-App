"""SentencePiece 어표 분석기 포장 클라스.

【초보자 안내】
  이 파일은 조선말 글자를 정수 번호로 바꾸고, 반대로도 바꾸는 도구다.

  왜 SentencePiece BPE를 쓰는가:
    - 조선말은 교착어(접미사를 붙여 단어를 만드는 언어)다.
      "먹다", "먹고", "먹어서", "먹겠다" 등 하나의 어근에서 수많은 형태가 나온다.
      BPE는 이 형태들을 자동으로 조각내어 처리한다.
    - 별도의 형태소 분석기 없이 원문 그대로 처리 가능.
    - .model 파일 하나만 있으면 밀리초 안에 불러올 수 있다.

  특수 어표 ID (4개):
    PAD_ID = 0  : 패딩 — 길이를 맞추기 위해 빈 자리를 채우는 데 씀
    BOS_ID = 1  : 글 시작 표시 (Beginning Of Sequence)
    EOS_ID = 2  : 글 끝 표시 (End Of Sequence)
    UNK_ID = 3  : 알 수 없는 글자 (어휘에 없는 글자)

  이 ID 순서는 train_tokenizer.py와 반드시 일치해야 한다.
"""

from __future__ import annotations

from pathlib import Path

import sentencepiece as spm


# 특수 어표 ID — train_tokenizer.py의 pad_id, bos_id, eos_id, unk_id 와 반드시 같아야 함
PAD_ID = 0  # 패딩 어표: 묶음 처리 시 길이를 맞추는 데 씀
BOS_ID = 1  # 글 시작 어표: 모든 문서/대화의 맨 앞에 추가
EOS_ID = 2  # 글 끝 어표: 모든 문서/대화의 맨 뒤에 추가, 생성 종료 신호
UNK_ID = 3  # 알 수 없는 어표: 어휘에 없는 글자를 이 번호로 처리


class Tokenizer:
    """SentencePiece BPE 어표 분석기 포장 클라스.

    【초보자 안내】
      이 클라스는 SentencePiece 라이브러리를 감싸서 사용하기 쉽게 만든 것이다.

      주요 기능:
        encode("김치 담그는 방법") → [342, 156, 891, ...]  (글자 → 번호)
        decode([342, 156, 891, ...]) → "김치 담그는 방법"   (번호 → 글자)
    """

    def __init__(self, model_path: str | Path):
        """어표 분석기를 초기화한다.

        인수:
            model_path: SentencePiece .model 파일의 경로
                        기본 경로: checkpoints/tokenizer/dprk_sp.model
        """
        self.sp = spm.SentencePieceProcessor()  # SentencePiece 처리기 생성
        self.sp.Load(str(model_path))           # .model 파일 불러오기
        self.model_path = str(model_path)       # 경로를 나중에 참고할 수 있게 저장

    @property
    def vocab_size(self) -> int:
        """어휘 크기 (정의된 어표 수)를 반환.

        이 값은 설정의 vocab_size 와 일치해야 한다.
        train_tokenizer.py가 자료 크기에 따라 자동으로 줄일 수 있으므로
        항상 이 속성으로 실제 크기를 확인해야 한다.
        """
        return self.sp.GetPieceSize()

    def encode(self, text: str, add_bos: bool = False, add_eos: bool = False) -> list[int]:
        """조선말 글자렬을 정수 번호 목록으로 변환.

        인수:
            text:    변환할 조선말 글자렬
            add_bos: True이면 목록 앞에 BOS_ID(1)를 추가
            add_eos: True이면 목록 뒤에 EOS_ID(2)를 추가

        반환값:
            정수 번호의 목록

        례:
            encode("김치")           → [342]
            encode("김치", add_bos=True, add_eos=True) → [1, 342, 2]
        """
        ids = self.sp.EncodeAsIds(text)  # 글자 → 번호 변환
        if add_bos:
            ids = [BOS_ID] + ids  # 맨 앞에 글 시작 표시 추가
        if add_eos:
            ids = ids + [EOS_ID]  # 맨 뒤에 글 끝 표시 추가
        return ids

    def decode(self, ids: list[int]) -> str:
        """정수 번호 목록을 조선말 글자렬로 변환.

        특수 어표(PAD, BOS, EOS, UNK)는 모두 제거한다.
        UNK의 표면 문자열("⁇")도 제거한다.

        인수:
            ids: 변환할 정수 번호 목록

        반환값:
            조선말 글자렬 (특수 어표 제거 후)

        례:
            decode([1, 342, 156, 2]) → "김치 담그는"
            (1=BOS, 342="김치", 156="담그는", 2=EOS → BOS와 EOS 제거됨)
        """
        # 특수 어표 ID를 먼저 걸러냄 (PAD, BOS, EOS, UNK 제거)
        clean = [i for i in ids if i not in (PAD_ID, BOS_ID, EOS_ID, UNK_ID)]
        # 번호 목록을 글자렬로 변환
        text = self.sp.DecodeIds(clean)
        # SentencePiece가 UNK 글자를 "⁇" 로 출력할 수 있음 → 제거
        return text.replace("⁇", "").replace(" ⁇ ", " ").replace("⁇", "").strip()

    def __repr__(self) -> str:
        """어표 분석기의 간략한 설명을 반환 (print 시 표시됨)."""
        return f"Tokenizer(vocab_size={self.vocab_size}, model={self.model_path})"


def load_tokenizer(model_path: str | Path) -> Tokenizer:
    """어표 분석기를 파일에서 불러온다.

    파일이 없으면 알기 쉬운 오류 메시지를 표시한다.

    인수:
        model_path: SentencePiece .model 파일의 경로

    반환값:
        Tokenizer 인스턴스

    오류:
        FileNotFoundError: .model 파일이 없을 때
                           (01_train_tokenizer.ps1을 먼저 실행해야 함)
    """
    p = Path(model_path)
    if not p.exists():
        raise FileNotFoundError(
            f"어표 분석기 모형 파일이 없습니다: {p}\n"
            f"scripts/01_train_tokenizer.ps1 을 먼저 실행하십시오."
        )
    return Tokenizer(p)
