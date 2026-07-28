# Joseon Dynasty artificial intelligence assistant — front page guide

> This document is the Korean language of the Democratic People's Republic of Korea.(North Korean)This is a technical guide written by.  
> It contains the entire process of learning a language model specialized in the field of food and exercise from scratch..

---

## 1. Program Overview

This system is a program that builds a language model specifically for Korean from the ground up..  
External pre-trained model(GPT-4, LLAMA etc.)without using, We only learn Korean language materials provided by users..

**Main features:**
- food(Recipes) and exercise(physical education plan) field question and answer conversation
- Gradio(Gradio) Provides 3 conversation methods through web screen
  - free conversation
  - Ryori Recommendation
  - exercise plan
- RTX 5070 High-speed learning on modern graphics processing devices such as

**technical configuration:**
| item | content |
|------|------|
| language | Python 3.10+ |
| Deep learning framework | PyTorch 2.7+ (CUDA 12.8) |
| ticket analyzer | SentencePiece BPE |
| web screen | Gradio 6.14+ |
| data structure | numpy.memmap binary file |

---

## 2. system structure (model design)

### 2-1. converter(Transformer) structure

This model is **GPT one-way converter**is. Processes input characters only in left to right order., Learn by predicting the next letter.

```
input string
    ↓
Insert a quote(Token Embedding) + Insert location(Positional Embedding)
    ↓
converter block × 8floor
    each floor = {attention mechanism(Multi-Head Attention) → normalization → forward neural network(FFN) → normalization}
    ↓
output projection(Linear) → Vocabulary Probability Distribution
    ↓
Select the next ticket(sampling)
```

### 2-2. default settings (30M parametric model)

| settings | value | Description |
|------|----|------|
| `n_layer` | 8 | Number of converter blocks |
| `n_head` | 8 | number of heads of attention |
| `n_embd` | 512 | Hidden vector dimension |
| `ffn_dim` | 2048 | Forward Neural Network Internal Dimensions (= 4 × n_embd) |
| `block_size` | 1024 | maximum context length(number of tickets) |
| `vocab_size` | 16384 | vocabulary size |
| `dropout` | 0.1 | dropout rate |
| `tie_weights` | true | Input/output projective weight sharing |

> **100Recommended settings when learning Giga material (85M parameter):**  
> `n_layer: 12`, `n_head: 12`, `n_embd: 768`, `ffn_dim: 3072`

### 2-3. attention mechanism (Attention)

Each attention head stores the input vector as three matrices.(Q, K, V)Convert to and calculate attention score::

```
attention score = softmax(Q × Kᵀ / √d_head) × V
```

causal mask(Causal Mask)Use to refer only to information from locations preceding the current location..

### 2-4. forward neural network (FFN)

Second part of each converter layer:
```
FFN(x) = GELU(x × W₁ + b₁) × W₂ + b₂
```
W₁: 512 → 2048, W₂: 2048 → 512

---

## 3. Learn about word tag analyzer

### 3-1. SentencePiece BPE

Korean is an agglutinative language(Abundant in endings and particles)Because, Operates directly on the original text without morphological analysis **BPE(Two-letter pair encoding)** method is used.

**BPE Principle:**
1. Start every letter as an individual
2. Combine the most frequently occurring pairs of adjacent letters to create a new unit
3. Repeat until target vocabulary size is reached

yes: `Recipes` → `['▁Ryo', 'Lee', 'law']` (three stamps)

### 3-2. special tag

| tag ID | meaning | Use |
|---------|------|------|
| 0 | PAD | fill bundle |
| 1 | BOS | Sentence Start |
| 2 | EOS | end of sentence |
| 3 | UNK | unknown ticket |

**caution:** UNK tag(⁇ symbol)is automatically filtered out when printing..

### 3-3. Automatically adjust vocabulary size

small amount of material(Less than a few thousand characters)Large vocabulary sizes are not possible in.  
The program automatically calculates:

```
maximum_possible_vocabulary_size = max(100, unique_letters_number × 6)
actual_vocabulary_size = min(settings_vocabulary_size, maximum_possible_vocabulary_size)
```

There is enough data(100Giga)If so, the setting value is 16,384is applied as is.

### 3-4. learning commands

