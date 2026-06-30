"""GPT 방식의 디코더 전용 트랜스포메르. PyTorch로 처음부터 작성함.

【초보자 안내】
  이 파일이 이 프로젝트의 심장부다. 조선말 료리법과 운동 안내를 생성하는
  신경망 구조 전체가 여기 정의되어 있다.

  클라스 구조 (아래에서 위로 쌓임):
    CausalSelfAttention  ← 주의 기제: 글자들 사이의 관계를 계산
    FeedForward          ← 전달 망: 각 글자의 내용을 처리
    Block                ← 위 두 가지를 하나로 묶은 층 단위
    GPT                  ← 전체 모형: 임베딩 + Block × 8 + LM머리

  구조 상세:
    사전정규화 트랜스포메르 블로크
    다중 머리 인과적 자기 주의 (torch.nn.functional.scaled_dot_product_attention 리용)
    Ampere+ / Blackwell GPU에서는 FlashAttention이 자동 적용됨
    GELU 활성화 함수를 쓰는 전달 망
    학습된 위치 임베딩
    선택적 가중치 공유 (입력 임베딩 ↔ 출력 사영)

  이것은 "처음부터 작성한" 참고 구현이다. transformers/peft 등 외부 라이브러리를 쓰지 않는다.
"""

from __future__ import annotations

import math

import torch
import torch.nn as nn
import torch.nn.functional as F

from .config import ModelConfig


class CausalSelfAttention(nn.Module):
    """인과적 다중 머리 자기 주의 기제.

    【초보자 안내】
      "인과적(Causal)" 이란 "과거만 볼 수 있고 미래는 볼 수 없다" 는 뜻이다.
      글자 생성 시 아직 나오지 않은 글자를 미리 보면 안 되기 때문이다.

      "다중 머리(Multi-Head)" 란 8개의 주의 기제를 동시에 돌린다는 뜻이다.
      각 머리가 서로 다른 종류의 관계(문법, 의미, 위치 등)를 학습한다.

    처리 흐름:
      입력 x: (묶음, 글자수, 512)
        → QKV 선형 변환: (묶음, 글자수, 1536)
        → Q, K, V 로 분리: 각각 (묶음, 글자수, 512)
        → 8개 머리로 나누기: (묶음, 8, 글자수, 64)
        → 인과적 주의 점수 계산 (FlashAttention 자동 적용)
        → 8개 머리 합치기: (묶음, 글자수, 512)
        → 출력 사영: (묶음, 글자수, 512)
    """

    def __init__(self, cfg: ModelConfig):
        super().__init__()
        # n_embd 는 머리 수로 나뉘어야 한다 (각 머리의 차원이 정수여야 함)
        assert cfg.n_embd % cfg.n_head == 0, "n_embd must be divisible by n_head"
        self.n_head = cfg.n_head                         # 머리 수 = 8
        self.n_embd = cfg.n_embd                         # 전체 임베딩 차원 = 512
        self.head_dim = cfg.n_embd // cfg.n_head         # 머리당 차원 = 512 ÷ 8 = 64
        self.dropout = cfg.dropout                       # 드롭아웃 비율 (훈련 시 0.1)

        # Q, K, V 세 가지를 한 번에 계산하는 선형 변환
        # 입력 512차원 → 출력 1536차원 (= 512 × 3)
        # 나중에 세 조각으로 나눔
        self.qkv = nn.Linear(cfg.n_embd, 3 * cfg.n_embd, bias=cfg.bias)

        # 8개 머리의 출력을 합친 뒤 다시 512차원으로 변환
        self.proj = nn.Linear(cfg.n_embd, cfg.n_embd, bias=cfg.bias)

        # 잔차 드롭아웃: 출력의 일부를 무작위로 0으로 만들어 과적합 방지
        self.resid_dropout = nn.Dropout(cfg.dropout)

    def forward(self, x: torch.Tensor) -> torch.Tensor:
        """주의 기제 순방향 계산.

        인수:
            x: 입력 텐서, 형태 (B, T, C)
               B = 묶음 크기, T = 글자 수, C = 512 (임베딩 차원)

        반환값:
            y: 출력 텐서, 형태 (B, T, C) — 입력과 같은 형태
        """
        B, T, C = x.shape  # B=묶음, T=글자수, C=512

        # Q, K, V 를 한 번에 계산한 뒤 세 조각으로 나눔
        # qkv: (B, T, 1536) → q, k, v: 각각 (B, T, 512)
        qkv = self.qkv(x)
        q, k, v = qkv.split(self.n_embd, dim=-1)

        # 8개 머리로 나누고 축을 바꿈
        # 변환 전:  (B, T, 512)
        # view 후:  (B, T, 8, 64)  ← 8개 머리, 머리당 64차원
        # transpose 후: (B, 8, T, 64)  ← 머리 축을 앞으로
        # (배치 계산을 위해 머리 축이 앞에 있어야 함)
        q = q.view(B, T, self.n_head, self.head_dim).transpose(1, 2)
        k = k.view(B, T, self.n_head, self.head_dim).transpose(1, 2)
        v = v.view(B, T, self.n_head, self.head_dim).transpose(1, 2)

        # 인과적 주의 계산 (FlashAttention이 지원 GPU에서 자동 적용됨)
        # is_causal=True: 미래 위치에 삼각형 가리개를 씌움
        #   → 각 글자는 자신과 그 이전 글자들만 볼 수 있음
        # dropout_p: 훈련 시에만 드롭아웃 적용 (추론 시 0.0)
        y = F.scaled_dot_product_attention(
            q, k, v,
            dropout_p=self.dropout if self.training else 0.0,
            is_causal=True,
        )

        # 8개 머리를 다시 합침
        # transpose: (B, 8, T, 64) → (B, T, 8, 64)
        # contiguous: 메모리를 연속으로 만듦 (view 전 필요)
        # view: (B, T, 8, 64) → (B, T, 512)  ← 8개 머리 합치기
        y = y.transpose(1, 2).contiguous().view(B, T, C)

        # 출력 사영 후 잔차 드롭아웃
        y = self.resid_dropout(self.proj(y))
        return y


