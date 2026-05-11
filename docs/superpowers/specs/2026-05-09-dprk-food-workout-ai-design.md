# DPRK Korean Food/Workout Generative AI — Design Spec

**Date:** 2026-05-09
**Project location:** `dprk-assistant/`
**Status:** Approved by user during brainstorming session

---

## Goal

Build a domain-specific generative AI that produces text in DPRK Korean (조선말) about food and workouts. The model is trained **from scratch** (no pretrained foundation model) on a user-supplied DPRK corpus and exposed through a Gradio web UI.

## Non-goals

- General-purpose conversational AI (ChatGPT-class quality)
- Multi-language output (target language is DPRK Korean only)
- Mobile/edge deployment (target is desktop with consumer GPU)
- Production-grade serving (single-user dev/research workflow)

---

## Architecture

### Model

GPT-style decoder-only transformer (causal LM, next-token prediction).

| Param | Default | Range |
|-------|---------|-------|
| Layers | 8 | 4–24 |
| Hidden dim | 512 | 256–1024 |
| Attn heads | 8 | 4–16 |
| FFN dim | 2048 | 1024–4096 |
| Context length | 1024 | up to 2048 |
| Vocab size | 16384 | 8192–32768 |
| Total params | ~30M | 10M–150M |

Implementation choices:
- Pre-norm (LayerNorm before attention/FFN)
- GELU activation
- Learned positional embeddings (sinusoidal optional via config)
- Weight tying: input embedding ↔ output projection share weights
- No bias on linear layers (small quality / memory win)
- Dropout 0.1 (configurable)

### Tokenizer

SentencePiece BPE trained on the user's DPRK corpus. Reasons:

- Korean is agglutinative; BPE handles morphological variation without explicit segmentation (KoNLPy unnecessary).
- SentencePiece handles raw text directly (no pretokenization needed).
- Vocab 16k is a sweet spot for Korean: covers most morphemes without bloating embedding tables.

Special tokens: `<bos>` `<eos>` `<pad>` `<unk>`.

### Data Pipeline

```
data/raw/         (user drops .txt or .jsonl files here)
   ↓ preprocess.py
data/processed/   (binary token shards: train.bin, val.bin)
   ↓ dataset.py (numpy.memmap)
training loop
```

- `.txt`: one document per file or one per line.
- `.jsonl`: each line `{"text": "..."}`.
- Cleaning: NFC normalize, collapse whitespace, dedupe by hash.
- Tokenize → flatten → save as `uint16` array (vocab fits in 16 bits).
- 95/5 train/val split.
- `numpy.memmap` for fast random access during training (nanoGPT pattern).

### Training Loop

Pure PyTorch (no `transformers.Trainer` dependency — preserves "from scratch" promise).

- Optimizer: AdamW, β=(0.9, 0.95), weight decay 0.1
- LR schedule: linear warmup (100 steps) → cosine decay to 10% of peak
- Mixed precision: bf16 (RTX 5070 supports it natively); fp16 fallback
- Gradient accumulation: effective batch size = micro_batch × grad_accum_steps
- Gradient clipping: 1.0
- Checkpoint every N steps; keep last 3 + best by val loss
- TensorBoard: train loss, val loss, LR, gradient norm
- Generation samples logged every M steps for qualitative monitoring

### Inference

- Greedy / top-k / top-p (nucleus) / temperature sampling
- Repetition penalty (essential for small models)
- Streaming generation (yield tokens as they're produced)
- Multi-turn chat: keep history in context window, truncate oldest if overflow

### Web UI (Gradio)

- Chat interface (`gr.ChatInterface`)
- Sidebar sliders: temperature, top-p, top-k, max-tokens, repetition penalty
- Tabs: free chat / recipe generator / workout plan generator (each tab seeds a different system-style prefix)
- Streaming output

---

## Project Structure

```
dprk-assistant/
├── README.md
├── requirements.txt
├── .gitignore
├── config/
│   └── model_config.yaml
├── data/
│   ├── raw/             # user-supplied; gitignored
│   ├── processed/       # generated; gitignored
│   └── sample/          # tiny sample so pipeline runs end-to-end
├── src/
│   ├── __init__.py
│   ├── tokenizer/
│   │   ├── __init__.py
│   │   ├── train_tokenizer.py
│   │   └── tokenizer.py
│   ├── model/
│   │   ├── __init__.py
│   │   ├── config.py
│   │   └── transformer.py
│   ├── data/
│   │   ├── __init__.py
│   │   ├── preprocess.py
│   │   └── dataset.py
│   ├── train/
│   │   ├── __init__.py
│   │   ├── train.py
│   │   └── utils.py
│   ├── inference/
│   │   ├── __init__.py
│   │   ├── generate.py
│   │   └── chat.py
│   └── app/
│       ├── __init__.py
│       └── gradio_app.py
├── scripts/
│   ├── 01_train_tokenizer.ps1
│   ├── 02_preprocess_data.ps1
│   ├── 03_train_model.ps1
│   └── 04_run_app.ps1
└── checkpoints/         # gitignored
```

---

## Hardware Target

- **GPU:** NVIDIA RTX 5070, 12GB VRAM, Blackwell (sm_120)
- **CUDA:** 12.8+
- **PyTorch:** 2.7+ (older versions don't support sm_120 — silently no-ops)
- **Estimated training time:** ~1–3 hours per epoch on a few hundred MB of Korean text at default 30M config

---

## Honest Limitations

- 30M-param from-scratch model is a tutorial/research-grade output, not a commercial chatbot. Expect coherent on-domain DPRK Korean with occasional hallucinations and limited reasoning.
- No factual world knowledge beyond the training corpus.
- Quality scales with corpus size and model depth — config makes both scalable, but more data and larger model means longer training.
- DPRK-style fidelity is a function of the user's corpus quality. Mixed ROK/DPRK data will produce mixed output.

---

## Open Decisions for Implementation

1. Whether to use Flash Attention (PyTorch SDPA already calls it on Blackwell; explicit `flash-attn` install not required).
2. Whether to add LR finder utility (defer; standard cosine schedule is sufficient for v1).
3. Mobile app integration (deferred — out of scope for v1, exposed via Gradio API if needed later).
