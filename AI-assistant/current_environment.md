# Comparison of the world's latest language model learning environments
## Actual usage status of each module of this project and learning environment of world research institutes

> Creation date: 2026-06-17  
> standard: 2024~2025year open paper, technical report, official announcement material

---

## 1. Comparison of key training tools

| module | this project | Status of use by major institutions around the world |
|------|-----------|----------------------|
| **PyTorch** | Full model training | Meta(developer), Google DeepMind, OpenAI, Mistral, xAI(Grok), Anthropic — Most modern large models PyTorchTrained by |
| **SentencePiece** | BPE ticket analyzer | Google(LaMDA, T5, Gemma), Meta(LLaMA 1·2·3 whole series), Mistral 7B/8×7B — Close to industry standards |
| **numpy + memmap** | training material file | nanoGPT(Karpathy), Mistral, Falcon — memmap Base data processing is standard practice in large corpora. |
| **AdamW + cosine LR** | optimizer / Learning rate planning | GPT-4, LLaMA 1·2·3, Gemma 2, Mistral everything — Currently a standard combination of large language models |
| **TensorBoard / WandB** | training log | Google, Hugging Face, Mistral, Anthropic — PyTorchThe most commonly used learning monitoring tool with |
| **FlashAttention** | attention mechanism calculation | Meta(LLaMA 3), Mistral, Anthropic(Claude 3), Together AI — Up to 8x faster than standard implementation |
| **bfloat16** | mixed precision | Google TPU standard, NVIDIA A100/H100/H200/B100, AMD MI300X — Excellent training stability and speed |

---

## 2. Comparing structural design choices

| design | this project | World standard or not? |
|------|-----------|--------------|
| **decoder only GPT structure** | ✓ | GPT-2/3/4, LLaMA 1/2/3, Gemma 1/2, Mistral, Falcon, Qwen, DeepSeek All the same structure |
| **Prenormalization (Pre-Norm)** | ✓ | GPT-2(2019) Since then, the de facto standard. LLaMA, Mistral adopt all |
| **GELU activation function** | ✓ | GPT-3, LLaMA 1 adopted. LLaMA 2/3is SwiGLUswitch to (better performance) |
| **weight sharing (tie_weights)** | ✓ | GPT-2 adopted. LLaMA 2/3is not adopted (Separation is advantageous due to the large vocabulary size.) |
| **FlashAttention auto apply** | ✓ (`scaled_dot_product_attention`) | Meta, Mistral, Anthropic, Together AI all FlashAttention 2/3 use |
| **bfloat16 mixed precision** | ✓ | Google TPU standard, A100/H100/RTX 30xx+ standard |
| **Gradient accumulation** | ✓ | A method used by all teams around the world when manifest memory is lacking. |
| **Cosine learning rate decay** | ✓ | GPT-3, LLaMA, Gemma — standard. Recently WSD(Warmup-Stable-Decay)degree increasing trend |

---

## 3. Detailed learning environment by major research institutes around the world

### 3-1. Meta — LLaMA 3 (2024)

**Source:** *"The Llama 3 Herd of Models"* (Meta AI, 2024)

```
model scale:   8B, 70B, 405B parameter
training materials:  15crude stamp (English-centric multilingual)
hardware:   NVIDIA H100 80GB GPU × 16,384chapter
            GPUliver connection: InfiniBand + RoCE net
            power consumption: About 39 megawatts
training time:  405B model = H100 × 16,38454 days long
framework: PyTorch 2.x + FSDP (Fully Sharded Data Parallel)
ticket analyzer: SentencePiece BPE → Tiktokenswitch to (vocab 128,256)
attention mechanism:  GQA (Grouped Query Attention) — Increased inference speed
activation function: SwiGLU (GELU instead)
location encoding: RoPE (Rotary Position Embedding) — Instead of learning location embeddings
```

**Differences from this project:**
- simple self-care → GQA (KV save cache)
- Learned location embeddings → RoPE (Longer context processing possible)
- GELU → SwiGLU (Improved performance)

---

### 3-2. Google DeepMind — Gemma 2 (2024)

**Source:** *"Gemma 2: Improving Open Language Models at a Practical Size"* (Google DeepMind, 2024)

