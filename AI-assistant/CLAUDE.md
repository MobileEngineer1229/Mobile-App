# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What This Project Is

A GPT-style language model trained **from scratch** on DPRK Korean (조선말) corpus, specialised for food (료리법) and workout domains. No pretrained weights are used. The full stack: SentencePiece BPE tokenizer → binary preprocessing → PyTorch training loop → Gradio 6 chat UI.

This directory (`AI-assistant/`) is a subdirectory of a larger Mobile-App monorepo; it is self-contained.

## Pipeline Commands

Run these in order from the project root (`AI-assistant/`):

```powershell
# 0. Verify environment (PyTorch, CUDA, packages)
.\scripts\00_setup_check.ps1

# 1. Train SentencePiece tokenizer on raw data
.\scripts\01_train_tokenizer.ps1
# equivalent: python -m src.tokenizer.train_tokenizer --config config/model_config.yaml

# 2. Tokenize raw data → binary memmap files
.\scripts\02_preprocess_data.ps1
# equivalent: python -m src.data.preprocess --config config/model_config.yaml

# 3. Train GPT model
.\scripts\03_train_model.ps1
# equivalent: python -m src.train.train --config config/model_config.yaml
# resume:     python -m src.train.train --config config/model_config.yaml --resume checkpoints/ckpt_step002500.pt

# 4. Run Gradio chat UI at http://127.0.0.1:8000
.\scripts\04_run_app.ps1
# equivalent: python -m src.app.gradio_app --server-port 8000
```

There are no unit tests. Smoke-test the training loop with `--max-steps 5`:
```powershell
python -m src.train.train --config config/model_config.yaml --max-steps 5
```

## Architecture

### Config system (`src/model/config.py`)
All hyperparameters live in `config/model_config.yaml` and are loaded into a `FullConfig` dataclass (`ModelConfig`, `TrainConfig`, `TokenizerConfig`, `DataConfig`, `InferenceConfig`). The training script overwrites `model.vocab_size` at runtime from `data/processed/meta.json`.

### Model (`src/model/transformer.py`)
Decoder-only GPT: learned token + position embeddings → N × `Block(pre-norm LayerNorm → CausalSelfAttention → LayerNorm → GELU FFN)` → LayerNorm → LM head. Uses `F.scaled_dot_product_attention` (auto-dispatches to FlashAttention on Ampere+). Default 30M-param config: 8 layers, 8 heads, 512 embedding dim, 1024 context.

### Tokenizer (`src/tokenizer/tokenizer.py`)
SentencePiece BPE wrapper. Special token IDs are fixed: PAD=0, BOS=1, EOS=2, UNK=3. The `.model` file lives at `checkpoints/tokenizer/dprk_sp.model`. UNK surface strings (`⁇`) are stripped on decode.

### Data pipeline (`src/data/`)
- `readers.py`: Reads `.txt`, `.jsonl`, `.json`, `.pdf`, `.docx`, and images (EasyOCR). Q&A pairs are merged as `질문: ...\n대답: ...`.
- `preprocess.py`: Deduplicates by SHA-1, shuffles, splits 95/5 train/val, wraps each doc with BOS/EOS, saves as `uint16` numpy binary files.
- `dataset.py`: `TokenDataset` — random-offset sliding-window sampling over a `numpy.memmap`.

### Training (`src/train/train.py`)
AdamW with cosine LR + linear warmup. Gradient accumulation (`grad_accum_steps`). Mixed precision: `bfloat16` by default (auto-falls-back to `fp16` if GPU doesn't support bf16). Checkpoints saved every `eval_interval` steps; `_best` and `_final` tags are never rotated.

### Inference (`src/inference/`)
- `generate.py`: Token-by-token generation with temperature, top-k, top-p, repetition penalty; streaming variant via `generate_stream`.
- `chat.py`: `ChatSession` manages multi-turn history in `사용자: ... \n조수: ...` format. Drops oldest turns when prompt would exceed `block_size`. System prefix is **only applied when tokenizer `vocab_size >= 8000`**.

### UI (`src/app/gradio_app.py`)
Gradio 6 `gr.Blocks` with three tabs (자유 대화, 료리 추천, 운동 계획). History format is Gradio 6 message-dict style (`{"role": ..., "content": ...}`).

## Key Constraints

- **PyTorch version**: RTX 5070 (Blackwell sm_120) requires PyTorch ≥ 2.7 with CUDA 12.8: `pip install torch==2.7.0+cu128 --index-url https://download.pytorch.org/whl/cu128`
- **Windows 11 port conflict**: Ports 7787–7886 are reserved by Hyper-V/WSL. Always use port 8000 for Gradio.
- **vocab_size auto-reduction**: If the dataset is too small, the tokenizer trainer silently reduces vocab size. `meta.json` is the source of truth at training time.
- **block_size auto-reduction**: If the smallest dataset shard has fewer tokens than `block_size + 1`, the trainer rebuilds the model with a reduced context window — do not treat the YAML value as final.
- **torch.compile**: Disabled by default (`compile: false`) — Windows support is limited and it complicates checkpoint resumption.