```powershell
# scripts/01_train_tokenizer.ps1
python -m src.tokenizer.train_tokenizer --config config/model_config.yaml
```

result: `checkpoints/tokenizer/dprk_sp.model`

---

## 4. Material preparation

### 4-1. Supported data format

| format | example | Description |
|------|------|------|
| plain text `.txt` | `Soybean paste soup is a traditional dish from Joseon..` | free sentences |
| JSON row `.jsonl` | `{"text": "..."}` | text field |
| questions and answers `.jsonl` | `{"question": "...", "answer": "..."}` | question and answer pair |

**Automatic conversion of question and answer format:**
```
{"question": "How to boil soybean paste soup", "answer": "prepare the ingredients..."}
  ↓
"question: How to boil soybean paste soup\nanswer: prepare the ingredients..."
```

### 4-2. material placement

```
AI-assistant/
└── data/
    └── raw/            ← Put your data file here
        ├── food.txt
        ├── recipes.jsonl
        └── workout.txt
```

### 4-3. preprocessing process

```powershell
# scripts/02_preprocess_data.ps1
python -m src.data.preprocess --config config/model_config.yaml
```

**processing process:**
1. Read original file → Text normalization
2. SentencePiecemark as
3. For learning(95%) / For verification(5%) separation
4. `uint16` Binary file storage (`train.bin`, `val.bin`)
5. Save statistical information (`meta.json`)

**output:**
```
data/processed/
├── train.bin      # learning materials (numpy.memmap)
├── val.bin        # Verification data
└── meta.json      # vocabulary size, number of tickets, etc.
```

---

## 5. model learning

### 5-1. learning algorithm

**AdamW optimizer** + **Cosine learning rate decay** combination:

```
Early learning (warmth stage): learning rate 0 → 3.0e-4  (100 step)
middle stage of learning (attenuation step): Learning rate cosine reduction  (100 → 5000 step)
end of learning (minimum value):   learning rate 3.0e-5        (5000 After the step)
```

### 5-2. Gradient accumulation (Gradient Accumulation)

This is a technique to overcome graphics processing device memory limitations.:

```
Effective bundle size = batch_size × grad_accum_steps × block_size
              = 16 × 4 × 1024 = 65,536 tag/step
```

4After one small batch forward calculation, one backpropagation and parameter update are performed..

### 5-3. mixed precision (bf16)

RTX 5070(Blackwell structure)silver `bfloat16` Supports high-speed computation:
- memory usage: float32half of
- speed: float32 Contrast approx. 2~3ship
- accuracy: float16Better numerical stability

### 5-4. learning commands

```powershell
# scripts/03_train_model.ps1
python -m src.train.train --config config/model_config.yaml
```

**Example output during training:**
```
[step    250] loss=4.2134  lr=3.00e-04  |grad|=1.23  tok/s=130,000
[step    500] val_loss=3.8921
[sample @ 500]
question: Recommended food for dinner tonight
answer: It is recommended to eat soybean paste soup and white rice....
```

### 5-5. Complete guide to system setup

file: `config/model_config.yaml`

#### Model settings (`model:`)

| parameter | default | Description |
|----------|--------|------|
| `n_layer` | 8 | Number of converter blocks (More is better↑, speed↓) |
| `n_head` | 8 | number of heads of attention (`n_embd`Must be a divisor of) |
| `n_embd` | 512 | hidden dimension (512→768→1024expand to) |
| `ffn_dim` | 2048 | Forward Neural Network Dimensions (≈ 4 × n_embd) |
| `block_size` | 1024 | maximum context length |
| `vocab_size` | 16384 | vocabulary size (Automatic setting after preprocessing) |
| `dropout` | 0.1 | Overfitting Prevention Dropout Rate |

#### Learning Settings (`train:`)

| parameter | default | Description |
|----------|--------|------|
| `batch_size` | 16 | Graphic processing unit small batch size |
| `grad_accum_steps` | 4 | Gradient accumulated count |
| `learning_rate` | 3.0e-4 | maximum learning rate |
| `min_lr` | 3.0e-5 | Minimum learning rate |
| `warmup_steps` | 100 | Warmth level number of steps |
| `max_steps` | 5000 | Total number of learning steps |
| `eval_interval` | 250 | Verification and intermediate storage cycle (step) |
| `ckpt_keep_last` | 3 | Number of recent interim saves to keep |
| `dtype` | bfloat16 | Operational precision |
| `compile` | false | torch.compile Used or not |

