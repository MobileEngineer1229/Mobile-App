"""GPT Decoder-only transformer. PyTorchWritten from scratch with.

【Beginner's Guide】
  This file is the heart of this project. Creating recipes and exercise guides in the Korean language
  The entire neural network architecture is defined here.

  class structure (stacked from bottom to top):
    CausalSelfAttention  ← attention mechanism: Calculate relationships between letters
    FeedForward          ← transmission network: Process the content of each letter
    Block                ← A floor unit that combines the above two into one
    GPT                  ← full mockup: Embedding + Block × 8 + LMhead

  Structure details:
    Prenormalized Transformer Block
    Multi-headed causal self-attention. (torch.nn.functional.scaled_dot_product_attention Lyon)
    Ampere+ / Blackwell GPUIn FlashAttentionThis is automatically applied
    GELU Forwarding network using activation function
    Learned location embeddings
    Optional weight sharing (Input Embedding ↔ output projection)

  This is "written from scratch" This is the reference implementation. transformers/peft Do not use external libraries, etc..
"""

from __future__ import annotations

import math

import torch
import torch.nn as nn
import torch.nn.functional as F

from .config import ModelConfig


class CausalSelfAttention(nn.Module):
    """Causal multi-headed self-attention mechanisms..

    【Beginner's Guide】
      "causal(Causal)" iran "You can only see the past, you can't see the future" means.
      This is because when creating letters, you should not preview letters that have not yet appeared..

      "multiple heads(Multi-Head)" This means that eight attention mechanisms operate simultaneously..
      Each head has a different kind of relationship(grammar, meaning, location, etc.)learn.

    processing flow:
      input x: (bundle, number of characters, 512)
        → QKV linear transformation: (bundle, number of characters, 1536)
        → Q, K, V separated into: each (bundle, number of characters, 512)
        → 8split into dog heads: (bundle, 8, number of characters, 64)
        → Calculating causal attention scores (FlashAttention auto apply)
        → 8combining dog heads: (bundle, number of characters, 512)
        → output projection: (bundle, number of characters, 512)
    """

    def __init__(self, cfg: ModelConfig):
        super().__init__()
        # n_embd must be divided by the number of heads (The dimensions of each head must be integers)
        assert cfg.n_embd % cfg.n_head == 0, "n_embd must be divisible by n_head"
        self.n_head = cfg.n_head                         # number of heads = 8
        self.n_embd = cfg.n_embd                         # Full embedding dimension = 512
        self.head_dim = cfg.n_embd // cfg.n_head         # Dimensions per head = 512 ÷ 8 = 64
        self.dropout = cfg.dropout                       # dropout rate (0 in training.1)

        # Q, K, V Linear transformation that computes three things at once
        # Input 512 dimensions → Output 1536 dimensions (= 512 × 3)
        # later divided into three pieces
        self.qkv = nn.Linear(cfg.n_embd, 3 * cfg.n_embd, bias=cfg.bias)

        # 8Combine the output of the dog head and convert it back to 512 dimensions
        self.proj = nn.Linear(cfg.n_embd, cfg.n_embd, bias=cfg.bias)

        # Residual dropout: Prevent overfitting by randomly zeroing out parts of the output
        self.resid_dropout = nn.Dropout(cfg.dropout)

    def forward(self, x: torch.Tensor) -> torch.Tensor:
        """Attention Mechanism Forward Computation.

        argument:
            x: input tensor, form (B, T, C)
               B = bundle size, T = number of characters, C = 512 (Embedding dimension)

        return value:
            y: output tensor, form (B, T, C) — Same form as input
        """
        B, T, C = x.shape  # B=bundle, T=number of characters, C=512

        # Q, K, V Calculate at once and divide into three pieces.
        # qkv: (B, T, 1536) → q, k, v: each (B, T, 512)
        qkv = self.qkv(x)
        q, k, v = qkv.split(self.n_embd, dim=-1)

        # 8Divide into dog heads and switch axes
        # Before conversion:  (B, T, 512)
        # view After:  (B, T, 8, 64)  ← 8dog head, 64 dimensions per head
        # transpose After: (B, 8, T, 64)  ← head axis forward
        # (Head axis must be in front for placement calculations)
        q = q.view(B, T, self.n_head, self.head_dim).transpose(1, 2)
        k = k.view(B, T, self.n_head, self.head_dim).transpose(1, 2)
        v = v.view(B, T, self.n_head, self.head_dim).transpose(1, 2)

        # Causal attention computation (FlashAttentionthis support GPUAutomatically applied from)
        # is_causal=True: Putting a triangular shade on the future location
        #   → Each letter is visible only to itself and the letters before it.
        # dropout_p: Dropout applies only during training (0 when inferred.0)
        y = F.scaled_dot_product_attention(
            q, k, v,
            dropout_p=self.dropout if self.training else 0.0,
            is_causal=True,
        )

        # 8putting the dog's head back together
        # transpose: (B, 8, T, 64) → (B, T, 8, 64)
        # contiguous: Make memory continuous (view i need)
        # view: (B, T, 8, 64) → (B, T, 512)  ← 8combining dog heads
        y = y.transpose(1, 2).contiguous().view(B, T, C)

        # Residual dropout after output projection
        y = self.resid_dropout(self.proj(y))
        return y


