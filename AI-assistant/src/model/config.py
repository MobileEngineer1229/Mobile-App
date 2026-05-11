"""Model configuration object loaded from YAML."""

from __future__ import annotations

from dataclasses import dataclass, field
from pathlib import Path
from typing import Any

import yaml


@dataclass
class ModelConfig:
    n_layer: int = 8
    n_head: int = 8
    n_embd: int = 512
    ffn_dim: int = 2048
    block_size: int = 1024
    vocab_size: int = 16384
    dropout: float = 0.1
    bias: bool = False
    tie_weights: bool = True

    def num_params_estimate(self) -> int:
        emb = self.vocab_size * self.n_embd
        pos = self.block_size * self.n_embd
        per_block = (
            4 * self.n_embd * self.n_embd      # qkv + out proj
            + 2 * self.n_embd * self.ffn_dim   # FFN up + down
        )
        blocks = per_block * self.n_layer
        head = 0 if self.tie_weights else self.vocab_size * self.n_embd
        return emb + pos + blocks + head


@dataclass
class TrainConfig:
    out_dir: str = "checkpoints"
    batch_size: int = 16
    grad_accum_steps: int = 4
    learning_rate: float = 3.0e-4
    min_lr: float = 3.0e-5
    warmup_steps: int = 100
    max_steps: int = 5000
    weight_decay: float = 0.1
    beta1: float = 0.9
    beta2: float = 0.95
    grad_clip: float = 1.0
    eval_interval: int = 250
    eval_iters: int = 50
    log_interval: int = 10
    sample_interval: int = 500
    ckpt_keep_last: int = 3
    dtype: str = "bfloat16"
    compile: bool = False


@dataclass
class TokenizerConfig:
    vocab_size: int = 16384
    model_type: str = "bpe"
    character_coverage: float = 0.9995
    output_prefix: str = "tokenizer/dprk_sp"


@dataclass
class DataConfig:
    raw_dir: str = "data/raw"
    processed_dir: str = "data/processed"
    train_split: float = 0.95


@dataclass
class InferenceConfig:
    default_max_new_tokens: int = 256
    default_temperature: float = 0.9
    default_top_k: int = 50
    default_top_p: float = 0.95
    default_repetition_penalty: float = 1.15


@dataclass
class FullConfig:
    model: ModelConfig = field(default_factory=ModelConfig)
    train: TrainConfig = field(default_factory=TrainConfig)
    tokenizer: TokenizerConfig = field(default_factory=TokenizerConfig)
    data: DataConfig = field(default_factory=DataConfig)
    inference: InferenceConfig = field(default_factory=InferenceConfig)


def _build(cls, raw: dict[str, Any] | None):
    if raw is None:
        return cls()
    return cls(**{k: v for k, v in raw.items() if k in cls.__dataclass_fields__})


def load_config(path: str | Path) -> FullConfig:
    with open(path, "r", encoding="utf-8") as f:
        raw: dict[str, Any] = yaml.safe_load(f) or {}

    return FullConfig(
        model=_build(ModelConfig, raw.get("model")),
        train=_build(TrainConfig, raw.get("train")),
        tokenizer=_build(TokenizerConfig, raw.get("tokenizer")),
        data=_build(DataConfig, raw.get("data")),
        inference=_build(InferenceConfig, raw.get("inference")),
    )


def save_config(cfg: FullConfig, path: str | Path) -> None:
    raw = {
        "model": cfg.model.__dict__,
        "train": cfg.train.__dict__,
        "tokenizer": cfg.tokenizer.__dict__,
        "data": cfg.data.__dict__,
        "inference": cfg.inference.__dict__,
    }
    with open(path, "w", encoding="utf-8") as f:
        yaml.safe_dump(raw, f, sort_keys=False, allow_unicode=True)
