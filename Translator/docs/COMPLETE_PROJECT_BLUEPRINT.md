# Complete Project Blueprint

This is the target architecture for a complete offline DPRK Korean translator.

## Pipeline

```text
raw URL / paired URL / manual sentence pair
  -> extraction
  -> boilerplate cleanup
  -> sentence splitting
  -> alignment candidates
  -> human approval
  -> translation memory
  -> train/dev/test splits
  -> neural fine-tuning
  -> evaluation
  -> offline inference backend
  -> web UI
```

## Current Implemented Layer

- local web UI with Translator and Data collection/learning tabs
- raw URL collection
- paired URL collection
- manual sentence-pair collection
- candidate alignment files
- approved parallel data import
- translation memory
- word and phrase candidate extraction
- train/dev/test split generation
- neural JSONL export
- local DPRK Korean character language model
- evaluation report with exact match, chrF, and BLEU
- optional backend interface for Transformers and CTranslate2

## Advanced Layer To Add With More Data

1. Sentence alignment:
   Replace lightweight length alignment with LaBSE, Vecalign, or SentAlign.

2. Word and phrase alignment:
   Use awesome-align or SimAlign for bilingual terminology extraction.

3. Neural translation:
   Fine-tune NLLB, Marian, or OpenNMT models with approved data.

4. Offline inference:
   Convert the model to CTranslate2 and use INT8 quantization.

5. Speech:
   Add whisper.cpp for ASR and Piper for TTS.

6. Image:
   Add PaddleOCR PP-OCRv5 for offline OCR.

## Quality Targets

```text
5,000 pairs: translation memory and small experiments
30,000 pairs: first serious fine-tune
100,000 pairs: domain-quality translator
500,000+ pairs: general translator target
```

## Daily Command

```powershell
python scripts\bootstrap_data_pipeline.py
python scripts\project_health.py
```

## Neural Training Command

```powershell
python scripts\train_neural_translation_model.py --direction ko_kp__en --base-model models\text\base_model --launch
python scripts\train_neural_translation_model.py --direction en__ko_kp --base-model models\text\base_model --launch
```

For offline use, `--base-model` should point to a local model directory.

