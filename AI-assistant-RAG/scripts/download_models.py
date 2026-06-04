"""Download local neural models for offline RAG.

Purpose:
    Cache the embedding and reranker models inside this project so the chatbot
    can run later without internet access.

Outputs:
    models/embedding/
    models/reranker/

Models:
    - intfloat/multilingual-e5-small
      Multilingual dense embedding model suitable for Korean and English.
    - cross-encoder/mmarco-mMiniLMv2-L12-H384-v1
      Multilingual cross-encoder reranker used after hybrid retrieval.
    - Qwen/Qwen2.5-0.5B-Instruct
      Small multilingual local instruction model for grounded answer writing.
"""

from __future__ import annotations

from pathlib import Path

from sentence_transformers import CrossEncoder, SentenceTransformer
from transformers import AutoModelForCausalLM, AutoTokenizer


ROOT = Path(__file__).resolve().parents[1]
MODELS_DIR = ROOT / "models"
EMBEDDING_MODEL_ID = "intfloat/multilingual-e5-small"
RERANKER_MODEL_ID = "cross-encoder/mmarco-mMiniLMv2-L12-H384-v1"
GENERATOR_MODEL_ID = "Qwen/Qwen2.5-0.5B-Instruct"


def main() -> int:
    MODELS_DIR.mkdir(parents=True, exist_ok=True)

    embedding_path = MODELS_DIR / "embedding"
    reranker_path = MODELS_DIR / "reranker"
    generator_path = MODELS_DIR / "generator"

    if (embedding_path / "config.json").exists():
        print(f"[models] embedding model already exists: {embedding_path}")
    else:
        print(f"[models] downloading embedding model: {EMBEDDING_MODEL_ID}")
        embedding = SentenceTransformer(EMBEDDING_MODEL_ID)
        embedding.save(str(embedding_path))
        print(f"[models] saved embedding model: {embedding_path}")

    if (reranker_path / "config.json").exists():
        print(f"[models] reranker model already exists: {reranker_path}")
    else:
        print(f"[models] downloading reranker model: {RERANKER_MODEL_ID}")
        reranker = CrossEncoder(RERANKER_MODEL_ID)
        reranker.save(str(reranker_path))
        print(f"[models] saved reranker model: {reranker_path}")

    if (generator_path / "config.json").exists():
        print(f"[models] generator model already exists: {generator_path}")
    else:
        print(f"[models] downloading generator model: {GENERATOR_MODEL_ID}")
        tokenizer = AutoTokenizer.from_pretrained(GENERATOR_MODEL_ID)
        generator = AutoModelForCausalLM.from_pretrained(GENERATOR_MODEL_ID)
        tokenizer.save_pretrained(generator_path)
        generator.save_pretrained(generator_path)
        print(f"[models] saved generator model: {generator_path}")

    print()
    print("Offline model cache is ready.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
