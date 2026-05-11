"""Training entry point.

Usage:
    python -m src.train.train --config config/model_config.yaml
    python -m src.train.train --config config/model_config.yaml --resume checkpoints/ckpt_step001000.pt
"""

from __future__ import annotations

import argparse
import json
import sys
import time
from contextlib import nullcontext
from pathlib import Path

import torch
from torch.utils.tensorboard import SummaryWriter

ROOT = Path(__file__).resolve().parents[2]
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))

from src.data.dataset import TokenDataset  # noqa: E402
from src.model.config import load_config, save_config  # noqa: E402
from src.model.transformer import GPT  # noqa: E402
from src.tokenizer.tokenizer import load_tokenizer  # noqa: E402
from src.train.utils import (  # noqa: E402
    cleanup_old_checkpoints,
    cosine_lr,
    load_checkpoint,
    save_checkpoint,
)


DTYPE_MAP = {
    "bfloat16": torch.bfloat16,
    "float16": torch.float16,
    "float32": torch.float32,
}


def evaluate(model: GPT, val_ds: TokenDataset, batch_size: int, eval_iters: int, device: torch.device) -> float:
    model.eval()
    losses = torch.zeros(eval_iters, device=device)
    with torch.no_grad():
        for i in range(eval_iters):
            x, y = val_ds.sample(batch_size, device=device)
            _, loss = model(x, y)
            losses[i] = loss
    model.train()
    return float(losses.mean().item())


@torch.no_grad()
def sample_text(model: GPT, tokenizer, prompt: str, device: torch.device, max_new_tokens: int = 100) -> str:
    from src.inference.generate import generate
    model.eval()
    out = generate(
        model, tokenizer, prompt,
        max_new_tokens=max_new_tokens,
        temperature=0.9, top_k=50, top_p=0.95, repetition_penalty=1.15,
        device=device,
    )
    model.train()
    return out


