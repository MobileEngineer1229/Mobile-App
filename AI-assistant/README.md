# DPRK Korean Food & Workout AI Assistant

A generative AI assistant trained **from scratch** on DPRK Korean (Joseon language) text. No pretrained foundation model is used — the entire neural network is trained on your own corpus using your local GPU.

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

This project builds a small language model (~30M parameters) that generates text in DPRK Korean (Joseon language). Unlike using an API such as ChatGPT, this model:

- Runs entirely on your local machine (offline after training).
- Is trained from scratch on data you provide — no third-party weights are used.
- Specialises in the DPRK Korean dialect: vocabulary, orthography, and register.
- Focuses on two domains: **food (Ryori)** and **workouts (exercise)**.
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
Korean is an agglutinative language: suffixes attach to roots to express grammar. Simple word-splitting would produce thousands of unique words that each appear rarely. Byte-Pair Encoding (BPE) instead finds common sub-word units — for example `exercise` (exercise) might be one token, while `exercise`, `I exercise` share the `exercise` token plus grammatical suffixes. This means:
- The vocabulary covers more text with fewer tokens.
- Even words the model has seen only in one grammatical form are partly familiar.

SentencePiece trains on raw text with no pre-segmentation — it processes Korean characters directly, which is correct for DPRK text that may use different spacing than South Korean Korean.

**Training process (`train_tokenizer.py`):**
1. Reads all `.txt` and `.jsonl` files from `data/raw/`.
2. Q&A records (`{"question": "...", "answer": "..."}`) are converted to `question: ...\nanswer: ...` format.
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
ids = tok.encode("How to make soybean paste soup", add_bos=True)   # → [1, 234, 56, ...]
text = tok.decode(ids)                                    # → "How to make soybean paste soup"
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
You are my Korean assistant who guides you through food and exercise..
user: How to make soybean paste soup?
assistant: To make soybean paste soup, boil the broth with anchovies and then...
user: How much tofu do you put in?
assistant:
```

The model then continues from `assistant:` — generating the assistant's reply. This prompt format is also what the model should be trained on for best chat performance: training data in Q&A JSONL format is automatically converted to `question: ...\nanswer: ...` which teaches the model this pattern.

**Context window management:** When the conversation history grows longer than `block_size - max_new_tokens`, the oldest turns are dropped to keep the prompt within the model's context window.

---

### 5.6 Web UI (Gradio)

**File:** `src/app/gradio_app.py`

The app loads the latest (or best) checkpoint and presents three tabs:

| Tab | Korean | Purpose |
|---|---|---|
| free conversation | Free conversation | General DPRK Korean chat |
| Ryori Recommendation | Recipe recommendation | Ask for recipes and meal plans |
| exercise plan | Workout plan | Ask for workout routines |

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
Soybean paste soup is a traditional Korean soup dish..
Make soup with anchovies and kelp and dissolve soybean paste....
```

### Free-text JSONL
One JSON object per line. The `"text"` field is used directly.

```jsonl
{"text": "Recipes: Miso soup\nmaterial: Two spoons of soybean paste, tofu half hair...\nCooking method:\n1. Wash the anchovies..."}
{"text": "Basic principles of exercise\nfirst, It is important to keep doing it every day...."}
```

### Q&A JSONL (recommended for the ask–answer assistant)
One JSON object per line with `"question"` and `"answer"` fields. These are automatically formatted as `question: ...\nanswer: ...` during tokenizer training and preprocessing.

```jsonl
{"question": "How to make soybean paste soup?", "answer": "After making the broth with anchovies, add soybean paste and boil tofu and vegetables.."}
{"question": "What are some good exercises to do every day??", "answer": "Walking 30 minutes every day is basic.. Do push-ups and sit-stands at home.."}
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

### 10.7 Step-by-step calculation logic — Detailed explanation of calculation logic
### (How every VRAM number is derived — How to derive all memory capacity figures)

> This section explains step by step how the numbers in all the tables above are calculated..  
> You can get an accurate estimate by directly substituting numbers according to the size of your model..  
> *(This section derives every number in the tables above, step by step.  
> Substitute your own model dimensions to get exact estimates.)*

---

#### Calculation A — How many parameters does a GPT model have?
#### calculation A — GPT Number of parameters in the model (Calculate total number of weights)
> **parameter(parameter)** is all the numbers that the model adjusts through learning..  
> Matrix multiplication for each layer(matrix multiplication)There is, Each element of each matrix is a parameter..  
> *(A parameter is any number the model adjusts during training — every element in every weight matrix.)*

**A-1. Master formula — Total number of parameters formula**

```
P = token_embedding                          ← Vocabulary insertion matrix (One vector for each word)
  + positional_embedding                     ← Positional insertion matrix (One vector at each position)
  + per_layer × n_layers                     ← Weights repeated for each transformer layer
  + output_head                              ← output projective matrix (0 when sharing weight)

────────────────────────────────────────────────────────
token_embedding      = vocab_size × n_embd
  ← Vocabulary size × Hidden dimension = every word n_embddog shame
  ← (vocabulary size × hidden dimension — one vector per word)

positional_embedding = block_size × n_embd
  ← maximum context length × Hidden dimension = per location n_embddog shame
  ← (max context length × hidden dimension — one vector per position)

