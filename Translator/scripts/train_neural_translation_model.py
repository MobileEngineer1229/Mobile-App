"""Legacy pretrained-model fine-tuning entry point.

The project now treats from-scratch training as the official path. This legacy
script is kept only as an explicitly gated experiment so pretrained NLLB/Marian
fine-tuning cannot be launched by accident.
"""

from __future__ import annotations

import argparse
import importlib.util
import json
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
NEURAL_DATA = ROOT / "data" / "training" / "neural"
PLAN_PATH = ROOT / "models" / "text" / "neural_training_plan.json"


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--base-model", default="facebook/nllb-200-distilled-600M")
    parser.add_argument("--direction", help="example: en__ko_kp")
    parser.add_argument("--output", default=str(ROOT / "models" / "text" / "neural"))
    parser.add_argument("--launch", action="store_true", help="actually run seq2seq fine-tuning")
    parser.add_argument("--epochs", type=float, default=1.0)
    parser.add_argument("--batch-size", type=int, default=2)
    parser.add_argument("--max-source-length", type=int, default=256)
    parser.add_argument("--max-target-length", type=int, default=256)
    parser.add_argument(
        "--allow-pretrained-finetune",
        action="store_true",
        help="dangerous legacy path: allow pretrained model fine-tuning",
    )
    args = parser.parse_args()

    if not args.allow_pretrained_finetune:
        plan = {
            "status": "disabled",
            "reason": "pretrained fine-tuning is not the official project path",
            "official_from_scratch_steps": [
                "python scripts\\train_tokenizer.py --vocab-size 16000",
                "python scripts\\train_transformer_from_scratch.py --direction ko_kp__en",
                "python scripts\\train_transformer_from_scratch.py --direction en__ko_kp",
            ],
        }
        PLAN_PATH.parent.mkdir(parents=True, exist_ok=True)
        PLAN_PATH.write_text(json.dumps(plan, ensure_ascii=False, indent=2), encoding="utf-8")
        print(json.dumps(plan, ensure_ascii=False, indent=2))
        return 0

    directions = _available_directions()
    selected = [args.direction] if args.direction else directions
    missing = [direction for direction in selected if direction not in directions]
    if missing:
        raise SystemExit(f"Missing training data for: {', '.join(missing)}")

    required_packages = ["torch", "transformers", "sentencepiece", "datasets", "accelerate", "sacrebleu", "protobuf"]
    missing_packages = [name for name in required_packages if not _has_package(name)]
    ready = not missing_packages
    plan = {
        "base_model": args.base_model,
        "directions": selected,
        "training_data": str(NEURAL_DATA.relative_to(ROOT)),
        "output": str(Path(args.output).resolve()),
        "dependencies_ready": ready,
        "required_python_packages": required_packages,
        "missing_python_packages": missing_packages,
        "status": "ready_to_train" if ready else "dependency_plan_written",
        "note": "Install dependencies in an offline wheelhouse before running heavy neural training.",
    }
    PLAN_PATH.parent.mkdir(parents=True, exist_ok=True)
    PLAN_PATH.write_text(json.dumps(plan, ensure_ascii=False, indent=2), encoding="utf-8")

    print(json.dumps(plan, ensure_ascii=False, indent=2))
    if not ready:
        print()
        print("Neural training was not launched because heavy ML packages are not installed.")
        print("The project is still learning through translation memory and local language models.")
        return 0
    if not args.launch:
        print()
        print("Neural dependencies are ready. Add --launch to start fine-tuning.")
        return 0

    for direction in selected:
        _train_direction(
            direction=direction,
            base_model=args.base_model,
            output_root=Path(args.output),
            epochs=args.epochs,
            batch_size=args.batch_size,
            max_source_length=args.max_source_length,
            max_target_length=args.max_target_length,
        )
    return 0


def _available_directions() -> list[str]:
    if not NEURAL_DATA.exists():
        return []
    return sorted(path.name for path in NEURAL_DATA.iterdir() if path.is_dir())