def main() -> int:
    parser = argparse.ArgumentParser(description="Train GPT from scratch.")
    parser.add_argument("--config", type=str, default="config/model_config.yaml")
    parser.add_argument("--resume", type=str, default=None, help="Checkpoint path to resume from.")
    parser.add_argument("--device", type=str, default="auto", help="auto | cuda | cuda:0 | cpu")
    parser.add_argument("--max-steps", type=int, default=None, help="Override train.max_steps from YAML (smoke tests).")
    args = parser.parse_args()

    cfg = load_config(args.config)
    if args.max_steps is not None:
        cfg.train.max_steps = args.max_steps

    # ----- device -----
    if args.device == "auto":
        device = torch.device("cuda" if torch.cuda.is_available() else "cpu")
    else:
        device = torch.device(args.device)
    device_type = "cuda" if device.type == "cuda" else "cpu"
    print(f"[train] device: {device}")
    if device_type == "cuda":
        print(f"[train] gpu:    {torch.cuda.get_device_name(device)}")

    # ----- dtype / autocast -----
    dtype = DTYPE_MAP.get(cfg.train.dtype, torch.bfloat16)
    if device_type == "cuda" and dtype == torch.bfloat16 and not torch.cuda.is_bf16_supported():
        print("[train] bf16 not supported on this GPU; falling back to fp16.")
        dtype = torch.float16
    autocast_ctx = (
        torch.amp.autocast(device_type="cuda", dtype=dtype)
        if device_type == "cuda" and dtype != torch.float32
        else nullcontext()
    )
    use_grad_scaler = (device_type == "cuda" and dtype == torch.float16)
    scaler = torch.amp.GradScaler("cuda", enabled=use_grad_scaler)

    torch.manual_seed(42)
    if device_type == "cuda":
        torch.cuda.manual_seed_all(42)
        # TF32 is fine for matmuls; small accuracy hit, big speedup on 3090/4090/5070.
        torch.set_float32_matmul_precision("high")

    # ----- vocab size from preprocessed meta -----
    processed_dir = ROOT / cfg.data.processed_dir
    meta_path = processed_dir / "meta.json"
    if not meta_path.exists():
        print(f"[train] missing {meta_path}. Run scripts/02_preprocess_data.ps1 first.", file=sys.stderr)
        return 1
    with open(meta_path, "r", encoding="utf-8") as f:
        meta = json.load(f)
    cfg.model.vocab_size = int(meta["vocab_size"])

    # ----- model -----
    model = GPT(cfg.model).to(device)
    n_params = model.num_params()
    n_params_ne = model.num_params(exclude_embedding=True)
    print(f"[train] model params: {n_params/1e6:.2f}M (non-embedding: {n_params_ne/1e6:.2f}M)")
    print(f"[train] vocab_size:   {cfg.model.vocab_size:,}")
    print(f"[train] block_size:   {cfg.model.block_size}")

    optimizer = model.configure_optimizers(
        weight_decay=cfg.train.weight_decay,
        learning_rate=cfg.train.learning_rate,
        betas=(cfg.train.beta1, cfg.train.beta2),
        device_type=device_type,
    )

    start_step = 0
    best_val = float("inf")
    if args.resume:
        ckpt = load_checkpoint(args.resume, model, optimizer, map_location=device)
        start_step = int(ckpt.get("step", 0))
        print(f"[train] resumed from {args.resume} at step {start_step}")

    if cfg.train.compile:
        try:
            model = torch.compile(model)
            print("[train] torch.compile enabled")
        except Exception as e:
            print(f"[train] torch.compile failed ({e}); continuing without it.")

    # ----- data -----
    # Auto-reduce block_size if the smallest dataset shard is too short.
    min_tokens = min(meta["train_tokens"], meta["val_tokens"])
    if min_tokens <= cfg.model.block_size + 1:
        safe_block = max(16, min_tokens - 2)
        print(
            f"[train] WARNING: dataset has only {min_tokens:,} tokens in the smallest shard "
            f"but block_size={cfg.model.block_size}. Auto-reducing block_size to {safe_block}. "
            f"Add more training data for the full context window."
        )
        cfg.model.block_size = safe_block
        # Rebuild model with new block_size
        model = GPT(cfg.model).to(device)
        optimizer = model.configure_optimizers(
            weight_decay=cfg.train.weight_decay,
            learning_rate=cfg.train.learning_rate,
            betas=(cfg.train.beta1, cfg.train.beta2),
            device_type=device_type,
        )
        if args.resume:
            ckpt = load_checkpoint(args.resume, model, optimizer, map_location=device)
            start_step = int(ckpt.get("step", 0))
        if cfg.train.compile:
            try:
                model = torch.compile(model)
            except Exception:
                pass

    train_ds = TokenDataset(processed_dir / "train.bin", cfg.model.block_size)
    val_ds = TokenDataset(processed_dir / "val.bin", cfg.model.block_size)
    print(f"[train] train tokens: {meta['train_tokens']:,}")
    print(f"[train] val tokens:   {meta['val_tokens']:,}")

    # ----- tokenizer for sample generations during training -----
    tok_path = ROOT / "checkpoints" / f"{cfg.tokenizer.output_prefix}.model"
    tokenizer = load_tokenizer(tok_path)
    sample_prompt = "료리법:"

    # ----- output dir + tensorboard -----
    out_dir = ROOT / cfg.train.out_dir
    out_dir.mkdir(parents=True, exist_ok=True)
    tb_dir = out_dir / "runs" / time.strftime("%Y%m%d-%H%M%S")
    writer = SummaryWriter(tb_dir)
    print(f"[train] tensorboard: {tb_dir}")
    save_config(cfg, out_dir / "training_config.yaml")

    # ----- main loop -----
    micro_bs = cfg.train.batch_size
    grad_accum = max(1, cfg.train.grad_accum_steps)
    max_steps = cfg.train.max_steps

    t0 = time.time()
    model.train()
    for step in range(start_step, max_steps):
        # LR schedule
        lr = cosine_lr(
            step,
            warmup=cfg.train.warmup_steps,
            max_steps=max_steps,
            peak_lr=cfg.train.learning_rate,
            min_lr=cfg.train.min_lr,
        )
        for pg in optimizer.param_groups:
            pg["lr"] = lr

        # Forward / backward with gradient accumulation
        optimizer.zero_grad(set_to_none=True)
        accum_loss = 0.0
        for _ in range(grad_accum):
            x, y = train_ds.sample(micro_bs, device=device)
            with autocast_ctx:
                _, loss = model(x, y)
                loss = loss / grad_accum
            if use_grad_scaler:
                scaler.scale(loss).backward()
            else:
                loss.backward()
            accum_loss += loss.item() * grad_accum

        # Clip + step
        if use_grad_scaler:
            scaler.unscale_(optimizer)
        grad_norm = torch.nn.utils.clip_grad_norm_(model.parameters(), cfg.train.grad_clip)
        if use_grad_scaler:
            scaler.step(optimizer)
            scaler.update()
        else:
            optimizer.step()

        train_loss = accum_loss / grad_accum

        # Logging
        if step % cfg.train.log_interval == 0:
            elapsed = time.time() - t0
            tokens_seen = (step + 1) * micro_bs * grad_accum * cfg.model.block_size
            tps = tokens_seen / max(elapsed, 1e-6)
            print(
                f"[step {step:>6}] loss={train_loss:.4f}  lr={lr:.2e}  "
                f"|grad|={grad_norm:.2f}  tok/s={tps:,.0f}"
            )
            writer.add_scalar("train/loss", train_loss, step)
            writer.add_scalar("train/lr", lr, step)
            writer.add_scalar("train/grad_norm", float(grad_norm), step)

        # Validation
        if step > 0 and step % cfg.train.eval_interval == 0:
            val_loss = evaluate(model, val_ds, micro_bs, cfg.train.eval_iters, device)
            print(f"[step {step:>6}] val_loss={val_loss:.4f}")
            writer.add_scalar("val/loss", val_loss, step)
            if val_loss < best_val:
                best_val = val_loss
                save_checkpoint(
                    out_dir, step, model, optimizer,
                    cfg_dict={"model": cfg.model.__dict__, "train": cfg.train.__dict__},
                    tag="best",
                )
            save_checkpoint(
                out_dir, step, model, optimizer,
                cfg_dict={"model": cfg.model.__dict__, "train": cfg.train.__dict__},
            )
            cleanup_old_checkpoints(out_dir, keep_last=cfg.train.ckpt_keep_last)

        # Qualitative sample
        if step > 0 and step % cfg.train.sample_interval == 0:
            try:
                sample = sample_text(model, tokenizer, sample_prompt, device, max_new_tokens=80)
                print(f"[sample @ {step}]\n{sample}\n")
                writer.add_text("samples", sample, step)
            except Exception as e:
                print(f"[sample @ {step}] generation failed: {e}")

    # Final checkpoint
    save_checkpoint(
        out_dir, max_steps, model, optimizer,
        cfg_dict={"model": cfg.model.__dict__, "train": cfg.train.__dict__},
        tag="final",
    )
    writer.close()
    print(f"\n[train] done. Best val loss: {best_val:.4f}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
