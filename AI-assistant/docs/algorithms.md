# Algorithms & Speed Optimization Guide
# 알고리듬 분석 및 속도 향상 안내서

> 이 문서는 현재 코드베이스의 각 파일, 각 행을 기준으로  
> **어느 부분을 어떻게 바꾸면 얼마나 빨라지는지** 구체적으로 설명합니다.

---

## 우선순위 한눈에 보기

| 우선순위 | 개선 항목 | 파일:행 | 학습 속도↑ | 추론 속도↑ | 난이도 |
|---------|----------|---------|-----------|-----------|--------|
| 🔴 최우선 | **KV Cache** | `generate.py:68-121` | — | **10~100배** | 중간 |
| 🔴 최우선 | **torch.compile** | `config.yaml:compile` | **10~20%** | 10~20% | 매우 쉬움 |
| 🟠 높음 | **RMSNorm** | `transformer.py:74,76,94` | 10~15% | 10~15% | 쉬움 |
| 🟠 높음 | **SwiGLU 활성함수** | `transformer.py:68` | 5~10% | 5~10% | 쉬움 |
| 🟠 높음 | **RoPE 위치 인코딩** | `transformer.py:91-92,132-133` | — | 문맥 2배+ | 중간 |
| 🟡 중간 | **GQA/MQA 주목** | `transformer.py:34-46` | 20~40% | 20~40% | 중간 |
| 🟡 중간 | **기울기 체크포인팅** | `train.py:135` | -(계산↑) | — | 쉬움 |
| 🟡 중간 | **자료 미리 읽기** | `dataset.py:39-53` | 5~15% | — | 쉬움 |
| 🟢 고급 | **INT8/INT4 양자화** | 추론 전용 | — | **2~4배** | 높음 |
| 🟢 고급 | **추측 복호화** | `generate.py` | — | **2~3배** | 높음 |
| 🟢 고급 | **MLA (DeepSeek식)** | `transformer.py:34-46` | — | KV 93% 절감 | 매우 높음 |
| 🟢 고급 | **MoE 구조** | `transformer.py:60-68` | — | 지식↑, 속도 유지 | 매우 높음 |

---

## 1. 🔴 KV Cache — 추론 속도 10~100배

### 현재 문제 (가장 큰 병목)

**파일:** `src/inference/generate.py:68-121`

```python
# generate.py:68-69 — 현재 코드
idx_cond = idx if idx.size(1) <= block_size else idx[:, -block_size:]
logits, _ = model(idx_cond)   # ← 매 스텝마다 전체 시퀀스를 처음부터 처리!
```

**문제:** 100번째 어표를 생성할 때도 앞의 99개 어표를 또 처음부터 계산합니다.

```
스텝 1: [A] 처리 → 1번 연산
스텝 2: [A, B] 처리 → 2번 연산
스텝 3: [A, B, C] 처리 → 3번 연산
...
스텝 N: [A, B, ..., N] 처리 → N번 연산
총 연산: 1+2+...+N = N²/2번  ← O(n²) 시간복잡도
```

**generate.py:121** — 매 스텝 컨텍스트 성장:
```python
idx = torch.cat([idx, nxt], dim=1)  # 계속 길어지는 텐서
```

### KV Cache란?

주목 기제(Attention)에서 K(키)와 V(값)는 이전 어표들의 것을 다시 계산할 필요가 없습니다.  
한 번 계산한 K, V를 저장해두고 재사용합니다.

```
KV Cache 없음:
  스텝 1: Q₁K₁V₁ → 1번 계산
  스텝 2: Q₂K₁V₁ + Q₂K₂V₂ → 이전 K₁V₁도 다시 계산! (낭비)

KV Cache 있음:
  스텝 1: Q₁K₁V₁ → 계산 + K₁,V₁ 저장
  스텝 2: Q₂K₂V₂ → 새 어표만 계산, K₁,V₁은 저장된 것 사용
  총 연산: O(n) (선형)
```

### 구현 방법 (transformer.py 수정)

