# Algorithms & Speed Optimization Guide
# Algorithm Analysis and Speedup Guide

> This document is for each file in the current codebase., Based on each row  
> **How much faster can I change which part?** Explain in detail.

---

## Priorities at a glance

| priority | improvement items | file:row | learning rate↑ | inference speed↑ | Difficulty level |
|---------|----------|---------|-----------|-----------|--------|
| 🔴 top priority | **KV Cache** | `generate.py:68-121` | — | **10~100ship** | middle |
| 🔴 top priority | **torch.compile** | `config.yaml:compile` | **10~20%** | 10~20% | very easy |
| 🟠 high | **RMSNorm** | `transformer.py:74,76,94` | 10~15% | 10~15% | easy |
| 🟠 high | **SwiGLU activation function** | `transformer.py:68` | 5~10% | 5~10% | easy |
| 🟠 high | **RoPE location encoding** | `transformer.py:91-92,132-133` | — | Double the context+ | middle |
| 🟡 middle | **GQA/MQA attention** | `transformer.py:34-46` | 20~40% | 20~40% | middle |
| 🟡 middle | **Tilt checkpointing** | `train.py:135` | -(calculation↑) | — | easy |
| 🟡 middle | **Read materials ahead** | `dataset.py:39-53` | 5~15% | — | easy |
| 🟢 advanced | **INT8/INT4 Quantization** | Inference only | — | **2~4ship** | high |
| 🟢 advanced | **Speculative decryption** | `generate.py` | — | **2~3ship** | high |
| 🟢 advanced | **MLA (DeepSeekceremony)** | `transformer.py:34-46` | — | KV 93% savings | very high |
| 🟢 advanced | **MoE structure** | `transformer.py:60-68` | — | knowledge↑, maintain speed | very high |

---

## 1. 🔴 KV Cache — Inference speed 10~100ship

### current problem (biggest bottleneck)

**file:** `src/inference/generate.py:68-121`

```python
# generate.py:68-69 — current code
idx_cond = idx if idx.size(1) <= block_size else idx[:, -block_size:]
logits, _ = model(idx_cond)   # ← Process the entire sequence from the beginning at each step!
```

**problem:** 100When creating the second tag, the previous 99 tags are calculated from the beginning again..

```
step 1: [A] processing → 1number operation
step 2: [A, B] processing → 2number operation
Step 3: [A, B, C] processing → 3number operation
...
step N: [A, B, ..., N] processing → Nnumber operation
total operations: 1+2+...+N = N²/2number  ← O(n²) Time complexity
```

**generate.py:121** — Context grows with every step:
```python
idx = torch.cat([idx, nxt], dim=1)  # Tensor keeps getting longer
```

### KV Cachecolumn?

attention mechanism(Attention)in K(key)Wow V(value)There is no need to recalculate previous tokens..  
calculated once K, VSave and reuse.

```
KV Cache None:
  step 1: Q₁K₁V₁ → 1count times
  step 2: Q₂K₁V₁ + Q₂K₂V₂ → Previous K₁V₁also recalculate! (waste)

KV Cache Yes:
  step 1: Q₁K₁V₁ → calculation + K₁,V₁ save
  step 2: Q₂K₂V₂ → Calculate new ticket only, K₁,V₁uses the saved one
  total operations: O(n) (linear)
```

### How to implement (transformer.py Edit)

**`src/model/transformer.py:25-57` — CausalSelfAttention Edit:**

```python
class CausalSelfAttention(nn.Module):
    def forward(
        self,
        x: torch.Tensor,
        kv_cache: tuple[torch.Tensor, torch.Tensor] | None = None,
    ) -> tuple[torch.Tensor, tuple[torch.Tensor, torch.Tensor]]:
        B, T, C = x.shape
        qkv = self.qkv(x)
        q, k, v = qkv.split(self.n_embd, dim=-1)

        q = q.view(B, T, self.n_head, self.head_dim).transpose(1, 2)
        k = k.view(B, T, self.n_head, self.head_dim).transpose(1, 2)
        v = v.view(B, T, self.n_head, self.head_dim).transpose(1, 2)

        # ← core: Previous cache and current K,V Merge
        if kv_cache is not None:
            past_k, past_v = kv_cache
            k = torch.cat([past_k, k], dim=2)  # Concatenated along the time axis
            v = torch.cat([past_v, v], dim=2)

        new_cache = (k, v)  # Save for next step

        y = F.scaled_dot_product_attention(q, k, v, is_causal=(kv_cache is None))
        y = y.transpose(1, 2).contiguous().view(B, T, C)
        y = self.resid_dropout(self.proj(y))
        return y, new_cache
```