class FeedForward(nn.Module):
    """위치별 전달 망 (Position-wise Feed-Forward Network).

    【초보자 안내】
      주의 기제가 "어떤 글자를 참고할지" 를 결정한다면,
      전달 망은 "그 참고한 정보로 무엇을 생각할지" 를 처리한다.

      512차원 → 2,048차원으로 확장했다가 다시 512차원으로 줄인다.
      확장 과정에서 복잡한 지식(료리법, 운동 정보)이 저장된다.

      GELU 함수: 음수 입력은 거의 무시하고 양수는 거의 그대로 통과시킴.
      비선형 함수가 없으면 여러 층을 쌓아도 선형 변환 하나와 같아짐.
    """

    def __init__(self, cfg: ModelConfig):
        super().__init__()
        # 512 → 2,048: 사고 공간 4배 확장
        self.fc1 = nn.Linear(cfg.n_embd, cfg.ffn_dim, bias=cfg.bias)
        # 2,048 → 512: 다음 층으로 전달하기 위해 압축
        self.fc2 = nn.Linear(cfg.ffn_dim, cfg.n_embd, bias=cfg.bias)
        # 과적합 방지용 드롭아웃
        self.dropout = nn.Dropout(cfg.dropout)

    def forward(self, x: torch.Tensor) -> torch.Tensor:
        """전달 망 순방향 계산.

        처리 순서: 확장(fc1) → GELU → 축소(fc2) → 드롭아웃
        """
        # F.gelu: GELU 비선형 활성화 함수 적용
        # 음수 값은 억제하고 양수 값은 통과시켜 중요한 정보를 선별
        return self.dropout(self.fc2(F.gelu(self.fc1(x))))