#### Inference defaults (`inference:`)

| parameter | default | Description |
|----------|--------|------|
| `default_max_new_tokens` | 256 | Maximum number of tickets to generate |
| `default_temperature` | 0.9 | Diversity control (The higher the more creative) |
| `default_top_k` | 50 | top KConsider only the dog tags |
| `default_top_p` | 0.95 | cumulative probability P In the meantime, only the ticket is considered |
| `default_repetition_penalty` | 1.15 | repetition suppression coefficient |

---

## 6. Stop and restart learning

### 6-1. Restart after normal stop

learning `Ctrl+C`If stopped with, You can continue learning from the last saved intermediate save point.:

```powershell
# Specify a specific intermediate save point
python -m src.train.train --resume checkpoints/ckpt_step002500.pt

# Use script (Automatically finds the latest intermediate savepoint)
.\scripts\03_train_model.ps1
```

**Information restored upon restart:**
- model weights (learning outcomes)
- Optimizer state (AdamW 1tea/2car moment)
- Current step number
- Setting information

### 6-2. Study in multiple sessions

Even if you do not completely finish learning, you can resume at any time if there is an intermediate save point..  
For example, it is possible to study divided into 8 hours each day.:

```
1primary: 0 → 1000 step learning → end → checkpoints/ckpt_step001000.pt save
2primary: 1000 Restart from step → 2000 up to the steps → ...
```

**Highest performance intermediate save point** (`ckpt_step*_best.pt`)is automatically preserved.

---

## 7. Force shutdown processing (power off)

### 7-1. technical explanation

Intermediate storage code (`src/train/utils.py:34`):
```python
torch.save({...}, path)
```

`torch.save()`Writes directly to the destination file without going through the temporary file..  
So save **on the way** If the power is cut off, those files may become corrupted..

### 7-2. real impact

| situation | influence |
|------|------|
| Turn off power after saving is complete | no effect — file normal |
| Power off during saving | The file may be damaged |
| During learning(Not when to save) power off | no effect — Previous save file normal |

### 7-3. safety device

`ckpt_keep_last: 3` By setting, the last 3 intermediate save points are always preserved.:
```
checkpoints/
├── ckpt_step000250.pt   ← 250 step (oldest)
├── ckpt_step000500.pt   ← 500 step
├── ckpt_step000750.pt   ← 750 step (most recent)
└── ckpt_step002500_best.pt  ← best performance (always preserved)
```

### 7-4. scale of loss

- Intermediate storage cycle: `eval_interval: 250` step
- RTX 5070 speed: about 130,000 tag/seconds
- 250 step = 16×4×1024×250 = 16,384,000 stamp processing
- Time required: 16,384,000 ÷ 130,000 ≈ **126seconds (About 2 minutes)**

**conclusion: If you force quit, you will lose up to 2 minutes of learning.**

### 7-5. How to recover

If you restart your computer after force quitting,:

```powershell
# 1step: Check the list of intermediate save points
ls checkpoints\ckpt_step*.pt

# 2step: Check for damage (Corruption if the file size is abnormally small)
# normal file: 100MB more than (30M model basis)

# 3step: Restart with last good file
python -m src.train.train --resume checkpoints\ckpt_step000500.pt
```

---

## 8. Reasoning and Conversation

### 8-1. Sampling Algorithm

The process of selecting the next word mark from the learned model:

**temperature control (Temperature):**
```
probability = softmax(logit / temperature)
```
- temperature = 1.0: Same as the original probability
- temperature < 1.0: Emphasizing probability differences (more consistent output)
- temperature > 1.0: Mitigating probability differences (More creative output)

**top K specimen (Top-k):**
Sampling after removing the rest, leaving only the top 50 high-probability tags

**nuclear specimen (Top-p / Nucleus Sampling):**
Cumulative probability is 95%Add tags and then sample until reached

**repetition suppression (Repetition Penalty):**
Count the probability of already created tags(1.15)Divide by to prevent repetition

### 8-2. conversation flow