**`src/model/transformer.py:25-57` — CausalSelfAttention 수정:**

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

        # ← 핵심: 이전 캐시와 현재 K,V 합치기
        if kv_cache is not None:
            past_k, past_v = kv_cache
            k = torch.cat([past_k, k], dim=2)  # 시간 축으로 이어 붙임
            v = torch.cat([past_v, v], dim=2)

        new_cache = (k, v)  # 다음 스텝을 위해 저장

        y = F.scaled_dot_product_attention(q, k, v, is_causal=(kv_cache is None))
        y = y.transpose(1, 2).contiguous().view(B, T, C)
        y = self.resid_dropout(self.proj(y))
        return y, new_cache
```

**`src/inference/generate.py:67-82` — sample_token 수정:**

```python
@torch.no_grad()
def sample_token_cached(model, idx_new, *, kv_caches, temperature, top_k, top_p, repetition_penalty):
    """idx_new: 새 어표만 (B, 1) — 전체 시퀀스가 아님"""
    logits, new_caches = model(idx_new, kv_caches=kv_caches)
    logits = logits[:, -1, :]
    # ... 동일한 샘플링 로직
    return next_token, new_caches
```

### 적용 후 효과

| 시퀀스 길이 | 현재(캐시 없음) | KV Cache 적용 | 속도 향상 |
|------------|--------------|--------------|---------|
| 100 어표 | 5,000 연산 | 100 연산 | **50배** |
| 256 어표 | 32,768 연산 | 256 연산 | **128배** |
| 1024 어표 | 524,288 연산 | 1,024 연산 | **512배** |

> 현재 코드에서 추론이 느린 **가장 큰 이유**가 KV Cache 미구현입니다.

---

## 2. 🔴 torch.compile — 즉시 10~20% 속도 향상

### 현재 설정

**파일:** `config/model_config.yaml:compile`

```yaml
train:
  compile: false   # ← 이것만 true로 바꾸면 됨
```

**파일:** `src/train/train.py:142-148` — 이미 코드 구현 완료:

```python
if cfg.train.compile:
    try:
        model = torch.compile(model)   # ← 이미 있음, 설정만 켜면 됨
        print("[train] torch.compile enabled")
    except Exception as e:
        print(f"[train] torch.compile failed ({e}); continuing without it.")
```

### 작동 원리

```
torch.compile = Python 코드 → XLA/Triton 커널로 컴파일

일반 PyTorch: Python 함수 호출 → CUDA 커널 수백 번 실행 (오버헤드 큼)
torch.compile: 전체 순방향 계산을 하나의 최적화된 CUDA 커널로 합침
```

### 적용 방법

```yaml
# config/model_config.yaml 에서
train:
  compile: true   # 이것만 변경
```

**주의사항:**
- 첫 실행 시 컴파일에 1~3분 소요 (이후부터는 빠름)
- Windows에서 일부 제한: `torch.compile`이 실패하면 자동으로 일반 모드로 전환
- RTX 5070(Blackwell)에서 지원 확인됨

**예상 효과:** 학습 속도 10~20%, 추론 속도 10~20% 향상

---

## 3. 🟠 RMSNorm — LayerNorm 교체 (10~15% 속도 향상)

### 현재 코드

**파일:** `src/model/transformer.py:74, 76, 94`

```python
# transformer.py:71-77
class Block(nn.Module):
    def __init__(self, cfg: ModelConfig):
        self.ln_1 = nn.LayerNorm(cfg.n_embd, bias=cfg.bias)  # ← 행 74
        self.attn = CausalSelfAttention(cfg)
        self.ln_2 = nn.LayerNorm(cfg.n_embd, bias=cfg.bias)  # ← 행 76
        self.ffn = FeedForward(cfg)

# transformer.py:94
self.ln_f = nn.LayerNorm(cfg.n_embd, bias=cfg.bias)          # ← 행 94
```

### 문제

LayerNorm은 평균과 분산을 모두 계산합니다:
```
LayerNorm(x) = (x - mean(x)) / sqrt(var(x) + ε) × γ + β
               ↑↑↑ 평균 계산 ↑↑↑ 분산 계산
```

LLaMA, Mistral, Gemma가 사용하는 RMSNorm은 평균 계산을 생략합니다:
```
RMSNorm(x) = x / sqrt(mean(x²) + ε) × γ
             ↑↑↑ 분산만 계산 (평균 생략)
