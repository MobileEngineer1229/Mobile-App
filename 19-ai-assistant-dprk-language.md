# AI Assistant with DPRK Language (Generative AI from Scratch)

## Overview
A domain-specific generative AI assistant that produces text in DPRK (Joseon language) Korean about food and workouts. The model is a small GPT-style decoder-only transformer trained from scratch (no pretrained foundation model) on a user-supplied DPRK Korean corpus. The assistant is exposed through a Gradio web UI for chat-style interaction.

This project bundles:
1. **Inference / chat** — the working assistant.
2. **Training** — the from-scratch training pipeline that produces the model.

---

## Features

### Core Features
- **DPRK Korean Generation** — Produces text in Joseon language (vocabulary, orthography, register), not Korean
- **Domain Focus: Food** — Recipe generation, meal planning, ingredient descriptions, cooking instructions
- **Domain Focus: Workouts** — Workout routine generation, exercise descriptions, training plan suggestions
- **Free Chat** — Open-ended conversation in DPRK Korean within the food/workout domain
- **Sampling Controls** — Adjustable temperature, top-k, top-p, max-tokens, repetition penalty
- **Conversation History** — Multi-turn context within the model's context window (1024 tokens default)

### Additional Features
- **Prompt Templates** — One-click templates: "Recommend today's meal", "30Make a minute exercise plan"
- **Streaming Output** — Tokens render progressively, not all-at-once
- **Model Switcher** — Load different checkpoints to compare training stages
- **Training Progress Dashboard** — TensorBoard logs: loss curves, learning-rate schedule, sample generations during training

---

## Application Logic

### Model Architecture
- GPT-style decoder-only transformer
- Default config (~30M params): 8 layers, 512 hidden dim, 8 heads, FFN dim 2048, context 1024
- Pre-norm, GELU, learned positional embeddings, weight tying (input embed ↔ output projection)
- Vocab ~16k (SentencePiece BPE trained on the user's DPRK corpus)
- Configurable in `config/model_config.yaml` to scale up to ~100M params on the same GPU

### Training Pipeline
- User drops `.txt` or `.jsonl` files in `data/raw/`
- `preprocess.py` cleans, dedupes, tokenizes, packs into binary token shards (`.bin`)
- `train.py` runs causal language-model training:
  - AdamW optimizer, cosine LR schedule with linear warmup
  - Mixed precision (bf16 on Blackwell / Ampere; fp16 on older)
  - Gradient accumulation for effective batch size scaling
  - Checkpoint every N steps; resumable
  - Sample generation every N steps so user can monitor quality drift

### Inference Pipeline
- Load checkpoint + tokenizer
- Causal decoding with top-k + top-p (nucleus) sampling, temperature, repetition penalty
- Streaming token-by-token to UI
- Multi-turn chat: prepend conversation history (truncated to context window) to each prompt

### Web UI (Gradio)
- Chat panel + sampling controls in sidebar
- Tabs: "free conversation" / "Ryori Recommendation" / "exercise plan"
- Each tab pre-fills the prompt template

### Project Structure
```
dprk-assistant/
├── README.md
├── requirements.txt
├── config/model_config.yaml
├── data/{raw, processed, sample}/
├── src/
│   ├── tokenizer/       # SentencePiece BPE training + load
│   ├── model/           # GPT transformer (PyTorch)
│   ├── data/            # preprocess.py, dataset.py
│   ├── train/           # train.py, utils
│   ├── inference/       # generate.py, chat.py
│   └── app/             # gradio_app.py
├── scripts/             # 4 numbered shell scripts
└── checkpoints/
```

---

## Challenges

| Challenge | Description |
|-----------|-------------|
| Training Data Scarcity | DPRK Korean food/workout corpora are not public — user must supply data |
| Quality Ceiling | A 30M-param model trained from scratch on hundreds of MB will produce coherent on-domain text but is not ChatGPT-quality |
| Tokenizer for Korean | Korean is agglutinative; SentencePiece BPE handles this without pre-segmentation |
| RTX 5070 (Blackwell) Compatibility | Compute capability 12.0 requires PyTorch 2.7+ with CUDA 12.8+ |
| Repetition / Loops | Small models tend to loop — repetition penalty and nucleus sampling are essential |
| DPRK-vs-ROK Vocabulary Drift | If training data is mixed, model may produce ROK-style words; data curation matters |
| No Foundation Knowledge | From-scratch model has no world knowledge — facts outside the training corpus will be hallucinated |

---

## Recommended Tech Stack
- **Language**: Python 3.10+
- **DL Framework**: PyTorch 2.7+ (required for RTX 5070 / sm_120) with CUDA 12.8+
- **Tokenizer**: SentencePiece (BPE, vocab ~16k)
- **Training Utilities**: `torch.amp`, TensorBoard
- **Inference / Chat UI**: Gradio 4.x
- **Data Loading**: `numpy.memmap` for token shards (nanoGPT-style fast loading)
- **Config**: YAML + `dataclass`-based config object
- **Hardware Target**: NVIDIA RTX 5070 (12GB VRAM, Blackwell sm_120)