────────────────────────────────────────────────────────
per_layer = attention_projections + ffn_projections

  attention_projections = 4 × n_embd × n_embd
    ← In attention mechanism Q(question), K(key), V(value), O(output) projective respectively n_embd×n_embd procession
    ← (Query, Key, Value, Output projection — each is an n_embd×n_embd matrix)
    ← 4 projections × n_embd rows × n_embd columns

  ffn_projections = 2 × n_embd × ffn_dim
    ← omnidirectional neural network: enlarged projection(n_embd→ffn_dim) + miniature projection(ffn_dim→n_embd)
    ← (Feed-forward network: up-projection expands, down-projection contracts)
    ← ffn_dim is typically 4 × n_embd

────────────────────────────────────────────────────────
output_head:
  = 0                    when tie_weights = true
    ← output matrix token_embedding Matrix and Share → No additional parameters
    ← (output matrix reuses token_embedding — saves vocab_size×n_embd params)
  = vocab_size × n_embd  when tie_weights = false
    ← Use separate output matrices → Additional parameters occur
```

---

**A-2. Worked example — Small config (default settings, ~30M parameter)**

```
────────── Setting value (Config values) ──────────
vocab_size  = 16,384   ← Vocabulary size (SentencePiece BPE 16K)
n_embd      =    512   ← Hidden dimension (hidden dimension)
block_size  =  1,024   ← maximum context length (max context length in tokens)
n_layers    =      8   ← converter layer number (number of transformer blocks)
ffn_dim     =  2,048   ← Omni-directional network internal dimensions (= 4 × n_embd)
tie_weights =   true   ← Share the output matrix with the input embedding expression

────────── Step by step calculation (Step-by-step) ──────────

STEP 1: Vocabulary insertion expression (Token embedding matrix)
  token_embedding = vocab_size × n_embd
                  = 16,384 × 512
                  = 8,388,608 params
  ← 16,384dog word × Each word is expressed as a 512-dimensional vector.

STEP 2: Position insertion expression (Positional embedding matrix)
  positional_embedding = block_size × n_embd
                       = 1,024 × 512
                       = 524,288 params
  ← max 1,024dog location × Each position is expressed as a 512-dimensional vector.

STEP 3: layer one parameter (Parameters per one transformer layer)
  projection of attention (Attention projections):
    = 4 × n_embd × n_embd
    = 4 × 512 × 512
    = 1,048,576 params
    ← Q procession: 512×512 = 262,144
    ← K procession: 512×512 = 262,144
    ← V procession: 512×512 = 262,144
    ← O procession: 512×512 = 262,144 (Total 1,048,576)

  omnidirectional network (FFN projections):
    = 2 × n_embd × ffn_dim
    = 2 × 512 × 2,048
    = 2,097,152 params
    ← enlargement matrix(up): 512×2,048 = 1,048,576
    ← reduction matrix(down): 2,048×512 = 1,048,576

  One floor total (per_layer total):
    = 1,048,576 + 2,097,152
    = 3,145,728 params

STEP 4: entire floor (All transformer layers)
  all_layers = n_layers × per_layer
             = 8 × 3,145,728
             = 25,165,824 params

STEP 5: output projection (Output head)
  output_head = 0   ← tie_weights = true So no addition

STEP 6: summation (Grand total)
  P = 8,388,608 + 524,288 + 25,165,824 + 0
    = 34,078,720
    ≈ 34M params

  ┌──────────────────────────────────────────────────┐
  │ (Note: READMEof "~30M"silver LayerNorm·bias exclusion value)  │
  │ Note: "~30M" in README excludes LayerNorm/bias   │
  └──────────────────────────────────────────────────┘
```

---

**A-3. Worked example — Large config (~350M parameter)**

```
────────── Setting value ──────────
vocab_size  = 16,384
n_embd      =  1,024   ← Hidden dimension doubled (512 → 1,024)
block_size  =  1,024
n_layers    =     24   ← Number of floors tripled (8 → 24)
ffn_dim     =  4,096   ← Omnidirectional network is also doubled (2,048 → 4,096)
tie_weights =   true

────────── calculation ──────────

STEP 1: token_embedding
  = 16,384 × 1,024 = 16,777,216 params

STEP 2: positional_embedding
  = 1,024 × 1,024 = 1,048,576 params

STEP 3: per_layer
  attention = 4 × 1,024 × 1,024 = 4,194,304
    ← n_embdSince this is twice, the matrix area is 4 times (512²→1024²)
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
  │ Small → Large Compare (Comparison):                                        │
  │   n_embd 2×, n_layers 3×  →  The parameters are about 10× increase (34M → 320M)      │
  │   n_embd doubled, n_layers tripled → params ~10× larger (34M → 320M)  │
  │   reason: The procession of the Lord n_embd²Since it is proportional to , the dimension is doubled = matrix 4 times            │
  │   Why: attention matrix scales as n_embd², so 2× dim = 4× matrix size  │
  └─────────────────────────────────────────────────────────────────────────┘
