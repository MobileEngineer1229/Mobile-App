"""Joseon language SentencePiece BPE Learn about phrase analyzer.

【Beginner's Guide】
  This file is Joseon language data(text, JSON, PDF etc.)read
  SentencePiece BPE Train a phrase analyzer from scratch.

  BPE (Byte Pair Encoding) column:
    1. Initially treat every letter as an individual unit
    2. Combining the two most frequently occurring together units into one
    3. This process is called vocabulary size(vocab_size)repeat until reached
    result: All words that appear frequently, Rare words are treated as fragments

  Supported data format: .txt .json .jsonl .pdf .docx image(.jpg .png etc.)

  output file:
    checkpoints/tokenizer/dprk_sp.model  ← Trained tag analyzer (Required for use)
    checkpoints/tokenizer/dprk_sp.vocab  ← vocabulary list (For reference only)

  How to run:
    python -m src.tokenizer.train_tokenizer --config config/model_config.yaml
"""

from __future__ import annotations

import argparse
import sys
import tempfile
import unicodedata
from pathlib import Path

import sentencepiece as spm

# Add project root to python path (So that it can be loaded no matter where you run it)
ROOT = Path(__file__).resolve().parents[2]
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))

from src.data.readers import iter_texts   # noqa: E402  Data read function
from src.model.config import load_config  # noqa: E402  Settings load function


def normalize(text: str) -> str:
    """Normalize strings.

    Processing details:
        1. Unicode NFC normalization: Unify different expressions of the same letter
           (yes: Combining Korean alphabets into complete letters)
        2. White space normalization: multiple spaces, tab, Line break with a single space

    When training materials come from a variety of sources, there is a need to unify the format..
    """
    text = unicodedata.normalize("NFC", text)  # Unicode NFC normalization
    return " ".join(text.split())              # White space normalization (tab, Includes line breaks)


def main() -> int:
    """Main function that handles the entire flow of tag analyzer training.

    Processing order:
      1. Read configuration file
      2. Extract all text from source material folder
      3. Store normalized corpora in temporary files
      4. Automatically adjust vocabulary size based on data size (When needed)
      5. SentencePiece training run
      6. Delete temporary files

    return value:
        0: success
        1: error (No data or text extraction failed)
    """
    # Command line argument processing
    parser = argparse.ArgumentParser(description="SentencePiece Learn about word tag analyzer")
    parser.add_argument("--config", type=str, default="config/model_config.yaml")
    args = parser.parse_args()

    # Load configuration file
    cfg = load_config(args.config)

    # route settings
    raw_dir = ROOT / cfg.data.raw_dir                               # Original Materials Folder
    out_prefix = ROOT / "checkpoints" / cfg.tokenizer.output_prefix # Output file path
    out_prefix.parent.mkdir(parents=True, exist_ok=True)            # Create folder if it does not exist

    # Make sure the source material folder is empty
    if not raw_dir.exists() or not any(raw_dir.iterdir()):
        print(
            f"{raw_dir}There is no data in.\n"
            f"Supported Format: .txt .json .jsonl .pdf .docx .jpg .png etc.\n"
            f"After inserting the data, run again.",
            file=sys.stderr,
        )
        return 1

    # SentencePiecereceives the file path as input.
    # First save the normalized corpus to a temporary file.
    n_lines = 0  # total number of lines (To display progress)
    n_chars = 0  # total number of characters (To display progress)
    with tempfile.NamedTemporaryFile(
        mode="w", encoding="utf-8", suffix=".txt", delete=False
    ) as tmp:
        tmp_path = Path(tmp.name)  # Save temporary file path (to delete later)
        for doc in iter_texts(raw_dir):       # Receive each document as a string
            for line in doc.splitlines():     # Divide the document into lines
                line = normalize(line)        # normalization
                if line:                      # Skip blank lines
                    tmp.write(line + "\n")    # Save to temporary file
                    n_lines += 1
                    n_chars += len(line)

    # Error if there is no text
    if n_lines == 0:
        print(f"{raw_dir}Failed to extract text from file in.", file=sys.stderr)
        tmp_path.unlink(missing_ok=True)  # Delete empty temporary files
        return 1

    # Automatic vocabulary resizing:
    # If there is little data SentencePiececannot create a vocabulary of the desired size.
    # Number of unique characters × 6 is estimated as the maximum feasible vocabulary size..
    unique_chars = len(set(open(tmp_path, encoding="utf-8").read()))  # Number of unique character types
    max_feasible = max(100, unique_chars * 6)  # At least 100 guaranteed
    vocab_size = min(cfg.tokenizer.vocab_size, max_feasible)  # Select something smaller than the set value
    if vocab_size < cfg.tokenizer.vocab_size:
        # Notify the user if the value decreases below the set value
        print(
            f"[ticket analyzer] The vocabulary size is limited due to the lack of data. {cfg.tokenizer.vocab_size} → {vocab_size}Adjust to.\n"
            f"              Adding more material allows for a larger vocabulary."
        )

    print(f"Learning to use a phrase analyzer: {n_lines:,}line / {n_chars:,}letters (material: {raw_dir})")
    print(f"output path: {out_prefix}")

    # SentencePiece training run
    spm.SentencePieceTrainer.Train(
        input=str(tmp_path),               # Temporary corpus file path
        model_prefix=str(out_prefix),      # Output file path prefix
        vocab_size=vocab_size,             # vocabulary size (Auto-adjusted value)
        model_type=cfg.tokenizer.model_type,             # "bpe"
        character_coverage=cfg.tokenizer.character_coverage,  # 0.9995

        # special tag ID — tokenizer.pyMust be equal to the constant of
        # order: PAD=0, BOS=1, EOS=2, UNK=3
        pad_id=0, bos_id=1, eos_id=2, unk_id=3,
        pad_piece="<pad>", bos_piece="<bos>", eos_piece="<eos>", unk_piece="<unk>",

        normalization_rule_name="nfkc",   # Unicode NFKC Apply normalization
        input_sentence_size=1_000_000,    # Up to 1 million lines (Automatic sampling if too large)
        shuffle_input_sentence=True,      # shuffle input lines (learning stability)
        num_threads=8,                    # Number of parallel processing threads
    )

    # Delete temporary files when training is over
    tmp_path.unlink(missing_ok=True)

    print(f"\ndone. Tag analyzer storage path:")
    print(f"  {out_prefix}.model")
    print(f"  {out_prefix}.vocab")
    return 0


if __name__ == "__main__":
    sys.exit(main())