**`src/inference/generate.py:67-82` — sample_token Edit:**

```python
@torch.no_grad()
def sample_token_cached(model, idx_new, *, kv_caches, temperature, top_k, top_p, repetition_penalty):
    """idx_new: New stamp only (B, 1) — Not the entire sequence"""
    logits, new_caches = model(idx_new, kv_caches=kv_caches)
    logits = logits[:, -1, :]
    # ... Same sampling logic
    return next_token, new_caches
```

### Effect after application

| sequence length | present(no cache) | KV Cache Apply | speed up |
|------------|--------------|--------------|---------|
| 100 tag | 5,000 operation | 100 operation | **50ship** |
| 256 tag | 32,768 operation | 256 operation | **128ship** |
| 1024 tag | 524,288 operation | 1,024 operation | **512ship** |

> Inference is slow in current code **the biggest reason**go KV Cache It is not implemented.

---

## 2. 🔴 torch.compile — 10 immediately~20% speed up

### Current settings

**file:** `config/model_config.yaml:compile`

```yaml
train:
  compile: false   # ← Just this trueJust change it to
```

**file:** `src/train/train.py:142-148` — Code implementation already completed:

```python
if cfg.train.compile:
    try:
        model = torch.compile(model)   # ← Already exists, Just turn on the setting
        print("[train] torch.compile enabled")
    except Exception as e:
        print(f"[train] torch.compile failed ({e}); continuing without it.")
```

### How it works

```
torch.compile = Python code → XLA/Triton Compile into kernel

general PyTorch: Python function call → CUDA Kernel execution hundreds of times (Overhead is large)
torch.compile: Complete forward computation in one optimized CUDA merge into kernel
```

### How to apply

```yaml
# config/model_config.yaml in
train:
  compile: true   # Just change this
```

