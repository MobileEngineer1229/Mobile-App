"""Convert raw data into binary word tag files for training.

【Beginner's Guide】
  This file is the second stage of the training pipeline.
  Convert human-readable text into a model-readable array of numbers.

  processing flow:
    original files (.txt, .json, .pdf etc.)
      ↓ iter_texts() Extract text with
    A string list of all documents
      ↓ SHA-1 Remove duplicates with hashes
    List of documents without duplicates
      ↓ 95 after random shuffling%/5% split
    training document / verification document
      ↓ tok.encode() mark as + BOS/EOS wrapping
    list of integer numbers
      ↓ numpy uint16 Save as array
    data/processed/train.bin, val.bin, meta.json

  output file:
    data/processed/train.bin   uint16 tag ID arrangement (For learning)
    data/processed/val.bin     uint16 tag ID arrangement (For verification)
    data/processed/meta.json   vocabulary size, Statistics such as number of tickets

  Wrap each document:
    [BOS_ID] + tag1 + tag2 + ... + [EOS_ID]
    → Enables the model to learn document boundaries

  Supported Format: .txt .json .jsonl .pdf .docx image(.jpg .png etc.)

  How to run:
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

# Add project root to python path
ROOT = Path(__file__).resolve().parents[2]
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))

from src.data.readers import iter_texts          # noqa: E402  Read various format files
from src.model.config import load_config         # noqa: E402  Load settings
from src.tokenizer.tokenizer import BOS_ID, EOS_ID, load_tokenizer  # noqa: E402  ticket analyzer


def normalize(text: str) -> str:
    """Normalize strings.

    Processing details:
        1. Unicode NFC normalization: Unification of Korean alphabet combinations, etc.
        2. Normalize whitespace on each line: tab, Consecutive spaces as one
        3. Remove blank lines

    argument:
        text: string to normalize

    return value:
        normalized string
    """
    text = unicodedata.normalize("NFC", text)  # Unicode NFC normalization
    # Normalize each line and remove blank lines
    lines = [" ".join(line.split()) for line in text.splitlines()]
    return "\n".join(line for line in lines if line)


def main() -> int:
    """Preprocessing pipeline main function.

    Processing order:
      1. Read configuration file
      2. Loading the tag analyzer
      3. Read the original material + normalization + SHA-1 Deduplication
      4. Training after random shuffling/Verification Split (95%/5%)
      5. tokenization: each document [BOS] + stamps + [EOS] convert to
      6. uint16 Save as binary file
      7. statistical information meta.json save

    return value:
        0: success
        1: error
    """
    # Command line argument processing
    parser = argparse.ArgumentParser(description="Preprocessing: original material → binary tag file")
    parser.add_argument("--config", type=str, default="config/model_config.yaml")
    parser.add_argument("--seed", type=int, default=42)  # Random shuffling reproducibility
    args = parser.parse_args()

    cfg = load_config(args.config)
    rng = random.Random(args.seed)  # Results are reproducible with fixed seed values

    # route settings
    raw_dir = ROOT / cfg.data.raw_dir          # Original Materials Folder
    out_dir = ROOT / cfg.data.processed_dir    # output folder
    out_dir.mkdir(parents=True, exist_ok=True) # If the output folder does not exist, create it

    # Loading the tag analyzer
    tokenizer_path = ROOT / "checkpoints" / f"{cfg.tokenizer.output_prefix}.model"
    tok = load_tokenizer(tokenizer_path)

    # uint16 maximum value of format = 65535 → If the vocabulary size exceeds this, storage is not possible.
    if tok.vocab_size > 65535:
        print(
            f"vocabulary size {tok.vocab_size} > 65535; "
            f"preprocess.pyin dtypeto uint32Please change to.",
            file=sys.stderr,
        )
        return 1

    # SHA-1 Hash-based deduplication
    # If a document with the same content appears multiple times, that content dominates learning, resulting in overfitting.
    seen: set[str] = set()   # A set of hash values for documents already viewed
    docs: list[str] = []     # List of documents without duplicates

    for doc in iter_texts(raw_dir):          # Read text from each file
        norm = normalize(doc)                # normalization
        if not norm:                         # Skip blank document
            continue
        # SHA-1 Compute document fingerprint with hash
        h = hashlib.sha1(norm.encode("utf-8")).hexdigest()
        if h in seen:                        # Skip this document already viewed
            continue
        seen.add(h)                          # hash record
        docs.append(norm)                    # Add document

    if not docs:
        print(f"{raw_dir}No documentation found on.", file=sys.stderr)
        print("Supported Format: .txt .json .jsonl .pdf .docx .jpg .png etc.", file=sys.stderr)
        return 1

    # random shuffle (Reproducible by fixing the seed price)
    rng.shuffle(docs)

    # training/Verification Split (95%/5%)
    n_train = max(1, int(len(docs) * cfg.data.train_split))
    train_docs = docs[:n_train]
    # If there is no verification document, the last training document is also used for verification.
    val_docs = docs[n_train:] or [docs[-1]]

    def encode_batch(batch: list[str]) -> np.ndarray:
        """list of documents ID convert to array.

        each document [BOS] + stamps + [EOS] Wrap it with
        Concatenate all documents into one long array.
        """
        all_ids: list[int] = []
        for d in tqdm(batch, desc="tokenization"):    # Show progress
            ids = tok.encode(d)                 # document IDconvert to
            all_ids.append(BOS_ID)              # Add document start indicator
            all_ids.extend(ids)                 # tag IDadd field
            all_ids.append(EOS_ID)              # Add end-of-document marker
        # uint16: 0~65535 range integer (vocabulary size 16,384enough to)
        return np.asarray(all_ids, dtype=np.uint16)

    print(f"study document {len(train_docs):,}Dog tagging in progress...")
    train_arr = encode_batch(train_docs)    # Learning document notation

    print(f"verification document {len(val_docs):,}Dog tagging in progress...")
    val_arr = encode_batch(val_docs)        # Verification document tabulation

    # Save as binary file (tofile: numpy Save array in raw binary format)
    train_path = out_dir / "train.bin"
    val_path   = out_dir / "val.bin"
    train_arr.tofile(train_path)
    val_arr.tofile(val_path)

    # Save statistical information (train.pyread this file vocab_size Check the back)
    meta = {
        "tokenizer_model": str(tokenizer_path),  # Tag analyzer path
        "vocab_size": tok.vocab_size,             # actual vocabulary size (May differ from settings)
        "train_tokens": int(train_arr.size),      # number of study notes
        "val_tokens": int(val_arr.size),          # number of verification votes
        "n_train_docs": len(train_docs),          # Number of training documents
        "n_val_docs": len(val_docs),              # Number of verification documents
        "dtype": "uint16",                        # Data type of binary file
    }
    with open(out_dir / "meta.json", "w", encoding="utf-8") as f:
        json.dump(meta, f, indent=2, ensure_ascii=False)

    print(f"\ndone.")
    print(f"  learning: {train_arr.size:,} tag → {train_path}")
    print(f"  verification: {val_arr.size:,} tag → {val_path}")
    print(f"  statistics: {out_dir / 'meta.json'}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
