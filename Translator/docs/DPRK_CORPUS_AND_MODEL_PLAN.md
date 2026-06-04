# DPRK Korean Corpus And Public Model Plan

## Can We Build The Corpus From The Beginning?

Yes. But the corpus must be treated as the main product, not as a side file.
For a DPRK Korean translator, the data is more important than the web UI.

The first goal is not immediately training a large model. The first goal is a
clean, reviewed, legally usable corpus:

```text
source collection
  -> license review
  -> OCR/text extraction
  -> UTF-8 normalization
  -> sentence splitting
  -> North/South/English/Chinese/Russian alignment
  -> human review
  -> train/dev/test split
  -> model training
```

## Can We Make A Public DPRK Korean Model?

Yes, if the training data license allows redistribution or if the model license
is compatible with the data sources. There are two possible model types:

1. DPRK Korean language model:
   Predicts and understands DPRK Korean text. Useful for spelling, style,
   scoring, masked-word tasks, and reranking.

2. DPRK Korean translation model:
   Translates between DPRK Korean and English, Chinese, Russian, or South
   Korean. This is harder because it needs aligned sentence pairs.

For this project, the useful public model is the second one. A language model
alone is not enough to translate.

For the detailed DPRK-BERT analysis, read:

```text
docs/DPRK_BERT_ANALYSIS.md
```

## Recommended Training Roadmap

### Stage 1: Corpus Registry

Create a source registry with these fields:

```text
source_id
title
url_or_location
language
license
permission_status
domain
notes
```

No source should enter training until its permission status is clear.

### Stage 2: DPRK Text Corpus

Collect monolingual DPRK Korean text first. This is useful for:

- tokenizer training
- spelling/style statistics
- DPRK vocabulary extraction
- language-model adaptation

Minimum useful size:

- small experiment: 5-20 MB clean text
- serious style model: 100 MB+ clean text
- large language model pretraining: much larger, usually not realistic first

### Stage 3: Parallel Corpus

For translation, aligned data is needed:

```text
ko_kp <-> en
ko_kp <-> zh
ko_kp <-> ru
ko_kp <-> ko_kr
```

Minimum useful sizes:

- glossary/translation memory: hundreds to thousands of pairs
- small fine-tune: 10k-50k pairs
- stronger translator: 100k+ reviewed pairs

### Stage 4: Baseline Translator

Use a general model as a base and fine-tune it:

- NLLB for multilingual translation
- Marian/OpenNMT if training a smaller direction-specific model
- KoBART-style model for North/South Korean conversion

Do not train from zero first. Fine-tuning a pretrained model is faster and
usually much better with limited data.

### Stage 5: Public Release

Before publishing a model:

1. Confirm all training data licenses.
2. Publish a model card.
3. Publish evaluation examples and known limitations.
4. Avoid claiming that the model represents all DPRK language usage.
5. Keep a clear changelog for data and model versions.

## Practical First Target

The first real public artifact should be:

```text
Translator-DPRK-v0
  data:
    reviewed glossary
    translation memory
    North-South Korean parallel data, if license allows
  model:
    fine-tuned translator for ko_kr -> ko_kp and ko_kp -> ko_kr
  service:
    local web UI using the model offline
```

After that works, add English, Chinese, and Russian using pivot or direct
fine-tuning.

## Why Start With ko_kr <-> ko_kp?

It is easier to build than English/Chinese/Russian direct translation because
the source and target are closely related. It also solves the biggest weakness
of existing Korean models: they tend to output South Korean style.

The practical pipeline can be:

```text
English/Chinese/Russian -> general Korean
general Korean -> DPRK Korean style converter
```

Later, direct translation can replace this two-step path when enough parallel
data exists.