def _has_package(name: str) -> bool:
    import_name = {"protobuf": "google.protobuf"}.get(name, name)
    return importlib.util.find_spec(import_name) is not None


def _train_direction(
    *,
    direction: str,
    base_model: str,
    output_root: Path,
    epochs: float,
    batch_size: int,
    max_source_length: int,
    max_target_length: int,
) -> None:
    """Fine-tune one language direction with Transformers when available."""
    from datasets import Dataset
    from transformers import (
        AutoModelForSeq2SeqLM,
        AutoTokenizer,
        DataCollatorForSeq2Seq,
        Seq2SeqTrainer,
        Seq2SeqTrainingArguments,
    )

    source_language, target_language = direction.split("__", 1)
    direction_dir = NEURAL_DATA / direction
    train_rows = _read_jsonl(direction_dir / "train.jsonl")
    dev_rows = _read_jsonl(direction_dir / "dev.jsonl")
    if not train_rows:
        raise SystemExit(f"No train rows for {direction}")

    tokenizer = AutoTokenizer.from_pretrained(base_model)
    model = AutoModelForSeq2SeqLM.from_pretrained(base_model)
    _configure_tokenizer_language(tokenizer, source_language, target_language)

    train_dataset = Dataset.from_list(train_rows).map(
        lambda batch: _tokenize_batch(
            tokenizer,
            batch,
            max_source_length=max_source_length,
            max_target_length=max_target_length,
        ),
        batched=True,
        remove_columns=list(train_rows[0].keys()),
    )
    eval_dataset = None
    if dev_rows:
        eval_dataset = Dataset.from_list(dev_rows).map(
            lambda batch: _tokenize_batch(
                tokenizer,
                batch,
                max_source_length=max_source_length,
                max_target_length=max_target_length,
            ),
            batched=True,
            remove_columns=list(dev_rows[0].keys()),
        )

    output_dir = output_root / direction
    training_args = Seq2SeqTrainingArguments(
        output_dir=str(output_dir),
        num_train_epochs=epochs,
        per_device_train_batch_size=batch_size,
        per_device_eval_batch_size=batch_size,
        learning_rate=2e-5,
        save_strategy="epoch",
        evaluation_strategy="epoch" if eval_dataset is not None else "no",
        logging_steps=10,
        predict_with_generate=True,
        fp16=False,
    )
    trainer = Seq2SeqTrainer(
        model=model,
        args=training_args,
        train_dataset=train_dataset,
        eval_dataset=eval_dataset,
        tokenizer=tokenizer,
        data_collator=DataCollatorForSeq2Seq(tokenizer=tokenizer, model=model),
    )
    trainer.train()
    trainer.save_model(str(output_dir / "final"))
    tokenizer.save_pretrained(str(output_dir / "final"))


def _tokenize_batch(tokenizer: object, batch: dict[str, list[str]], *, max_source_length: int, max_target_length: int) -> dict[str, object]:
    inputs = tokenizer(
        batch["source_text"],
        max_length=max_source_length,
        truncation=True,
    )
    labels = tokenizer(
        text_target=batch["target_text"],
        max_length=max_target_length,
        truncation=True,
    )
    inputs["labels"] = labels["input_ids"]
    return inputs


def _configure_tokenizer_language(tokenizer: object, source_language: str, target_language: str) -> None:
    language_map = {
        "ko_kp": "kor_Hang",
        "en": "eng_Latn",
        "zh": "zho_Hans",
        "ru": "rus_Cyrl",
    }
    src = language_map.get(source_language)
    tgt = language_map.get(target_language)
    if src and hasattr(tokenizer, "src_lang"):
        tokenizer.src_lang = src
    if tgt and hasattr(tokenizer, "tgt_lang"):
        tokenizer.tgt_lang = tgt


def _read_jsonl(path: Path) -> list[dict[str, str]]:
    if not path.exists():
        return []
    rows = []
    for line in path.read_text(encoding="utf-8").splitlines():
        if line.strip():
            rows.append(json.loads(line))
    return rows


if __name__ == "__main__":
    raise SystemExit(main())
