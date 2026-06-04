# DPRK Korean Notes

The app target is DPRK Korean style, not just generic Korean.

## Practical Approach

Use a general Korean translation model first, then post-edit output.

```text
generic Korean output
  -> glossary replacement
  -> spelling/style replacement
  -> DPRK Korean output
```

## Glossary File

Glossary entries live in:

```text
data/glossary/dprk_glossary.tsv
```

Columns:

```text
source_language    target_language    source_term    target_term    note
```

## Post-Edit Rules

Rules live in:

```text
data/rules/dprk_postedit_rules.tsv
```

Columns:

```text
target_language    find    replace    note
```

## Later Fine-Tuning

If enough DPRK Korean parallel text is collected, the project can fine-tune a
translation model. Until then, glossary + post-editing is the fastest route.