```

---

#### Calculation B — Training VRAM (Training shape processing device memory calculation)
> **training** is the model weights as well as the slope, Optimizer state, And for backpropagation  
> Since the activation values of all layers must be stored in memory at the same time, much more memory is required than inference..  
> *(Training keeps weights, gradients, optimizer states, AND all layer activations simultaneously —  
> far more than inference which only needs weights.)*

**B-1. Formula — training memory formula**

```
training memory (Training VRAM) = based memory + activated memory + Additional costs
                           = Base Memory + Activation Memory + Overhead

══════════════════════════════════════════════════════════════
[1] based memory (Base Memory) = P × 12 bytes
    ← Why do you need 12 bytes per parameter?:

  ┌─────────────────────────────┬───────┬──────────┬────────────────────────────────────────────┐
  │ component (Component)        │ Bytes │ data type   │ reason (Why)                                 │
  ├─────────────────────────────┼───────┼──────────┼────────────────────────────────────────────┤
  │ model weights (Model weights)  │   2   │ bf16     │ neural network itself — Also needed for inference                │
  │                              │       │          │ The network itself — also needed at infer. │
  ├─────────────────────────────┼───────┼──────────┼────────────────────────────────────────────┤
  │ slope (Gradients)           │   2   │ bf16     │ Partial differentiation for each weight of the loss function          │
  │                              │       │          │ ∂Loss/∂w for every parameter               │
  ├─────────────────────────────┼───────┼──────────┼────────────────────────────────────────────┤
  │ Adam equal amount (Momentum)         │   4   │ fp32     │ Exponential moving average of historical slope — direction memory      │
  │                              │       │          │ Exponential moving average of gradients    │
  ├─────────────────────────────┼───────┼──────────┼────────────────────────────────────────────┤
  │ Adam dispersion (Variance)         │   4   │ fp32     │ Moving average of historical slope squared — resize    │
  │                              │       │          │ Moving average of squared gradients        │
  ├─────────────────────────────┼───────┼──────────┼────────────────────────────────────────────┤
  │ total (Total per param)       │  12   │          │                                            │
  └─────────────────────────────┴───────┴──────────┴────────────────────────────────────────────┘

  ← AdamThis fp32Why?: Because numerical precision is required in gradient update (bf16It is lost by)
  ← (Adam uses fp32 because weight updates need precision — bf16 underflows on small changes)