```
user input → system prefix + conversation history + Combine new inputs
    ↓
tokenization (maximum block_size=1024 truncate)
    ↓
Model forward calculation → logit vector
    ↓
temperature/top-k/top-p/Apply repetition suppression
    ↓
Select a quote → convert to string → display on screen
    (EOS stamp or max_new_tokens repeat until reached)
```

### 8-3. system prefix

Once the model has been sufficiently trained(vocabulary size ≥ 8,000) Prefix the role:

| conversation style | prefix |
|-----------|--------|
| free conversation | `You are my Korean assistant who guides you through food and exercise..` |
| Ryori Recommendation | `You are a guide to Joseon-style cooking methods.. ...` |
| exercise plan | `You are the workout plan guide assistant. ...` |

### 8-4. Run web screen

```powershell
# scripts/04_run_app.ps1
python -m src.app.gradio_app --config config/model_config.yaml --server-port 8000
```

**access address:** `http://127.0.0.1:8000`

> **caution (Windows 11):** port 7787~7886silver Hyper-V/WSLSo make this reservation  
> default port(7860) Use 8000 instead.

---

## 9. 100Giga Material Study Guide

### 9-1. Recommended model size

| data size | recommended model | parameter | learning steps |
|-----------|-----------|----------|-----------|
| 1MB less than | default settings | 30M | 5,000 |
| 1~10GB | medium setting | 50M | 50,000 |
| 10~100GB | large setting | 85M | 200,000+ |

### 9-2. 85M Model settings

`config/model_config.yaml`Modify it as below::

```yaml
model:
  n_layer: 12
  n_head: 12
  n_embd: 768
  ffn_dim: 3072
  block_size: 1024
  vocab_size: 16384
  dropout: 0.1
  tie_weights: true

train:
  batch_size: 8          # Reduce batch size as this is a large model
  grad_accum_steps: 8    # Maintain effective bundle size
  max_steps: 200000
  eval_interval: 1000
  ckpt_keep_last: 5
```

### 9-3. Estimated study time

**RTX 5070 (12GB VRAM) standard:**

| material | number of tickets | speed | estimated time |
|------|---------|------|----------|
| 10GB | ~50billion dollar bill | 130k tok/s | About 11 hours |
| 100GB | ~500billion dollar bill | 130k tok/s | about 4.5work |

> Actually 200,000 step standard:  
> 200,000 × 8 × 8 × 1024 ÷ 130,000 ÷ 3600 ≈ **28time**  
> (Based on 1 epoch without repeating the entire material multiple times)

### 9-4. How to provide data

```
AI-assistant/data/raw/ Put the data files in the folder.
Supported Format: .txt, .jsonl
Subfolders are also processed automatically.
```

---

## 10. Full execution procedure

### 10-1. Check your environment

```powershell
.\scripts\00_setup_check.ps1
```

Check items: Python version, PyTorch, CUDA, SentencePiece, Gradio

### 10-2. Full execution sequence

```powershell
# 1step: Learn about word tag analyzer
.\scripts\01_train_tokenizer.ps1

# 2step: Data preprocessing
.\scripts\02_preprocess_data.ps1

# 3step: model learning
.\scripts\03_train_model.ps1

# 4step: Launch conversation screen
.\scripts\04_run_app.ps1
```

### 10-3. file structure

```
AI-assistant/
├── config/
│   └── model_config.yaml          # full settings
├── data/
│   ├── raw/                       # Original study material
│   └── processed/                 # Preprocessed binary data
├── checkpoints/
│   ├── tokenizer/dprk_sp.model    # tag analyzer mockup
│   ├── ckpt_step*.pt              # Learning intermediate save points
│   └── ckpt_step*_best.pt         # Best performing savepoint
├── src/
│   ├── app/gradio_app.py          # web screen
│   ├── data/
│   │   ├── dataset.py             # TokenDataset (memmap)
│   │   └── preprocess.py         # original→binary conversion
│   ├── inference/
│   │   ├── chat.py                # ChatSession
│   │   └── generate.py           # Tag generation algorithm
│   ├── model/
│   │   ├── config.py              # Setting data type
│   │   └── transformer.py        # GPT model code
│   ├── tokenizer/
│   │   ├── tokenizer.py          # Tokenizer packaging machine
│   │   └── train_tokenizer.py    # SentencePiece learning
│   └── train/
│       ├── train.py              # main learning loop
│       └── utils.py              # learning rate, Intermediate storage tool
└── scripts/
    ├── 00_setup_check.ps1
    ├── 01_train_tokenizer.ps1
    ├── 02_preprocess_data.ps1
    ├── 03_train_model.ps1
    └── 04_run_app.ps1
```

