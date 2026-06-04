# Translation Method Decision

## Short Decision

Use a hybrid offline translation system:

```text
translation memory / exact phrases
  -> neural machine translation model
  -> DPRK glossary
  -> DPRK post-edit rules
  -> evaluation tests
```

The main translator should be a neural machine translation model, not a pure
grammar parser and not a pure frequency table.

## Option 1: Grammar / Sentence-Part Parser

This approach analyzes grammar, sentence members, endings, particles, and word
roles, then generates the target language by rules.

Strengths:

- Predictable for narrow official templates.
- Easier to enforce terminology.
- Useful as a post-processing layer for DPRK style.

Weaknesses:

- Very expensive to build for Korean, English, Chinese, and Russian.
- Idioms and free word order become difficult quickly.
- Output is often stiff unless thousands of rules are written.

Best use in this project:

- Do not use it as the main engine.
- Use it for normalization, terminology checks, and post-edit rules.

## Option 2: Frequency / Statistical Translator

This approach chooses likely translations from phrase tables, n-gram language
models, and counts from a parallel corpus.

Strengths:

- Works offline.
- Gives stable results for repeated phrases.
- Good for translation memory and official fixed expressions.

Weaknesses:

- Needs a large parallel corpus for each language pair.
- Handles unseen sentences poorly.
- Usually lower fluency than modern neural translation.

Best use in this project:

- Use it as translation memory before the neural model.
- Use it for high-confidence fixed terms and repeated sentences.

## Option 3: LLM-Style Translator

There are two meanings here:

1. A general chat LLM such as Llama/Qwen/Mistral.
2. A dedicated neural machine translation model such as NLLB, Marian, M2M100,
   or an Argos Translate package.

For this project, choose the second meaning first: a dedicated translation
model. It is smaller, more predictable, easier to evaluate, and better suited
to offline service deployment.

General LLMs can be useful later for:

- explaining ambiguous sentences
- suggesting terminology
- helping human review

But they should not be the first production translation engine because they are
larger, slower, and more likely to add unrequested words.

## Recommended First Engine

Start with one of these two paths:

1. Fastest practical path: Argos Translate packages where language pairs exist.
2. Better multilingual path: NLLB model converted to CTranslate2 or another
   local runtime.

For English, Chinese, Russian, and Korean-style output, NLLB is the better long
term baseline because one multilingual model can cover all directions. DPRK
Korean should be handled as Korean base output plus project glossary and
post-editing.

## What Must Be Built

1. Language codes and routing:
   `ko_kp`, `en`, `zh`, `ru`.

2. Sentence splitting:
   Long text should be split into sentences before translation.

3. Normalization:
   Clean spaces, punctuation, quotes, line breaks, and language-specific forms.

4. Translation memory:
   Exact approved phrase pairs should be returned before the model is called.

5. Neural model runtime:
   A local engine such as Argos Translate or CTranslate2 must load model files
   from `models/`.

6. DPRK glossary:
   `data/glossary/dprk_glossary.tsv` should enforce preferred terms.

7. Post-edit rules:
   `data/rules/dprk_postedit_rules.tsv` should fix recurring style problems.

8. Evaluation set:
   `data/evaluation/phrase_pairs.tsv` should grow into the main quality test.

9. Web service:
   A local UI and JSON API should test the engine offline.

10. Model preparation scripts:
    Every download, conversion, and test script belongs in `scripts/`.

## Quality Method

Do not judge the translator only by one example. Keep a test table:

```text
source_language  target_language  source_text  expected_meaning
```

Each time a model or rule changes, run the same examples again. This is how the
project avoids randomly improving one sentence while breaking ten others.