class Block(nn.Module):
    """트랜스포메르 블로크 — 한 층의 전체 구조.

    【초보자 안내】
      블로크 하나가 "트랜스포메르 한 층" 이다.
      이 프로그람은 이 블로크를 8개 쌓는다 (n_layer = 8).

      각 블로크 안에는:
        1. 층 정규화 → 주의 기제  (글자들 사이의 관계 계산)
        2. 층 정규화 → 전달 망   (각 글자의 내용 처리)

      사전정규화(Pre-Norm): 정규화를 주의 기제 앞에 수행한다.
        순서: LayerNorm → Attention → 잔차 추가
        (기존 방식: Attention → LayerNorm. 사전정규화가 더 안정적임)

      잔차 연결(Residual Connection):
        x = x + self.attn(self.ln_1(x))
        의미: "원래 정보 + 주의 기제가 학습한 새로운 정보"
        효과: 기울기 소실 문제 해결 → 8층도 안정적으로 훈련 가능
    """

    def __init__(self, cfg: ModelConfig):
        super().__init__()
        self.ln_1 = nn.LayerNorm(cfg.n_embd, bias=cfg.bias)  # 주의 기제 앞 정규화
        self.attn = CausalSelfAttention(cfg)                  # 주의 기제
        self.ln_2 = nn.LayerNorm(cfg.n_embd, bias=cfg.bias)  # 전달 망 앞 정규화
        self.ffn = FeedForward(cfg)                           # 전달 망

    def forward(self, x: torch.Tensor) -> torch.Tensor:
        """블로크 순방향 계산.

        잔차 연결 패턴:
          x = x + attn(norm(x))   ← 주의 기제 처리 후 원래 값에 더함
          x = x + ffn(norm(x))    ← 전달 망 처리 후 원래 값에 더함
        """
        # 사전정규화 후 주의 기제 → 잔차 연결
        # ln_1으로 정규화한 후 주의 기제에 넣고, 결과를 원래 x에 더함
        x = x + self.attn(self.ln_1(x))

        # 사전정규화 후 전달 망 → 잔차 연결
        # ln_2로 정규화한 후 전달 망에 넣고, 결과를 원래 x에 더함
        x = x + self.ffn(self.ln_2(x))
        return x