```

**결과:** 연산량 약 15% 감소, 학습 안정성 동등 이상

### 교체 방법

**`src/model/transformer.py` 상단에 추가:**

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

**`Block.__init__`에서 교체 (transformer.py:74, 76):**

```python
# 변경 전
self.ln_1 = nn.LayerNorm(cfg.n_embd, bias=cfg.bias)
self.ln_2 = nn.LayerNorm(cfg.n_embd, bias=cfg.bias)

# 변경 후
self.ln_1 = RMSNorm(cfg.n_embd)
self.ln_2 = RMSNorm(cfg.n_embd)
```

**`GPT.__init__`에서 교체 (transformer.py:94):**

```python
# 변경 전
self.ln_f = nn.LayerNorm(cfg.n_embd, bias=cfg.bias)

# 변경 후
self.ln_f = RMSNorm(cfg.n_embd)
```

**`ModelConfig`에서 `bias` 매개변수 불필요:**

```yaml
# config/model_config.yaml
model:
  bias: false  # RMSNorm은 편향 없음, 이미 false이므로 변경 불필요
```

---

## 4. 🟠 SwiGLU 활성함수 — GELU 교체 (품질 향상 + 속도 유지)

### 현재 코드

**파일:** `src/model/transformer.py:60-68`

```python
class FeedForward(nn.Module):
    def __init__(self, cfg: ModelConfig):
        self.fc1 = nn.Linear(cfg.n_embd, cfg.ffn_dim, bias=cfg.bias)
        self.fc2 = nn.Linear(cfg.ffn_dim, cfg.n_embd, bias=cfg.bias)
        self.dropout = nn.Dropout(cfg.dropout)

    def forward(self, x: torch.Tensor) -> torch.Tensor:
        return self.dropout(self.fc2(F.gelu(self.fc1(x))))  # ← 행 68
        #                           ↑ GELU 활성함수
```

### 문제

GELU: 단순한 비선형 함수, LLM에서는 SwiGLU가 더 우수함

```
GELU(x) = x × Φ(x)          (Φ: 정규분포 CDF)
SwiGLU(x, W, V) = Swish(xW) × (xV)   (두 경로의 곱)
```

**LLaMA, PaLM, GPT-4(추정)**: 모두 SwiGLU 또는 GeGLU 사용

### 교체 방법

**`src/model/transformer.py:60-68` 교체:**

```python
class FeedForward(nn.Module):
    def __init__(self, cfg: ModelConfig):
        super().__init__()
        # SwiGLU는 두 개의 상향 투영 필요 (fc1 → gate + up)
        # ffn_dim은 2/3로 줄여 총 매개변수 수 유지
        hidden = int(cfg.ffn_dim * 2 / 3)
        hidden = (hidden + 63) // 64 * 64  # 64의 배수로 정렬 (CUDA 효율)

        self.gate = nn.Linear(cfg.n_embd, hidden, bias=False)  # 게이트 경로
        self.up   = nn.Linear(cfg.n_embd, hidden, bias=False)  # 상향 경로
        self.down = nn.Linear(hidden, cfg.n_embd, bias=False)  # 하향 경로
        self.dropout = nn.Dropout(cfg.dropout)

    def forward(self, x: torch.Tensor) -> torch.Tensor:
        # SwiGLU: Swish(gate) × up
        return self.dropout(self.down(F.silu(self.gate(x)) * self.up(x)))
```

**config/model_config.yaml에서 ffn_dim 조정:**

```yaml
model:
  ffn_dim: 2730   # 512 × (8/3) = 1365 → ×2 = 2730 (SwiGLU용)
  # 기존 GELU: ffn_dim=2048 (n_embd × 4)
  # SwiGLU:   ffn_dim=2730 (n_embd × 8/3 × 2) → 같은 매개변수 수 유지
```

**예상 효과:** 언어 품질 향상, 속도는 동등 또는 미세 향상

---

## 5. 🟠 RoPE — 학습된 위치 삽입 교체 (문맥 길이 확장 가능)

### 현재 코드

**파일:** `src/model/transformer.py:91-92, 132-133`

```python
# transformer.py:91-92 — GPT.__init__
self.tok_emb = nn.Embedding(cfg.vocab_size, cfg.n_embd)
self.pos_emb = nn.Embedding(cfg.block_size, cfg.n_embd)  # ← 학습된 절대 위치

