# DPRK Korean Food & Workout AI Assistant

A generative AI assistant trained **from scratch** on DPRK Korean (조선말) text. No pretrained foundation model is used — the entire neural network is trained on your own corpus using your local GPU.

---

## Table of Contents

1. [What This Project Does](#1-what-this-project-does)
2. [System Requirements](#2-system-requirements)
3. [Installation](#3-installation)
4. [Project Structure](#4-project-structure)
5. [How It Works — Full Logic Explained](#5-how-it-works--full-logic-explained)
   - [5.1 Tokenizer](#51-tokenizer-sentencepiece-bpe)
   - [5.2 Data Preprocessing](#52-data-preprocessing)
   - [5.3 Model Architecture (GPT Transformer)](#53-model-architecture-gpt-transformer)
   - [5.4 Training Pipeline](#54-training-pipeline)
   - [5.5 Inference and Chat](#55-inference-and-chat)
   - [5.6 Web UI (Gradio)](#56-web-ui-gradio)
6. [Step-by-Step Workflow](#6-step-by-step-workflow)
7. [Training Data Format](#7-training-data-format)
8. [Configuration Reference](#8-configuration-reference)
9. [Scaling Up](#9-scaling-up)
10. [GPU Memory: Training vs. Running an LLM](#10-gpu-memory-training-vs-running-an-llm)
11. [Honest Expectations](#11-honest-expectations)
12. [Troubleshooting](#12-troubleshooting)

---

## 1. What This Project Does

This project builds a small language model (~30M parameters) that generates text in DPRK Korean (조선말). Unlike using an API such as ChatGPT, this model:

- Runs entirely on your local machine (offline after training).
- Is trained from scratch on data you provide — no third-party weights are used.
- Specialises in the DPRK Korean dialect: vocabulary, orthography, and register.
- Focuses on two domains: **food (료리)** and **workouts (운동)**.
- Is served through a three-tab chat interface in your browser.

The project also supports general Q&A training data (`{"question": "...", "answer": "..."}` format) so the model learns the ask–answer interaction pattern.

---

## 2. System Requirements

| Requirement | Minimum | Recommended |
|---|---|---|
| Python | 3.10 | 3.12+ |
| GPU | 8 GB VRAM (NVIDIA) | RTX 5070 (12 GB, Blackwell) |
| CUDA | 12.1 | 12.8 |
| PyTorch | 2.1 | 2.7+ (required for RTX 5070 / sm_120) |
| RAM | 16 GB | 32 GB |
| Disk | 5 GB | 20 GB |

> **RTX 5070 (Blackwell sm_120) users:** PyTorch 2.7+ with CUDA 12.8 is required. Older PyTorch versions silently fail on Blackwell.

---

## 3. Installation

```powershell
# 1. Create and activate a virtual environment
python -m venv .venv
.\.venv\Scripts\Activate.ps1

# 2. Install PyTorch with CUDA 12.8 support (required for RTX 5070 / Blackwell)
pip install --pre torch --index-url https://download.pytorch.org/whl/nightly/cu128

# 3. Install all other dependencies
pip install -r requirements.txt

# 4. Verify everything is correct
.\scripts\00_setup_check.ps1
```

The setup check prints your GPU name, VRAM, CUDA compute capability, and confirms all packages are installed.

---

## 4. Project Structure

```
AI-assistant/
├── config/
│   └── model_config.yaml          # All hyperparameters (model, training, inference)
├── data/
│   ├── raw/                        # Drop your .txt / .jsonl training files here
│   ├── processed/                  # Auto-generated: train.bin, val.bin, meta.json
│   └── sample/                     # Sample DPRK Korean data (smoke-test only)
│       ├── sample_food.jsonl
│       ├── sample_workout.jsonl
│       └── sample_qa.jsonl
├── src/
│   ├── tokenizer/
│   │   ├── tokenizer.py            # SentencePiece wrapper (encode / decode)
│   │   └── train_tokenizer.py      # Trains the BPE tokenizer on raw data
│   ├── model/
│   │   ├── config.py               # Dataclass config objects (loaded from YAML)
│   │   └── transformer.py          # GPT decoder-only transformer (pure PyTorch)
│   ├── data/
│   │   ├── preprocess.py           # Tokenizes raw text → binary token shards
│   │   └── dataset.py              # numpy.memmap dataset for fast training I/O
│   ├── train/
│   │   ├── train.py                # Main training loop
│   │   └── utils.py                # LR schedule, checkpoint save/load
│   ├── inference/
│   │   ├── generate.py             # Token sampling (temperature, top-k, top-p)
│   │   └── chat.py                 # Multi-turn chat session wrapper
│   └── app/
│       └── gradio_app.py           # Three-tab Gradio web UI
├── checkpoints/                    # Saved model weights, tokenizer, TensorBoard logs
├── scripts/
│   ├── 00_setup_check.ps1          # Verify environment before starting
│   ├── 01_train_tokenizer.ps1      # Step 1: train the BPE tokenizer
│   ├── 02_preprocess_data.ps1      # Step 2: tokenise data → binary shards
│   ├── 03_train_model.ps1          # Step 3: train the GPT model
│   └── 04_run_app.ps1              # Step 4: launch the chat UI
└── requirements.txt
```

---

## 5. How It Works — Full Logic Explained

### 5.1 Tokenizer (SentencePiece BPE)

**File:** `src/tokenizer/train_tokenizer.py` and `src/tokenizer/tokenizer.py`

**What is a tokenizer?**
A language model cannot work with raw text. Text must be converted to integers (token IDs) first. The tokenizer defines the vocabulary — the set of all recognised "pieces" — and maps between text and IDs.

**Why SentencePiece BPE?**
Korean is an agglutinative language: suffixes attach to roots to express grammar. Simple word-splitting would produce thousands of unique words that each appear rarely. Byte-Pair Encoding (BPE) instead finds common sub-word units — for example `운동` (exercise) might be one token, while `운동하다`, `운동합니다` share the `운동` token plus grammatical suffixes. This means:
- The vocabulary covers more text with fewer tokens.
- Even words the model has seen only in one grammatical form are partly familiar.

SentencePiece trains on raw text with no pre-segmentation — it processes Korean characters directly, which is correct for DPRK text that may use different spacing than South Korean Korean.

**Training process (`train_tokenizer.py`):**
1. Reads all `.txt` and `.jsonl` files from `data/raw/`.
2. Q&A records (`{"question": "...", "answer": "..."}`) are converted to `질문: ...\n대답: ...` format.
3. All text is normalised (NFKC Unicode normalisation, whitespace collapsed).
4. Text is written to a temporary file.
5. SentencePiece BPE trainer is called. It merges the most frequent character pairs iteratively until the target vocabulary size is reached.
6. For small corpora (like the sample data), vocab size is auto-reduced to what the data can support.
7. The tokenizer is saved as `checkpoints/tokenizer/dprk_sp.model`.

**Special tokens** (must match in both the tokenizer and the model):

| ID | Token | Purpose |
|---|---|---|
| 0 | `<pad>` | Padding (unused during training) |
| 1 | `<bos>` | Beginning of sequence |
| 2 | `<eos>` | End of sequence |
| 3 | `<unk>` | Unknown character |

**Using the tokenizer:**
```python
from src.tokenizer.tokenizer import load_tokenizer
tok = load_tokenizer("checkpoints/tokenizer/dprk_sp.model")
ids = tok.encode("된장국을 만드는 방법", add_bos=True)   # → [1, 234, 56, ...]
text = tok.decode(ids)                                    # → "된장국을 만드는 방법"
```

---

### 5.2 Data Preprocessing

**File:** `src/data/preprocess.py` and `src/data/dataset.py`

**Goal:** Convert raw text files into a binary format the training loop can read quickly.

**Why binary (`.bin`) files?**
Training needs to read hundreds of thousands of random sequences per hour. Reading text from disk, parsing UTF-8, and tokenizing on-the-fly would be the bottleneck. Instead, all tokens are pre-computed and stored as a flat array of 16-bit integers in `.bin` files. During training, `numpy.memmap` maps these files directly into virtual memory — random reads cost only a memory lookup.

**Process (`preprocess.py`):**
1. Reads all documents from `data/raw/`.
2. Deduplicates by SHA-1 hash of normalised content so duplicate documents do not dominate training.
3. Shuffles documents randomly.
4. Splits into **train (95%)** and **val (5%)** by document count.
5. Each document is encoded as `[BOS] token1 token2 ... tokenN [EOS]` — the BOS/EOS boundary teaches the model where documents start and end.
6. All tokens are concatenated into a single flat array and written to `data/processed/train.bin` and `val.bin`.
7. A `meta.json` is written with token counts and vocabulary size.

**The dataset class (`dataset.py`):**
```
token array: [BOS t1 t2 t3 ... EOS BOS t1 t2 ...]
              |←── block_size ──→|
              x = tokens[i : i+block_size]
              y = tokens[i+1 : i+block_size+1]   ← shifted by 1
```
Each training step draws a random starting position `i` and creates:
- `x` — the input context window (block_size tokens)
- `y` — the target (x shifted right by one position)

The model learns to predict token `y[t]` given tokens `x[0..t]`.

---

### 5.3 Model Architecture (GPT Transformer)

**File:** `src/model/transformer.py` and `src/model/config.py`

The model is a **decoder-only GPT-style transformer**. This is the same architecture family as GPT-2, GPT-3, and LLaMA — but trained from scratch on your data.

#### Architecture overview

```
Input token IDs
       ↓
Token Embedding  +  Positional Embedding
       ↓
Dropout
       ↓
┌─────────────────────┐
│  Transformer Block  │  × n_layer (8 by default)
│  ┌───────────────┐  │
│  │  LayerNorm    │  │
│  │  Causal Attn  │  │  ← masked self-attention
│  └───────────────┘  │
│       + residual    │
│  ┌───────────────┐  │
│  │  LayerNorm    │  │
│  │  FFN (GELU)   │  │  ← feed-forward network
│  └───────────────┘  │
│       + residual    │
└─────────────────────┘
       ↓
LayerNorm (final)
       ↓
LM Head (Linear → vocab logits)
       ↓
Cross-entropy loss  (training)
  or  softmax + sample  (inference)
```

#### Key design decisions

**Pre-norm:** LayerNorm is applied *before* attention and FFN (not after, as in the original Transformer paper). This makes training more stable at larger scale.

**Causal self-attention:** Each token can only attend to itself and tokens to its left. This is enforced by a causal mask — during generation, the model cannot "cheat" by looking at future tokens. PyTorch's `scaled_dot_product_attention` with `is_causal=True` automatically applies this mask and dispatches to FlashAttention on Ampere/Blackwell GPUs for speed.

**Weight tying:** The input embedding matrix and the output projection (LM head) share the same weights. This reduces parameters by `vocab_size × n_embd` and often improves generalisation because the model learns one representation for each token rather than two separate ones.

**Residual connections:** Each block adds its output to its input (`x = x + attn(norm(x))`). Residuals allow gradients to flow directly back to early layers, enabling training of many layers.

**GELU activation:** The feed-forward network uses GELU (Gaussian Error Linear Unit) — smoother than ReLU, empirically better for language models.

#### Default configuration (~30M parameters)

| Parameter | Value | Meaning |
|---|---|---|
| `n_layer` | 8 | Number of transformer blocks stacked |
| `n_head` | 8 | Attention heads per block |
| `n_embd` | 512 | Hidden dimension (width of the model) |
| `ffn_dim` | 2048 | Inner dimension of the feed-forward layer (4× n_embd) |
| `block_size` | 1024 | Maximum context length in tokens |
| `vocab_size` | 16384 | Set automatically from tokenizer |
| `dropout` | 0.1 | Regularisation: randomly zero 10% of activations |
| `tie_weights` | true | Share input embedding ↔ output projection |

---

### 5.4 Training Pipeline

**File:** `src/train/train.py` and `src/train/utils.py`

Training is **causal language model (CLM)** training: the model learns to predict the next token given all previous tokens. Over many steps on a large corpus, this forces the model to learn grammar, vocabulary, and domain knowledge.

#### Training loop (step by step)

```
for step in range(max_steps):
    1. Compute learning rate (cosine schedule with warmup)
    2. Repeat grad_accum_steps times:
       a. Sample a random batch (x, y) from train dataset
       b. Forward pass → logits, loss
       c. Scale loss by 1/grad_accum_steps (for correct gradient scale)
       d. Backward pass (accumulate gradients)
    3. Clip gradient norm to grad_clip (prevents gradient explosion)
    4. Optimiser step (update weights)
    5. Every log_interval steps: print loss, lr, grad norm, tokens/sec
    6. Every eval_interval steps: compute val loss, save checkpoint if improved
    7. Every sample_interval steps: generate a text sample to monitor quality
```

#### Learning rate schedule

```
LR
 ↑ peak_lr (3e-4)
 │  /‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾\
 │ /                   \______ min_lr (3e-5)
 │/
 └──────────────────────────── step
   warmup       cosine decay
   (100 steps)  (to max_steps)
```

Linear warmup prevents large early gradient steps from destabilising the model. Cosine decay smoothly reduces the learning rate as training converges.

#### Gradient accumulation

The GPU can only hold a limited batch in VRAM. To use an effectively larger batch without more VRAM, gradients are accumulated over `grad_accum_steps` micro-batches before each optimiser step. Effective batch size = `batch_size × grad_accum_steps × block_size` tokens.

#### Mixed precision (bf16)

On Blackwell (RTX 5070) and Ampere GPUs, training uses `bfloat16` — a 16-bit float with the same exponent range as float32 but half the memory and compute cost. This roughly doubles training speed. On older GPUs, `float16` is used with a gradient scaler (to prevent underflow).

#### Checkpointing

- Every `eval_interval` steps, the model is evaluated on the validation set.
- If the validation loss improves, a `_best` checkpoint is saved.
- A regular checkpoint (by step number) is also saved.
- Old step checkpoints are cleaned up, keeping only the last `ckpt_keep_last`.
- A `_final` checkpoint is always saved at the end.
- All checkpoints include the model state, optimiser state, and config — training can be resumed.

---

### 5.5 Inference and Chat

**Files:** `src/inference/generate.py` and `src/inference/chat.py`

#### Token generation (`generate.py`)

After training, the model is a probability distribution over the next token given context. Inference means sampling from this distribution repeatedly to produce text:

```
prompt tokens → model → logits (one per vocab token)
                          ↓
                  Apply temperature (divide logits by T)
                          ↓
                  Filter to top-k candidates
                          ↓
                  Filter to top-p (nucleus) candidates
                          ↓
                  Apply repetition penalty
                          ↓
                  Softmax → probability distribution
                          ↓
                  Sample one token
                          ↓
                  Append to sequence, repeat
```

**Temperature** (`0.1`–`1.5`): Controls randomness.
- Low (0.1–0.5): Model picks the most probable token → coherent but repetitive.
- High (1.0–1.5): More diverse outputs → creative but may become incoherent.

**Top-k** (`1`–`200`): Only consider the k most probable tokens. Prevents the model from picking very unlikely tokens.

**Top-p / nucleus sampling** (`0.1`–`1.0`): Consider only the smallest set of tokens whose cumulative probability exceeds p. More adaptive than top-k — on confident predictions only 2–3 tokens are considered; on uncertain predictions more are included.

**Repetition penalty** (`1.0`–`2.0`): Divides the logit of tokens that already appeared in the output. Prevents the common small-model failure of looping the same phrase. `1.0` = no penalty; `1.15`–`1.3` is typically effective.

**Streaming:** The `generate_stream` function yields one decoded text delta per generated token, so the UI can display text as it arrives rather than waiting for the full response.

#### Multi-turn chat (`chat.py`)

The base model is a text completer, not a chat model. The `ChatSession` class wraps it with a simple turn format:

```
당신은 음식과 운동에 대해 안내하는 조선말 조수입니다.
사용자: 된장국을 어떻게 만듭니까?
조수: 된장국을 만들려면 멸치로 국물을 우려낸 다음...
사용자: 두부는 얼마나 넣습니까?
조수:
```

The model then continues from `조수:` — generating the assistant's reply. This prompt format is also what the model should be trained on for best chat performance: training data in Q&A JSONL format is automatically converted to `질문: ...\n대답: ...` which teaches the model this pattern.

**Context window management:** When the conversation history grows longer than `block_size - max_new_tokens`, the oldest turns are dropped to keep the prompt within the model's context window.

---

### 5.6 Web UI (Gradio)

**File:** `src/app/gradio_app.py`

The app loads the latest (or best) checkpoint and presents three tabs:

| Tab | Korean | Purpose |
|---|---|---|
| 자유 대화 | Free conversation | General DPRK Korean chat |
| 료리 추천 | Recipe recommendation | Ask for recipes and meal plans |
| 운동 계획 | Workout plan | Ask for workout routines |

Each tab has its own conversation history (isolated via `gr.State`) and a shared sidebar for sampling controls (temperature, top-k, top-p, repetition penalty, max tokens). Responses stream token-by-token.

---

## 6. Step-by-Step Workflow

### Step 0 — Check environment

```powershell
.\scripts\00_setup_check.ps1
```

Confirms Python version, GPU, PyTorch CUDA build, and all required packages. Run this first every time you set up on a new machine.

### Step 1 — Add training data

Drop your DPRK Korean `.txt` or `.jsonl` files into `data/raw/`. See [Section 7](#7-training-data-format) for formats.

```powershell
# Use the sample data for a quick smoke test:
Copy-Item .\data\sample\*.jsonl .\data\raw\
```

### Step 2 — Train tokenizer

```powershell
.\scripts\01_train_tokenizer.ps1
```

Reads all files in `data/raw/`, trains a SentencePiece BPE tokenizer, and saves it to `checkpoints/tokenizer/dprk_sp.model`. Re-run this whenever you change or expand your training data.

### Step 3 — Preprocess data

```powershell
.\scripts\02_preprocess_data.ps1
```

Tokenizes all documents and writes binary token shards to `data/processed/`. Also writes `meta.json` with token counts. Re-run after re-training the tokenizer.

### Step 4 — Train the model

```powershell
.\scripts\03_train_model.ps1
```

Runs the full training loop. Progress is printed every 10 steps. To run a quick smoke test with 50 steps:

```powershell
.\scripts\03_train_model.ps1 --max-steps 50
```

To resume a stopped run:

```powershell
.\scripts\03_train_model.ps1 --resume checkpoints\ckpt_step002500.pt
```

Monitor training in TensorBoard:

```powershell
tensorboard --logdir checkpoints\runs
# Open http://localhost:6006 in your browser
```

### Step 5 — Run the chat UI

```powershell
.\scripts\04_run_app.ps1
```

Opens the Gradio chat UI at `http://localhost:7860`. The app automatically loads the best or most recent checkpoint. To load a specific checkpoint:

```powershell
.\scripts\04_run_app.ps1 --checkpoint checkpoints\ckpt_step005000_best.pt
```

To create a public shareable link (via Gradio's tunnel):

```powershell
.\scripts\04_run_app.ps1 --share
```

---

## 7. Training Data Format

Place all training files in `data/raw/`. The preprocessor accepts three formats — mix them freely.

### Plain text (`.txt`)
One document per file, or one document per line. UTF-8 encoding.

```
된장국은 조선식 전통 국물 요리입니다.
멸치와 다시마로 국물을 내고 된장을 풀어...
```

### Free-text JSONL
One JSON object per line. The `"text"` field is used directly.

```jsonl
{"text": "료리법: 된장국\n재료: 된장 두 숟갈, 두부 반모...\n조리방법:\n1. 멸치를 씻어..."}
{"text": "운동의 기본원칙\n첫째, 매일 꾸준히 하는것이 중요합니다..."}
```

### Q&A JSONL (recommended for the ask–answer assistant)
One JSON object per line with `"question"` and `"answer"` fields. These are automatically formatted as `질문: ...\n대답: ...` during tokenizer training and preprocessing.

```jsonl
{"question": "된장국을 어떻게 만듭니까?", "answer": "멸치로 국물을 우려낸 다음 된장을 풀고 두부와 야채를 넣어 끓이면 됩니다."}
{"question": "매일 하기 좋은 운동은 무엇입니까?", "answer": "매일 30분 걷기가 기본입니다. 집에서는 팔굽혀펴기와 앉았다 일어서기를 하십시오."}
```

**Data quality tips:**
- Use pure DPRK Korean vocabulary and orthography. ROK-style spellings will confuse the model.
- More data is always better. The 30M-param model benefits from at least 10 MB of clean text; 100 MB+ is much better.
- Deduplicate your data before adding it — the preprocessor deduplicates by hash, but near-duplicates still waste training budget.
- Include diverse examples: different recipes, different exercise types, different difficulty levels.

---

## 8. Configuration Reference

All hyperparameters are in `config/model_config.yaml`.

### Model

| Key | Default | Meaning |
|---|---|---|
| `n_layer` | 8 | Number of transformer blocks |
| `n_head` | 8 | Attention heads per block (must divide n_embd) |
| `n_embd` | 512 | Hidden dimension |
| `ffn_dim` | 2048 | Feed-forward inner dimension |
| `block_size` | 1024 | Context length (tokens the model sees at once) |
| `dropout` | 0.1 | Dropout rate (set 0 for inference-only) |
| `bias` | false | Whether linear layers have bias terms |
| `tie_weights` | true | Share input embed ↔ output projection weights |

### Training

| Key | Default | Meaning |
|---|---|---|
| `batch_size` | 16 | Micro-batch (sequences per gradient step) |
| `grad_accum_steps` | 4 | Accumulate gradients this many times before update |
| `learning_rate` | 3e-4 | Peak learning rate |
| `min_lr` | 3e-5 | Cosine decay floor (10% of peak) |
| `warmup_steps` | 100 | Steps for linear LR warmup |
| `max_steps` | 5000 | Total training steps |
| `weight_decay` | 0.1 | AdamW weight decay (applied to matrices only) |
| `grad_clip` | 1.0 | Maximum gradient norm |
| `eval_interval` | 250 | Steps between validation runs |
| `eval_iters` | 50 | Number of batches averaged for val loss |
| `log_interval` | 10 | Steps between console log lines |
| `sample_interval` | 500 | Steps between qualitative text samples |
| `ckpt_keep_last` | 3 | Keep this many recent untagged checkpoints |
| `dtype` | bfloat16 | Training dtype: `bfloat16`, `float16`, or `float32` |
| `compile` | false | Enable `torch.compile()` for ~15% speedup on long runs |

### Tokenizer

| Key | Default | Meaning |
|---|---|---|
| `vocab_size` | 16384 | Target vocabulary size (auto-reduced for small corpora) |
| `model_type` | bpe | SentencePiece model type |
| `character_coverage` | 0.9995 | Fraction of characters covered by the vocabulary |

### Inference

| Key | Default | Meaning |
|---|---|---|
| `default_max_new_tokens` | 256 | Default maximum tokens to generate |
| `default_temperature` | 0.9 | Default temperature |
| `default_top_k` | 50 | Default top-k |
| `default_top_p` | 0.95 | Default top-p (nucleus) |
| `default_repetition_penalty` | 1.15 | Default repetition penalty |

---

## 9. Scaling Up

### 9.1 Why training from scratch is so VRAM-expensive

During training the GPU must hold simultaneously, for every parameter:

| What | Bytes | Why |
|---|---|---|
| bf16 model weights | 2 | The actual neural network |
| bf16 gradients | 2 | Computed during back-propagation |
| fp32 Adam momentum | 4 | Optimizer state 1 |
| fp32 Adam variance | 4 | Optimizer state 2 |
| **Base total** | **12** | Before activations |

On top of that, **activations** (intermediate layer outputs stored for backprop) add 2–8 GB depending on batch size and sequence length. This is the dominant cost for large models.

> A "10 GB model" means ~5 billion fp16 parameters → inference file is 10 GB, but **training from scratch requires 5B × 12 bytes + activations ≈ 60–80 GB VRAM**. That is 5–6× a 12 GB consumer card.

**Gradient checkpointing** (`torch.utils.checkpoint`) trades ~30 % training speed to cut activation memory roughly in half, allowing ~1.5–2× larger models on the same card. Enable it in this project by adding a checkpointing wrapper in `src/train/train.py`.

---

### 9.2 GPT-style model configs for this project

To train a larger model, change these values in `config/model_config.yaml`:

| Size label | n_layer | n_embd | n_head | ffn_dim | ~Params | Min VRAM (bf16) | Fits on |
|---|---|---|---|---|---|---|---|
| Nano | 4 | 256 | 4 | 1024 | ~5M | ~2 GB | Any modern GPU |
| Small (default) | 8 | 512 | 8 | 2048 | ~30M | ~4 GB | 6 GB+ |
| Medium | 12 | 768 | 12 | 3072 | ~85M | ~8 GB | 8 GB+ |
| Large | 24 | 1024 | 16 | 4096 | ~350M | ~12 GB | 12 GB+ (batch 8) |
| XL | 28 | 1280 | 16 | 5120 | ~800M | ~24 GB | 24 GB+ |
| XXL | 32 | 1600 | 16 | 6400 | ~1.5B | ~40 GB | 40 GB+ or 2× GPU |

Enable `torch.compile` for ~15 % faster throughput on long runs:

```yaml
train:
  compile: true
```

---

### 9.3 Maximum model trainable from scratch — by PC hardware

All estimates assume: GPT-style transformer, bf16 (fp16 for pre-Ampere cards), Flash Attention, Adam optimiser, batch size tuned to fill ~85 % VRAM, sequence length 1024.  
"With grad-ckpt" column uses gradient checkpointing (30 % slower, ~1.7× larger model).

| # | PC / GPU | VRAM | bf16 | Max params (standard) | Max params (grad-ckpt) | Notes |
|---|---|---|---|---|---|---|
| 1 | **CPU only, no GPU** (any system RAM) | — | — | ~5–10M | ~10M | Extremely slow; hours per training step; only feasible as a proof of concept |
| 2 | **Laptop iGPU** (Intel Iris / AMD Radeon integrated) | 0–2 GB shared | No | ~1–5M | ~8M | Shared system RAM; no CUDA; use PyTorch CPU mode; impractical beyond toy models |
| 3 | **GTX 1060 6 GB** / GTX 1650 / GTX 1660 | 6 GB GDDR5/6 | No (fp16) | ~80–100M | ~150M | No native bf16; use fp16 + gradient scaler; Pascal arch has no Tensor Cores for fp16 speedup |
| 4 | **GTX 1080 Ti 11 GB** | 11 GB GDDR5X | No (fp16) | ~200–250M | ~350M | High VRAM for its era; still useful; no bf16 so slower than equivalent modern cards |
| 5 | **RTX 2060 6 GB** / RTX 2060 Super 8 GB | 6–8 GB GDDR6 | No (fp16) | ~100–150M | ~200M | Turing Tensor Cores accelerate fp16; no native bf16 |
| 6 | **RTX 2080 Ti 11 GB** | 11 GB GDDR6 | No (fp16) | ~250M | ~400M | Was the flagship; still competitive; train in fp16 with gradient scaler |
| 7 | **RTX 3060 12 GB** / RTX 3080 Ti 12 GB | 12 GB GDDR6/6X | Yes | ~350M | ~550M | First affordable Ampere with 12 GB; bf16 support; same ceiling as RTX 5070 |
| 8 | **RTX 3060 Ti 8 GB** / RTX 3070 8 GB / RTX 3070 Ti 8 GB | 8 GB GDDR6 | Yes | ~150–200M | ~300M | Popular mid-range Ampere; good bf16 speed; limited by 8 GB |
| 9 | **RTX 3080 10 GB** | 10 GB GDDR6X | Yes | ~250M | ~400M | Fast bandwidth (760 GB/s); 2 GB less than 3080 Ti hurts model size ceiling |
| 10 | **RTX 3090 24 GB** / RTX 3090 Ti 24 GB | 24 GB GDDR6X | Yes | ~900M–1B | ~1.5B | Consumer record until RTX 5090; NVLink for 2-GPU 48 GB |
| 11 | **RTX 4060 8 GB** | 8 GB GDDR6 | Yes | ~150–200M | ~300M | Efficient Ada arch; fast for its VRAM; bottlenecked at 8 GB |
| 12 | **RTX 4060 Ti 16 GB** | 16 GB GDDR6 | Yes | ~450–500M | ~750M | Unusual 16 GB mid-range card; good value for training |
| 13 | **RTX 4070 12 GB** / RTX 4070 Super 12 GB / RTX 4070 Ti 12 GB | 12 GB GDDR6X | Yes | ~350M | ~550M | Fast Ada Tensor Cores; same VRAM ceiling as RTX 3060 12 GB but much faster training throughput |
| 14 | **RTX 4070 Ti Super 16 GB** / RTX 4080 16 GB / RTX 4080 Super 16 GB | 16 GB GDDR6X | Yes | ~450–500M | ~750M | Excellent training speed; 16 GB allows medium-large models |
| 15 | **RTX 4090 24 GB** | 24 GB GDDR6X | Yes | ~900M–1B | ~1.5B | Fastest single consumer GPU until Blackwell; preferred for serious from-scratch training |
| 16 | **RTX 5070 12 GB ← Your GPU** | 12 GB GDDR7 | Yes | **~350M** | **~550M** | Blackwell sm_120; GDDR7 gives faster bandwidth than RTX 3060; needs PyTorch 2.7+ CUDA 12.8 |
| 17 | **RTX 5070 Ti 16 GB** / RTX 5080 16 GB | 16 GB GDDR7 | Yes | ~500M | ~800M | Faster than Ada 16 GB cards; excellent mid-tier training rigs |
| 18 | **RTX 5090 32 GB** | 32 GB GDDR7 | Yes | ~1.5B | ~2.5B | Current consumer flagship; 1.78 TB/s bandwidth; best single-GPU from-scratch training available in 2025 |
| 19 | **Apple MacBook / Mac Studio — M-series 16 GB unified** | 16 GB shared | Yes (MPS) | ~350–400M | ~600M | Unified memory = GPU + CPU share same RAM; MPS backend ~2–3× slower than CUDA per step |
| 20 | **Apple Mac Pro — M-series 64 GB unified** | 64 GB shared | Yes (MPS) | ~1.5–2B | ~3B | High memory bandwidth (400 GB/s); still slower than CUDA but no VRAM ceiling |
| 21 | **Apple Mac Studio — M4 Max 128 GB unified** | 128 GB shared | Yes (MPS) | ~3–4B | ~6B | Largest consumer unified memory available; genuinely useful for medium LLMs from scratch |
| 22 | **2× RTX 4090 / 2× RTX 3090 (DDP)** | 48 GB total | Yes | ~2–2.5B | ~3.5B | PyTorch DDP splits batch across GPUs; model fits on one card; effective for data parallelism |
| 23 | **4× RTX 4090 (DDP or FSDP)** | 96 GB total | Yes | ~4–5B | ~7B | FSDP (Fully Sharded Data Parallel) shards optimizer states across GPUs; larger model than DDP alone |
| 24 | **NVIDIA A100 40 GB** (cloud/workstation) | 40 GB HBM2e | Yes | ~2B | ~3B | Data-centre card; HBM2e = 1.6 TB/s bandwidth; MFU much higher than consumer cards |
| 25 | **NVIDIA A100 80 GB** (cloud/workstation) | 80 GB HBM2e | Yes | ~4–5B | ~7B | Standard research card; 2 TB/s bandwidth; most open LLMs up to 7B were trained on these |
| 26 | **NVIDIA H100 80 GB** (cloud/workstation) | 80 GB HBM3 | Yes | ~5–6B | ~8B | 3.35 TB/s bandwidth; ~3× faster than A100 per step; used for GPT-4 / Llama training |
| 27 | **8× H100 80 GB** (DGX H100 node) | 640 GB total | Yes | ~30–40B | ~60B | Full DGX node with NVLink; standard for training modern frontier LLMs |

---

### 9.4 Reading the table — key rules of thumb

1. **VRAM is the hard limit.** You cannot train a model whose total memory footprint (weights + gradients + optimizer + activations) exceeds your GPU VRAM. It will OOM-crash, not run slowly.
2. **"10 GB LLM" ≠ trainable on a 12 GB GPU.** A 10 GB download is compressed inference weights. Training requires ~6× that VRAM.
3. **Gradient checkpointing** roughly halves activation memory at ~30 % speed cost. Always worth enabling for the largest model your card can run.
4. **Batch size is a free lever.** Halving `batch_size` roughly halves activation memory, letting you fit a somewhat larger model. Use `grad_accum_steps` to keep effective batch size constant.
5. **Sequence length matters.** Cutting `block_size` from 1024 → 512 roughly halves activation memory; increasing to 2048 roughly doubles it.
6. **Multi-GPU with FSDP** shards optimizer states and gradients across cards, enabling models larger than a single card's VRAM.
7. **Apple MPS** is 2–4× slower per step than equivalent VRAM CUDA, but the large unified memory (up to 128 GB) allows very large models that no consumer NVIDIA card can match.

---

## 10. GPU Memory: Training vs. Running an LLM

Training and inference are completely different operations on the GPU. They use the same hardware but store very different things in VRAM, which is why a model you can *run* on 5 GB often needs 80 GB to *train*.

---

### 10.1 What each mode stores in VRAM

| What is stored in VRAM | Training (from scratch) | Inference (running) |
|---|---|---|
| Model weights | Yes — bf16, 2 bytes/param | Yes — bf16 or quantized |
| Gradients | Yes — 2 bytes/param | No — never computed |
| Optimizer states (Adam) | Yes — 8 bytes/param (fp32) | No |
| **All** layer activations | Yes — must keep every layer's output for backprop | No — one layer at a time, then discarded |
| KV Cache | No | Yes — grows with each generated token |
| **Total per parameter** | **~12–16 bytes** | **~0.5–2 bytes** |

The core difference:

- **Inference** computes one layer, produces the next token, and immediately throws away intermediate values. Only the model weights and the growing KV cache live in VRAM.
- **Training** runs a full forward pass AND a backward pass. The backward pass needs the intermediate outputs of every layer (to compute gradients), so they must all stay in VRAM simultaneously. This is why training needs 6–8× more VRAM than inference for the same model.

---

### 10.2 Visual: what fills VRAM

```
INFERENCE (running a 7B model in fp16, ~15 GB total)
┌──────────────────────────────────────────┐  14.0 GB
│                                          │
│           Model Weights (fp16)           │
│           7B × 2 bytes = 14 GB           │
│                                          │
├──────────────────────────────────────────┤   0.5 GB
│        KV Cache (grows per token)        │
│  K and V tensors for every past token    │
│  stored so attention is not recomputed   │
├──────────────────────────────────────────┤   0.3 GB
│  Activations (current layer only)        │
│  One layer at a time — tiny              │
└──────────────────────────────────────────┘
Total: ~15 GB

TRAINING FROM SCRATCH (same 7B model, bf16 + Adam, ~92 GB total)
┌──────────────────────────────────────────┐  14.0 GB
│         Model Weights (bf16)             │
│         7B × 2 bytes = 14 GB             │
├──────────────────────────────────────────┤  14.0 GB
│         Gradients (bf16)                 │
│         7B × 2 bytes = 14 GB             │
├──────────────────────────────────────────┤  28.0 GB
│         Adam Optimizer States (fp32)     │
│         momentum: 7B × 4 bytes = 28 GB   │
│         variance: 7B × 4 bytes = 28 GB   │
├──────────────────────────────────────────┤  ~8–15 GB
│         ALL Layer Activations            │
│  Every layer's output kept for backprop  │
│  batch × seq_len × hidden × n_layers     │
└──────────────────────────────────────────┘
Total: ~78–92 GB  (needs 2× A100 80 GB)
```

---

### 10.3 The bytes-per-parameter breakdown

#### Training (from scratch, bf16 weights + fp32 Adam)

| Component | dtype | Bytes per param | Why |
|---|---|---|---|
| Model weights | bf16 | 2 | The neural network itself |
| Gradients | bf16 | 2 | dLoss/dWeight for each param, computed in backward pass |
| Adam momentum | fp32 | 4 | Running average of past gradients |
| Adam variance | fp32 | 4 | Running average of squared gradients |
| **Base subtotal** | | **12** | Before activations |
| Activations | bf16 | +2–8 GB fixed | Depends on batch × seq × layers, NOT on param count directly |

> Gradient checkpointing recomputes activations instead of storing them, roughly halving the activation cost at ~30 % speed loss.

#### Inference (running)

| Component | dtype | Bytes per param | Why |
|---|---|---|---|
| Model weights | fp16 | 2 | Full precision inference |
| Model weights | 8-bit | 1 | Integer quantization |
| Model weights | 4-bit | 0.5 | Aggressive quantization (GGUF Q4) |
| KV Cache | fp16 | +varies | 2 × n_layers × kv_heads × head_dim × seq_len × 2 bytes |
| Activations | — | tiny | One layer at a time, immediately discarded |

---

### 10.4 KV Cache — why inference VRAM grows as you chat

Every time the model generates a new token, it runs multi-head attention over **all previous tokens**. Recomputing the Key (K) and Value (V) tensors from scratch for every past token would be O(n²) per step and extremely slow.

Instead, inference caches K and V for every token it has already seen:

```
Step 1: generate token 1
  KV cache = [K₁, V₁]                           ← 1 token cached

Step 2: generate token 2
  KV cache = [K₁, V₁, K₂, V₂]                  ← 2 tokens cached

Step N: generate token N
  KV cache = [K₁, V₁, K₂, V₂, ..., Kₙ, Vₙ]    ← N tokens cached
```

The KV cache size formula:

```
KV Cache = 2 × n_layers × n_kv_heads × head_dim × seq_len × batch_size × bytes_per_element
```

Example — Llama-3.1-8B (fp16, max 4096 context, 1 user):

```
2 × 32 layers × 8 GQA-heads × 128 head_dim × 4096 seq × 1 batch × 2 bytes
= 536,870,912 bytes ≈ 0.5 GB
```

For a 70B model at the same settings:

```
2 × 80 layers × 8 GQA-heads × 128 head_dim × 4096 seq × 1 batch × 2 bytes
= 13.4 GB  ← just the cache, on top of ~35 GB of weights
```

This is why very long conversations or large batch sizes can OOM even when the model itself fits in VRAM.

---

### 10.5 Concrete VRAM numbers for common model sizes

#### Inference VRAM (single user, 4096 token context)

| Model size | fp16 weights | 4-bit weights | KV cache (fp16) | **Total fp16** | **Total 4-bit** |
|---|---|---|---|---|---|
| 350M (this project) | 0.7 GB | 0.2 GB | ~0.05 GB | **~1 GB** | **~0.3 GB** |
| 1B | 2 GB | 0.5 GB | ~0.1 GB | **~2.5 GB** | **~0.7 GB** |
| 3B | 6 GB | 1.5 GB | ~0.2 GB | **~7 GB** | **~2 GB** |
| 7B | 14 GB | 3.5 GB | ~0.5 GB | **~15 GB** | **~4.5 GB** |
| 13B | 26 GB | 6.5 GB | ~0.8 GB | **~27 GB** | **~8 GB** |
| 30B | 60 GB | 15 GB | ~1.5 GB | **~62 GB** | **~17 GB** |
| 70B | 140 GB | 35 GB | ~3.5 GB | **~144 GB** | **~39 GB** |

#### Training from scratch VRAM (bf16 + Adam, standard batch)

| Model size | Weights (bf16) | Gradients (bf16) | Adam states (fp32) | Activations | **Total** | Min GPU |
|---|---|---|---|---|---|---|
| 30M | 0.06 GB | 0.06 GB | 0.24 GB | ~3.6 GB | **~4 GB** | 6 GB |
| 85M | 0.17 GB | 0.17 GB | 0.68 GB | ~7 GB | **~8 GB** | 8 GB |
| 350M | 0.7 GB | 0.7 GB | 2.8 GB | ~8 GB | **~12 GB** | 12 GB |
| 1B | 2 GB | 2 GB | 8 GB | ~10 GB | **~22 GB** | 24 GB |
| 3B | 6 GB | 6 GB | 24 GB | ~15 GB | **~51 GB** | 2× A100 40 GB |
| 7B | 14 GB | 14 GB | 56 GB | ~15 GB | **~99 GB** | 2× A100 80 GB |
| 13B | 26 GB | 26 GB | 104 GB | ~20 GB | **~176 GB** | 4× A100 80 GB |
| 30B | 60 GB | 60 GB | 240 GB | ~30 GB | **~390 GB** | 8× A100 80 GB |
| 70B | 140 GB | 140 GB | 560 GB | ~50 GB | **~890 GB** | 16× A100 80 GB |

> Activations in training grow with `batch_size × seq_len`, not just with model size. The numbers above assume batch_size=16, seq_len=1024. Halving batch_size nearly halves activation VRAM.

---

### 10.6 Why a 12 GB GPU can run a 7B model but not train one

```
RTX 5070 — 12 GB VRAM

Running 7B (4-bit GGUF):          Training 7B from scratch:
┌─────────────┐ 3.5 GB  weights   ┌─────────────┐ 14 GB   weights
│  4-bit      │                   │  bf16       │
│  weights    │                   ├─────────────┤ 14 GB   gradients
├─────────────┤ 0.5 GB  KV cache  ├─────────────┤ 56 GB   Adam states
│  KV cache   │                   ├─────────────┤ ~15 GB  activations
├─────────────┤ 0.1 GB  misc      └─────────────┘
└─────────────┘                   TOTAL: ~99 GB
TOTAL: ~4 GB ✓                    NEED: 8–9× more VRAM ✗
```

**The rule:**
- To **run** a model: VRAM ≈ model_size_in_GB (at the quantization level you choose)
- To **train** a model from scratch: VRAM ≈ model_size_in_GB × **6–8×** (at fp16) plus activation overhead

---

### 10.7 Step-by-step calculation logic — 계산 론리 상세설명
### (How every VRAM number is derived — 모든 기억용량 수치의 도출 방법)

> 이 절은 우의 모든 표에 나온 수치들이 어떻게 계산되는가를 단계별로 설명한다.  
> 자기 모형의 크기에 맞게 직접 수치를 대입하여 정확한 추정치를 얻을 수 있다.  
> *(This section derives every number in the tables above, step by step.  
> Substitute your own model dimensions to get exact estimates.)*

---

#### Calculation A — How many parameters does a GPT model have?
#### 계산 A — GPT 모형의 매개변수 수 (전체 가중치 개수 계산)
> **매개변수(parameter)** 란 모형이 학습을 통해 조정하는 모든 수치이다.  
> 층마다 행렬곱(matrix multiplication)이 있고, 각 행렬의 원소 하나하나가 매개변수이다.  
> *(A parameter is any number the model adjusts during training — every element in every weight matrix.)*

**A-1. Master formula — 전체 매개변수 수 공식**

```
P = token_embedding                          ← 어휘 삽입표현 행렬 (각 단어에 벡터 하나)
  + positional_embedding                     ← 위치 삽입표현 행렬 (각 위치에 벡터 하나)
  + per_layer × n_layers                     ← 변환기 층마다 반복되는 가중치
  + output_head                              ← 출력 사영 행렬 (가중치 공유시 0)

────────────────────────────────────────────────────────
token_embedding      = vocab_size × n_embd
  ← 어휘목록 크기 × 은닉차원 = 단어마다 n_embd개 수치
  ← (vocabulary size × hidden dimension — one vector per word)

positional_embedding = block_size × n_embd
  ← 최대 문맥길이 × 은닉차원 = 위치마다 n_embd개 수치
  ← (max context length × hidden dimension — one vector per position)

────────────────────────────────────────────────────────
per_layer = attention_projections + ffn_projections

  attention_projections = 4 × n_embd × n_embd
    ← 주의기제에서 Q(질문), K(열쇠), V(값), O(출력) 사영 각각 n_embd×n_embd 행렬
    ← (Query, Key, Value, Output projection — each is an n_embd×n_embd matrix)
    ← 4 projections × n_embd rows × n_embd columns

  ffn_projections = 2 × n_embd × ffn_dim
    ← 전방향신경망: 확대 사영(n_embd→ffn_dim) + 축소 사영(ffn_dim→n_embd)
    ← (Feed-forward network: up-projection expands, down-projection contracts)
    ← ffn_dim is typically 4 × n_embd

────────────────────────────────────────────────────────
output_head:
  = 0                    when tie_weights = true
    ← 출력 행렬을 token_embedding 행렬과 공유 → 추가 매개변수 없음
    ← (output matrix reuses token_embedding — saves vocab_size×n_embd params)
  = vocab_size × n_embd  when tie_weights = false
    ← 별도의 출력 행렬 사용 → 추가 매개변수 발생
```

---

**A-2. Worked example — Small config (기본 설정, ~30M 매개변수)**

```
────────── 설정값 (Config values) ──────────
vocab_size  = 16,384   ← 어휘목록 크기 (SentencePiece BPE 16K)
n_embd      =    512   ← 은닉차원 (hidden dimension)
block_size  =  1,024   ← 최대 문맥길이 (max context length in tokens)
n_layers    =      8   ← 변환기 층 수 (number of transformer blocks)
ffn_dim     =  2,048   ← 전방향망 내부 차원 (= 4 × n_embd)
tie_weights =   true   ← 출력 행렬을 입력 삽입표현과 공유

────────── 단계별 계산 (Step-by-step) ──────────

STEP 1: 어휘 삽입표현 (Token embedding matrix)
  token_embedding = vocab_size × n_embd
                  = 16,384 × 512
                  = 8,388,608 params
  ← 16,384개 단어 × 각 단어를 512차원 벡터로 표현

STEP 2: 위치 삽입표현 (Positional embedding matrix)
  positional_embedding = block_size × n_embd
                       = 1,024 × 512
                       = 524,288 params
  ← 최대 1,024개 위치 × 각 위치를 512차원 벡터로 표현

STEP 3: 층 하나의 매개변수 (Parameters per one transformer layer)
  주의 사영 (Attention projections):
    = 4 × n_embd × n_embd
    = 4 × 512 × 512
    = 1,048,576 params
    ← Q 행렬: 512×512 = 262,144
    ← K 행렬: 512×512 = 262,144
    ← V 행렬: 512×512 = 262,144
    ← O 행렬: 512×512 = 262,144 (합계 1,048,576)

  전방향망 (FFN projections):
    = 2 × n_embd × ffn_dim
    = 2 × 512 × 2,048
    = 2,097,152 params
    ← 확대 행렬(up): 512×2,048 = 1,048,576
    ← 축소 행렬(down): 2,048×512 = 1,048,576

  층 하나 합계 (per_layer total):
    = 1,048,576 + 2,097,152
    = 3,145,728 params

STEP 4: 전체 층 (All transformer layers)
  all_layers = n_layers × per_layer
             = 8 × 3,145,728
             = 25,165,824 params

STEP 5: 출력 사영 (Output head)
  output_head = 0   ← tie_weights = true 이므로 추가 없음

STEP 6: 합산 (Grand total)
  P = 8,388,608 + 524,288 + 25,165,824 + 0
    = 34,078,720
    ≈ 34M params

  ┌──────────────────────────────────────────────────┐
  │ (참고: README의 "~30M"은 LayerNorm·편향 제외값)  │
  │ Note: "~30M" in README excludes LayerNorm/bias   │
  └──────────────────────────────────────────────────┘
```

---

**A-3. Worked example — Large config (~350M 매개변수)**

```
────────── 설정값 ──────────
vocab_size  = 16,384
n_embd      =  1,024   ← 은닉차원이 2배로 늘어남 (512 → 1,024)
block_size  =  1,024
n_layers    =     24   ← 층 수가 3배로 늘어남 (8 → 24)
ffn_dim     =  4,096   ← 전방향망도 2배 (2,048 → 4,096)
tie_weights =   true

────────── 계산 ──────────

STEP 1: token_embedding
  = 16,384 × 1,024 = 16,777,216 params

STEP 2: positional_embedding
  = 1,024 × 1,024 = 1,048,576 params

STEP 3: per_layer
  attention = 4 × 1,024 × 1,024 = 4,194,304
    ← n_embd이 2배이므로 행렬 면적은 4배 (512²→1024²)
    ← (doubling n_embd quadruples each attention matrix)
  FFN       = 2 × 1,024 × 4,096 = 8,388,608
  per_layer = 4,194,304 + 8,388,608 = 12,582,912

STEP 4: all_layers
  = 24 × 12,582,912 = 301,989,888 params

STEP 5: output_head = 0

STEP 6: Grand total
  P = 16,777,216 + 1,048,576 + 301,989,888
    = 319,815,680
    ≈ 320M params
  (README says ~350M — the 30M gap is LayerNorm weights, bias terms, rounding)

  ┌─────────────────────────────────────────────────────────────────────────┐
  │ Small → Large 비교 (Comparison):                                        │
  │   n_embd 2×, n_layers 3×  →  매개변수는 약 10× 증가 (34M → 320M)      │
  │   n_embd doubled, n_layers tripled → params ~10× larger (34M → 320M)  │
  │   이유: 주의 행렬이 n_embd²에 비례하므로 차원 2배 = 행렬 4배            │
  │   Why: attention matrix scales as n_embd², so 2× dim = 4× matrix size  │
  └─────────────────────────────────────────────────────────────────────────┘
```

---

#### Calculation B — Training VRAM (훈련 도형처리장치 기억 계산)
> **훈련** 은 모형 가중치뿐 아니라 경사도, 최적화기 상태, 그리고 역전파를 위한  
> 모든 층의 활성화값을 동시에 기억장치에 보관해야 하므로 추론보다 훨씬 많은 기억이 필요하다.  
> *(Training keeps weights, gradients, optimizer states, AND all layer activations simultaneously —  
> far more than inference which only needs weights.)*

**B-1. Formula — 훈련 기억 공식**

```
훈련 기억 (Training VRAM) = 기반기억 + 활성화기억 + 부가비용
                           = Base Memory + Activation Memory + Overhead

══════════════════════════════════════════════════════════════
[1] 기반기억 (Base Memory) = P × 12 bytes
    ← 매개변수 하나당 12바이트 필요 이유:

  ┌─────────────────────────────┬───────┬──────────┬────────────────────────────────────────────┐
  │ 구성 요소 (Component)        │ Bytes │ 자료형   │ 이유 (Why)                                 │
  ├─────────────────────────────┼───────┼──────────┼────────────────────────────────────────────┤
  │ 모형 가중치 (Model weights)  │   2   │ bf16     │ 신경망 자체 — 추론에도 필요                │
  │                              │       │          │ The network itself — also needed at infer. │
  ├─────────────────────────────┼───────┼──────────┼────────────────────────────────────────────┤
  │ 경사도 (Gradients)           │   2   │ bf16     │ 손실함수의 각 가중치에 대한 편미분          │
  │                              │       │          │ ∂Loss/∂w for every parameter               │
  ├─────────────────────────────┼───────┼──────────┼────────────────────────────────────────────┤
  │ Adam 동량 (Momentum)         │   4   │ fp32     │ 과거 경사도의 지수이동평균 — 방향 기억      │
  │                              │       │          │ Exponential moving average of gradients    │
  ├─────────────────────────────┼───────┼──────────┼────────────────────────────────────────────┤
  │ Adam 분산 (Variance)         │   4   │ fp32     │ 과거 경사도 제곱의 이동평균 — 크기 조절    │
  │                              │       │          │ Moving average of squared gradients        │
  ├─────────────────────────────┼───────┼──────────┼────────────────────────────────────────────┤
  │ 합계 (Total per param)       │  12   │          │                                            │
  └─────────────────────────────┴───────┴──────────┴────────────────────────────────────────────┘

  ← Adam이 fp32인 이유: 경사도 갱신에서 수치 정밀도가 필요하기 때문 (bf16으로는 소실됨)
  ← (Adam uses fp32 because weight updates need precision — bf16 underflows on small changes)

══════════════════════════════════════════════════════════════
[2] 활성화기억 (Activation Memory)
    ← 역전파(backward pass)는 각 층의 출력값을 다시 필요로 한다.
    ← 따라서 전방향계산(forward pass) 중 모든 층의 중간값을 보관해야 한다.
    ← (Backprop needs each layer's output to compute gradients; store all during forward pass)

  층 하나당 보관해야 하는 텐서 (Tensors kept per layer for backprop):
  ┌────────────────────────────────────────────┬─────────────────────────────────┬──────────────────────────────┐
  │ 텐서 이름 (Tensor)                          │ 원소 수 (Elements)              │ 이유 (Why stored)            │
  ├────────────────────────────────────────────┼─────────────────────────────────┼──────────────────────────────┤
  │ 층 입력 (Layer input — residual stream)     │ B × S × n_embd                 │ 잔류 연결 경사도 계산용       │
  │                                            │                                 │ residual gradient            │
  ├────────────────────────────────────────────┼─────────────────────────────────┼──────────────────────────────┤
  │ 주의 Q 행렬 (Attention Q)                   │ B × S × n_embd                 │ Q 사영 가중치 갱신용          │
  ├────────────────────────────────────────────┼─────────────────────────────────┼──────────────────────────────┤
  │ 주의 K 행렬 (Attention K)                   │ B × S × n_embd                 │ K 사영 가중치 갱신용          │
  ├────────────────────────────────────────────┼─────────────────────────────────┼──────────────────────────────┤
  │ 주의 V 행렬 (Attention V)                   │ B × S × n_embd                 │ V 사영 가중치 갱신용          │
  ├────────────────────────────────────────────┼─────────────────────────────────┼──────────────────────────────┤
  │ 주의 출력 (Attention output)                │ B × S × n_embd                 │ O 사영 가중치 갱신용          │
  ├────────────────────────────────────────────┼─────────────────────────────────┼──────────────────────────────┤
  │ 전방향망 입력 (FFN input, after LayerNorm)  │ B × S × n_embd                 │ 층정규화 가중치 갱신용         │
  ├────────────────────────────────────────────┼─────────────────────────────────┼──────────────────────────────┤
  │ 전방향망 은닉층 (FFN hidden, after GELU)    │ B × S × ffn_dim                │ 확대 가중치 갱신용             │
  │                                            │ ← ffn_dim = 4 × n_embd 보통    │                              │
  └────────────────────────────────────────────┴─────────────────────────────────┴──────────────────────────────┘

  공식 (Formula):
  Activation Memory = n_layers × B × S × (6 × n_embd + ffn_dim) × 2 bytes
    ← 6 × n_embd: 6개 텐서(입력·Q·K·V·주의출력·FFN입력) 각각 n_embd 원소
    ← ffn_dim:    FFN 은닉 텐서 하나 (보통 4 × n_embd)
    ← × 2 bytes:  bf16 자료형 (원소당 2바이트)
    ← × B × S:   일괄처리 크기(batch) × 련속렬 길이(sequence length)

══════════════════════════════════════════════════════════════
[3] 부가비용 (Overhead) ≈ 0.5 GB
    ← CUDA 문맥, cuBLAS 완충기억기, 임시 작업공간 등
    ← (CUDA context, cuBLAS workspace, temporary buffers, fragmentation)
```

---

**B-2. Worked example — Large config, batch_size = 8**
**실례 — 대형 설정, 일괄처리 크기 8 (RTX 5070에서 훈련 가능한 최대 설정)**

```
────────── 설정값 ──────────
P        = 320,000,000  ← 대략 3.2억 매개변수
n_layers = 24           ← 변환기 층 24개
n_embd   = 1,024        ← 은닉차원
ffn_dim  = 4,096        ← 전방향망 내부 차원 (= 4 × 1,024)
B        = 8            ← 일괄처리 크기 (sequences per GPU step)
S        = 1,024        ← 문맥길이 (tokens per sequence)

────────── STEP 1: 기반기억 계산 ──────────
Base Memory = P × 12 bytes
            = 320,000,000 × 12
            = 3,840,000,000 bytes
            ÷ 1,073,741,824 (bytes per GB)
            = 3.58 GB

  세부 내역 (Breakdown):
    가중치  (weights):   320M × 2 bytes = 640 MB  = 0.60 GB
    경사도  (grads):     320M × 2 bytes = 640 MB  = 0.60 GB
    Adam동량(momentum):  320M × 4 bytes = 1,280 MB = 1.19 GB
    Adam분산(variance):  320M × 4 bytes = 1,280 MB = 1.19 GB
    ─────────────────────────────────────────────────────
    합계    (total):                               3.58 GB ✓

────────── STEP 2: 활성화기억 계산 ──────────
Activation Memory = n_layers × B × S × (6 × n_embd + ffn_dim) × 2 bytes

  내부 계산 (Inner calculation):
    6 × n_embd + ffn_dim
    = 6 × 1,024 + 4,096
    = 6,144 + 4,096
    = 10,240   ← 층 하나의 bf16 원소 수 (per-layer elements per token per sequence item)

    B × S = 8 × 1,024 = 8,192  ← 일괄처리 내 전체 토큰 수 (total tokens in one batch)

    한 층의 활성화 바이트 수 (bytes per layer):
    = 8,192 × 10,240 × 2
    = 167,772,160 bytes
    = 160 MB

    전체 층 (all 24 layers):
    = 24 × 160 MB = 3,840 MB = 3.75 GB
    ← 24개 층 모두의 중간값을 동시에 보관

────────── STEP 3: 합산 ──────────
Total VRAM = Base + Activation + Overhead
           = 3.58 + 3.75 + 0.50
           = 7.83 GB

  ┌─────────────────────────────────────────────────┐
  │ RTX 5070 (12 GB) 에서 훈련 가능 ✓               │
  │ 여유 기억: 12 - 7.83 = 4.17 GB                  │
  │ Can train on RTX 5070 (12 GB) with 4.17 GB free │
  └─────────────────────────────────────────────────┘
```

---

**B-3. Same model, batch_size = 16 — 일괄처리 크기 16일 때**

```
STEP 2 (changed):
  B × S = 16 × 1,024 = 16,384 tokens  ← 일괄처리 2배 = 활성화도 2배
  Activation Memory = 24 × 16,384 × 10,240 × 2
                    = 24 × 335,544,320
                    = 8,053,063,680 bytes = 7.50 GB

Total = 3.58 + 7.50 + 0.50 = 11.58 GB

  ┌───────────────────────────────────────────────────────────────────┐
  │ 아슬아슬하게 12 GB 초과 가능 — 기억 부족(OOM) 오류 가능성 있음  │
  │ Tight fit — may OOM on 12 GB; reduce batch_size to 8 instead      │
  │ 일괄처리 크기를 8로 줄이면 batch_size 8 예제처럼 안전하게 훈련   │
  └───────────────────────────────────────────────────────────────────┘
```

---

**B-4. With gradient checkpointing — 경사도 검사점 기법 사용 시**

```
경사도 검사점이란? (What is gradient checkpointing?)
  ← 역전파 중 필요한 중간값을 저장하지 않고 필요할 때 재계산하는 기법
  ← 활성화 기억을 약 87% 절감하는 대신 훈련 속도가 약 30% 느려짐
  ← (Do not store internal activations; recompute them during backprop)
  ← (Saves ~87% of activation memory at ~30% speed cost)

검사점 적용시 보관하는 것 (What IS stored with checkpointing):
  → 층 입력(layer boundary inputs)만 저장 — 층 내부 텐서는 재계산
  → Only layer inputs stored — internal tensors (Q,K,V,FFN hidden) recomputed

층 하나당 저장 바이트 (bytes stored per layer):
  = B × S × n_embd × 2 bytes   ← 층 경계 입력 하나만
  = 16 × 1,024 × 1,024 × 2
  = 33,554,432 bytes = 32 MB per layer

전체 24층 (all 24 layers):
  = 24 × 33,554,432
  = 805,306,368 bytes = 0.75 GB

Total VRAM (검사점 적용, batch_size=16):
  = 3.58 + 0.75 + 0.50 = 4.83 GB

  ┌────────────────────────────────────────────────────────────────────────┐
  │ 활성화 기억 절감: 7.50 GB → 0.75 GB  (약 10분의 1로 줄어듦)          │
  │ 전체 기억 절감: 11.58 GB → 4.83 GB  (약 58% 절감)                    │
  │ Activation: 7.50 GB → 0.75 GB (10× reduction)                        │
  │ Total: 11.58 GB → 4.83 GB (58% reduction, 30% slower training speed) │
  └────────────────────────────────────────────────────────────────────────┘
```

---

**B-5. How batch_size and sequence_length affect activation memory**
**일괄처리·문맥길이에 따른 활성화기억 변화 (Large config 기준)**

```
Activation Memory = n_layers × B × S × (6×n_embd + ffn_dim) × 2
                  = 24 × B × S × 10,240 × 2
                  = B × S × 491,520 bytes per (B×S) token

  ┌──────────────┬────────────┬───────────────────┬───────────────────┐
  │ batch_size B │ seq_len  S │ B × S (총 토큰 수) │ Activation Memory │
  ├──────────────┼────────────┼───────────────────┼───────────────────┤
  │      4       │    512     │      2,048         │      1.00 GB      │
  │      4       │  1,024     │      4,096         │      1.88 GB      │
  │      8       │  1,024     │      8,192   ★     │      3.75 GB      │  ← B-2 예제
  │     16       │  1,024     │     16,384         │      7.50 GB      │  ← B-3 예제
  │     16       │  2,048     │     32,768         │     15.00 GB      │  ← OOM on 12 GB
  │      1       │  2,048     │      2,048         │      1.00 GB      │  ← 단일 긴 문맥
  └──────────────┴────────────┴───────────────────┴───────────────────┘

  ← 일괄처리와 문맥길이를 줄이면 같은 GPU에서 더 큰 모형을 훈련할 수 있다
  ← (Reducing batch_size or seq_len lets you train a larger model on the same GPU)
  ← 단, 일괄처리 크기 축소는 grad_accum_steps로 보완해야 학습 효율 유지됨
  ← (Compensate with larger grad_accum_steps to keep effective batch size constant)
```

---

#### Calculation C — Inference VRAM (추론 도형처리장치 기억 계산)
> **추론(inference)** 은 훈련보다 훨씬 단순하다.  
> 경사도도, 최적화기 상태도, 활성화 보관도 필요 없다.  
> 필요한 것은 가중치와, 대화가 길어질수록 커지는 키값 완충기억기(KV cache)뿐이다.  
> *(Inference is much simpler than training — no gradients, no optimizer, minimal activations.  
> Only weights and the growing KV cache matter.)*

**C-1. Formula — 추론 기억 공식**

```
추론 기억 (Inference VRAM) = 가중치 기억 + KV 완충기억기 + 활성화 부가비용
                           = Weight Memory + KV Cache + Activation Overhead

══════════════════════════════════════════════════════════════
[1] 가중치 기억 (Weight Memory) = P × bytes_per_param

  량자화 방법별 매개변수당 바이트 수 (bytes per param by quantization method):
  ┌─────────────────┬───────────────┬───────────────────────────────────────────────────┐
  │ 정밀도           │ 매개변수당    │ 설명 (Description)                                │
  │ (Precision)      │ 바이트 수     │                                                   │
  ├─────────────────┼───────────────┼───────────────────────────────────────────────────┤
  │ fp32            │ 4.0 bytes     │ 완전 32비트 부동소수점 — 가장 정확, 가장 큰 기억  │
  │                 │               │ Full 32-bit float — most precise, largest memory  │
  ├─────────────────┼───────────────┼───────────────────────────────────────────────────┤
  │ fp16 / bf16     │ 2.0 bytes     │ 16비트 부동소수점 — 훈련과 추론 모두에 사용       │
  │                 │               │ 16-bit float — used in both training and inference │
  ├─────────────────┼───────────────┼───────────────────────────────────────────────────┤
  │ 8비트 (Q8_0)    │ 1.0 byte      │ 정수 량자화 — 품질 손실 미미, 기억 절반           │
  │                 │               │ Integer quant — minimal quality loss, half memory  │
  ├─────────────────┼───────────────┼───────────────────────────────────────────────────┤
  │ 4비트 (Q4_K_M)  │ 0.5 bytes     │ 4비트 량자화 — 소비자 GPU에 가장 많이 씀         │
  │                 │               │ Best quality-per-byte for consumer GPUs            │
  ├─────────────────┼───────────────┼───────────────────────────────────────────────────┤
  │ 2비트 (Q2_K)    │ 0.25 bytes    │ 극한 압축 — 품질 저하 심함, 거의 안 씀           │
  │                 │               │ Extreme compression — significant quality loss     │
  └─────────────────┴───────────────┴───────────────────────────────────────────────────┘

  ← 4비트 량자화 = 가중치를 16수준(4bit=2⁴)으로 반올림
  ← 원소 두 개를 1바이트에 묶어서 저장 → 원래의 4분의 1 기억
  ← (4-bit quant packs 2 values per byte → ¼ of fp16 storage)

══════════════════════════════════════════════════════════════
[2] KV 완충기억기 (KV Cache)

  KV Cache = 2 × n_layers × n_kv_heads × head_dim × seq_len × batch × bytes

  변수 설명 (Variable definitions):
    2          ← K(열쇠) 텐서 1개 + V(값) 텐서 1개
                 one Key tensor + one Value tensor per layer

    n_kv_heads ← KV 주의 머리 수
                 = n_heads  (다중머리주의, MHA — 모든 머리가 각자 K·V 보유)
                 < n_heads  (군화주의, GQA — 여러 Q 머리가 하나의 K·V 공유)
                 ← 현대 대형 모형(Llama-3, Qwen2.5)은 GQA로 KV 기억 절감

    head_dim   ← 머리 하나의 차원 = n_embd ÷ n_heads

    seq_len    ← 현재 문맥 길이 (토큰 수)
                 ← 대화할수록 증가 → KV 기억도 계속 증가
                 current context length — grows with every generated token

    batch      ← 병렬 처리 중인 대화 수 (concurrent conversations)

    bytes      ← KV를 보관하는 자료형 바이트
                 = 2  (fp16)   ← 대부분의 추론 서버
                 = 1  (int8)   ← 량자화 KV 기억으로 절감 가능

══════════════════════════════════════════════════════════════
[3] 활성화 부가비용 (Activation Overhead) ≈ 0.1–0.3 GB
    ← 추론 중에는 한 번에 층 하나만 계산하고 즉시 해제
    ← (inference computes one layer at a time, frees it immediately)
    ← 훈련의 활성화기억(수 GB)과 달리 매우 작다
    ← (unlike training's activation memory of several GB — this is tiny)
```

---

**C-2. Worked example — This project's Small model (34M), fp16**
**실례 A — 본 프로젝트 소형 모형 (34M 매개변수), 반정밀도 추론**

```
────────── 설정값 ──────────
P         = 34,000,000    ← 소형 모형 매개변수 수 (계산 A에서 산출)
n_layers  =  8            ← 변환기 층 수
n_heads   =  8            ← 주의 머리 수
n_embd    = 512           ← 은닉차원
head_dim  = 512 ÷ 8 = 64  ← 머리 하나의 차원
seq_len   = 512           ← 대화 중간 시점 (512 토큰 대화)
batch     =  1            ← 이용자 1명

────────── STEP 1: 가중치 기억 ──────────
Weight Memory = 34,000,000 × 2 bytes   ← fp16
              = 68,000,000 bytes
              = 0.063 GB

────────── STEP 2: KV 완충기억기 계산 ──────────
KV Cache = 2 × n_layers × n_heads × head_dim × seq_len × batch × bytes

단계별 곱셈 (step-by-step multiplication):
  2           = 2            ← K와 V 각 1개
  × n_layers  = 2 × 8  = 16  ← 8개 층
  × n_heads   = 16 × 8 = 128  ← 8개 주의 머리
  × head_dim  = 128 × 64 = 8,192  ← 머리당 64차원
  × seq_len   = 8,192 × 512 = 4,194,304  ← 512 토큰
  × batch     = 4,194,304 × 1 = 4,194,304  ← 이용자 1명
  × bytes     = 4,194,304 × 2 = 8,388,608 bytes  ← fp16

  KV Cache = 8,388,608 bytes = 0.008 GB = 8 MB

  ← 소형 모형이라 KV 완충기억기가 매우 작다 (거의 무시 가능)
  ← (Tiny model → KV cache is negligible — only 8 MB)

────────── STEP 3: 합산 ──────────
Total = 0.063 + 0.008 + 0.1 (overhead) = 0.171 GB

  ┌───────────────────────────────────────────────────────────┐
  │ 사실상 어떤 GPU에서도 실행 가능 (심지어 1 GB 내장그래픽도) │
  │ Runs on virtually any GPU — even 1 GB integrated graphics  │
  └───────────────────────────────────────────────────────────┘
```

---

**C-3. Worked example — Llama-3.1-8B in 4-bit, 4096 context**
**실례 B — Llama-3.1-8B 4비트 량자화, 4096 문맥**

```
────────── 설정값 ──────────
P          = 8,000,000,000  ← 80억 매개변수
n_layers   = 32             ← 32층
n_heads    = 32             ← 32 주의 머리
n_kv_heads = 8              ← GQA: 8 KV 머리 (4개 Q머리가 1 KV 머리 공유)
                              ← GQA reduces KV memory by 4× vs MHA
head_dim   = 4,096 ÷ 32 = 128  ← 머리당 128차원
seq_len    = 4,096          ← 최대 4,096 토큰 문맥
batch      = 1

────────── STEP 1: 4비트 가중치 기억 ──────────
Weight Memory = 8,000,000,000 × 0.5 bytes   ← Q4_K_M 량자화
              = 4,000,000,000 bytes
              = 3.73 GB

  ← fp16이었다면: 8,000,000,000 × 2 = 14.9 GB → 12 GB GPU에 들어가지 않음
  ← (In fp16 would need 14.9 GB — doesn't fit 12 GB GPU)
  ← 4bit 량자화로 14.9 GB → 3.73 GB (4배 절감, 75% 절약)
  ← (4-bit cuts 14.9 GB → 3.73 GB — 4× smaller, 75% saved)

────────── STEP 2: KV 완충기억기 ──────────
KV Cache = 2 × n_layers × n_kv_heads × head_dim × seq_len × batch × bytes

  2                 = 2
  × n_layers        = 2 × 32  = 64
  × n_kv_heads      = 64 × 8  = 512    ← GQA: n_kv_heads=8 (not 32)
  × head_dim        = 512 × 128 = 65,536
  × seq_len         = 65,536 × 4,096 = 268,435,456
  × batch           = 268,435,456 × 1 = 268,435,456
  × bytes (fp16)    = 268,435,456 × 2 = 536,870,912 bytes = 0.50 GB

  ← GQA 없이 MHA(n_kv_heads=32)였다면: 0.50 × 4 = 2.0 GB
  ← (Without GQA, MHA would need 2.0 GB KV cache — 4× more)
  ← GQA가 KV 기억을 4배 줄여주는 것 확인
  ← (GQA confirmed to give exactly 4× KV cache reduction)

────────── STEP 3: 합산 ──────────
Total = 3.73 + 0.50 + 0.15 = 4.38 GB

  ┌──────────────────────────────────────────────────────────────────┐
  │ 6 GB GPU (RTX 2060)에서도 쾌적하게 실행 가능 ✓                  │
  │ RTX 5070 (12 GB)에서 3개 동시 대화도 가능                        │
  │ Comfortably fits in a 6 GB GPU ✓                                 │
  │ RTX 5070 can run 3 simultaneous users (3 × 4.38 GB ≈ 13 GB)     │
  └──────────────────────────────────────────────────────────────────┘
```

---

**C-4. Worked example — 70B model in 4-bit**
**실례 C — 700억 매개변수 모형, 4비트 량자화**

```
────────── 설정값 ──────────
P          = 70,000,000,000  ← 700억 매개변수
n_layers   = 80              ← 80층
n_kv_heads = 8               ← GQA (Llama-3.1-70B 기준)
head_dim   = 128
seq_len    = 4,096
batch      = 1

────────── STEP 1: 가중치 기억 ──────────
Weight Memory = 70,000,000,000 × 0.5 bytes
              = 35,000,000,000 bytes
              = 32.6 GB

  ← fp16 이었다면: 70B × 2 = 130 GB → 소비자 GPU 불가능
  ← 4비트로 줄여도 32.6 GB → RTX 5090 (32 GB)도 아슬아슬

────────── STEP 2: KV 완충기억기 ──────────
  2 × 80 × 8 × 128 × 4,096 × 1 × 2
  = 160 × 8 × 128 × 4,096 × 2
  = 1,280 × 128 = 163,840
  × 4,096 = 671,088,640
  × 2     = 1,342,177,280 bytes = 1.25 GB

────────── STEP 3: 합산 ──────────
Total = 32.6 + 1.25 + 0.15 = 34.0 GB

  ┌──────────────────────────────────────────────────────────────────────────┐
  │ RTX 5090 (32 GB): 약간 초과 — OOM 가능성 있음 ✗ (아슬아슬)             │
  │ RTX 5090 (32 GB): slightly over — likely OOM ✗                          │
  │ 필요한 최소 GPU: 2× RTX 5090, 또는 A100 40 GB × 1 (타이트)             │
  │ Minimum: 2× RTX 5090, or A100 40 GB (tight)                             │
  │ 실용적 선택: 2× RTX 4090 (48 GB 합산) 또는 A100 40 GB × 1              │
  └──────────────────────────────────────────────────────────────────────────┘
```

---

#### Calculation D — KV cache growth per token (토큰당 KV 완충기억기 증가량)
> 대화를 할수록 KV 완충기억기는 선형으로 증가한다.  
> 새 토큰 하나가 생성될 때마다 아래의 고정 크기만큼 기억이 늘어난다.  
> *(Every new generated token adds a fixed amount to the KV cache — it grows linearly.)*

**D-1. Formula — 토큰당 증가량 공식**

```
토큰당 KV 증가 (KV bytes added per new token)
  = 2 × n_layers × n_kv_heads × head_dim × batch × bytes

  ← 토큰 하나를 생성하면, 모든 층의 K·V 텐서에 새 행(row)이 추가된다
  ← (Each new token appends one new row to K and V in every layer)
  ← 이 증가는 '토큰 위치' 차원에서만 발생한다 (seq_len 방향으로 성장)
  ← (Growth is only along the seq_len dimension — all other dims are fixed)
```

**D-2. Worked example — Llama-3.1-8B (fp16)**

```
토큰당 증가 = 2 × 32 × 8 × 128 × 1 × 2
            = 2 × 32 = 64
              × 8    = 512
              × 128  = 65,536
              × 1    = 65,536   (batch=1)
              × 2    = 131,072 bytes

  → 토큰 1개 생성 = 131,072 bytes = 128 KB (키보드 자판 한 번 = 128 KB)
  → (Every single token generated adds 128 KB to VRAM)

  문맥 길이별 누적 KV 기억 (KV cache total at different context lengths):
  ┌──────────────────────────────────┬─────────────┬────────────────────────────────────────────┐
  │ 문맥 길이 (Context length)        │ KV 기억     │ 비고 (Note)                                │
  ├──────────────────────────────────┼─────────────┼────────────────────────────────────────────┤
  │     512 tokens  (짧은 대화)       │   64 MB     │ 이 프로젝트 기본 block_size                │
  │   1,024 tokens  (중간 대화)       │  128 MB     │ 이 프로젝트 기본 block_size                │
  │   4,096 tokens  (표준 최대 문맥)  │  512 MB     │ 계산 C-3 예제와 일치 ✓                     │
  │   8,192 tokens  (확장 문맥)       │    1 GB     │ 가중치 기억의 27% 추가                     │
  │  32,768 tokens  (긴 문서)         │    4 GB     │ 가중치 기억의 107% ← KV > 가중치 기억       │
  │ 131,072 tokens  (128K 문맥)       │   16 GB     │ 가중치(3.7 GB) 4배 초과 — RTX 5070 불가능  │
  └──────────────────────────────────┴─────────────┴────────────────────────────────────────────┘

  ← 128K 문맥 모형은 가중치보다 KV 기억이 더 크다
  ← (For 128K context models, the KV cache is larger than the model weights)
  ← 긴 문맥 지원이 왜 기억 집약적인지 이 계산이 설명해준다
  ← (This explains why long-context support is so memory-intensive)
```

**D-3. Worked example — This project's Small model (34M, fp16)**

```
토큰당 증가 = 2 × 8 × 8 × 64 × 1 × 2
            = 16,384 bytes = 16 KB per token  ← 극히 작음

  ┌──────────────────────────────────────────────────────────────────────────────┐
  │ 문맥 1,024 토큰에서도 KV = 1,024 × 16 KB = 16 MB → 무시할 수 있는 수준    │
  │ Even at 1,024 tokens context, KV = only 16 MB — completely negligible       │
  │ 소형 모형은 문맥 길이가 KV 기억에 사실상 영향을 주지 않는다               │
  │ Small models: context length barely affects VRAM at all                      │
  └──────────────────────────────────────────────────────────────────────────────┘
```

---

#### Calculation E — Complete summary formulas (종합 추정 공식)
> 계산기 없이 머릿속으로 빠르게 추정하기 위한 간략 규칙과,  
> 정확한 계산을 위한 전체 공식을 함께 제공한다.

**E-1. Full formulas — 전체 공식**

```
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
훈련 기억 (TRAINING VRAM) — 처음부터 훈련시:
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

  VRAM_train =  P × 12                                        [기반기억: 바이트]
             +  n_layers × B × S × (6 × n_embd + ffn_dim) × 2  [활성화: 바이트]
             +  536,870,912                                    [부가비용 약 0.5 GB]

  경사도 검사점 사용시 활성화기억 항을 아래로 교체:
  (With gradient checkpointing, replace activation term with:)
             +  n_layers × B × S × n_embd × 2                 [검사점: 훨씬 작음]

  단위: 위 결과(바이트) ÷ 1,073,741,824 = GB

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
추론 기억 (INFERENCE VRAM) — 생성시:
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

  VRAM_infer =  P × bytes_per_param                           [가중치 기억]
             +  2 × n_layers × n_kv_heads × head_dim
                  × seq_len × batch × 2                       [KV 기억: fp16]
             +  161,061,273                                   [부가비용 약 0.15 GB]

  bytes_per_param: fp32=4, fp16=2, Q8=1, Q4=0.5, Q2=0.25

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
매개변수 수 (PARAMETER COUNT):
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

  P = vocab_size × n_embd                                     [어휘 삽입표현]
    + block_size × n_embd                                     [위치 삽입표현]
    + n_layers × (4 × n_embd² + 2 × n_embd × ffn_dim)        [변환기 층들]
    (+ vocab_size × n_embd  if tie_weights = false)           [출력 행렬]
```

**E-2. Quick mental math rules — 암산 간략 규칙**

```
훈련 (TRAINING):
  ★ GPU 기억 1 GB ≈ 훈련 가능 매개변수 30M 개
      (표준 일괄처리, 문맥길이 1024 기준)
  ★ 정확히: VRAM_GB ≈ P / 83,333,333  (83.3M params per GB)
      — 단, 이는 기반기억만; 활성화 포함하면 약 30M params/GB

  ★ 경사도 검사점 사용 → 활성화 기억 약 90% 절감, 속도 30% 저하
  ★ 일괄처리 반감 (B÷2) → 활성화 기억 반감, 유효 배치 유지하려면 grad_accum×2

추론 (INFERENCE):
  ★ fp16 가중치 기억 (GB) ≈ 매개변수 수 (B) × 2
      예: 7B 모형 → 7 × 2 = 14 GB
  ★ 4비트 가중치 기억 (GB) ≈ 매개변수 수 (B) × 0.5
      예: 7B 모형 → 7 × 0.5 = 3.5 GB
  ★ Q4 KV 기억 (GB) ≈ n_layers × n_kv_heads × head_dim × seq_len × 4 / 10⁹

훈련 대 추론 비율 (TRAINING vs INFERENCE):
  ★ 훈련 기억 ≈ 추론(fp16) 기억 × 6 ~ 8 배
  ★ 즉, fp16 추론이 가능한 GPU로는 같은 크기 모형을 훈련할 수 없다
  ★ (If a GPU can run a model in fp16, it cannot train that model from scratch)
```

**E-3. Full worked comparison — 3가지 모형 크기, 훈련 대 추론 나란히 비교**

```
┌──────────────────┬────────────────────────────────────┬────────────────────────────────────┐
│                  │ 훈련 (Training from scratch)        │ 추론 (Inference)                   │
│ 모형             │ 기반+활성화+부가 (B=8, S=1024)      │ fp16 가중치 + KV(4K ctx) + 부가    │
├──────────────────┼────────────────────────────────────┼────────────────────────────────────┤
│ 소형 ~34M        │ 0.41 + 0.94 + 0.50 = 1.85 GB       │ 0.06 + 0.01 + 0.15 = 0.22 GB      │
│ (본 프로젝트,    │ ← 기반: 34M×12=408MB               │ ← 가중치: 34M×2=68MB              │
│  Small config)   │ ← 활성화: 8×8×1024×(6×512+2048)×2 │ ← KV: 2×8×8×64×4096×2=32MB       │
│                  │ = 943MB                             │                                    │
├──────────────────┼────────────────────────────────────┼────────────────────────────────────┤
│ 대형 ~320M       │ 3.58 + 3.75 + 0.50 = 7.83 GB ✓    │ 0.60 + 0.50 + 0.15 = 1.25 GB      │
│ (본 프로젝트,    │ ← 기반: 320M×12=3,600MB            │ ← 가중치: 320M×2=600MB            │
│  Large config)   │ ← 활성화: 8×24×1024×10240×2        │ ← KV: 2×24×8×128×4096×2=500MB    │
│  RTX 5070 ✓      │ = 3,750MB                           │                                    │
├──────────────────┼────────────────────────────────────┼────────────────────────────────────┤
│ 7B 모형          │ 14+14+56+15 = ~99 GB ✗              │ fp16: 14+0.5+0.15 = 14.65 GB ✗    │
│ (Llama-3.1-8B    │ ← 단일 소비자 GPU 불가능            │ 4bit: 3.73+0.50+0.15 = 4.38 GB ✓  │
│  기준)           │ ← 2× A100 80GB 필요                │ ← RTX 5070에서 실행 가능           │
└──────────────────┴────────────────────────────────────┴────────────────────────────────────┘

핵심 결론 (Key takeaway):
  훈련 7.83 GB (대형 설정)  vs.  추론 1.25 GB (같은 모형)  →  6.3배 차이
  Training 7.83 GB (Large) vs. Inference 1.25 GB (same model) → 6.3× difference

  7B 모형: 훈련 ~99 GB  vs.  추론(4bit) 4.38 GB  →  22.6배 차이
  7B model: Training ~99 GB vs. Inference (4-bit) 4.38 GB → 22.6× difference
```

---

## 11. Honest Expectations

A 30M-parameter model trained from scratch on a custom corpus is **not** comparable to ChatGPT or other large foundation models. Realistic expectations:

- The model will produce fluent DPRK Korean text in the domains it was trained on.
- It will hallucinate facts, especially outside the training corpus.
- It has no knowledge of the world beyond what is in the training data.
- Quality improves substantially with more data (aim for 50 MB+ of clean text) and more training steps.
- Repetition is common at small scale — the default repetition penalty (`1.15`) mitigates this.
- The model works best when asked questions that closely resemble the training data format.

---

## 12. Troubleshooting

### "No CUDA GPU detected"
- Confirm `nvidia-smi` shows the GPU.
- Reinstall PyTorch with the correct CUDA version for your GPU.
- For RTX 5070 (Blackwell): `pip install --pre torch --index-url https://download.pytorch.org/whl/nightly/cu128`

### "Vocabulary size too high" during tokenizer training
- Your corpus is too small for the configured vocab size. The tokenizer auto-reduces vocab size, but the message will tell you the limit. Add more training data or reduce `tokenizer.vocab_size` in `model_config.yaml`.

### "Dataset has N tokens but block_size is M"
- Your val split is too small. The preprocessor may put only 1–2 documents in val. Solutions:
  - Add more data.
  - Training auto-reduces block_size in this case (a warning is printed).

### Training loss is not decreasing
- Check that data contains actual DPRK Korean text (not HTML, code, or empty files).
- Reduce `learning_rate` if loss is unstable (spiky, then goes to NaN).
- The sample data (20 documents) is too small to produce good results — use real training data.

### App says "No checkpoint found"
- Run `.\scripts\03_train_model.ps1` first. Checkpoints are saved in `checkpoints/`.
- Pass a specific checkpoint: `.\scripts\04_run_app.ps1 --checkpoint checkpoints\ckpt_step005000_best.pt`

### Korean text shows as `????` in the terminal
- Run Python with UTF-8 mode: `python -X utf8 -m src.app.gradio_app`
- Or set the environment variable: `$env:PYTHONUTF8 = "1"` before running scripts.
- The Gradio web UI (in your browser) always displays Korean correctly regardless of terminal encoding.