**Precautions:**
- 1 for compilation on first run~3Takes minutes (From then on it's fast)
- WindowsSome restrictions in: `torch.compile`If this fails, automatically switches to normal mode
- RTX 5070(Blackwell)Support confirmed in

**expected effect:** learning rate 10~20%, Inference speed 10~20% improve

---

## 3. 🟠 RMSNorm — LayerNorm replace (10~15% speed up)

### current code

**file:** `src/model/transformer.py:74, 76, 94`

```python
# transformer.py:71-77
class Block(nn.Module):
    def __init__(self, cfg: ModelConfig):
        self.ln_1 = nn.LayerNorm(cfg.n_embd, bias=cfg.bias)  # ← Line 74
        self.attn = CausalSelfAttention(cfg)
        self.ln_2 = nn.LayerNorm(cfg.n_embd, bias=cfg.bias)  # ← Line 76
        self.ffn = FeedForward(cfg)

# transformer.py:94
self.ln_f = nn.LayerNorm(cfg.n_embd, bias=cfg.bias)          # ← Line 94
```

### problem

LayerNormcalculates both mean and variance:
```
LayerNorm(x) = (x - mean(x)) / sqrt(var(x) + ε) × γ + β
               ↑↑↑ average calculation ↑↑↑ distributed calculation
```

LLaMA, Mistral, Gemmato use RMSNormomits the average calculation:
```
RMSNorm(x) = x / sqrt(mean(x²) + ε) × γ
             ↑↑↑ Calculate variance only (average omitted)
```

**result:** Computation amount: approximately 15% decrease, Learning stability equivalent or higher

### How to replace

**`src/model/transformer.py` add to top:**

```python
class RMSNorm(nn.Module):
    def __init__(self, dim: int, eps: float = 1e-6):
        super().__init__()
        self.weight = nn.Parameter(torch.ones(dim))
        self.eps = eps

    def forward(self, x: torch.Tensor) -> torch.Tensor:
        rms = x.pow(2).mean(-1, keepdim=True).add(self.eps).sqrt()
        return x / rms * self.weight
```

**`Block.__init__`replace from (transformer.py:74, 76):**

```python
# Before change
self.ln_1 = nn.LayerNorm(cfg.n_embd, bias=cfg.bias)
self.ln_2 = nn.LayerNorm(cfg.n_embd, bias=cfg.bias)

# After change
self.ln_1 = RMSNorm(cfg.n_embd)
self.ln_2 = RMSNorm(cfg.n_embd)
```

**`GPT.__init__`replace from (transformer.py:94):**

```python
# Before change
self.ln_f = nn.LayerNorm(cfg.n_embd, bias=cfg.bias)

# After change
self.ln_f = RMSNorm(cfg.n_embd)
```

**`ModelConfig`in `bias` No parameters needed:**

```yaml
# config/model_config.yaml
model:
  bias: false  # RMSNormno deflection, already falseSo no need to change
```

---

## 4. 🟠 SwiGLU activation function — GELU replace (improve quality + maintain speed)

### current code

**file:** `src/model/transformer.py:60-68`

```python
class FeedForward(nn.Module):
    def __init__(self, cfg: ModelConfig):
        self.fc1 = nn.Linear(cfg.n_embd, cfg.ffn_dim, bias=cfg.bias)
        self.fc2 = nn.Linear(cfg.ffn_dim, cfg.n_embd, bias=cfg.bias)
        self.dropout = nn.Dropout(cfg.dropout)

    def forward(self, x: torch.Tensor) -> torch.Tensor:
        return self.dropout(self.fc2(F.gelu(self.fc1(x))))  # ← Line 68
        #                           ↑ GELU activation function
```

### problem

GELU: Simple nonlinear function, LLMIn SwiGLUis better

```
GELU(x) = x × Φ(x)          (Φ: normal distribution CDF)
SwiGLU(x, W, V) = Swish(xW) × (xV)   (product of two paths)
```

**LLaMA, PaLM, GPT-4(estimate)**: all SwiGLU or GeGLU use

### How to replace

**`src/model/transformer.py:60-68` replace:**

```python
class FeedForward(nn.Module):
    def __init__(self, cfg: ModelConfig):
        super().__init__()
        # SwiGLUrequires two upward projections (fc1 → gate + up)
        # ffn_dimsilver 2/3Maintain total number of parameters by reducing to
        hidden = int(cfg.ffn_dim * 2 / 3)
        hidden = (hidden + 63) // 64 * 64  # 64Sort by multiple of (CUDA efficiency)

        self.gate = nn.Linear(cfg.n_embd, hidden, bias=False)  # gate path
        self.up   = nn.Linear(cfg.n_embd, hidden, bias=False)  # upward path
        self.down = nn.Linear(hidden, cfg.n_embd, bias=False)  # downward path
        self.dropout = nn.Dropout(cfg.dropout)

    def forward(self, x: torch.Tensor) -> torch.Tensor:
        # SwiGLU: Swish(gate) × up
        return self.dropout(self.down(F.silu(self.gate(x)) * self.up(x)))
```

**config/model_config.yamlin ffn_dim adjustment:**

```yaml
model:
  ffn_dim: 2730   # 512 × (8/3) = 1365 → ×2 = 2730 (SwiGLUdragon)
  # existing GELU: ffn_dim=2048 (n_embd × 4)
  # SwiGLU:   ffn_dim=2730 (n_embd × 8/3 × 2) → Keep the same number of parameters
```

**expected effect:** Improved language quality, Speed is equal or slightly improved

---

## 5. 🟠 RoPE — Insert and replace learned positions (Context length can be expanded)

### current code

**file:** `src/model/transformer.py:91-92, 132-133`

```python
# transformer.py:91-92 — GPT.__init__
self.tok_emb = nn.Embedding(cfg.vocab_size, cfg.n_embd)
self.pos_emb = nn.Embedding(cfg.block_size, cfg.n_embd)  # ← Learned absolute position

# transformer.py:132-133 — GPT.forward
pos = torch.arange(T, device=idx.device, dtype=torch.long)
x = self.drop(self.tok_emb(idx) + self.pos_emb(pos))      # ← Add location
```

### problem

Limitations of learned absolute position insertion:
```
When learning block_size=1024learn by
→ 1025th word mark in inference? No position insertion → An error occurred
→ Not generalizable to longer contexts
```

### RoPEcolumn?

Rotary Position Embedding — location information Q, K Injecting a vector directly into a rotation matrix:

```
general attention: score(Q, K) = QᵀK
RoPE:      score(Q, K) = (RΘ,mQ)ᵀ(RΘ,nK)
           → two positions m, nRelative distances are automatically encoded
```

**Advantages:**
- 1024 context when learning → Inference Poem 2048, 4096expandable to (without additional learning)
- LLaMA 1/2/3, Mistral, Gemma use all

### How to implement

**`src/model/transformer.py`to RoPE add:**

```python
def precompute_freqs_cis(dim: int, max_seq_len: int, theta: float = 10000.0) -> torch.Tensor:
    """RoPE Frequency matrix precomputation."""
    freqs = 1.0 / (theta ** (torch.arange(0, dim, 2).float() / dim))
    t = torch.arange(max_seq_len)
    freqs = torch.outer(t, freqs)
    return torch.polar(torch.ones_like(freqs), freqs)  # complex number form

def apply_rotary_emb(q: torch.Tensor, k: torch.Tensor, freqs_cis: torch.Tensor):
    """Q, Kto RoPE Apply."""
    q_ = torch.view_as_complex(q.float().reshape(*q.shape[:-1], -1, 2))
    k_ = torch.view_as_complex(k.float().reshape(*k.shape[:-1], -1, 2))
    q_ = torch.view_as_real(q_ * freqs_cis).flatten(-2)
    k_ = torch.view_as_real(k_ * freqs_cis).flatten(-2)
    return q_.type_as(q), k_.type_as(k)
```

**`GPT.__init__`in pos_emb remove, freqs_cis registration (transformer.py:91-92):**

```python
# Before change
self.pos_emb = nn.Embedding(cfg.block_size, cfg.n_embd)

# After change
freqs_cis = precompute_freqs_cis(cfg.n_embd // cfg.n_head, cfg.block_size * 2)
self.register_buffer("freqs_cis", freqs_cis)  # Not a learnable parameter
```

---

## 6. 🟡 GQA/MQA — Pay attention to multiple queries (inference KV memory savings)

### current code

**file:** `src/model/transformer.py:34`

```python
self.qkv = nn.Linear(cfg.n_embd, 3 * cfg.n_embd, bias=cfg.bias)
# Q: n_headdog, K: n_headdog, V: n_headdog ← all the same number
```

### Comparison of three methods

```
MHA (present): Q=8head, K=8head, V=8head → KV Cache size = n_head × d × T
GQA:        Q=8head, K=2head, V=2head → KV Cache size = 2 × d × T (42x savings)
MQA:        Q=8head, K=1head, V=1head → KV Cache size = 1 × d × T (82x savings)
```

LLaMA-2 70B, Mistral 7B: GQA use (quality↑ + KV memory↓)

### How to implement

**`src/model/config.py:ModelConfig`add to:**

```python
@dataclass
class ModelConfig:
    n_layer: int = 8
    n_head: int = 8
    n_kv_head: int = 8   # ← Add new: GQAfor KV number of heads (n_headdivisor of)
    # n_kv_head = n_head → MHA (basic)
    # n_kv_head = n_head // 4 → GQA
    # n_kv_head = 1 → MQA
```

**`src/model/transformer.py:CausalSelfAttention.__init__` Edit:**

```python
def __init__(self, cfg: ModelConfig):
    n_kv = getattr(cfg, 'n_kv_head', cfg.n_head)
    self.n_head = cfg.n_head
    self.n_kv_head = n_kv
    self.n_rep = cfg.n_head // n_kv  # each KV How many times do you repeat the hair

    self.q_proj = nn.Linear(cfg.n_embd, cfg.n_embd, bias=False)
    self.k_proj = nn.Linear(cfg.n_embd, n_kv * self.head_dim, bias=False)
    self.v_proj = nn.Linear(cfg.n_embd, n_kv * self.head_dim, bias=False)
```

**config.yamlSettings in:**

```yaml
model:
  n_head: 8
  n_kv_head: 2   # GQA: KV Reduce your hair by 4 times
```

---

## 7. 🟡 Tilt checkpointing — Large model memory savings

### current problem

**file:** `src/train/train.py:135`

```python
for block in self.blocks:
    x = block(x)   # ← Intermediate output of each block(activation)Stored in memory for backpropagation
```

During forward computation, all intermediate results are kept in memory for backpropagation..  
The larger the model(The more floors there are) Memory usage increases linearly.

### What is checkpointing??

```
basic: Save all intermediate results → memory 多, 1 calculation
checkpointing: Do not save intermediate results → memory 少, calculation 1.33Sashimi (Recalculation during backpropagation)
```

**compromise:** Memory savings 60~70%, Slowdown 20~30%

### How to implement

**`src/train/train.py`to import add (top):**

```python
from torch.utils.checkpoint import checkpoint
```

**`src/model/transformer.py:79-82` — Block.forward Edit:**

```python
# GPT.forward my loop (train.py:135Called from)
# Before change
for block in self.blocks:
    x = block(x)

# After change (gradient checkpointing)
for block in self.blocks:
    if self.training and use_checkpoint:
        x = checkpoint(block, x, use_reentrant=False)
    else:
        x = block(x)
```

**`config/model_config.yaml`add to:**

```yaml
train:
  gradient_checkpointing: true   # 85M Recommended for ideal models
```

---

## 8. 🟡 Read materials ahead — learning GPU Reduce waiting time

### current code

**file:** `src/data/dataset.py:39-53`

```python
def sample(self, batch_size, device):
    ix = np.random.randint(0, len(self.data) - self.block_size - 1, size=batch_size)
    x = np.stack([self.data[i:i+self.block_size].astype(np.int64) for i in ix])
    # ↑ CPUin numpy After processing GPUtransfer to — GPUWait for this while
    x_t = x_t.pin_memory().to(device, non_blocking=True)  # ← Already using asynchronous transfer
```

present `pin_memory()`Wow `non_blocking=True`has already been applied, so it is relatively optimized..  
Additional improvements: Preparing the next batch in a separate thread **prefetcher**

### How to implement

**`src/data/dataset.py`Add prefetcher to:**

```python
import threading
from queue import Queue

class DataPrefetcher:
    """Prepare the next batch in advance in a separate thread GPU Eliminate waiting time."""
    def __init__(self, dataset: TokenDataset, batch_size: int, device, queue_size: int = 2):
        self.dataset = dataset
        self.batch_size = batch_size
        self.device = device
        self.queue = Queue(maxsize=queue_size)
        self.thread = threading.Thread(target=self._producer, daemon=True)
        self.thread.start()

    def _producer(self):
        while True:
            batch = self.dataset.sample(self.batch_size, self.device)
            self.queue.put(batch)

    def next(self):
        return self.queue.get()
```

---

## 9. 🟢 INT8/INT4 Quantization — Inference only, memory 50~75% savings

### Principle

```
bf16 (present): each parameter = 16bit = 2byte
INT8:        each parameter = 8bit = 1byte → half memory, 2x speed
INT4:        each parameter = 4bit = 0.5byte → memory 75% savings, speed 3~4ship
```

**loss of precision:** INT4Slight decrease in quality in (30M Almost none in the model)

### How to apply (Inference only, bitsandbytes library)

```powershell
pip install bitsandbytes
```

```python
# Apply quantization after loading the model during inference
import bitsandbytes as bnb

# INT8 Quantization
model = model.to(torch.int8)  # simple way

# or bitsandbytesof NF4 (4bit, high quality)
from bitsandbytes.nn import Linear4bit
# each Linear layer Linear4bitreplace with
```

**an easier way — GGUF convert to format (llama.cpp use):**

```powershell
pip install llama-cpp-python
# checkpoint GGUFAfter converting to CPUCan also run in
```

---

## 10. 🟢 Speculative decryption (Speculative Decoding) — inference 2~3ship

### Principle

```
general inference:
  large model → tag1 → tag2 → tag3 → ...  (sequential)

Speculative decryption:
  miniature draft model → [tag1, tag2, tag3, tag4, tag5] Create quickly
  Large validation model → 5Verify your dog in one go (1forward calculation)
  → What was hit is the same, Recreate from the wrong place
```

**Prerequisites:** current model(30M)Draft mockup much smaller than(5M etc.)need this  
→ Train the draft model separately, or, Use a smaller version of the same structure

---

## 11. 🟢 MLA (DeepSeekceremony) — KV Cache 93% savings

**file:** `src/model/transformer.py:34-46`

DeepSeek-V2method invented by. K,Vcompresses into a low-dimensional latent vector.:

```python
# present MHA
self.qkv = nn.Linear(cfg.n_embd, 3 * cfg.n_embd, bias=False)
# KV Cache: n_head × d_head × seq_len = 8 × 64 × 1024 = 524,288 value

# MLA way
self.kv_compress = nn.Linear(cfg.n_embd, cfg.n_embd // 8, bias=False)  # compression
self.kv_restore_k = nn.Linear(cfg.n_embd // 8, cfg.n_embd, bias=False)  # restore
self.kv_restore_v = nn.Linear(cfg.n_embd // 8, cfg.n_embd, bias=False)
# KV Cache: n_embd//8 × seq_len = 64 × 1024 = 65,536 value (82x savings!)
```

---

## Comprehensive recommended application order

### Apply immediately (Change settings only, No code modification required)

```yaml
# config/model_config.yaml
train:
  compile: true        # 10~20% speed up
  dtype: bfloat16      # Already set
```

**effect:** Learning speed instantly 10~20% improve, Inference speed 10~20% improve

---

### short term application (Code modification required, 1~3work)

1. **RMSNorm** (`transformer.py:74,76,94`) → LayerNorm replace
2. **SwiGLU** (`transformer.py:68`) → GELU replace
3. **KV Cache** (`generate.py:68-121`) → Biggest inference speedup

**effect:** Inference speed 10~100belly enhancement (KV Cache), learning rate 15~25% improve

---

### Mid-term application (1~2state)

4. **RoPE** (`transformer.py:91-92`) → Longer context processing possible
5. **GQA** (`transformer.py:34`) → KV memory savings (Important in large models)
6. **Tilt checkpointing** (`train.py`) → 85M Ideal model required

---

### long term application (1months+)

7. **INT4 Quantization** → CPUModels can also be run in
8. **Speculative decryption** → inference 2~3ship
9. **MLA** → DeepSeekceremony KV Cache 93% savings
10. **MoE** → More knowledge with the same calculations

---

## Already optimized parts of the current code (No changes required)

| item | location | Description |
|------|------|------|
| FlashAttention automatic use | `transformer.py:49` | `F.scaled_dot_product_attention`This RTX 5070automatically from FlashAttention use |
| Fused AdamW | `transformer.py:169-171` | `fused=True`optimized with AdamW Kernel usage |
| Only calculate last position during inference | `transformer.py:149` | `x[:, [-1], :]`as unnecessary logit remove calculation |
| Pin Memory + Non-blocking | `dataset.py:48-49` | CPU→GPU Minimize latency with asynchronous transfers |
| bf16 mixed precision | `train.py:97-100` | RTX 5070automatic selection of optimal precision |
| Residual projection weight initialization | `transformer.py:104-105` | GPT-2Stable learning with expression depth correction initialization |