```
model scale:   2B, 9B, 27B parameter
training materials:  2B model: 2crude stamp / 27B model: 13crude stamp
hardware:   Google TPU v5p (self-developed chip)
framework: JAX + XLA (PyTorchnot Google own framework)
ticket analyzer: SentencePiece (256,000 vocabulary)
attention mechanism:  Local Attention (sliding window) + Global Attention shift
activation function: GeGLU (GELUGate variant of)
knowledge distillation:  large model(27B)this little model(2B, 9B) Teacher role in training
```

**Special note:**
- Googlesilver TPUDesign and use directly → PyTorch instead JAX
- TPU v5p: 460 per chip TFLOPS (H100About 1 of.7ship)
- knowledge distillation(Knowledge Distillation): The small model learns the probability distribution of the large model.

---

### 3-3. Mistral AI — Mixtral 8×7B (2024)

**Source:** *"Mixtral of Experts"* (Mistral AI, 2024)

```
model structure:   MoE (Mixture of Experts) — 8Only 2 of the 2 expert networks are active
actual parameters: 46.7B (activation parameters: 12.9B)
training materials:  multilingual (english, French, German, italian, spanish)
hardware:   NVIDIA H100 cluster
framework: PyTorch + Megablocks (MoE specialization)
ticket analyzer: SentencePiece (32,000 vocabulary)
attention mechanism:  GQA + Sliding Window Attention (Context 4,096 → Effective 128K)
```

**MoE Advantages of structure:**
```
General dense model:  all 46.7B Parameters participate in tag processing
MoE model:       46.7B middle 12.9B(28%)only activated → Much less computation
result:           GPT-3.5 level performance GPT-3.5Achieved 6x faster than
```

---

### 3-4. Anthropic — Claude 3 series (2024)

**Source:** *"Claude 3 Model Card"* (Anthropic, 2024) — Details private

```
model type:   Haiku (small), Sonnet (medium size), Opus (large)
training materials:  self-curation + constitution AI (Constitutional AI) Apply
hardware:   NVIDIA A100/H100 large cluster (Quantity private)
framework: JAX + PyTorch mixed (private)
safety training:  RLHF (Human feedback reinforcement learning) + CAI (constitution AI)
context length:  Opus 200K tag (195 times this project)
```

**constitution AI (Constitutional AI):**
- Not just human feedback "constitution" (list of principles)The model evaluates the response itself
- How to increase helpful responses while reducing harmful responses

---

### 3-5. DeepSeek — DeepSeek-V3 (2024~2025)

**Source:** *"DeepSeek-V3 Technical Report"* (DeepSeek AI, 2024)

```
model structure:   MoE, 671B full parameters (activation: 37B)
training materials:  14.8crude stamp
hardware:   NVIDIA H800 GPU × 2,048chapter
            (H100 due to export restrictions H800 use)
training cost:  Approximately 5.57 million dollars (H100 Extremely low contrast)
innovation:       FP8 mixed precision (bfloat16lower precision)
            Multi-head Latent Attention (MLA) — KV Cache up to 93.3% savings
            DualPipe Pipeline Parallelization
```

**Special note:**
- GPT-4 level performance 1/10 Achieved at cost, making it a global topic
- FP8 training: of this project bfloat16Save memory with lower precision

---

### 3-6. xAI — Grok (2024)

**Source:** xAI official announcement

```
hardware:   NVIDIA H100 × 100,000chapter (Colossus cluster)
            The world's largest single GPU one of the clusters
framework: PyTorch + Own distributed training framework
training power:  About 150 megawatts
```

---

## 4. this project vs World's largest scale comparison

```
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
Item This Project        LLaMA 3 405B      DeepSeek-V3
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
Parameter 3,400only (34M)      4,050billion (405B)    6,710billion (671B)
Training materials Tens of millions of stamps 15 trillion stamps 14.8crude stamp
GPU Number 1 (RTX 5070)     H100 × 16,384chapter   H800 × 2,048chapter
Manifestation memory 12 GB              1,310,720 GB       163,840 GB
hours of training ~ Several days 54 days Approximately 2 months
Training costs alone include tens of millions of dollars and $5.57 million in electricity costs.
context length 1,024 Quote 128,000 Quote 128,000 tag
ticket analyzer       SentencePiece BPE  Tiktoken BPE       SentencePiece BPE
Attention Mechanism Standards MHA           GQA                MLA
activation function       GELU               SwiGLU             SwiGLU
Position Encoding Learned Embedding      RoPE               RoPE
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
```