# transformer.py:132-133 — GPT.forward
pos = torch.arange(T, device=idx.device, dtype=torch.long)
x = self.drop(self.tok_emb(idx) + self.pos_emb(pos))      # ← 위치 더하기
```

### 문제

학습된 절대 위치 삽입의 한계:
```
학습 시 block_size=1024로 학습
→ 추론 시 1025번째 어표? 위치 삽입 없음 → 오류 발생
→ 더 긴 문맥으로 일반화 불가능
```

### RoPE란?

Rotary Position Embedding — 위치 정보를 Q, K 벡터에 회전 행렬로 직접 주입:

```
일반 주목: score(Q, K) = QᵀK
RoPE:      score(Q, K) = (RΘ,mQ)ᵀ(RΘ,nK)
           → 두 위치 m, n의 상대적 거리가 자동으로 인코딩됨
```

**장점:**
- 학습 시 1024 문맥 → 추론 시 2048, 4096까지 확장 가능 (추가 학습 없이)
- LLaMA 1/2/3, Mistral, Gemma 모두 사용

### 구현 방법

**`src/model/transformer.py`에 RoPE 추가:**

```python
def precompute_freqs_cis(dim: int, max_seq_len: int, theta: float = 10000.0) -> torch.Tensor:
    """RoPE 주파수 행렬 사전 계산."""
    freqs = 1.0 / (theta ** (torch.arange(0, dim, 2).float() / dim))
    t = torch.arange(max_seq_len)
    freqs = torch.outer(t, freqs)
    return torch.polar(torch.ones_like(freqs), freqs)  # 복소수 형태

def apply_rotary_emb(q: torch.Tensor, k: torch.Tensor, freqs_cis: torch.Tensor):
    """Q, K에 RoPE 적용."""
    q_ = torch.view_as_complex(q.float().reshape(*q.shape[:-1], -1, 2))
    k_ = torch.view_as_complex(k.float().reshape(*k.shape[:-1], -1, 2))
    q_ = torch.view_as_real(q_ * freqs_cis).flatten(-2)
    k_ = torch.view_as_real(k_ * freqs_cis).flatten(-2)
    return q_.type_as(q), k_.type_as(k)
```

**`GPT.__init__`에서 pos_emb 제거, freqs_cis 등록 (transformer.py:91-92):**

```python
# 변경 전
self.pos_emb = nn.Embedding(cfg.block_size, cfg.n_embd)