class FeedForward(nn.Module):
    """Delivery network by location (Position-wise Feed-Forward Network).

    【Beginner's Guide】
      attention mechanism "Which letters to refer to" If you decide,
      The transmission network is "What to think from that information" handle.

      512dimension → 2,048Expand it to 512 dimensions and then reduce it to 512 dimensions..
      Complex knowledge in the process of expansion(Recipes, exercise information)This is saved.

      GELU function: Negative inputs are almost ignored and positive numbers are almost passed through..
      If there is no non-linear function, even if multiple layers are stacked, it will be the same as a single linear transformation..
    """

    def __init__(self, cfg: ModelConfig):
        super().__init__()
        # 512 → 2,048: 4x expansion of thinking space
        self.fc1 = nn.Linear(cfg.n_embd, cfg.ffn_dim, bias=cfg.bias)
        # 2,048 → 512: Compressed to pass to the next layer
        self.fc2 = nn.Linear(cfg.ffn_dim, cfg.n_embd, bias=cfg.bias)
        # Dropout to prevent overfitting
        self.dropout = nn.Dropout(cfg.dropout)

    def forward(self, x: torch.Tensor) -> torch.Tensor:
        """Forwarding network forward calculation.

        Processing order: expansion(fc1) → GELU → zoom out(fc2) → dropout
        """
        # F.gelu: GELU Applying a nonlinear activation function
        # Select important information by suppressing negative values and passing positive values
        return self.dropout(self.fc2(F.gelu(self.fc1(x))))


class Block(nn.Module):
    """Transformer Block — entire structure on one floor.

    【Beginner's Guide】
      One block "Transformer one layer" is.
      This program stacks 8 of these blocks. (n_layer = 8).

      Inside each block:
        1. layer normalization → attention mechanism  (Calculating relationships between letters)
        2. layer normalization → transmission network   (Processing the content of each letter)

      Prenormalization(Pre-Norm): Normalization is performed before the attention mechanism..
        order: LayerNorm → Attention → Add residuals
        (Conventional method: Attention → LayerNorm. Prenormalization is more stable)

      Residual concatenation(Residual Connection):
        x = x + self.attn(self.ln_1(x))
        meaning: "original information + New information learned by attention mechanisms"
        effect: Solving the Vanishing Gradient Problem → 8Even floors can be trained stably
    """

    def __init__(self, cfg: ModelConfig):
        super().__init__()
        self.ln_1 = nn.LayerNorm(cfg.n_embd, bias=cfg.bias)  # Normalization before attention mechanism
        self.attn = CausalSelfAttention(cfg)                  # attention mechanism
        self.ln_2 = nn.LayerNorm(cfg.n_embd, bias=cfg.bias)  # Normalization in front of the forwarding network
        self.ffn = FeedForward(cfg)                           # transmission network

    def forward(self, x: torch.Tensor) -> torch.Tensor:
        """Block forward calculation.

        Residual connection pattern:
          x = x + attn(norm(x))   ← After processing the caution mechanism, it is added to the original value.
          x = x + ffn(norm(x))    ← After processing the transmission network, it is added to the original value.
        """
        # Attention mechanism after prenormalization → Residual concatenation
        # ln_1After normalizing it to , put it in the attention mechanism, the result is original xAdd to
        x = x + self.attn(self.ln_1(x))

        # Forwarding network after prenormalization → Residual concatenation
        # ln_2After normalizing it, put it into the transmission network., the result is original xAdd to
        x = x + self.ffn(self.ln_2(x))
        return x