══════════════════════════════════════════════════════════════
[2] activated memory (Activation Memory)
    ← Backpropagation(backward pass)requires the output value of each layer again..
    ← Therefore, omnidirectional calculation(forward pass) The median of all layers must be kept..
    ← (Backprop needs each layer's output to compute gradients; store all during forward pass)

  Tensors to store per layer (Tensors kept per layer for backprop):
  ┌────────────────────────────────────────────┬─────────────────────────────────┬──────────────────────────────┐
  │ tensor name (Tensor)                          │ number of elements (Elements)              │ reason (Why stored)            │
  ├────────────────────────────────────────────┼─────────────────────────────────┼──────────────────────────────┤
  │ floor input (Layer input — residual stream)     │ B × S × n_embd                 │ For calculation of residual connection slope       │
  │                                            │                                 │ residual gradient            │
  ├────────────────────────────────────────────┼─────────────────────────────────┼──────────────────────────────┤
  │ caution Q procession (Attention Q)                   │ B × S × n_embd                 │ Q For projective weight update          │
  ├────────────────────────────────────────────┼─────────────────────────────────┼──────────────────────────────┤
  │ caution K procession (Attention K)                   │ B × S × n_embd                 │ K For projective weight update          │
  ├────────────────────────────────────────────┼─────────────────────────────────┼──────────────────────────────┤
  │ caution V procession (Attention V)                   │ B × S × n_embd                 │ V For projective weight update          │
  ├────────────────────────────────────────────┼─────────────────────────────────┼──────────────────────────────┤
  │ Attention output (Attention output)                │ B × S × n_embd                 │ O For projective weight update          │
  ├────────────────────────────────────────────┼─────────────────────────────────┼──────────────────────────────┤
  │ Omnidirectional network input (FFN input, after LayerNorm)  │ B × S × n_embd                 │ For floor normalization weight update         │
  ├────────────────────────────────────────────┼─────────────────────────────────┼──────────────────────────────┤
  │ Omnidirectional network hidden layer (FFN hidden, after GELU)    │ B × S × ffn_dim                │ For update of enlarged weights             │
  │                                            │ ← ffn_dim = 4 × n_embd Normal    │                              │
  └────────────────────────────────────────────┴─────────────────────────────────┴──────────────────────────────┘

  formula (Formula):
  Activation Memory = n_layers × B × S × (6 × n_embd + ffn_dim) × 2 bytes
    ← 6 × n_embd: 6dog tensor(input·Q·K·V·Attention output·FFNinput) each n_embd element
    ← ffn_dim:    FFN One hidden tensor (Normal 4 × n_embd)
    ← × 2 bytes:  bf16 data type (2 bytes per element)
    ← × B × S:   Batch size(batch) × Sequential row length(sequence length)

══════════════════════════════════════════════════════════════
[3] Additional costs (Overhead) ≈ 0.5 GB
    ← CUDA context, cuBLAS buffer memory, Temporary work space, etc.
    ← (CUDA context, cuBLAS workspace, temporary buffers, fragmentation)
```

---

**B-2. Worked example — Large config, batch_size = 8**
**Excuse me — large setting, Batch size 8 (RTX 5070Maximum trainable settings at)**

```
────────── Setting value ──────────
P        = 320,000,000  ← about 3.2billion parameters
n_layers = 24           ← 24 transducer layers
n_embd   = 1,024        ← Hidden dimension
ffn_dim  = 4,096        ← Omni-directional network internal dimensions (= 4 × 1,024)
B        = 8            ← Batch size (sequences per GPU step)
S        = 1,024        ← context length (tokens per sequence)

────────── STEP 1: Based memory calculation ──────────
Base Memory = P × 12 bytes
            = 320,000,000 × 12
            = 3,840,000,000 bytes
            ÷ 1,073,741,824 (bytes per GB)
            = 3.58 GB

  Details (Breakdown):
    weight  (weights):   320M × 2 bytes = 640 MB  = 0.60 GB
    slope  (grads):     320M × 2 bytes = 640 MB  = 0.60 GB
    Adamequal amount(momentum):  320M × 4 bytes = 1,280 MB = 1.19 GB
    Adamdispersion(variance):  320M × 4 bytes = 1,280 MB = 1.19 GB
    ─────────────────────────────────────────────────────
    total    (total):                               3.58 GB ✓

────────── STEP 2: Active memory calculation ──────────
Activation Memory = n_layers × B × S × (6 × n_embd + ffn_dim) × 2 bytes

  internal calculation (Inner calculation):
    6 × n_embd + ffn_dim
    = 6 × 1,024 + 4,096
    = 6,144 + 4,096
    = 10,240   ← one floor bf16 number of elements (per-layer elements per token per sequence item)

    B × S = 8 × 1,024 = 8,192  ← Total number of tokens in batch (total tokens in one batch)

    Number of active bytes per layer (bytes per layer):
    = 8,192 × 10,240 × 2
    = 167,772,160 bytes
    = 160 MB

    entire floor (all 24 layers):
    = 24 × 160 MB = 3,840 MB = 3.75 GB
    ← 24Store the median of all layers simultaneously

────────── STEP 3: summation ──────────
Total VRAM = Base + Activation + Overhead
           = 3.58 + 3.75 + 0.50
           = 7.83 GB

  ┌─────────────────────────────────────────────────┐
  │ RTX 5070 (12 GB) Training available at ✓               │
  │ spare memory: 12 - 7.83 = 4.17 GB                  │
  │ Can train on RTX 5070 (12 GB) with 4.17 GB free │
  └─────────────────────────────────────────────────┘
```

---

**B-3. Same model, batch_size = 16 — When batch size is 16**

```
STEP 2 (changed):
  B × S = 16 × 1,024 = 16,384 tokens  ← Double batch processing = Activation is doubled
  Activation Memory = 24 × 16,384 × 10,240 × 2
                    = 24 × 335,544,320
                    = 8,053,063,680 bytes = 7.50 GB

Total = 3.58 + 7.50 + 0.50 = 11.58 GB

  ┌───────────────────────────────────────────────────────────────────┐
  │ Just barely 12 GB can exceed — lack of memory(OOM) Error possible  │
  │ Tight fit — may OOM on 12 GB; reduce batch_size to 8 instead      │
  │ If you reduce the batch size to 8, batch_size 8 Train safely like an example   │
  └───────────────────────────────────────────────────────────────────┘
```

---

**B-4. With gradient checkpointing — When using the gradient checkpoint technique**

```
What is a slope checkpoint?? (What is gradient checkpointing?)
  ← A technique that does not store the required intermediate values during backpropagation and recalculates them when needed.
  ← Activate memories around 87% Instead of reducing training speed by about 30% slow down
  ← (Do not store internal activations; recompute them during backprop)
  ← (Saves ~87% of activation memory at ~30% speed cost)

What to keep when applying checkpoints (What IS stored with checkpointing):
  → floor input(layer boundary inputs)save only — The inner tensor of the layer is recalculated
  → Only layer inputs stored — internal tensors (Q,K,V,FFN hidden) recomputed

Bytes stored per layer (bytes stored per layer):
  = B × S × n_embd × 2 bytes   ← Only one floor boundary input
  = 16 × 1,024 × 1,024 × 2
  = 33,554,432 bytes = 32 MB per layer

Total 24 floors (all 24 layers):
  = 24 × 33,554,432
  = 805,306,368 bytes = 0.75 GB

Total VRAM (Apply checkpoint, batch_size=16):
  = 3.58 + 0.75 + 0.50 = 4.83 GB

  ┌────────────────────────────────────────────────────────────────────────┐
  │ Activation memory savings: 7.50 GB → 0.75 GB  (Reduced to about 1/10th)          │
  │ Total memory savings: 11.58 GB → 4.83 GB  (about 58% savings)                    │
  │ Activation: 7.50 GB → 0.75 GB (10× reduction)                        │
  │ Total: 11.58 GB → 4.83 GB (58% reduction, 30% slower training speed) │
  └────────────────────────────────────────────────────────────────────────┘
```

---

**B-5. How batch_size and sequence_length affect activation memory**
**batch processing·Activation memory changes according to context length (Large config standard)**

```
Activation Memory = n_layers × B × S × (6×n_embd + ffn_dim) × 2
                  = 24 × B × S × 10,240 × 2
                  = B × S × 491,520 bytes per (B×S) token

  ┌──────────────┬────────────┬───────────────────┬───────────────────┐
  │ batch_size B │ seq_len  S │ B × S (Total number of tokens) │ Activation Memory │
  ├──────────────┼────────────┼───────────────────┼───────────────────┤
  │      4       │    512     │      2,048         │      1.00 GB      │
  │      4       │  1,024     │      4,096         │      1.88 GB      │
  │      8       │  1,024     │      8,192   ★     │      3.75 GB      │  ← B-2 example
  │     16       │  1,024     │     16,384         │      7.50 GB      │  ← B-3 example
  │     16       │  2,048     │     32,768         │     15.00 GB      │  ← OOM on 12 GB
  │      1       │  2,048     │      2,048         │      1.00 GB      │  ← single long context
  └──────────────┴────────────┴───────────────────┴───────────────────┘

  ← Same thing if you reduce batch processing and context length GPUYou can train a larger model in
  ← (Reducing batch_size or seq_len lets you train a larger model on the same GPU)
  ← sweet, Batch size reduction grad_accum_stepsLearning efficiency must be maintained by supplementing with
  ← (Compensate with larger grad_accum_steps to keep effective batch size constant)
```

---

#### Calculation C — Inference VRAM (Inference graphic processing device memory calculation)
> **inference(inference)** is much simpler than training.  
> Gradient, Optimizer state diagram, No need to activate or store.  
> What you need is weight and, Key value buffer memory that grows as the conversation lasts(KV cache)It's just.  
> *(Inference is much simpler than training — no gradients, no optimizer, minimal activations.  
> Only weights and the growing KV cache matter.)*

**C-1. Formula — inferential memory formula**

```
inference memory (Inference VRAM) = weight memory + KV buffer memory + Activation additional cost
                           = Weight Memory + KV Cache + Activation Overhead

══════════════════════════════════════════════════════════════
[1] weight memory (Weight Memory) = P × bytes_per_param

  Bytes per parameter by quantization method (bytes per param by quantization method):
  ┌─────────────────┬───────────────┬───────────────────────────────────────────────────┐
  │ precision           │ per parameter    │ Description (Description)                                │
  │ (Precision)      │ number of bytes     │                                                   │
  ├─────────────────┼───────────────┼───────────────────────────────────────────────────┤
  │ fp32            │ 4.0 bytes     │ Fully 32-bit floating point — most accurate, greatest memory  │
  │                 │               │ Full 32-bit float — most precise, largest memory  │
  ├─────────────────┼───────────────┼───────────────────────────────────────────────────┤
  │ fp16 / bf16     │ 2.0 bytes     │ 16bit floating point — Used for both training and inference       │
  │                 │               │ 16-bit float — used in both training and inference │
  ├─────────────────┼───────────────┼───────────────────────────────────────────────────┤
  │ 8bit (Q8_0)    │ 1.0 byte      │ Integer Quantization — Minor quality loss, half memory           │
  │                 │               │ Integer quant — minimal quality loss, half memory  │
  ├─────────────────┼───────────────┼───────────────────────────────────────────────────┤
  │ 4bit (Q4_K_M)  │ 0.5 bytes     │ 4bit quantization — consumer GPUMost used in         │
  │                 │               │ Best quality-per-byte for consumer GPUs            │
  ├─────────────────┼───────────────┼───────────────────────────────────────────────────┤
  │ 2bit (Q2_K)    │ 0.25 bytes    │ extreme compression — Severe quality decline, I hardly ever use it           │
  │                 │               │ Extreme compression — significant quality loss     │
  └─────────────────┴───────────────┴───────────────────────────────────────────────────┘

  ← 4bit quantization = Weight level 16(4bit=2⁴)round to
  ← Store two elements in one byte → original quarter memory
  ← (4-bit quant packs 2 values per byte → ¼ of fp16 storage)

══════════════════════════════════════════════════════════════
[2] KV buffer memory (KV Cache)

  KV Cache = 2 × n_layers × n_kv_heads × head_dim × seq_len × batch × bytes

  Variable Description (Variable definitions):
    2          ← K(key) 1 tensor + V(value) 1 tensor
                 one Key tensor + one Value tensor per layer

    n_kv_heads ← KV Number of heads in the state
                 = n_heads  (multi-headedness, MHA — Every head is different K·V holding)
                 < n_heads  (military boots, GQA — several Q one head K·V share)
                 ← modern large model(Llama-3, Qwen2.5)silver GQAby KV memory savings

    head_dim   ← head one dimension = n_embd ÷ n_heads

    seq_len    ← current context length (number of tokens)
                 ← Increases as you talk → KV Memories continue to increase
                 current context length — grows with every generated token

    batch      ← Number of conversations being processed in parallel (concurrent conversations)

    bytes      ← KVdata type byte that stores
                 = 2  (fp16)   ← Most inference servers
                 = 1  (int8)   ← Quantization KV Save money by remembering

══════════════════════════════════════════════════════════════
[3] Activation additional cost (Activation Overhead) ≈ 0.1–0.3 GB
    ← During inference, compute only one layer at a time and release immediately
    ← (inference computes one layer at a time, frees it immediately)
    ← activated memory of training(number GB)Unlike, it is very small
    ← (unlike training's activation memory of several GB — this is tiny)
```

---

**C-2. Worked example — This project's Small model (34M), fp16**
**Excuse me A — Small model of this project (34M parameter), Half-precision inference**

```
────────── Setting value ──────────
P         = 34,000,000    ← Number of small model parameters (calculation Aoutput from)
n_layers  =  8            ← converter layer number
n_heads   =  8            ← Number of heads in the state
n_embd    = 512           ← Hidden dimension
head_dim  = 512 ÷ 8 = 64  ← head one dimension
seq_len   = 512           ← mid-point of conversation (512 token conversation)
batch     =  1            ← 1 user

────────── STEP 1: weight memory ──────────
Weight Memory = 34,000,000 × 2 bytes   ← fp16
              = 68,000,000 bytes
              = 0.063 GB

────────── STEP 2: KV Buffer memory calculation ──────────
KV Cache = 2 × n_layers × n_heads × head_dim × seq_len × batch × bytes

step by step multiplication (step-by-step multiplication):
  2           = 2            ← KWow V 1 each
  × n_layers  = 2 × 8  = 16  ← 8dog floor
  × n_heads   = 16 × 8 = 128  ← 8dog attention head
  × head_dim  = 128 × 64 = 8,192  ← 64 dimensions per head
  × seq_len   = 8,192 × 512 = 4,194,304  ← 512 token
  × batch     = 4,194,304 × 1 = 4,194,304  ← 1 user
  × bytes     = 4,194,304 × 2 = 8,388,608 bytes  ← fp16

  KV Cache = 8,388,608 bytes = 0.008 GB = 8 MB

  ← It's a small model KV The buffer memory is very small. (Almost ignorable)
  ← (Tiny model → KV cache is negligible — only 8 MB)

────────── STEP 3: summation ──────────
Total = 0.063 + 0.008 + 0.1 (overhead) = 0.171 GB

  ┌───────────────────────────────────────────────────────────┐
  │ in fact any GPUCan also run in (even 1 GB Built-in graphics) │
  │ Runs on virtually any GPU — even 1 GB integrated graphics  │
  └───────────────────────────────────────────────────────────┘
```

---

**C-3. Worked example — Llama-3.1-8B in 4-bit, 4096 context**
**Excuse me B — Llama-3.1-8B 4bit quantization, 4096 context**

```
────────── Setting value ──────────
P          = 8,000,000,000  ← 80billion parameters
n_layers   = 32             ← 32floor
n_heads    = 32             ← 32 attention head
n_kv_heads = 8              ← GQA: 8 KV head (4dog Qhead 1 KV head sharing)
                              ← GQA reduces KV memory by 4× vs MHA
head_dim   = 4,096 ÷ 32 = 128  ← 128 dimensions per head
seq_len    = 4,096          ← up to 4,096 token context
batch      = 1

────────── STEP 1: 4Remember bit weights ──────────
Weight Memory = 8,000,000,000 × 0.5 bytes   ← Q4_K_M Quantization
              = 4,000,000,000 bytes
              = 3.73 GB

  ← fp16If it were: 8,000,000,000 × 2 = 14.9 GB → 12 GB GPUdoes not enter
  ← (In fp16 would need 14.9 GB — doesn't fit 12 GB GPU)
  ← 4bit 14 Liangjahwa Road.9 GB → 3.73 GB (42x savings, 75% saving)
  ← (4-bit cuts 14.9 GB → 3.73 GB — 4× smaller, 75% saved)

────────── STEP 2: KV buffer memory ──────────
KV Cache = 2 × n_layers × n_kv_heads × head_dim × seq_len × batch × bytes

  2                 = 2
  × n_layers        = 2 × 32  = 64
  × n_kv_heads      = 64 × 8  = 512    ← GQA: n_kv_heads=8 (not 32)
  × head_dim        = 512 × 128 = 65,536
  × seq_len         = 65,536 × 4,096 = 268,435,456
  × batch           = 268,435,456 × 1 = 268,435,456
  × bytes (fp16)    = 268,435,456 × 2 = 536,870,912 bytes = 0.50 GB

  ← GQA without MHA(n_kv_heads=32)If it were: 0.50 × 4 = 2.0 GB
  ← (Without GQA, MHA would need 2.0 GB KV cache — 4× more)
  ← GQAgo KV Confirmed to reduce memory by 4 times
  ← (GQA confirmed to give exactly 4× KV cache reduction)

────────── STEP 3: summation ──────────
Total = 3.73 + 0.50 + 0.15 = 4.38 GB

  ┌──────────────────────────────────────────────────────────────────┐
  │ 6 GB GPU (RTX 2060)Can run comfortably in ✓                  │
  │ RTX 5070 (12 GB)Three simultaneous conversations are also possible.                        │
  │ Comfortably fits in a 6 GB GPU ✓                                 │
  │ RTX 5070 can run 3 simultaneous users (3 × 4.38 GB ≈ 13 GB)     │
  └──────────────────────────────────────────────────────────────────┘
```

---

**C-4. Worked example — 70B model in 4-bit**
**Excuse me C — 700billion parameter model, 4bit quantization**

```
────────── Setting value ──────────
P          = 70,000,000,000  ← 700billion parameters
n_layers   = 80              ← 80floor
n_kv_heads = 8               ← GQA (Llama-3.1-70B standard)
head_dim   = 128
seq_len    = 4,096
batch      = 1

────────── STEP 1: weight memory ──────────
Weight Memory = 70,000,000,000 × 0.5 bytes
              = 35,000,000,000 bytes
              = 32.6 GB

  ← fp16 If it were: 70B × 2 = 130 GB → consumer GPU impossible
  ← 4Even if reduced to bits, it is 32.6 GB → RTX 5090 (32 GB)It's too close

────────── STEP 2: KV buffer memory ──────────
  2 × 80 × 8 × 128 × 4,096 × 1 × 2
  = 160 × 8 × 128 × 4,096 × 2
  = 1,280 × 128 = 163,840
  × 4,096 = 671,088,640
  × 2     = 1,342,177,280 bytes = 1.25 GB

────────── STEP 3: summation ──────────
Total = 32.6 + 1.25 + 0.15 = 34.0 GB

  ┌──────────────────────────────────────────────────────────────────────────┐
  │ RTX 5090 (32 GB): slightly over — OOM Possible ✗ (Just barely)             │
  │ RTX 5090 (32 GB): slightly over — likely OOM ✗                          │
  │ minimum required GPU: 2× RTX 5090, or A100 40 GB × 1 (tight)             │
  │ Minimum: 2× RTX 5090, or A100 40 GB (tight)                             │
  │ a practical choice: 2× RTX 4090 (48 GB summation) or A100 40 GB × 1              │
  └──────────────────────────────────────────────────────────────────────────┘
```

---

#### Calculation D — KV cache growth per token (per token KV Buffer memory increase)
> The more we talk KV Buffer memory increases linearly.  
> Each time a new token is created, the memory increases by a fixed size below:.  
> *(Every new generated token adds a fixed amount to the KV cache — it grows linearly.)*

**D-1. Formula — Formula for increase per token**

```
per token KV increase (KV bytes added per new token)
  = 2 × n_layers × n_kv_heads × head_dim × batch × bytes

  ← When you create a token, on all floors K·V new row in tensor(row)is added
  ← (Each new token appends one new row to K and V in every layer)
  ← This increase is 'token location' It only happens in one dimension (seq_len grow in the direction)
  ← (Growth is only along the seq_len dimension — all other dims are fixed)
```

**D-2. Worked example — Llama-3.1-8B (fp16)**

```
Increase per token = 2 × 32 × 8 × 128 × 1 × 2
            = 2 × 32 = 64
              × 8    = 512
              × 128  = 65,536
              × 1    = 65,536   (batch=1)
              × 2    = 131,072 bytes

  → Generate 1 token = 131,072 bytes = 128 KB (keyboard once = 128 KB)
  → (Every single token generated adds 128 KB to VRAM)

  Accumulation by context length KV memory (KV cache total at different context lengths):
  ┌──────────────────────────────────┬─────────────┬────────────────────────────────────────────┐
  │ context length (Context length)        │ KV memory     │ Remarks (Note)                                │
  ├──────────────────────────────────┼─────────────┼────────────────────────────────────────────┤
  │     512 tokens  (short conversation)       │   64 MB     │ This project basics block_size                │
  │   1,024 tokens  (mid conversation)       │  128 MB     │ This project basics block_size                │
  │   4,096 tokens  (standard maximum context)  │  512 MB     │ calculation C-3 matches the example ✓                     │
  │   8,192 tokens  (extended context)       │    1 GB     │ 27 of weighted memory% add                     │
  │  32,768 tokens  (long document)         │    4 GB     │ 107 of weighted memory% ← KV > weight memory       │
  │ 131,072 tokens  (128K context)       │   16 GB     │ weight(3.7 GB) 4double excess — RTX 5070 impossible  │
  └──────────────────────────────────┴─────────────┴────────────────────────────────────────────┘

  ← 128K Contextual models are better than weights. KV the memory is bigger
  ← (For 128K context models, the KV cache is larger than the model weights)
  ← This calculation explains why long context support is memory intensive.
  ← (This explains why long-context support is so memory-intensive)
```

**D-3. Worked example — This project's Small model (34M, fp16)**

```
Increase per token = 2 × 8 × 8 × 64 × 1 × 2
            = 16,384 bytes = 16 KB per token  ← extremely small

  ┌──────────────────────────────────────────────────────────────────────────────┐
  │ Context 1,024 Even in tokens KV = 1,024 × 16 KB = 16 MB → negligible level    │
  │ Even at 1,024 tokens context, KV = only 16 MB — completely negligible       │
  │ The small model has portal length KV Has virtually no effect on memory               │
  │ Small models: context length barely affects VRAM at all                      │
  └──────────────────────────────────────────────────────────────────────────────┘
```

---

#### Calculation E — Complete summary formulas (Comprehensive estimation formula)
> Simple rules for quick estimation in your head without a calculator,  
> Provides complete formulas for accurate calculations.

**E-1. Full formulas — full formula**

```
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
training memory (TRAINING VRAM) — Training from the beginning:
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

  VRAM_train =  P × 12                                        [based memory: byte]
             +  n_layers × B × S × (6 × n_embd + ffn_dim) × 2  [activation: byte]
             +  536,870,912                                    [Additional costs approximately 0.5 GB]

  When using gradient checkpoints, change the activation memory term to below.:
  (With gradient checkpointing, replace activation term with:)
             +  n_layers × B × S × n_embd × 2                 [checkpoint: much smaller]

  unit: above result(byte) ÷ 1,073,741,824 = GB

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
inference memory (INFERENCE VRAM) — When created:
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

  VRAM_infer =  P × bytes_per_param                           [weight memory]
             +  2 × n_layers × n_kv_heads × head_dim
                  × seq_len × batch × 2                       [KV memory: fp16]
             +  161,061,273                                   [Additional costs approximately 0.15 GB]

  bytes_per_param: fp32=4, fp16=2, Q8=1, Q4=0.5, Q2=0.25

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
number of parameters (PARAMETER COUNT):
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

  P = vocab_size × n_embd                                     [Vocabulary insertion expression]
    + block_size × n_embd                                     [Position insertion expression]
    + n_layers × (4 × n_embd² + 2 × n_embd × ffn_dim)        [converter layers]
    (+ vocab_size × n_embd  if tie_weights = false)           [output matrix]
```

**E-2. Quick mental math rules — Simple mental arithmetic rules**

```
training (TRAINING):
  ★ GPU memory 1 GB ≈ Trainable parameters 30M dog
      (Standard batch processing, Based on context length 1024)
  ★ exactly: VRAM_GB ≈ P / 83,333,333  (83.3M params per GB)
      — sweet, This is only basic memory; Approximately 30 including activationM params/GB

  ★ Use gradient checkpoint → Activate memory approximately 90% savings, speed 30% degradation
  ★ Batch processing halved (B÷2) → Activation memory antagonism, To maintain valid placement grad_accum×2

inference (INFERENCE):
  ★ fp16 weight memory (GB) ≈ number of parameters (B) × 2
      yes: 7B model → 7 × 2 = 14 GB
  ★ 4Remember bit weights (GB) ≈ number of parameters (B) × 0.5
      yes: 7B model → 7 × 0.5 = 3.5 GB
  ★ Q4 KV memory (GB) ≈ n_layers × n_kv_heads × head_dim × seq_len × 4 / 10⁹

Training to inference ratio (TRAINING vs INFERENCE):
  ★ training memory ≈ inference(fp16) memory × 6 ~ 8 ship
  ★ That is, fp16 capable of inference GPUcannot train models of the same size
  ★ (If a GPU can run a model in fp16, it cannot train that model from scratch)
```

**E-3. Full worked comparison — 3eggplant model size, Training vs. inference side by side comparison**

```
┌──────────────────┬────────────────────────────────────┬────────────────────────────────────┐
│                  │ training (Training from scratch)        │ inference (Inference)                   │
│ model             │ based+activation+addition (B=8, S=1024)      │ fp16 weight + KV(4K ctx) + addition    │
├──────────────────┼────────────────────────────────────┼────────────────────────────────────┤
│ small ~34M        │ 0.41 + 0.94 + 0.50 = 1.85 GB       │ 0.06 + 0.01 + 0.15 = 0.22 GB      │
│ (This project,    │ ← based: 34M×12=408MB               │ ← weight: 34M×2=68MB              │
│  Small config)   │ ← activation: 8×8×1024×(6×512+2048)×2 │ ← KV: 2×8×8×64×4096×2=32MB       │
│                  │ = 943MB                             │                                    │
├──────────────────┼────────────────────────────────────┼────────────────────────────────────┤
│ large ~320M       │ 3.58 + 3.75 + 0.50 = 7.83 GB ✓    │ 0.60 + 0.50 + 0.15 = 1.25 GB      │
│ (This project,    │ ← based: 320M×12=3,600MB            │ ← weight: 320M×2=600MB            │
│  Large config)   │ ← activation: 8×24×1024×10240×2        │ ← KV: 2×24×8×128×4096×2=500MB    │
│  RTX 5070 ✓      │ = 3,750MB                           │                                    │
├──────────────────┼────────────────────────────────────┼────────────────────────────────────┤
│ 7B model          │ 14+14+56+15 = ~99 GB ✗              │ fp16: 14+0.5+0.15 = 14.65 GB ✗    │
│ (Llama-3.1-8B    │ ← single consumer GPU impossible            │ 4bit: 3.73+0.50+0.15 = 4.38 GB ✓  │
│  standard)           │ ← 2× A100 80GB need                │ ← RTX 5070Can run on           │
└──────────────────┴────────────────────────────────────┴────────────────────────────────────┘

Key Conclusion (Key takeaway):
  training 7.83 GB (large setting)  vs.  inference 1.25 GB (same model)  →  6.3pear difference
  Training 7.83 GB (Large) vs. Inference 1.25 GB (same model) → 6.3× difference

  7B model: training ~99 GB  vs.  inference(4bit) 4.38 GB  →  22.6pear difference
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