# 변경 후
freqs_cis = precompute_freqs_cis(cfg.n_embd // cfg.n_head, cfg.block_size * 2)
self.register_buffer("freqs_cis", freqs_cis)  # 학습 가능 매개변수 아님
```

---

## 6. 🟡 GQA/MQA — 다중 쿼리 주목 (추론 KV 기억 절감)

### 현재 코드

**파일:** `src/model/transformer.py:34`

```python
self.qkv = nn.Linear(cfg.n_embd, 3 * cfg.n_embd, bias=cfg.bias)
# Q: n_head개, K: n_head개, V: n_head개 ← 모두 같은 수
```

### 세 가지 방식 비교

```
MHA (현재): Q=8머리, K=8머리, V=8머리 → KV Cache 크기 = n_head × d × T
GQA:        Q=8머리, K=2머리, V=2머리 → KV Cache 크기 = 2 × d × T (4배 절감)
MQA:        Q=8머리, K=1머리, V=1머리 → KV Cache 크기 = 1 × d × T (8배 절감)
```

LLaMA-2 70B, Mistral 7B: GQA 사용 (품질↑ + KV 기억↓)

### 구현 방법

**`src/model/config.py:ModelConfig`에 추가:**

```python
@dataclass
class ModelConfig:
    n_layer: int = 8
    n_head: int = 8
    n_kv_head: int = 8   # ← 새로 추가: GQA를 위한 KV 머리 수 (n_head의 약수)
    # n_kv_head = n_head → MHA (기본)
    # n_kv_head = n_head // 4 → GQA
    # n_kv_head = 1 → MQA
```

**`src/model/transformer.py:CausalSelfAttention.__init__` 수정:**

```python
def __init__(self, cfg: ModelConfig):
    n_kv = getattr(cfg, 'n_kv_head', cfg.n_head)
    self.n_head = cfg.n_head
    self.n_kv_head = n_kv
    self.n_rep = cfg.n_head // n_kv  # 각 KV 머리를 몇 번 반복하는가

    self.q_proj = nn.Linear(cfg.n_embd, cfg.n_embd, bias=False)
    self.k_proj = nn.Linear(cfg.n_embd, n_kv * self.head_dim, bias=False)
    self.v_proj = nn.Linear(cfg.n_embd, n_kv * self.head_dim, bias=False)
```

**config.yaml에서 설정:**

```yaml
model:
  n_head: 8
  n_kv_head: 2   # GQA: KV 머리를 4배 줄임
```

---

## 7. 🟡 기울기 체크포인팅 — 대형 모형 기억 절감

### 현재 문제

**파일:** `src/train/train.py:135`

```python
for block in self.blocks:
    x = block(x)   # ← 각 블록의 중간 결과물(활성화)이 역전파용으로 기억에 쌓임
```

순방향 계산 시 역전파를 위해 모든 중간 결과를 기억에 유지합니다.  
모형이 클수록(층 수 많을수록) 기억 사용량이 선형으로 증가합니다.

### 체크포인팅이란?

```
기본: 모든 중간 결과 저장 → 기억 多, 계산 1회
체크포인팅: 중간 결과 저장 안 함 → 기억 少, 계산 1.33회 (역전파 시 재계산)
```

**절충:** 기억 절감 60~70%, 속도 저하 20~30%

### 구현 방법

**`src/train/train.py`에 import 추가 (상단):**

```python
from torch.utils.checkpoint import checkpoint
```

**`src/model/transformer.py:79-82` — Block.forward 수정:**

```python
# GPT.forward 내 루프 (train.py:135에서 호출됨)
# 변경 전
for block in self.blocks:
    x = block(x)

# 변경 후 (gradient checkpointing)
for block in self.blocks:
    if self.training and use_checkpoint:
        x = checkpoint(block, x, use_reentrant=False)
    else:
        x = block(x)
```

**`config/model_config.yaml`에 추가:**

```yaml
train:
  gradient_checkpointing: true   # 85M 이상 모형에서 권장
```

---

## 8. 🟡 자료 미리 읽기 — 학습 중 GPU 대기 시간 절감

### 현재 코드

**파일:** `src/data/dataset.py:39-53`

```python
def sample(self, batch_size, device):
    ix = np.random.randint(0, len(self.data) - self.block_size - 1, size=batch_size)
    x = np.stack([self.data[i:i+self.block_size].astype(np.int64) for i in ix])
    # ↑ CPU에서 numpy 처리 후 GPU로 전송 — GPU가 이 동안 대기
    x_t = x_t.pin_memory().to(device, non_blocking=True)  # ← 이미 비동기 전송 사용
```

현재 `pin_memory()`와 `non_blocking=True`가 이미 적용되어 있어 비교적 최적화됨.  
추가 개선: 다음 묶음을 별도 스레드에서 미리 준비하는 **프리페처**

### 구현 방법

**`src/data/dataset.py`에 프리페처 추가:**

```python
import threading
from queue import Queue

class DataPrefetcher:
    """별도 스레드에서 다음 묶음을 미리 준비하여 GPU 대기 시간 제거."""
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

## 9. 🟢 INT8/INT4 양자화 — 추론 전용, 기억 50~75% 절감

### 원리

```
bf16 (현재): 각 매개변수 = 16비트 = 2바이트
INT8:        각 매개변수 = 8비트 = 1바이트 → 기억 절반, 속도 2배
INT4:        각 매개변수 = 4비트 = 0.5바이트 → 기억 75% 절감, 속도 3~4배
```

**정밀도 손실:** INT4에서 미미한 품질 저하 (30M 모형에서는 거의 없음)

### 적용 방법 (추론만, bitsandbytes 라이브러리)

```powershell
pip install bitsandbytes
```

```python
# 추론 시 모형 로드 후 양자화 적용
import bitsandbytes as bnb

# INT8 양자화
model = model.to(torch.int8)  # 간단한 방법

# 또는 bitsandbytes의 NF4 (4비트, 고품질)
from bitsandbytes.nn import Linear4bit
# 각 Linear 층을 Linear4bit으로 교체
```

**더 쉬운 방법 — GGUF 형식으로 변환 (llama.cpp 사용):**

```powershell
pip install llama-cpp-python
# 체크포인트를 GGUF로 변환 후 CPU에서도 실행 가능
```

---

## 10. 🟢 추측 복호화 (Speculative Decoding) — 추론 2~3배

### 원리

```
일반 추론:
  대형 모형 → 어표1 → 어표2 → 어표3 → ...  (순차)

추측 복호화:
  소형 초안 모형 → [어표1, 어표2, 어표3, 어표4, 어표5] 빠르게 생성
  대형 검증 모형 → 5개를 한 번에 검증 (1번의 순방향 계산)
  → 맞은 것은 그대로, 틀린 곳부터 다시 생성
```

**전제조건:** 현재 모형(30M)보다 훨씬 작은 초안 모형(5M 등)이 필요  
→ 초안 모형을 별도로 학습하거나, 같은 구조의 소형 버전 사용

---

## 11. 🟢 MLA (DeepSeek식) — KV Cache 93% 절감

**파일:** `src/model/transformer.py:34-46`

DeepSeek-V2가 발명한 방식. K,V를 저차원 잠재 벡터로 압축:

```python
# 현재 MHA
self.qkv = nn.Linear(cfg.n_embd, 3 * cfg.n_embd, bias=False)
# KV Cache: n_head × d_head × seq_len = 8 × 64 × 1024 = 524,288 값

# MLA 방식
self.kv_compress = nn.Linear(cfg.n_embd, cfg.n_embd // 8, bias=False)  # 압축
self.kv_restore_k = nn.Linear(cfg.n_embd // 8, cfg.n_embd, bias=False)  # 복원
self.kv_restore_v = nn.Linear(cfg.n_embd // 8, cfg.n_embd, bias=False)
# KV Cache: n_embd//8 × seq_len = 64 × 1024 = 65,536 값 (8배 절감!)
```

---

## 종합 권장 적용 순서

### 즉시 적용 (설정 변경만, 코드 수정 불필요)

```yaml
# config/model_config.yaml
train:
  compile: true        # 10~20% 속도 향상
  dtype: bfloat16      # 이미 설정됨
```

**효과:** 학습 속도 즉시 10~20% 향상, 추론 속도 10~20% 향상

---

### 단기 적용 (코드 수정 필요, 1~3일)

1. **RMSNorm** (`transformer.py:74,76,94`) → LayerNorm 교체
2. **SwiGLU** (`transformer.py:68`) → GELU 교체
3. **KV Cache** (`generate.py:68-121`) → 가장 큰 추론 속도 향상

**효과:** 추론 속도 10~100배 향상 (KV Cache), 학습 속도 15~25% 향상

---

### 중기 적용 (1~2주)

4. **RoPE** (`transformer.py:91-92`) → 더 긴 문맥 처리 가능
5. **GQA** (`transformer.py:34`) → KV 기억 절감 (대형 모형에서 중요)
6. **기울기 체크포인팅** (`train.py`) → 85M 이상 모형 필수

---

### 장기 적용 (1개월+)

7. **INT4 양자화** → CPU에서도 모형 실행 가능
8. **추측 복호화** → 추론 2~3배
9. **MLA** → DeepSeek식 KV Cache 93% 절감
10. **MoE** → 같은 계산으로 더 많은 지식

---

## 현재 코드의 이미 최적화된 부분 (변경 불필요)

| 항목 | 위치 | 설명 |
|------|------|------|
| FlashAttention 자동 사용 | `transformer.py:49` | `F.scaled_dot_product_attention`이 RTX 5070에서 자동으로 FlashAttention 사용 |
| Fused AdamW | `transformer.py:169-171` | `fused=True`로 최적화된 AdamW 커널 사용 |
| 추론 시 마지막 위치만 계산 | `transformer.py:149` | `x[:, [-1], :]`로 불필요한 logit 계산 제거 |
| Pin Memory + Non-blocking | `dataset.py:48-49` | CPU→GPU 비동기 전송으로 대기 시간 최소화 |
| bf16 혼합 정밀도 | `train.py:97-100` | RTX 5070에서 최적 정밀도 자동 선택 |
| 잔차 투영 가중치 초기화 | `transformer.py:104-105` | GPT-2식 깊이 보정 초기화로 안정적 학습 |