---

## 11. problem solving

This section summarizes errors encountered during actual execution and solutions..

### error 1: Vocabulary size exceeded

**error message:**
```
RuntimeError: Vocabulary size too high (16384). Please set it to a value <= 2333
```

**cause:** There is too little material to create the requested vocabulary size

**solved:** The program automatically adjusts your vocabulary size.  
Add more materials to unlock a large vocabulary.

---

### error 2: Insufficient data size

**error message:**
```
ValueError: Dataset has 156 tokens but block_size is 1024
```

**cause:** Number of preprocessed data tags is less than context length

**solved:** The program automatically `block_size`reduce.  
Adding more data is the fundamental solution.

---

### error 3: Windows Port blocking

**error message:**
```
OSError: [WinError 10013] Cannot find empty port in range: 7860-7860
```

**cause:** Windows 11of Hyper-V/WSLThis 7787~7886 reserve a port

**solved:** `scripts/04_run_app.ps1`Specify port as 8000

```powershell
python -m src.app.gradio_app --server-port 8000
```

---

### error 4: localhost Unable to connect

**symptoms:** The program runs, but the browser does not connect.

**cause:** Windows 11This `localhost`to IPv6(::1)Interpreted as

**solved:** `http://127.0.0.1:8000`Direct connection to  
`gradio_app.py`in `server_name="127.0.0.1"` set

---

### error 5: on output ⁇ Symbol Appears

**symptoms:**
```
⁇ 4.: ⁇ 4.six views
```

**cause:** A word analyzer trained with a small amount of data cannot process system prefix characters.

**solved:**
1. `tokenizer.py`in UNK Fish tag automatically removed
2. vocabulary size 8,000 If less than, the system prefix is omitted.
3. fundamental solution: Increase vocabulary size by increasing material

---

### error 6: Server crashes when submitting conversation (Gradio 6)

**cause:** Gradio 6has changed the conversation history format  
Previous: `[["user", "assistant"], ...]` (pair list)  
present: `[{"role": "user", "content": "..."}, ...]` (dictionary list)

**solved:** `gradio_app.py`to Gradio 6 Completely rewritten in format ✓

---

### error 7: RTX 5070 (Blackwell) Compatibility

**cause:** RTX 5070silver Compute Capability 12.0 (sm_120)  
PyTorch 2.7 Not supported below

**solved:**
```powershell
pip install torch==2.7.0+cu128 --index-url https://download.pytorch.org/whl/cu128
```

---

### error 8: PowerShell script error (SentencePiece log)

**cause:** `$ErrorActionPreference = "Stop"` When set SentencePiece  
Recognize informational messages as errors

**solved:** in all scripts `$LASTEXITCODE` Replaced with based inspection ✓

---

## 12. Note

### Loss function analysis

| Verification Loss | meaning |
|-----------|------|
| 9~10 | Steps to start learning (random level) |
| 5~7 | Learning basic language patterns |
| 3~5 | Capable of creating meaningful sentences |
| 2~3 | good quality (Large amount of data required) |
| 2 less than | very excellent (Dozens of gigabytes or more data required) |

### Relationship between learning materials and quality

- **thousands of sentences:** Identify language patterns, awkward output
- **hundreds of thousands of sentences (hundredsMB):** Basic questions and answers available
- **millions of sentences (numberGB):** Natural conversation possible
- **hundreds of millions of sentences (100GB):** Create expert-level Korean language

### characteristics of the Joseon language

This model is standard Korean(south side)not **Joseon language(north side)** For exclusive use:
- `program` → `program`
- `computer` → `computer`
- `algorithm` → `algorithm`
- `internet` → `internet`
- `software` → `software`

The more learning materials are provided in the original Korean language, the better the quality of the model in Korean language..

---

*This guide contains all errors and solutions that occurred during actual development and testing..*  
*Material inquiry: AI-assistant Reference to project settings file*