class GPT(nn.Module):
    """GPT 스타일 디코더 전용 언어 모형.

    【초보자 안내】
      이 클라스가 전체 트랜스포메르를 담고 있다.

      구성:
        tok_emb   : 토큰 임베딩 — 단어 번호 → 512차원 벡토르
        pos_emb   : 위치 임베딩 — 위치 번호 → 512차원 벡토르
        drop      : 입력 드롭아웃
        blocks    : 8개의 Block (트랜스포메르 층)
        ln_f      : 최종 층 정규화
        lm_head   : 언어 모형 머리 — 512차원 → 16,384개 확률

      순방향 계산 흐름:
        글자 번호 목록
        → 토큰 임베딩 + 위치 임베딩
        → 드롭아웃
        → Block × 8
        → 최종 층 정규화
        → LM 머리 → 다음 글자 확률

      가중치 공유 (tie_weights=True):
        lm_head.weight = tok_emb.weight
        이유: LM 머리와 입력 임베딩이 같은 공간을 공유하면 매개변수가 줄면서
              성능은 유지되거나 향상된다.
    """

    def __init__(self, cfg: ModelConfig):
        super().__init__()
        self.cfg = cfg  # 설정값을 저장해두어 나중에 참고함

        # 토큰 임베딩 테이블: (어휘 수, 임베딩 차원) = (16384, 512)
        # 단어 번호를 입력하면 해당하는 512개 숫자 벡토르를 돌려줌
        self.tok_emb = nn.Embedding(cfg.vocab_size, cfg.n_embd)

        # 위치 임베딩 테이블: (최대 위치, 임베딩 차원) = (1024, 512)
        # 위치 번호를 입력하면 해당하는 512개 숫자 벡토르를 돌려줌
        self.pos_emb = nn.Embedding(cfg.block_size, cfg.n_embd)

        # 입력 드롭아웃: 훈련 시 일부 입력을 무작위로 0으로 만들어 과적합 방지
        self.drop = nn.Dropout(cfg.dropout)

        # 트랜스포메르 블로크 8개를 순서대로 담은 목록
        # ModuleList: PyTorch가 이 목록 안의 매개변수를 자동으로 추적함
        self.blocks = nn.ModuleList([Block(cfg) for _ in range(cfg.n_layer)])

        # 마지막 층 정규화: 모든 블로크를 통과한 후 적용
        self.ln_f = nn.LayerNorm(cfg.n_embd, bias=cfg.bias)

        # 언어 모형 머리: 512차원 → 어휘 크기(16,384)
        # bias=False: 언어 모형 머리에는 편향을 쓰지 않음 (GPT-2 관례)
        self.lm_head = nn.Linear(cfg.n_embd, cfg.vocab_size, bias=False)

        # 가중치 공유: 입력 임베딩과 출력 사영이 같은 가중치 행렬을 씀
        # 이렇게 하면 매개변수 수가 약 800만 개 줄면서 성능은 유지됨
        if cfg.tie_weights:
            self.lm_head.weight = self.tok_emb.weight

        # 모든 가중치를 초기화 (아래 _init_weights 참고)
        self.apply(self._init_weights)

        # 잔차 사영에 특별한 초기화 적용 (GPT-2 방식)
        # 층이 깊어질수록 잔차 경로의 기여가 작아지게 조정
        # std = 0.02 / √(2 × 층 수) 로 잔차 사영(proj, fc2)을 초기화
        for name, p in self.named_parameters():
            if name.endswith("proj.weight") or name.endswith("fc2.weight"):
                nn.init.normal_(p, mean=0.0, std=0.02 / math.sqrt(2 * cfg.n_layer))

    def _init_weights(self, module: nn.Module) -> None:
        """가중치 초기화 함수.

        선형 변환 층: 평균 0, 표준편차 0.02 의 정규분포로 초기화
        임베딩 층: 동일하게 정규분포로 초기화
        편향: 0으로 초기화

        0.02를 쓰는 리유: GPT-2 논문에서 검증된 값.
        너무 크면 초반 훈련이 불안정하고, 너무 작으면 학습이 느리다.
        """
        if isinstance(module, nn.Linear):
            nn.init.normal_(module.weight, mean=0.0, std=0.02)
            if module.bias is not None:
                nn.init.zeros_(module.bias)  # 편향은 0으로 시작
        elif isinstance(module, nn.Embedding):
            nn.init.normal_(module.weight, mean=0.0, std=0.02)

    def num_params(self, exclude_embedding: bool = False) -> int:
        """전체 매개변수 수를 반환.

        인수:
            exclude_embedding: True이면 임베딩 매개변수를 제외한 수를 반환.
                               임베딩은 실제 "계산" 매개변수가 아니므로
                               모형 크기 비교 시 종종 제외함.
        """
        n = sum(p.numel() for p in self.parameters())  # 전체 매개변수 수
        if exclude_embedding:
            n -= self.tok_emb.weight.numel()  # 토큰 임베딩 제외
            n -= self.pos_emb.weight.numel()  # 위치 임베딩 제외
        return n

    def forward(
        self,
        idx: torch.Tensor,
        targets: torch.Tensor | None = None,
    ) -> tuple[torch.Tensor, torch.Tensor | None]:
        """GPT 순방향 계산.

        【초보자 안내】
          이 함수가 실제로 "글자 번호 목록"을 받아 "다음 글자 확률"을 내보내는 함수다.
          훈련 시와 추론 시의 동작이 약간 다르다.

        인수:
            idx:     입력 글자 번호 목록, 형태 (B, T)
                     B = 묶음 크기, T = 현재 글자 수
            targets: 목표 글자 번호 목록, 형태 (B, T) — 훈련 시에만 제공
                     targets[i][j] = idx[i][j+1] (다음 글자)

        반환값:
            logits: 다음 글자 점수, 형태 (B, T, vocab_size) 또는 (B, 1, vocab_size)
            loss:   교차 엔트로피 손실 (훈련 시), None (추론 시)
        """
        B, T = idx.shape
        # T(현재 글자 수)가 block_size(최대 1024)를 넘으면 안 됨
        assert T <= self.cfg.block_size, (
            f"글자 수 {T}가 최대 문맥 길이 {self.cfg.block_size}를 초과함"
        )

        # 위치 번호 생성: [0, 1, 2, ..., T-1]
        # 각 글자의 위치 번호를 나타냄
        pos = torch.arange(T, device=idx.device, dtype=torch.long)

        # 토큰 임베딩 + 위치 임베딩 → 입력 표현
        # tok_emb(idx): (B, T, 512) — 각 글자의 의미 벡토르
        # pos_emb(pos): (T, 512)    — 각 위치의 위치 벡토르 (B축 자동 확장)
        # 둘을 더하면: (B, T, 512)  — 의미 + 위치 정보가 합쳐진 표현
        x = self.drop(self.tok_emb(idx) + self.pos_emb(pos))

        # 8개 블로크를 차례로 통과
        # 각 블로크를 지날수록 글자들 사이의 관계가 더 정교하게 파악됨
        for block in self.blocks:
            x = block(x)

        # 최종 층 정규화
        x = self.ln_f(x)  # (B, T, 512)

        if targets is not None:
            # 훈련 시: 모든 위치에서 다음 글자를 예측하고 손실 계산
            logits = self.lm_head(x)  # (B, T, 16384)
            # 교차 엔트로피 손실: 예측 확률이 실제 다음 글자와 얼마나 다른지
            # -100은 무시할 위치 (PAD 토큰 등에 씀)
            loss = F.cross_entropy(
                logits.view(-1, logits.size(-1)),  # (B×T, 16384)
                targets.view(-1),                  # (B×T,)
                ignore_index=-100,
            )
            return logits, loss

        # 추론 시: 마지막 위치의 출력만 계산 (현현기억기 절약)
        # x[:, [-1], :] = 마지막 글자의 벡토르만 선택
        logits = self.lm_head(x[:, [-1], :])  # (B, 1, 16384)
        return logits, None

    def configure_optimizers(
        self,
        weight_decay: float,
        learning_rate: float,
        betas: tuple[float, float],
        device_type: str,
    ):
        """AdamW 최적화기를 설정하고 반환.

        【초보자 안내】
          AdamW는 모형 훈련에 흔히 쓰이는 최적화 알고리듬이다.
          "가중치 감쇠(weight decay)" 를 포함하여 과적합을 방지한다.

          매개변수를 두 그룹으로 나누는 리유:
            - 2D 이상 행렬(가중치): 가중치 감쇠 적용 → 과적합 방지
            - 1D 벡토르(편향, LayerNorm 파라미터): 감쇠 없음 → 감쇠하면 오히려 나빠짐

          fused=True: CUDA에서 최적화기 계산을 한 번에 합쳐서 빠르게 수행
        """
        # requires_grad=True 인 매개변수만 최적화 대상
        params = [p for p in self.parameters() if p.requires_grad]

        # 2D 이상 = 행렬 형태 가중치 (가중치 감쇠 적용)
        decay = [p for p in params if p.dim() >= 2]
        # 1D = 편향, LayerNorm 게인/편향 (가중치 감쇠 없음)
        no_decay = [p for p in params if p.dim() < 2]

        groups = [
            {"params": decay, "weight_decay": weight_decay},    # 감쇠 O
            {"params": no_decay, "weight_decay": 0.0},          # 감쇠 X
        ]

        # PyTorch 2.x의 fused AdamW: CUDA에서 더 빠름
        # 지원하지 않는 버전에서는 일반 AdamW로 대체
        try:
            opt = torch.optim.AdamW(
                groups, lr=learning_rate, betas=betas,
                fused=(device_type == "cuda"),  # CUDA일 때만 fused 적용
            )
        except TypeError:
            # fused 인수를 지원하지 않는 구 버전 PyTorch
            opt = torch.optim.AdamW(groups, lr=learning_rate, betas=betas)
        return opt
