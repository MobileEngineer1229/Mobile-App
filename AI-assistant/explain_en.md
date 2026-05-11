# Why My PC Can Only Train Up to ~350M Parameters (550M with Tricks)

> **GPU:** NVIDIA RTX 5070 — 12 GB GDDR7 VRAM  
> **Architecture:** Blackwell (Compute Capability 12.0 / sm_120)  
> **Required environment:** PyTorch 2.7+ · CUDA 12.8

---

## Table of Contents

1. [The Core Rule — VRAM Is an Absolute Hard Limit](#1-the-core-rule--vram-is-an-absolute-hard-limit)
2. [What Training Stores in VRAM](#2-what-training-stores-in-vram)
3. [Bytes-per-Parameter Breakdown in Detail](#3-bytes-per-parameter-breakdown-in-detail)
4. [Logic Calculation — Why 350M Is the Ceiling](#4-logic-calculation--why-350m-is-the-ceiling)
5. [Gradient Checkpointing — Stretching to 550M](#5-gradient-checkpointing--stretching-to-550m)
6. [VRAM Size vs. Training Limit — Reference Table](#6-vram-size-vs-training-limit--reference-table)
7. [Why Inference Is So Much Cheaper Than Training](#7-why-inference-is-so-much-cheaper-than-training)
8. [Why I Can Run a 7B Model but Cannot Train One](#8-why-i-can-run-a-7b-model-but-cannot-train-one)
9. [Tuning Levers — Pushing the Limit Without Buying More VRAM](#9-tuning-levers--pushing-the-limit-without-buying-more-vram)
10. [Full Summary](#10-full-summary)

---

## 1. The Core Rule — VRAM Is an Absolute Hard Limit

> **VRAM is a hard physical ceiling.**  
> If the total memory footprint of a training run exceeds 12 GB,  
> the process does **not** slow down — it **crashes immediately** with an Out-Of-Memory (OOM) error.

```
OOM = Out of Memory — training process is killed by the operating system
```

There is no negotiation here. The GPU can only operate on data that fits inside VRAM. The moment that capacity is exceeded, the entire training process is force-terminated — not throttled, not paged to RAM, terminated.

---

## 2. What Training Stores in VRAM

Training a model from scratch and running (inference on) a finished model are fundamentally different operations, even though they use the same hardware.

During training, the GPU must hold **four separate pieces of information for every single parameter simultaneously**:

| Stored item | Data type | Bytes per parameter | Why it exists |
|-------------|-----------|--------------------:|---------------|
| Model weights | bf16 | 2 | The neural network itself — all learned values |
| Gradients | bf16 | 2 | Rate of change of loss w.r.t. each weight (dLoss/dWeight), computed in backward pass |
| Adam momentum (1st moment) | fp32 | 4 | Optimizer state 1 — exponential moving average of past gradients |
| Adam variance (2nd moment) | fp32 | 4 | Optimizer state 2 — exponential moving average of squared gradients |
| **Base subtotal** | | **12** | Before activations |

**On top of that — activations.** Activations are the intermediate outputs produced by each layer during the forward pass. They must all stay in VRAM simultaneously so the backward pass can use them to compute gradients.

Activation memory does **not** scale with parameter count. It scales with `batch_size × sequence_length × n_layers`.

```
Total VRAM required = (number of parameters × 12 bytes) + activations
```

---

## 3. Bytes-per-Parameter Breakdown in Detail

### 3-1. Model weights — 2 bytes per parameter (bf16)

Weights are the "memory" of the neural network — every matrix, bias, and embedding value in every layer. Using `bfloat16` (bf16) format stores each number in 2 bytes.

```
350,000,000 parameters × 2 bytes = 700 MB  (0.70 GB)
```

### 3-2. Gradients — 2 bytes per parameter (bf16)

Backpropagation computes one gradient value per weight — how much that weight contributed to the loss and in which direction it should move. Gradient storage is therefore identical in size to weight storage.

```
350,000,000 × 2 bytes = 700 MB  (0.70 GB)
```

**Why are gradients needed?**  
They tell the optimizer which direction and how much to adjust each weight to reduce the loss. They are consumed every optimizer step and recomputed fresh each backward pass.

### 3-3. Adam optimizer states — 8 bytes per parameter (fp32 × 2)

AdamW does not simply apply the current gradient directly. It maintains a running history of past gradients in two forms:

**1st moment — momentum (4 bytes):**
```
m_t = β₁ × m_(t-1) + (1 - β₁) × gradient_t
```
Smooths the gradient signal, damping oscillation across steps. (β₁ = 0.9)

**2nd moment — variance (4 bytes):**
```
v_t = β₂ × v_(t-1) + (1 - β₂) × gradient_t²
```
Scales the effective learning rate per parameter adaptively. (β₂ = 0.999)

Both states must be stored in `float32` (fp32, 4 bytes each) to avoid numerical precision loss that accumulates over thousands of update steps and destabilises training.

```
350,000,000 × 8 bytes = 2,800 MB  (2.80 GB)
```

### 3-4. Activations — proportional to batch × sequence × layers

Each layer takes an input tensor, applies its computation, and produces an output tensor. That output must stay in VRAM for the entire forward pass so the backward pass can use it to compute gradients via the chain rule.

```
Activation memory ≈ batch_size × seq_len × n_embd × n_layers × 2 bytes

350M model (batch=8, seq=1024, n_embd=1024, n_layer=24, bf16):
= 8 × 1024 × 1024 × 24 × 2 bytes
= 402,653,184 bytes
≈ 7.80 GB
```

Notice: activations (7.80 GB) dwarf the parameter subtotal (4.20 GB). Activations are the dominant memory cost during training.

---

## 4. Logic Calculation — Why 350M Is the Ceiling

### 4-1. 350M model — fits exactly ✓

**Model config:** `n_layer: 24, n_head: 16, n_embd: 1024`  
**Training config:** `batch_size: 8, seq_len: 1024`

```
─────────────────────────────────────────────────────────────────
 Item                  Calculation                        Result
─────────────────────────────────────────────────────────────────
 Weights  (bf16)       350,000,000 × 2 bytes          =  0.70 GB
 Gradients (bf16)      350,000,000 × 2 bytes          =  0.70 GB
 Adam momentum (fp32)  350,000,000 × 4 bytes          =  1.40 GB
 Adam variance (fp32)  350,000,000 × 4 bytes          =  1.40 GB
                                                       ─────────
 Parameter subtotal                                    =  4.20 GB

 Activations (bf16)    8×1024×1024×24×2 bytes         ≈  7.80 GB
                                                       ─────────
 TOTAL                                                 ≈ 12.00 GB  ✓
─────────────────────────────────────────────────────────────────
 RTX 5070 limit: 12 GB   →   fits exactly
```

VRAM utilisation is ~100 %. Raising batch size above 8 pushes activations over the limit and causes an OOM crash.

---

### 4-2. 500M model — does NOT fit ✗

```
─────────────────────────────────────────────────────────────────
 Item                  Calculation                        Result
─────────────────────────────────────────────────────────────────
 Weights  (bf16)       500,000,000 × 2 bytes          =  1.00 GB
 Gradients (bf16)      500,000,000 × 2 bytes          =  1.00 GB
 Adam momentum (fp32)  500,000,000 × 4 bytes          =  2.00 GB
 Adam variance (fp32)  500,000,000 × 4 bytes          =  2.00 GB
                                                       ─────────
 Parameter subtotal                                    =  6.00 GB

 Activations (same batch/seq)                         ≈  7.80 GB
                                                       ─────────
 TOTAL                                                 ≈ 13.80 GB  ✗
─────────────────────────────────────────────────────────────────
 Shortfall: 1.80 GB   →   OOM crash at launch
```

The process crashes before training begins — 1.80 GB over budget.

---

### 4-3. 1B model — far exceeds the budget ✗

```
─────────────────────────────────────────────────────────────────
 Item                   Calculation                       Result
─────────────────────────────────────────────────────────────────
 Weights   (bf16)    1,000,000,000 × 2 bytes          =  2.00 GB
 Gradients (bf16)    1,000,000,000 × 2 bytes          =  2.00 GB
 Adam momentum (fp32) 1,000,000,000 × 4 bytes         =  4.00 GB
 Adam variance (fp32) 1,000,000,000 × 4 bytes         =  4.00 GB
                                                       ─────────
 Parameter subtotal                                    = 12.00 GB

 Activations (batch=16, typical)                      ≈ 10.00 GB
                                                       ─────────
 TOTAL                                                 ≈ 22.00 GB  ✗
─────────────────────────────────────────────────────────────────
 Shortfall: 10 GB   →   requires a 24 GB card (RTX 4090)
```

The parameter subtotal alone already consumes all 12 GB — there is zero room left for activations.

---

### 4-4. Why the base floor is exactly 12 bytes per parameter

```
One parameter during training occupies:

  Weights    ██  2 bytes  (bf16)
  Gradients  ██  2 bytes  (bf16)
  Momentum   ████  4 bytes  (fp32)
  Variance   ████  4 bytes  (fp32)
             ─────────────────────
  Total         12 bytes
```

**Why are optimizer states in fp32?**  
Summing gradients across thousands of steps in bf16 accumulates rounding errors that cause training instability or divergence. Weights and gradients are fine in bf16 since they are overwritten each step, but optimizer states must preserve full fp32 precision.

---

## 5. Gradient Checkpointing — Stretching to 550M

### 5-1. How it works

`torch.utils.checkpoint` does **not** store every layer's activations during the forward pass. Instead, it recomputes them on-demand when the backward pass needs them. This roughly halves activation memory at the cost of ~30 % slower training (each segment of the network is computed twice).

```
Standard training:  store ALL layer activations  →  large memory,  fast
Checkpointing:      recompute activations on-demand  →  ~half memory,  ~30 % slower
```

### 5-2. Calculation — 550M model with checkpointing

```
─────────────────────────────────────────────────────────────────
 Item                  Calculation                        Result
─────────────────────────────────────────────────────────────────
 Weights  (bf16)       550,000,000 × 2 bytes          =  1.10 GB
 Gradients (bf16)      550,000,000 × 2 bytes          =  1.10 GB
 Adam momentum (fp32)  550,000,000 × 4 bytes          =  2.20 GB
 Adam variance (fp32)  550,000,000 × 4 bytes          =  2.20 GB
                                                       ─────────
 Parameter subtotal                                    =  6.60 GB

 Activations (halved by checkpointing)                ≈  4.00 GB
                                                       ─────────
 TOTAL                                                 ≈ 10.60 GB  ✓
─────────────────────────────────────────────────────────────────
 Headroom: 1.40 GB   →   fits within RTX 5070 12 GB
```

### 5-3. Why activations halve

```
Standard:
  forward → [layer 1 out] [layer 2 out] [layer 3 out] ... [layer 24 out]  — all kept
  backward → pull each stored output, compute gradient

Checkpointing:
  forward → [checkpoint] ... [checkpoint] ... [checkpoint]  — only boundary outputs kept
  backward → recompute each segment from the nearest checkpoint, then compute gradient
```

Memory is traded for computation — a deliberate and worthwhile exchange when VRAM is the bottleneck.

---

## 6. VRAM Size vs. Training Limit — Reference Table

| Available VRAM | Max training (standard) | Max training (+ checkpointing) | Max inference (4-bit) |
|----------------|------------------------:|-------------------------------:|----------------------:|
| 4 GB | 30M params | ~50M params | 8B params |
| 8 GB | 85M params | ~150M params | 16B params |
| **12 GB (RTX 5070 ← this machine)** | **350M params** | **~550M params** | **24B params** |
| 16 GB | 450–500M params | ~750M params | 32B params |
| 24 GB (RTX 4090) | 900M–1B params | ~1.5B params | 48B params |
| 80 GB (A100) | 4B params | ~6B params | 160B params |
| 128 GB | 7B params | ~10B params | 260B params |

**Key takeaway:** Inference can run a model 6–8× larger than what the same VRAM can train from scratch.

---

## 7. Why Inference Is So Much Cheaper Than Training

Running a finished model (inference) and training one from scratch use the same hardware but store completely different things in VRAM.

| What is stored in VRAM | Training | Inference |
|------------------------|----------|-----------|
| Model weights | ✓ bf16 — 2 bytes/param | ✓ bf16 or quantized (smaller) |
| Gradients | ✓ 2 bytes/param | ✗ never computed |
| Adam optimizer states | ✓ 8 bytes/param | ✗ not needed |
| All layer activations | ✓ must keep every layer's output | ✗ one layer at a time, then discarded |
| KV cache | ✗ not used | ✓ grows with each generated token |
| **Total per parameter** | **~12–16 bytes** | **~0.5–2 bytes** |

**The core difference:**

- **Inference** processes one layer, moves to the next, and immediately discards the previous layer's intermediate values. Only the current layer's data and the growing KV cache ever live in VRAM at once.
- **Training** runs a full forward pass AND a full backward pass. The backward pass must traverse every layer in reverse, needing the intermediate outputs that were produced in the forward pass. All of them must remain in VRAM simultaneously.

This is why training requires 6–8× more VRAM than inference for the same model.

---

## 8. Why I Can Run a 7B Model but Cannot Train One

This is the most counterintuitive fact. The RTX 5070 (12 GB) can run a 7B model for conversation, yet training that same model from scratch would require approximately 99 GB of VRAM.

```
RTX 5070 — 12 GB VRAM

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
RUNNING a 7B model (4-bit GGUF quantization):
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
┌──────────────────────────┐
│  4-bit quantized weights │  3.50 GB   7B × 0.5 bytes
├──────────────────────────┤
│  KV cache                │  0.50 GB   grows with context length
├──────────────────────────┤
│  Overhead (runtime misc) │  0.10 GB
└──────────────────────────┘
TOTAL: ~4 GB  ✓  runs with room to spare

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
TRAINING the same 7B model from scratch (bf16 + AdamW):
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
┌──────────────────────────┐
│  Weights      (bf16)     │  14.00 GB   7B × 2 bytes
├──────────────────────────┤
│  Gradients    (bf16)     │  14.00 GB   7B × 2 bytes
├──────────────────────────┤
│  Adam momentum (fp32)    │  28.00 GB   7B × 4 bytes
├──────────────────────────┤
│  Adam variance (fp32)    │  28.00 GB   7B × 4 bytes
├──────────────────────────┤
│  All layer activations   │  ~15.00 GB  batch × seq × layers
└──────────────────────────┘
TOTAL: ~99 GB  ✗  requires 8× more VRAM than this machine has
```

**The rule:**
- To **run** a model: VRAM ≈ `model_file_size` at the chosen quantisation level
- To **train** a model from scratch: VRAM ≈ `parameters × 12 bytes` + activations

The RTX 5070 can run a 7B model in 4 GB. Training that same model from scratch requires 99 GB — a 25× difference for the exact same model.

---

## 9. Tuning Levers — Pushing the Limit Without Buying More VRAM

If you want to train the largest possible model on the RTX 5070 (12 GB), four techniques can be combined:

### Lever 1: Reduce batch size — cuts activation memory

```
batch_size 16 → 8:   activation memory roughly halved
batch_size 8  → 4:   activation memory roughly quartered
```

To maintain training quality, increase `grad_accum_steps` by the same factor so the effective batch size stays constant:

```yaml
# Before (standard)
batch_size: 16
grad_accum_steps: 4
# Effective batch: 16 × 4 = 64

# After (half the VRAM, same training dynamics)
batch_size: 8
grad_accum_steps: 8
# Effective batch: 8 × 8 = 64   (identical)
```

### Lever 2: Reduce sequence length — cuts activation memory

```
block_size 1024 → 512:   activation memory roughly halved
block_size 1024 → 256:   activation memory roughly quartered
```

Trade-off: the model loses the ability to attend to longer contexts.

### Lever 3: Enable gradient checkpointing — halves activation memory at 30 % speed cost

```python
# Applied in src/train/train.py
from torch.utils.checkpoint import checkpoint

# Wrap each transformer block forward call
output = checkpoint(transformer_block, x)
```

Activations drop by approximately half; training throughput drops by approximately 30 %.

### Lever 4: Enable torch.compile — faster throughput (no memory saving)

```yaml
train:
  compile: true
```

Does not reduce VRAM usage, but improves training speed by ~15 %. Effective on RTX 5070 Blackwell architecture.

### Combined effect

```
Default config:
  batch=16, seq=1024, no checkpointing  →  ceiling: 350M

Adjusted config:
  batch=8,  seq=1024, checkpointing ON  →  ceiling: ~550M
  batch=4,  seq=512,  checkpointing ON  →  ceiling: ~700M  (quality trade-off)
```

These levers shift the ceiling from 350M to 550M, but they cannot overcome the irreducible floor of `parameters × 12 bytes` for weights + gradients + optimizer states.

---

## 10. Full Summary

### Key questions and answers

| Question | Answer |
|----------|--------|
| Why does training use so much VRAM? | Weights + gradients + Adam states + all layer activations must coexist → **12 bytes per parameter** |
| Why can I run larger models than I can train? | Inference drops gradients, optimizer states, and most activations → only ~2 bytes/param needed |
| What is the hard limit for my RTX 5070 (12 GB)? | Standard: **350M parameters** / With gradient checkpointing: **~550M parameters** |
| What would it take to train a 1B model? | A 24 GB GPU (RTX 4090 or RTX 3090) |
| What would it take to train a 7B model from scratch? | 4–8× A100 80 GB cards (~320–640 GB total VRAM) |
| Can I push the limit without new hardware? | Reduce `batch_size` + reduce `block_size` + enable gradient checkpointing: 350M → 550M |

### Per-model-size VRAM calculation summary

```
──────────────────────────────────────────────────────────────────────────
 Parameters   Weights   Gradients   Adam states   Activations   Total   OK?
──────────────────────────────────────────────────────────────────────────
    30M       0.06 GB   0.06 GB     0.24 GB       ~3.60 GB     ~4 GB   ✓
    85M       0.17 GB   0.17 GB     0.68 GB       ~7.00 GB     ~8 GB   ✓
   350M       0.70 GB   0.70 GB     2.80 GB       ~7.80 GB    ~12 GB   ✓ ceiling
   500M       1.00 GB   1.00 GB     4.00 GB       ~7.80 GB    ~14 GB   ✗
     1B       2.00 GB   2.00 GB     8.00 GB      ~10.00 GB    ~22 GB   ✗
     7B      14.00 GB  14.00 GB    56.00 GB      ~15.00 GB    ~99 GB   ✗
──────────────────────────────────────────────────────────────────────────
 RTX 5070 12 GB ceiling: 350M (standard) / 550M (gradient checkpointing)
──────────────────────────────────────────────────────────────────────────
```

### One-sentence summary

> The RTX 5070's 12 GB VRAM must hold 12 bytes per parameter during training,  
> and including activations, 350M parameters is the physical ceiling —  
> gradient checkpointing stretches this to 550M,  
> but anything beyond that requires a larger GPU.

---

*This document explains the hardware constraints of the DPRK Korean AI Assistant project through first-principles logic and calculation.*  
*Related config: `config/model_config.yaml` · Related docs: `environment.md` · `README.md` sections 9 and 10*