class GPT(nn.Module):
    """GPT Style decoder-specific language model.

    【Beginner's Guide】
      This class contains the entire transformer.

      composition:
        tok_emb   : Token Embedding — word number → 512dimension vector
        pos_emb   : Location Embedding — location number → 512dimension vector
        drop      : Input dropout
        blocks    : 8dog Block (Transformer layer)
        ln_f      : Final layer normalization
        lm_head   : language model head — 512dimension → 16,384dog odds

      Forward calculation flow:
        letter number list
        → Token Embedding + Location Embedding
        → dropout
        → Block × 8
        → Final layer normalization
        → LM head → Next letter probability

      weight sharing (tie_weights=True):
        lm_head.weight = tok_emb.weight
        reason: LM When the head and input embeddings share the same space, the parameters are reduced.
              Performance is maintained or improved.
    """

    def __init__(self, cfg: ModelConfig):
        super().__init__()
        self.cfg = cfg  # Save the settings for future reference

        # Token Embedding Table: (Vocabulary count, Embedding dimension) = (16384, 512)
        # When you enter a word number, the corresponding 512 number vector is returned.
        self.tok_emb = nn.Embedding(cfg.vocab_size, cfg.n_embd)

        # Position Embedding Table: (maximum position, Embedding dimension) = (1024, 512)
        # When you enter a location number, the corresponding 512 number vector is returned.
        self.pos_emb = nn.Embedding(cfg.block_size, cfg.n_embd)

        # Input dropout: Prevent overfitting by randomly setting some inputs to 0 during training
        self.drop = nn.Dropout(cfg.dropout)

        # List of 8 Transformer Blocks in order
        # ModuleList: PyTorchautomatically tracks the parameters in this list
        self.blocks = nn.ModuleList([Block(cfg) for _ in range(cfg.n_layer)])

        # Last layer normalization: Apply after passing all blocks
        self.ln_f = nn.LayerNorm(cfg.n_embd, bias=cfg.bias)

        # language model head: 512dimension → vocabulary size(16,384)
        # bias=False: No bias in language model head (GPT-2 custom)
        self.lm_head = nn.Linear(cfg.n_embd, cfg.vocab_size, bias=False)

        # weight sharing: Input embedding and output projection use the same weight matrix
        # This reduces the number of parameters by approximately 8 million while maintaining performance.
        if cfg.tie_weights:
            self.lm_head.weight = self.tok_emb.weight

        # Initialize all weights (below _init_weights Note)
        self.apply(self._init_weights)

        # Apply special initialization to residual projection (GPT-2 way)
        # As the layer gets deeper, the contribution of the residual path is adjusted to decrease.
        # std = 0.02 / √(2 × number of floors) Projective residuals as(proj, fc2)initialize
        for name, p in self.named_parameters():
            if name.endswith("proj.weight") or name.endswith("fc2.weight"):
                nn.init.normal_(p, mean=0.0, std=0.02 / math.sqrt(2 * cfg.n_layer))

    def _init_weights(self, module: nn.Module) -> None:
        """Weight initialization function.

        linear transformation layer: average 0, standard deviation 0.02 Initialized with a normal distribution of
        Embedding layer: Initialize to the same normal distribution
        bias: 0initialized to

        0.02Reason for writing: GPT-2 Values verified in the paper.
        If it is too big, initial training will be unstable., If it is too small, learning is slow..
        """
        if isinstance(module, nn.Linear):
            nn.init.normal_(module.weight, mean=0.0, std=0.02)
            if module.bias is not None:
                nn.init.zeros_(module.bias)  # Bias starts at 0
        elif isinstance(module, nn.Embedding):
            nn.init.normal_(module.weight, mean=0.0, std=0.02)

    def num_params(self, exclude_embedding: bool = False) -> int:
        """Returns the total number of parameters.

        argument:
            exclude_embedding: TrueIf it returns the number excluding the embedding parameter..
                               Embedding is real "calculation" Because it is not a parameter
                               Often excluded when comparing model sizes.
        """
        n = sum(p.numel() for p in self.parameters())  # Total number of parameters
        if exclude_embedding:
            n -= self.tok_emb.weight.numel()  # Excluding token embedding
            n -= self.pos_emb.weight.numel()  # Excluding location embeddings
        return n

    def forward(
        self,
        idx: torch.Tensor,
        targets: torch.Tensor | None = None,
    ) -> tuple[torch.Tensor, torch.Tensor | None]:
        """GPT forward calculation.

        【Beginner's Guide】
          This function actually "letter number list"take it "Next letter probability"It is a function that emits.
          The behavior is slightly different during training and inference..

        argument:
            idx:     List of input character numbers, form (B, T)
                     B = bundle size, T = Current number of characters
            targets: Target letter number list, form (B, T) — Provided during training only
                     targets[i][j] = idx[i][j+1] (next letter)

        return value:
            logits: next letter score, form (B, T, vocab_size) or (B, 1, vocab_size)
            loss:   Cross entropy loss (When training), None (When inferring)
        """
        B, T = idx.shape
        # T(Current number of characters)go block_size(Up to 1024)Must not exceed
        assert T <= self.cfg.block_size, (
            f"number of characters {T}is the maximum context length {self.cfg.block_size}exceeds"
        )

        # Create location number: [0, 1, 2, ..., T-1]
        # Indicates the position number of each letter
        pos = torch.arange(T, device=idx.device, dtype=torch.long)

        # Token Embedding + Location Embedding → input expression
        # tok_emb(idx): (B, T, 512) — Meaning of each letter: Vector
        # pos_emb(pos): (T, 512)    — Location vector for each location (BAxis auto-scaling)
        # If you add two: (B, T, 512)  — meaning + Expression combining location information
        x = self.drop(self.tok_emb(idx) + self.pos_emb(pos))

        # 8Pass the dog blocks one after another
        # As each block passes, the relationships between letters become more precise.
        for block in self.blocks:
            x = block(x)

        # Final layer normalization
        x = self.ln_f(x)  # (B, T, 512)

        if targets is not None:
            # When training: Predict next letter at any position and calculate loss
            logits = self.lm_head(x)  # (B, T, 16384)
            # Cross entropy loss: How different the predicted probability is from the actual next letter
            # -100is the position to ignore (PAD Used for tokens, etc.)
            loss = F.cross_entropy(
                logits.view(-1, logits.size(-1)),  # (B×T, 16384)
                targets.view(-1),                  # (B×T,)
                ignore_index=-100,
            )
            return logits, loss

        # When inferring: Calculate only the output of the last position (Manifest Memory Savings)
        # x[:, [-1], :] = Select only the vector of the last letter
        logits = self.lm_head(x[:, [-1], :])  # (B, 1, 16384)
        return logits, None

    def configure_optimizers(
        self,
        weight_decay: float,
        learning_rate: float,
        betas: tuple[float, float],
        device_type: str,
    ):
        """AdamW Set optimizer and return.

        【Beginner's Guide】
          AdamWis an optimization algorithm commonly used in model training..
          "weight attenuation(weight decay)" Prevent overfitting, including.

          Reason for dividing parameters into two groups:
            - 2D ideal matrix(weight): Apply weighted attenuation → Avoid overfitting
            - 1D vector(bias, LayerNorm parameters): No attenuation → It gets worse when attenuated.

          fused=True: CUDAperforms quickly by combining optimizer calculations in one step
        """
        # requires_grad=True Only parameters are optimized for
        params = [p for p in self.parameters() if p.requires_grad]

        # 2D more than = matrix type weights (Apply weighted attenuation)
        decay = [p for p in params if p.dim() >= 2]
        # 1D = bias, LayerNorm gain/bias (No weight attenuation)
        no_decay = [p for p in params if p.dim() < 2]

        groups = [
            {"params": decay, "weight_decay": weight_decay},    # Attenuation O
            {"params": no_decay, "weight_decay": 0.0},          # Attenuation X
        ]

        # PyTorch 2.xof fused AdamW: CUDAis faster at
        # In versions that do not support it, AdamWreplaced with
        try:
            opt = torch.optim.AdamW(
                groups, lr=learning_rate, betas=betas,
                fused=(device_type == "cuda"),  # CUDAonly when fused Apply
            )
        except TypeError:
            # fused Old version not supporting arguments PyTorch
            opt = torch.optim.AdamW(groups, lr=learning_rate, betas=betas)
        return opt
