# Model Strategy

## Main Decision

Use a dedicated offline neural machine translation engine as the main
translator. Do not use a pure grammar parser or a pure frequency translator as
the main engine.

Recommended shape:

```text
approved phrase memory
  -> neural machine translation
  -> DPRK glossary
  -> post-edit rules
  -> evaluation
```

## Sentence Translation

Recommended candidates:

```text
Path A: Argos Translate packages for quick offline testing
Path B: NLLB model converted/quantized with CTranslate2 for stronger multilingual coverage
```

Reason:

- Argos is quick when the exact language package exists.
- NLLB gives one multilingual baseline for Korean, English, Chinese, and Russian.
- CTranslate2 is a practical local runtime for faster CPU/GPU inference.

Internal language mapping:

```text
ko_kp -> Korean/Hangul model language + DPRK glossary post-edit
en    -> English/Latin
zh    -> Chinese/Simplified
ru    -> Russian/Cyrillic
```

## DPRK Korean Handling

Most open translation models treat Korean as one language. To make output closer
to DPRK Korean, use three layers:

1. glossary replacements
2. spelling/style post-edit rules
3. later fine-tuning if enough parallel DPRK Korean data is collected

## Voice Translation

ASR options:

- Whisper.cpp: strong multilingual offline recognition, C++ mobile path
- Vosk: smaller offline ASR models, easier for constrained devices

TTS options:

- Piper: offline neural TTS when voice models are available
- platform TTS fallback: useful early, but language/style control is weaker

## Image Translation

OCR options:

- PaddleOCR: multilingual OCR with offline models
- platform OCR fallback: Android/iOS on-device OCR if acceptable

Image translation should first display extracted text + translated blocks. Full
AR overlay can come later.

## Service Model Packaging

Recommended service stages:

```text
Stage 1: dependency-free web UI works offline with demo engine.
Stage 2: Argos or NLLB model works from Python.
Stage 3: convert/quantize for CTranslate2 if needed.
Stage 4: connect /api/translate to the real model.
Stage 5: add evaluation and model comparison scripts.
```

## Quality Evaluation

Keep a test set in:

```text
data/evaluation/phrase_pairs.tsv
```

Each row should contain:

```text
source_language    target_language    source_text    expected_meaning
```

Use it to compare model versions before replacing app models.
