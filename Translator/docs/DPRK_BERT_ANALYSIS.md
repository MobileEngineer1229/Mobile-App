# DPRK-BERT Project Analysis

## What DPRK-BERT Is

DPRK-BERT is an open-source attempt to create a DPRK Korean language model. It
is not a translation model. Its core idea is:

```text
collect DPRK Korean text
  -> clean it
  -> train/fine-tune a BERT-style masked language model
  -> use vectors and masked-word prediction for analysis tasks
```

The project and related papers are valuable because they prove that DPRK Korean
needs its own data pipeline. Existing Korean NLP systems usually lean toward
South Korean language data.

Important references:

- GitHub: `https://github.com/ardakdemir/DPRK-BERT`
- LREC 2022 paper: `https://aclanthology.org/2022.lrec-1.600/`
- arXiv paper: `https://arxiv.org/abs/2112.00567`

## Repository Shape

The DPRK-BERT repository contains utilities such as:

- text cleaning
- tokenizer work
- masked-language-model training/evaluation
- sentence-vector generation
- co-occurrence analysis

The README mentions Rodong Sinmun data and New Year address data. It also
shows training/evaluation commands for masked language modeling and scripts for
generating sentence vectors from several BERT-family models.

Concrete files worth noting:

- `mlm_trainer.py`: masked-language-model training and evaluation entry point.
- `prepare_mlm_dataset.py`: converts Rodong/New Year style sources into JSON
  train/validation data.
- `cleaner.py`: normalizes text and maps some DPRK spellings/syllables toward
  South Korean forms.
- `vector_generation.py`: exports sentence vectors for DPRK-BERT, KR-BERT,
  KR-BERT-MEDIUM, and mBERT comparisons.
- `cooccurrence.py`: finds term co-occurrence patterns in DPRK text.
- `requirements.txt`: uses an older training stack, including
  `transformers==4.2.2` and `torch==1.7.1`.

The dataset format in `prepare_mlm_dataset.py` is close to:

```json
{
  "data": [
    {
      "id": "document-id",
      "data": "main document text",
      "source": "rodong"
    }
  ]
}
```

Our project should produce simpler `.txt` and `.tsv` training splits first,
then add JSON exports if we decide to reproduce DPRK-BERT-style training.

## What We Should Copy

Useful ideas for `Translator`:

1. Treat corpus cleaning as a first-class pipeline.
2. Keep raw text, cleaned text, and training data separated.
3. Build DPRK-specific embeddings/language-model scores.
4. Use co-occurrence and vocabulary analysis to find DPRK-style terms.
5. Compare DPRK-specific models against general Korean models.

## What We Should Not Copy Directly

DPRK-BERT alone cannot translate. BERT is an encoder model trained with masked
language modeling. It can score, classify, embed, and help analyze text, but it
does not naturally generate a translated sentence in another language.

For translation, this project still needs a sequence-to-sequence model:

```text
NLLB / Marian / OpenNMT / mBART-style model
```

Therefore DPRK-BERT belongs beside the translator, not inside it as the main
translation engine.

Also, the original dependency versions are old. We should not install them
directly into this project environment unless we create a separate virtual
environment for reproducing DPRK-BERT experiments.

## Best Use In This Translator Project

Use a DPRK-BERT-style model for:

- checking whether output sounds closer to DPRK Korean than South Korean
- ranking multiple candidate translations
- extracting terminology from DPRK Korean corpora
- detecting South Korean-style words that should be rewritten
- preparing a future ko_kr -> ko_kp style converter

Use a seq2seq model for:

- English -> DPRK Korean
- Chinese -> DPRK Korean
- Russian -> DPRK Korean
- DPRK Korean -> English/Chinese/Russian

## Our Concrete Direction

The project should grow in this order:

```text
1. Build clean DPRK Korean monolingual corpus.
2. Build reviewed parallel pairs.
3. Build exact translation memory.
4. Train/fine-tune ko_kr <-> ko_kp style conversion.
5. Fine-tune en/zh/ru <-> ko_kp translation.
6. Add DPRK-BERT-style scoring/reranking.
```

This gives us a usable translator early while still moving toward a real
public DPRK Korean model.
