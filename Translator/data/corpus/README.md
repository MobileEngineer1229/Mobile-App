# Corpus Workspace

This folder is for building a DPRK Korean / Joseon language corpus from the beginning.

```text
data/corpus/
  raw/
    Original downloaded or manually collected files.

  processed/
    Cleaned UTF-8 text files after OCR cleanup, deduplication, and normalization.

  parallel/
    Sentence-aligned files for training translation models.

  licenses/
    License notes, source URLs, permissions, and citation requirements.
```

Do not mix source material with unclear license status into training data until
the license note is written in `licenses/`.

Recommended initial datasets:

1. Public North-South Korean parallel corpus, if its license fits the project.
2. Manually reviewed DPRK-style glossary entries.
3. Project-owned translations created by human reviewers.
4. Public-domain or permissively licensed texts only after source checking.

## Daily Workflow

Put monolingual DPRK Korean text here:

```text
data/corpus/raw/
```

Supported formats:

```text
.txt .md .json .jsonl .tsv .csv
```

Put reviewed translation pairs here:

```text
data/corpus/parallel/inbox/
```

Required TSV columns:

```text
source_language  target_language  source_text  target_text
```

Then run:

```powershell
python scripts\bootstrap_data_pipeline.py
```

The pipeline creates:

```text
data/corpus/processed/monolingual_sentences.tsv
data/corpus/parallel/reviewed_parallel.tsv
data/translation_memory.json
data/training/mlm/
data/training/translation/
```
