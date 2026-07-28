# Environment Setup Guide
# Environment Configuration Guide — Joseon Dynasty artificial intelligence assistant

---

## Table of Contents / table of contents

1. [Computer Requirements by Model Size / Computer requirements by model size](#1-computer-requirements-by-model-size)
   - Small 30M · Medium 85M · Large 350M · XLarge 1B
   - **128GB VRAM (7B)** · **256GB VRAM (13B)** · **512GB VRAM (30B)** · **1TB VRAM (70B+)**
   - VRAM Quick Reference Table
2. [Software Requirements / Software Requirements](#2-software-requirements)
3. [Online Setup (First Time) / Online initial installation](#3-online-setup-first-time)
4. [Offline Setup / Offline installation](#4-offline-setup)
5. [Raw Data Requirements / Study Material Requirements](#5-raw-data-requirements)
6. [Running the Pipeline / execution procedure](#6-running-the-pipeline)
7. [Activating the Environment / Activation of virtual environment](#7-activating-the-environment)
8. [Troubleshooting / problem solving](#8-troubleshooting)

> **AI Platform comparative analysis** (OpenAI, Gemini, Copilot, Claude, DeepSeek) →  
> separate document: [`docs/ai_platforms.md`](docs/ai_platforms.md)

---

## 1. Computer Requirements by Model Size

### Model Size Overview / Model scale overview

| Tier | Parameters | VRAM Needed | Use Case | Training Data |
|------|------------|-------------|----------|---------------|
| **Small**   | 30M   | 4–8 GB        | Testing, small corpus         | < 1 GB    |
| **Medium**  | 85M   | 8–12 GB       | General use                   | 1–50 GB   |
| **Large**   | 350M  | 12–24 GB      | High quality single domain    | 50–500 GB |
| **XLarge**  | 1B    | 24–80 GB      | Production single GPU         | 500 GB+   |
| **128GB**   | 7B    | 128 GB VRAM   | Comparable to LLaMA-3 8B      | 1–10 TB   |
| **256GB**   | 13B   | 256 GB VRAM   | Comparable to LLaMA-2 13B     | 5–50 TB   |
| **512GB**   | 30B   | 512 GB VRAM   | GPT-3 class (175B inference)  | 10–100 TB |
| **1TB**     | 70B+  | 1 TB VRAM     | Frontier model territory      | 100 TB+   |

---

### Small Model — 30M Parameters (Default Config)
**`n_layer: 8, n_head: 8, n_embd: 512`**

| Component | Minimum | Recommended |
|-----------|---------|-------------|
| **CPU** | 4 cores, 2.5 GHz | 8 cores, 3.5 GHz |
| **RAM** | 8 GB | 16 GB |
| **GPU VRAM** | 4 GB (NVIDIA) | 8 GB |
| **GPU** | GTX 1660, RTX 2060 | RTX 3060, RTX 4060 |
| **Storage (OS + Software)** | 20 GB | 40 GB |
| **Storage (Training Data)** | 1 GB | 10 GB |
| **Storage (Checkpoints)** | 2 GB | 5 GB |
| **CUDA Version** | 11.8 | 12.8 |
| **Training Speed** | ~20k tok/s | ~80k tok/s |
| **Training Time (5,000 steps)** | ~30 min | ~8 min |

```yaml
# config/model_config.yaml — Small Model
model:
  n_layer: 8
  n_head: 8
  n_embd: 512
  ffn_dim: 2048
  block_size: 1024
train:
  batch_size: 16
  grad_accum_steps: 4
  max_steps: 5000
```

---

### Medium Model — 85M Parameters
**`n_layer: 12, n_head: 12, n_embd: 768`**

| Component | Minimum | Recommended |
|-----------|---------|-------------|
| **CPU** | 8 cores, 3.0 GHz | 16 cores, 4.0 GHz |
| **RAM** | 16 GB | 32 GB |
| **GPU VRAM** | 8 GB | 12 GB |
| **GPU** | RTX 3060 12GB, RTX 3070 | RTX 3090, RTX 4070 Ti, RTX 5070 |
| **Storage (OS + Software)** | 20 GB | 40 GB |
| **Storage (Training Data)** | 10 GB | 100 GB |
| **Storage (Checkpoints)** | 10 GB | 30 GB |
| **CUDA Version** | 11.8 | 12.8 |
| **Training Speed** | ~40k tok/s | ~100k tok/s |
| **Training Time (50,000 steps)** | ~20 hours | ~7 hours |

```yaml
# config/model_config.yaml — Medium Model
model:
  n_layer: 12
  n_head: 12
  n_embd: 768
  ffn_dim: 3072
  block_size: 1024
train:
  batch_size: 8
  grad_accum_steps: 8
  learning_rate: 1.0e-4
  min_lr: 1.0e-5
  max_steps: 50000
  eval_interval: 1000
  ckpt_keep_last: 5
```

---

### Large Model — 350M Parameters
**`n_layer: 24, n_head: 16, n_embd: 1024`**

| Component | Minimum | Recommended |
|-----------|---------|-------------|
| **CPU** | 16 cores, 3.5 GHz | 32 cores, 4.5 GHz |
| **RAM** | 32 GB | 64 GB |
| **GPU VRAM** | 12 GB | 24 GB |
| **GPU** | RTX 3090, RTX 4090, **RTX 5070 12GB** | RTX 4090 24GB, A100 |
| **Storage (OS + Software)** | 40 GB | 80 GB |
| **Storage (Training Data)** | 50 GB | 500 GB |
| **Storage (Checkpoints)** | 30 GB | 100 GB |
| **CUDA Version** | 12.1 | 12.8 |
| **Training Speed** | ~30k tok/s (12GB) | ~80k tok/s (24GB) |
| **Training Time (200,000 steps)** | ~4 days | ~36 hours |

```yaml
# config/model_config.yaml — Large Model
model:
  n_layer: 24
  n_head: 16
  n_embd: 1024
  ffn_dim: 4096
  block_size: 1024
train:
  batch_size: 4
  grad_accum_steps: 16
  learning_rate: 6.0e-5
  min_lr: 6.0e-6
  max_steps: 200000
  warmup_steps: 2000
  eval_interval: 2000
  ckpt_keep_last: 5
  dtype: bfloat16
```

---

### XLarge Model — 1B Parameters
**`n_layer: 24, n_head: 16, n_embd: 2048`**

| Component | Minimum | Recommended |
|-----------|---------|-------------|
| **CPU** | 32 cores | 64 cores |
| **RAM** | 64 GB | 128 GB |
| **GPU VRAM** | 24 GB (single) | 80 GB (A100) or multi-GPU |
| **GPU** | RTX 4090 24GB | A100 80GB, H100 |
| **Storage (Training Data)** | 200 GB | 1 TB+ |
| **Storage (Checkpoints)** | 100 GB | 500 GB |
| **CUDA Version** | 12.1 | 12.8 |
| **Training Time** | Weeks | Days (A100) |

> **Note:** XLarge requires gradient checkpointing and possibly model parallelism.
> This config is NOT included in the default scripts — contact a specialist.

---

## ═══ ENTERPRISE / RESEARCH SCALE ═══
> Models below require **multi-GPU server hardware**.  
> Single consumer GPU (RTX 5070) cannot train these sizes.  
> Additional software: **DeepSpeed** or **PyTorch FSDP** for distributed training.

---

### 128GB VRAM Tier — 7B Parameters
**Comparable to: LLaMA-3 8B, Mistral 7B, Gemma 2 9B**

> 128GB VRAM = 2× NVIDIA A100 80GB (NVLink) or 1× NVIDIA H200 141GB

| Component | Minimum Config | Recommended Config |
|-----------|---------------|-------------------|
| **GPU** | 2× A100 40GB (NVLink) | 2× A100 80GB (NVLink) or H200 |
| **Total VRAM** | 80 GB | 160 GB |
| **System RAM** | 256 GB | 512 GB |
| **CPU** | 2× Xeon Gold / EPYC (32 cores total) | 2× EPYC 64-core |
| **Storage (NVMe SSD)** | 2 TB | 8 TB |
| **Training Data** | 1 TB | 10 TB |
| **Network** | 100 GbE | InfiniBand HDR (200Gb/s) |
| **Power Supply** | 3,000W | 6,500W (server rack) |
| **Training Time (100K steps)** | ~5 days | ~2 days |
| **Checkpoint Size** | ~14 GB per ckpt | ~14 GB per ckpt |

**Memory breakdown for 7B model training:**

| Component | Memory Usage |
|-----------|-------------|
| Weights (bf16) | 14 GB |
| Gradients (fp32) | 28 GB |
| Optimizer states (AdamW fp32) | 56 GB |
| Activations (batch dependent) | 10–30 GB |
| **Total needed** | **~110–130 GB** |

**Required techniques:**
- ✅ Gradient checkpointing (saves ~40% activation memory)
- ✅ bf16 mixed precision
- ✅ ZeRO-2 optimizer (DeepSpeed) or FSDP
- ⬜ Tensor parallelism (optional, for speed)

```yaml
# config/model_config.yaml — 7B Model
model:
  n_layer: 32
  n_head: 32
  n_embd: 4096
  ffn_dim: 11008    # LLaMA style: 2.7× n_embd
  block_size: 4096
  vocab_size: 32000
train:
  batch_size: 2
  grad_accum_steps: 32
  learning_rate: 3.0e-5
  max_steps: 100000
  warmup_steps: 2000
  dtype: bfloat16
  compile: true
```

**Estimated cost (cloud):**
- AWS: 2× A100 80GB = ~$10/hr → $1,200 for 5 days
- RunPod: 2× A100 80GB = ~$3.5/hr → $420

---

### 256GB VRAM Tier — 13B Parameters
**Comparable to: LLaMA-2 13B, Mistral 12B, Gemma 2 12B**

> 256GB VRAM = 4× NVIDIA A100 80GB (NVSwitch) or 4× H100 SXM 80GB

| Component | Minimum Config | Recommended Config |
|-----------|---------------|-------------------|
| **GPU** | 4× A100 40GB | 4× A100 80GB or 4× H100 SXM |
| **Total VRAM** | 160 GB | 320 GB |
| **System RAM** | 512 GB | 1 TB |
| **CPU** | 4× EPYC (64 cores total) | 4× EPYC 96-core |
| **Storage (NVMe SSD)** | 4 TB | 20 TB |
| **Training Data** | 5 TB | 50 TB |
| **Network** | InfiniBand HDR | InfiniBand NDR (400Gb/s) |
| **Power Supply** | 6,500W | 13,000W (dual server rack) |
| **Training Time (200K steps)** | ~8 days | ~3 days |
| **Checkpoint Size** | ~26 GB per ckpt | ~26 GB per ckpt |

**Required techniques:**
- ✅ Gradient checkpointing
- ✅ bf16 mixed precision
- ✅ ZeRO-3 optimizer (DeepSpeed) — shards across all 4 GPUs
- ✅ Data Parallel (DDP) for 4-GPU coordination
- ⬜ Flash Attention 2 (recommended for speed)

```yaml
# config/model_config.yaml — 13B Model
model:
  n_layer: 40
  n_head: 40
  n_embd: 5120
  ffn_dim: 13824
  block_size: 4096
  vocab_size: 32000
train:
  batch_size: 1
  grad_accum_steps: 64
  learning_rate: 2.0e-5
  max_steps: 200000
  warmup_steps: 3000
  dtype: bfloat16
```

**Estimated cost (cloud):**
- AWS: 4× A100 80GB (p4d.24xlarge) = ~$32/hr → $6,100 for 8 days
- RunPod: 4× A100 80GB = ~$12/hr → $2,300

---

### 512GB VRAM Tier — 30B Parameters
**Training scale of: Falcon 40B, LLaMA-2 34B**
**Inference scale of: GPT-3 175B (with 4-bit quantization)**

> 512GB VRAM = 8× NVIDIA A100 80GB = 1× DGX A100 System

| Component | Spec |
|-----------|------|
| **GPU** | 8× A100 80GB SXM (NVSwitch full mesh) |
| **Total VRAM** | 640 GB |
| **System RAM** | 1 TB – 2 TB |
| **CPU** | 2× AMD EPYC 7763 (128 cores total) |
| **Storage** | 8× NVMe SSD = 30 TB (RAID) |
| **Training Data** | 10 TB – 100 TB |
| **Network** | 8× InfiniBand HDR 200Gb/s |
| **Power** | ~6,500W (DGX A100 full system) |
| **System Price** | ~$300,000 USD (DGX A100) |
| **Cloud Alternative** | AWS p4de.24xlarge ~$40/hr |
| **Training Time (500K steps)** | ~2 weeks |
| **Checkpoint Size** | ~60 GB per checkpoint |

**Required techniques:**
- ✅ Gradient checkpointing
- ✅ bf16 mixed precision
- ✅ ZeRO-3 + CPU offloading (DeepSpeed)
- ✅ Tensor Parallelism (split attention heads across GPUs)
- ✅ Flash Attention 2/3
- ⬜ Pipeline Parallelism (optional)

**What you can run for inference (not training) at 512GB:**
| Model | Size | Method |
|-------|------|--------|
| GPT-3 | 175B | bf16 inference (350 GB) |
| LLaMA-3 | 70B | bf16 training |
| Mixtral 8×7B | 46B active | MoE inference |
| DeepSeek-V2 | 21B active (236B total MoE) | MoE inference |

---

### 1TB VRAM Tier — 70B+ Parameters
**Comparable to: LLaMA-3 70B, LLaMA-2 70B, Falcon 180B (inference)**

> 1TB VRAM = 2× DGX A100 (16× A100 80GB = 1.28TB) or 8× H200 141GB = 1.13TB

| Component | Spec |
|-----------|------|
| **GPU** | 16× A100 80GB (2× DGX) or 8× H200 141GB |
| **Total VRAM** | 1.28 TB (A100) / 1.13 TB (H200) |
| **System RAM** | 2 TB – 4 TB |
| **CPU** | 4× AMD EPYC 9654 (384 cores total) |
| **Storage** | 100 TB+ (Lustre parallel filesystem) |
| **Training Data** | 100 TB+ |
| **Network** | InfiniBand NDR 400Gb/s between nodes |
| **Power** | 15,000W – 30,000W |
| **System Price** | ~$600,000 – $2,000,000 USD |
| **Cloud Alternative** | AWS p4de × 2 ~$80/hr |
| **Training Time (1M steps, 70B)** | ~30 days |
| **Checkpoint Size** | ~140 GB per checkpoint |

**Required techniques:**
- ✅ 3D Parallelism: Data + Tensor + Pipeline
- ✅ ZeRO-3 with CPU and NVMe offloading
- ✅ Flash Attention 3
- ✅ Gradient checkpointing
- ✅ Activation recomputation
- ✅ BF16 master weights, FP8 forward pass (H100/H200)
- ✅ High-speed inter-node networking (InfiniBand mandatory)

**What runs at 1TB VRAM:**

| Model | Scale | Notes |
|-------|-------|-------|
| LLaMA-3 70B | Training | Full bf16 training fits |
| GPT-3.5 175B | Inference | bf16 inference (350 GB) |
| Falcon 180B | Inference | bf16 inference (360 GB) |
| Mixtral 8×22B | Training | 141B total, 39B active |
| DeepSeek-V3 | Inference only | 671B total MoE, 37B active |

> **Note for 128GB / 256GB / 512GB / 1TB tiers:**  
> The current project codebase (`src/train/train.py`) supports **single GPU only**.  
> To train at these scales, you need **DeepSpeed** or **PyTorch FSDP**:  
> ```powershell
> pip install deepspeed
> deepspeed --num_gpus=8 src/train/train_deepspeed.py --config config/model_config.yaml
> ```
> This is a significant engineering task — multi-GPU training scripts are not included.

---

### VRAM Quick Reference Table

| Available VRAM | Max Training (bf16+AdamW) | Max Inference (bf16) | Max Inference (4-bit) |
|----------------|--------------------------|---------------------|----------------------|
| 4 GB | 30M params | 2B params | 8B params |
| 8 GB | 85M params | 4B params | 16B params |
| 12 GB (RTX 5070) | 350M params | 6B params | 24B params |
| 24 GB (RTX 4090) | 1B params | 12B params | 48B params |
| 80 GB (A100) | 4B params | 40B params | 160B params |
| 128 GB | 7B params | 65B params | 260B params |
| 256 GB | 13B params | 130B params | 520B params |
| 512 GB (DGX A100) | 30B params | 260B params | ~1T params |
| 1 TB | 65B params | 512B params | >1T params |

---

### This Machine (RTX 5070 12GB) — Recommended Config

> The RTX 5070 uses NVIDIA Blackwell architecture (Compute Capability 12.0 / sm_120).
> **Requires PyTorch 2.7+ with CUDA 12.8.**

| Component | Spec |
|-----------|------|
| GPU | RTX 5070 12GB VRAM (sm_120) |
| Training Speed | ~130,000 tokens/sec (30M model) |
| Best Fit | Medium model (85M), or Large model with batch_size=4 |

---

## 2. Software Requirements

### Required Software

| Software | Version | Download |
|----------|---------|----------|
| **Windows** | 10 / 11 (64-bit) | — |
| **Python** | 3.10 – 3.14 | https://www.python.org/downloads/ |
| **CUDA Toolkit** | 12.8 (for RTX 5070) | NVIDIA Developer site |
| **cuDNN** | 9.x (matches CUDA) | NVIDIA Developer site |
| **Git** | 2.40+ | https://git-scm.com/ |

### Python Packages (installed automatically)

| Package | Version | Purpose |
|---------|---------|---------|
| `torch` | 2.7+ (cu128) | Deep learning framework |
| `numpy` | 1.26+ | Array processing |
| `pyyaml` | 6.0+ | Config file reading |
| `sentencepiece` | 0.2.0+ | Korean tokenizer |
| `tensorboard` | 2.16+ | Training visualization |
| `tqdm` | 4.66+ | Progress bars |
| `gradio` | 6.0+ | Web chat interface |
| `pdfplumber` | 0.11+ | PDF text extraction |
| `python-docx` | 1.1+ | Word document reading |
| `easyocr` | 1.7+ | Image OCR (Korean + English) |

### Optional (for development)

| Software | Purpose |
|----------|---------|
| VS Code | Code editor |
| TensorBoard | Training monitoring (`tensorboard --logdir checkpoints/runs`) |

---

## 3. Online Setup (First Time)

### Step 1 — Check Python Version

```powershell
python --version
# Must be 3.10 or higher
# Python 3.14.x ← confirmed working
```

### Step 2 — Install PyTorch with CUDA 12.8

> **RTX 5070 (Blackwell sm_120) REQUIRES this exact command:**

```powershell
pip install torch>=2.7 --index-url https://download.pytorch.org/whl/cu128
```

> For other NVIDIA GPUs (Ampere/Ada — RTX 3000/4000 series):
> ```powershell
> pip install torch>=2.7 --index-url https://download.pytorch.org/whl/cu121
> ```

> For CPU only (no GPU):
> ```powershell
> pip install torch>=2.7
> ```

### Step 3 — Activate Project Virtual Environment

```powershell
cd E:\Github\Mobile\Mobile-App\AI-assistant

# Activate the project venv (created during setup)
.\.venv\Scripts\Activate.ps1

# Verify activation — prompt should show (.venv)
```

### Step 4 — Install All Other Dependencies

```powershell
# Install all packages (torch already installed above)
pip install -r requirements.txt

# Verify installation
python -c "import torch, gradio, sentencepiece, pdfplumber, easyocr; print('All OK')"
```

### Step 5 — Verify GPU Access

```powershell
python -c "
import torch
print('CUDA available:', torch.cuda.is_available())
print('GPU:', torch.cuda.get_device_name(0) if torch.cuda.is_available() else 'None')
print('VRAM:', round(torch.cuda.get_device_properties(0).total_memory/1e9, 1), 'GB')
"
```

Expected output:
```
CUDA available: True
GPU: NVIDIA GeForce RTX 5070
VRAM: 12.0 GB
```

---

## 4. Offline Setup

### What Is Available Offline

This project includes a `.venv` folder with all dependencies pre-installed.
The `wheels/` folder contains package files for reinstallation without internet.

### File Structure for Offline Use

```
AI-assistant/
├── .venv/              ← Pre-built virtual environment (use directly)
├── wheels/             ← Downloaded wheel files (for reinstall)
│   ├── numpy-*.whl
│   ├── gradio-*.whl
│   └── ... (all packages except torch)
├── requirements.txt    ← Package list
└── setup_offline.ps1   ← Offline reinstall script
```

### Using the Pre-built venv (Simplest)

```powershell
cd E:\Github\Mobile\Mobile-App\AI-assistant

# Activate the included venv
.\.venv\Scripts\Activate.ps1

# Run any script
python -m src.app.gradio_app
```

### Reinstalling from wheels/ (If venv is broken)

```powershell
cd E:\Github\Mobile\Mobile-App\AI-assistant

# Run the offline setup script
.\setup_offline.ps1
```

Or manually:

```powershell
# 1. Delete old venv
Remove-Item -Recurse -Force .venv

# 2. Create new venv
python -m venv .venv --system-site-packages

# 3. Install from wheels (no internet needed)
.\.venv\Scripts\pip install --no-index --find-links wheels\ -r requirements.txt

# 4. Torch must be pre-installed globally (or from wheels if downloaded)
#    wheels/torch*.whl is NOT included (too large: ~2.5GB)
#    Torch must be installed while online:
#    pip install torch>=2.7 --index-url https://download.pytorch.org/whl/cu128
```

### Downloading Torch for Offline Use (Optional, ~2.5 GB)

If you need to use on a completely offline machine that has no PyTorch:

```powershell
# Download torch wheel while online (saves to wheels/ folder)
pip download "torch>=2.7" --index-url https://download.pytorch.org/whl/cu128 --dest wheels\

# On the offline machine, install:
pip install --no-index --find-links wheels\ torch
```

> **Warning:** The torch wheel file is approximately 2.5 GB.
> Make sure you have enough disk space.

---

## 5. Raw Data Requirements

### Supported File Formats

| Format | Extension | Notes |
|--------|-----------|-------|
| Plain text | `.txt` | UTF-8 encoding required |
| JSON Lines | `.jsonl` | One JSON object per line |
| JSON | `.json` | Array or single object |
| PDF | `.pdf` | Text-based PDFs (not scanned images) |
| Word Document | `.docx` | Microsoft Word 2007+ format |
| Image (JPEG) | `.jpg`, `.jpeg` | OCR text extraction |
| Image (PNG) | `.png` | OCR text extraction |
| Image (BMP) | `.bmp` | OCR text extraction |
| Image (GIF) | `.gif` | OCR text extraction |
| Image (WebP) | `.webp` | OCR text extraction |
| Image (TIFF) | `.tif`, `.tiff` | OCR text extraction |

> **Note:** Scanned PDF files (images inside PDF) should be converted to image format first,
> then OCR will process them. Or use a tool like `pdfimages` to extract images.

### JSON / JSONL Format Options

The preprocessor automatically recognizes these structures:

```jsonl
{"text": "Soybean paste soup is a traditional dish from Joseon.."}
{"question": "How to boil soybean paste soup", "answer": "prepare the ingredients..."}
{"content": "Protein intake is important after exercise."}
{"title": "Joseon cuisine", "content": "Joseon Ryori..."}
{"body": "When making a sports plan..."}
```

### Data Volume by Model Size

| Model Size | Minimum Data | Recommended Data | Expected Quality |
|------------|-------------|-----------------|-----------------|
| Small 30M  | 1 MB | 100 MB | Basic patterns |
| Medium 85M | 100 MB | 10 GB | Natural sentences |
| Large 350M | 10 GB | 100 GB | High quality |
| XLarge 1B  | 100 GB | 1 TB | Expert level |

### Data Quality Guidelines

1. **Language:** Must be Korean text (Joseon language preferred for this project)
2. **Encoding:** UTF-8 only — convert other encodings before placing in `data/raw/`
3. **Minimum document length:** At least 10 characters per document
4. **Duplicates:** Automatically removed by content hash (SHA-1)
5. **Mixed formats:** All formats can coexist in `data/raw/` — subfolders supported

### Folder Structure for Data

```
data/
└── raw/                    ← Place ALL training data here
    ├── food/               ← Subfolders are automatically scanned
    │   ├── recipes.txt
    │   ├── nutrition.jsonl
    │   └── cookbook.pdf
    ├── workout/
    │   ├── exercises.txt
    │   └── plans.docx
    ├── images/             ← Image files for OCR
    │   ├── scan001.jpg
    │   └── document.png
    └── general.jsonl
```

### Storage Requirements

| Item | Size | Notes |
|------|------|-------|
| Python + venv | ~2 GB | Including all packages |
| PyTorch (CUDA) | ~2.5 GB | Stored in pip cache or venv |
| EasyOCR models | ~200 MB | Downloaded on first image OCR |
| Training data | Your data | Place in `data/raw/` |
| Processed data | ~50% of raw | Token arrays in `data/processed/` |
| Checkpoints (30M) | ~400 MB each | 3 kept = ~1.2 GB |
| Checkpoints (85M) | ~1.5 GB each | 3 kept = ~4.5 GB |
| Checkpoints (350M) | ~5 GB each | 3 kept = ~15 GB |
| TensorBoard logs | ~100 MB | In `checkpoints/runs/` |

---

## 6. Running the Pipeline

### Quick Start

```powershell
# Activate venv
.\.venv\Scripts\Activate.ps1

# Step 1: Check environment
.\scripts\00_setup_check.ps1

# Step 2: Train tokenizer (only once, or when adding large amounts of new data)
.\scripts\01_train_tokenizer.ps1

# Step 3: Preprocess data
.\scripts\02_preprocess_data.ps1

# Step 4: Train model
.\scripts\03_train_model.ps1

# Step 5: Run chat UI
.\scripts\04_run_app.ps1
```

### Resume Interrupted Training

```powershell
.\.venv\Scripts\Activate.ps1

# Auto-find latest checkpoint and resume
python -m src.train.train --resume checkpoints\ckpt_step005000_final.pt

# Or let the script find it automatically
.\scripts\03_train_model.ps1
```

### Adding New Data and Continuing Training

```powershell
# 1. Copy new data to data/raw/
# 2. Re-run preprocessing (tokenizer stays the same)
.\scripts\02_preprocess_data.ps1

# 3. Update max_steps in config/model_config.yaml (increase by desired steps)
# 4. Resume from last checkpoint
.\scripts\03_train_model.ps1
```

### Monitor Training (TensorBoard)

```powershell
# Open a new PowerShell window
.\.venv\Scripts\Activate.ps1
tensorboard --logdir checkpoints\runs
# Open http://localhost:6006 in browser
```

---

## 7. Activating the Environment

### Every time you open PowerShell

```powershell
cd E:\Github\Mobile\Mobile-App\AI-assistant
.\.venv\Scripts\Activate.ps1
```

You will see `(.venv)` at the beginning of the prompt when activated.

### Check which Python is being used

```powershell
Get-Command python | Select-Object -ExpandProperty Source
# Should show: E:\Github\Mobile\Mobile-App\AI-assistant\.venv\Scripts\python.exe
```

### Deactivate when done

```powershell
deactivate
```

### PowerShell Execution Policy (if Activate.ps1 is blocked)

```powershell
# Run once as Administrator
Set-ExecutionPolicy -ExecutionPolicy RemoteSigned -Scope CurrentUser
```

---

## 8. Troubleshooting

### CUDA Not Detected

```
CUDA available: False
```

**Causes and fixes:**
1. PyTorch version doesn't match CUDA version
   ```powershell
   # RTX 5070 requires cu128:
   pip install torch>=2.7 --index-url https://download.pytorch.org/whl/cu128
   ```
2. NVIDIA driver not installed or outdated — update from NVIDIA website
3. CUDA Toolkit not installed — install CUDA 12.8 from NVIDIA

---

### RTX 5070 Not Recognized (sm_120 Error)

```
RuntimeError: CUDA error: no kernel image is available for execution on the device
```

**Fix:** Must use PyTorch 2.7+ compiled for CUDA 12.8:
```powershell
pip install torch>=2.7 --index-url https://download.pytorch.org/whl/cu128
```

---

### Out of Memory (CUDA OOM)

```
torch.cuda.OutOfMemoryError: CUDA out of memory
```

**Fix:** Reduce memory usage in `config/model_config.yaml`:
```yaml
train:
  batch_size: 4       # Reduce from 16
  grad_accum_steps: 16  # Increase to keep effective batch size
```

---

### Windows Port Blocked (Gradio)

```
OSError: Cannot find empty port in range: 7860-7860
```

**Fix:** Windows 11 Hyper-V/WSL reserves ports 7787–7886. The app uses port 8000 by default.
If port 8000 is also blocked:
```powershell
python -m src.app.gradio_app --server-port 8080
```

---

### PowerShell Script Execution Blocked

```
.\scripts\01_train_tokenizer.ps1 cannot be loaded because running scripts is disabled
```

**Fix:**
```powershell
Set-ExecutionPolicy -ExecutionPolicy RemoteSigned -Scope CurrentUser
```

---

### PDF Extraction Returns Empty

Some PDFs are scanned images (not text-based). These cannot be extracted with pdfplumber.

**Fix:** Convert to image files (.jpg/.png) and let EasyOCR process them:
```powershell
# Install poppler for pdf-to-image conversion
# Download: https://github.com/oschwartz10612/poppler-windows/releases
# Then use pdftoppm:
pdftoppm input.pdf output_page -r 300 -jpeg
# Place output_page-*.jpg in data/raw/
```

---

### EasyOCR Model Download Required

First time using image files:
```
Downloading detection model...
Downloading recognition model...
```

This is normal — EasyOCR downloads ~200 MB of models on first use.
After download, it works offline.

---

### Tokenizer Vocab Size Error

```
RuntimeError: Vocabulary size too high (16384). Please set it to a value <= 2333
```

**Cause:** Not enough training data for the requested vocabulary size.  
**Fix:** Add more data, or reduce `vocab_size` in `config/model_config.yaml`:
```yaml
tokenizer:
  vocab_size: 2000   # Reduce to match data size
model:
  vocab_size: 2000   # Keep same as tokenizer
```

---

## Quick Reference Card

```
PROJECT ROOT: E:\Github\Mobile\Mobile-App\AI-assistant\

ACTIVATE:     .\.venv\Scripts\Activate.ps1
DEACTIVATE:   deactivate

DATA IN:      data\raw\         (txt, json, jsonl, pdf, docx, jpg, png, ...)
DATA OUT:     data\processed\   (train.bin, val.bin, meta.json)
CHECKPOINTS:  checkpoints\      (ckpt_step*.pt, tokenizer\dprk_sp.model)
CONFIG:       config\model_config.yaml

SCRIPTS:
  00_setup_check.ps1     Check environment
  01_train_tokenizer.ps1 Train SentencePiece (once)
  02_preprocess_data.ps1 Convert raw → binary tokens
  03_train_model.ps1     Train GPT model
  04_run_app.ps1         Launch Gradio UI at http://127.0.0.1:8000

RESUME:  python -m src.train.train --resume checkpoints\ckpt_stepXXXXXX.pt
MONITOR: tensorboard --logdir checkpoints\runs  → http://localhost:6006
```