---

## 5. World standard technology not included in this project (Can be added in the future)

### 5-1. RoPE (Rotary Position Embedding)
Encoding positions with a mathematical rotation matrix instead of learned position embeddings.
LLaMA 2/3, Mistral, Gemma 2, DeepSeek adopt all.
**Advantages:** Can handle contexts longer than training (Extrapolability).

### 5-2. GQA (Grouped Query Attention)
8Dog heads are all separate K, VInstead of having, several heads K, Vshare.
LLaMA 2 70B, LLaMA 3 whole series, Mistral adopted.
**Advantages:** When inferring KV Reduced cache allows for longer conversations.

### 5-3. SwiGLU activation function
GELU Swish instead(Swish)and gate(GLU)A function that combines.
LLaMA 2/3, PaLM, Gemma adopted.
**Advantages:** At the same number of parameters GELUbetter performance.

### 5-4. MoE (Mixture of Experts)
Multiple transmission networks "expert"Divide by and activate only part of each ticket.
Mixtral, DeepSeek-V2/V3, GPT-4(estimate) adopted.
**Advantages:** Even if you increase the parameters, the amount of calculation does not increase proportionally..

### 5-5. Distributed Training Framework
single GPUneeded at a scale that cannot be trained with:

| framework | developer | way |
|-----------|-------|------|
| **FSDP** | Meta/PyTorch | parameters GPUdispersed in the fields |
| **DeepSpeed** | Microsoft | ZeRO Optimization (tilt/Optimizer distribution) |
| **Megatron-LM** | NVIDIA | tensor/Pipeline Parallelization |
| **JAX + XLA** | Google | TPU Specialized Compilation |

### 5-6. FP8 training (DeepSeek-V3)
bfloat16(16bit)Train with lower 8-bit precision.
NVIDIA H100/H800of FP8 Take advantage of tensor cores.
**Advantages:** Manifest Memory Savings + Speed up calculations.

---

## 6. Development trend of tag analyzer

| generation | way | Lyon model | vocabulary size |
|------|------|---------|---------|
| 1generation | SentencePiece BPE | LLaMA 1, Mistral 7B | 32,000 |
| 2generation | Tiktoken BPE | GPT-4, LLaMA 3 | 128,256 |
| present | SentencePiece BPE | DeepSeek-V3, Gemma 2 | 100,000~256,000 |

**this project:** SentencePiece BPE, vocabulary size 16,384 (Small-scale specialized in the Joseon Dynasty)

The larger the vocabulary, the more letters each word contains. → The same sentence can be expressed with fewer word marks.

---

## 7. conclusion

```
The structure and tools are the same as world standards.. Only the scale is different.
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

What this project writes about:                Is it consistent with global standards?:
  PyTorch                             ✓ world standard
  SentencePiece BPE                   ✓ world standard (some are Tiktokenswitch to)
  AdamW + cosine attenuation                 ✓ world standard
  GPT Decoder-only structure                ✓ world standard
  Prenormalization (Pre-Norm)               ✓ world standard
  FlashAttention (automatic)               ✓ world standard
  bfloat16 mixed precision                ✓ world standard
  Gradient accumulation                         ✓ world standard
  GELU activation function                    △ (latest: SwiGLU preference)
  Learned location embeddings                  △ (latest: RoPE preference)
  standard MHA                            △ (latest: GQA preference)
  single GPU training                       × (large model: thousands GPU distributed training)
```

If I understand the code of this project LLaMA, GPT, MistralThe training code of can also be read in the same way..
The key difference is not structure **Scale and state-of-the-art optimization techniques** (RoPE, GQA, SwiGLU, MoE) is.

---

*reference material:*
- *"The Llama 3 Herd of Models"* — Meta AI, 2024
- *"Gemma 2: Improving Open Language Models at a Practical Size"* — Google DeepMind, 2024
- *"Mixtral of Experts"* — Mistral AI, 2024
- *"DeepSeek-V3 Technical Report"* — DeepSeek AI, 2024
- *"Claude 3 Model Card"* — Anthropic, 2024
- *"FlashAttention-2: Faster Attention with Better Parallelism"* — Dao et al., 2023
- *"RoFormer: Enhanced Transformer with Rotary Position Embedding"* — Su et al., 2022
