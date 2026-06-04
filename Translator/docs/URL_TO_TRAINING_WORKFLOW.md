# URL To Training Workflow

This document explains what happens when the user gives a URL.

## Command Line

```powershell
python scripts\import_url_corpus.py "https://example.com/page" --language ko_kp
python scripts\bootstrap_data_pipeline.py
```

## Web UI

1. Open `http://127.0.0.1:8765`.
2. Paste a URL into the Corpus area.
3. Choose the language of the page.
4. Press `Import URL`.
5. Press `Rebuild / Train local models`.

## Pipeline

```text
URL
  -> scripts/import_url_corpus.py
  -> data/corpus/raw/url_<hash>.corpus
  -> data/corpus/source_registry.tsv
  -> scripts/bootstrap_data_pipeline.py
  -> data/corpus/processed/monolingual_sentences.tsv
  -> data/training/mlm/*.txt
  -> models/text/ko_kp_char_lm.json
```

If reviewed parallel translation pairs exist:

```text
data/corpus/parallel/inbox/*.tsv
  -> data/corpus/parallel/reviewed_parallel.tsv
  -> data/translation_memory.json
  -> data/training/translation/*.tsv
```

## What Is Learned Today

The current local learning step trains:

1. exact translation memory from reviewed sentence pairs
2. a DPRK Korean character n-gram language model from monolingual text

The character model is used as a local score for `ko_kp` output. It is not a
full neural translator.

## Future Neural Training

When enough reviewed data exists, the same `data/training/translation/*.tsv`
files can be used to fine-tune a seq2seq translation model. That model should
then replace the current demo fallback inside `python_lab/app/translator_service.py`.
