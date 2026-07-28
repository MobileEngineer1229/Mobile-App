"""SentencePiece Tag analyzer packaging class.

【Beginner's Guide】
  This file converts Korean characters into integer numbers., It is also a tool that changes the other way around..

  why SentencePiece BPEDo you write:
    - Korean is an agglutinative language(A language that creates words by adding suffixes)Everything.
      "eat", "eat", "By eating", "I will eat it" Many forms arise from one root, such as.
      BPEautomatically fragments and processes these shapes..
    - The original text can be processed as is without a separate morpheme analyzer..
    - .model All you need is one file and it can be loaded in milliseconds..

  special tag ID (4dog):
    PAD_ID = 0  : padding — Used to fill empty spaces to adjust length
    BOS_ID = 1  : Post start indicator (Beginning Of Sequence)
    EOS_ID = 2  : Show end of post (End Of Sequence)
    UNK_ID = 3  : unknown letters (letters not in vocabulary)

  This ID The order is train_tokenizer.pymust match.
"""

from __future__ import annotations

from pathlib import Path

import sentencepiece as spm


# special tag ID — train_tokenizer.pyof pad_id, bos_id, eos_id, unk_id Must be equal to
PAD_ID = 0  # padded tag: Used to adjust length when bundled.
BOS_ID = 1  # start of article: all documents/Add to the beginning of the conversation
EOS_ID = 2  # End of article quote: all documents/Add to the end of the conversation, Generation end signal
UNK_ID = 3  # unknown mark: Handle letters not in the vocabulary with this number


class Tokenizer:
    """SentencePiece BPE Tag analyzer packaging class.

    【Beginner's Guide】
      This class SentencePiece It wraps the library to make it easier to use..

      Main features:
        encode("How to make kimchi") → [342, 156, 891, ...]  (letters → number)
        decode([342, 156, 891, ...]) → "How to make kimchi"   (number → letters)
    """

    def __init__(self, model_path: str | Path):
        """Initialize the phrase analyzer.

        argument:
            model_path: SentencePiece .model path to file
                        default path: checkpoints/tokenizer/dprk_sp.model
        """
        self.sp = spm.SentencePieceProcessor()  # SentencePiece Create a handler
        self.sp.Load(str(model_path))           # .model Load file
        self.model_path = str(model_path)       # Save the route for future reference

    @property
    def vocab_size(self) -> int:
        """vocabulary size (number of tokens defined)return.

        This value is set to vocab_size must match.
        train_tokenizer.pycan be automatically reduced according to the data size, so
        You should always check the actual size with this property.
        """
        return self.sp.GetPieceSize()

    def encode(self, text: str, add_bos: bool = False, add_eos: bool = False) -> list[int]:
        """Convert Korean character string to list of integer numbers.

        argument:
            text:    Korean character string to convert
            add_bos: Truefront of list BOS_ID(1)add
            add_eos: TrueAfter the side list EOS_ID(2)add

        return value:
            list of integer numbers

        example:
            encode("kimchi")           → [342]
            encode("kimchi", add_bos=True, add_eos=True) → [1, 342, 2]
        """
        ids = self.sp.EncodeAsIds(text)  # letters → number conversion
        if add_bos:
            ids = [BOS_ID] + ids  # Add post start mark at the beginning
        if add_eos:
            ids = ids + [EOS_ID]  # Add end of post mark at the end
        return ids

    def decode(self, ids: list[int]) -> str:
        """Convert list of integer numbers to Korean character string.

        special tag(PAD, BOS, EOS, UNK)remove all.
        UNKsurface string of("⁇")also remove.

        argument:
            ids: List of integer numbers to convert

        return value:
            Korean character string (After removing the special tag)

        example:
            decode([1, 342, 156, 2]) → "making kimchi"
            (1=BOS, 342="kimchi", 156="dipping", 2=EOS → BOSWow EOS removed)
        """
        # special tag IDfilter out first (PAD, BOS, EOS, UNK remove)
        clean = [i for i in ids if i not in (PAD_ID, BOS_ID, EOS_ID, UNK_ID)]
        # Convert list of numbers to string of characters
        text = self.sp.DecodeIds(clean)
        # SentencePiecego UNK letters "⁇" Can be output as → remove
        return text.replace("⁇", "").replace(" ⁇ ", " ").replace("⁇", "").strip()

    def __repr__(self) -> str:
        """Returns a brief description of the tag analyzer (print Hours displayed)."""
        return f"Tokenizer(vocab_size={self.vocab_size}, model={self.model_path})"


def load_tokenizer(model_path: str | Path) -> Tokenizer:
    """Load the phrase analyzer from a file.

    If the file does not exist, an easy-to-understand error message is displayed..

    argument:
        model_path: SentencePiece .model path to file

    return value:
        Tokenizer instance

    error:
        FileNotFoundError: .model When there is no file
                           (01_train_tokenizer.ps1must be run first)
    """
    p = Path(model_path)
    if not p.exists():
        raise FileNotFoundError(
            f"There is no tag analyzer model file: {p}\n"
            f"scripts/01_train_tokenizer.ps1 Run first."
        )
    return Tokenizer(p)
