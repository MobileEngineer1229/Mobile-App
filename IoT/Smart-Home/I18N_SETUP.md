# String Resources Setup Guide

## Overview

The Smart Home Android app uses English as its default and currently supported
interface language.

## Project Structure

```
app/src/main/res/
└── values/
    └── strings.xml    # English UI strings
```

## Adding a String

1. Add the string to `app/src/main/res/values/strings.xml`.
2. Reference it from layouts with `@string/your_string_name`.
3. Reference it from Java with `getString(R.string.your_string_name)`.

Example:

```xml
<string name="device_connected">Device connected</string>
```

## Rules

- Keep user-facing text in `strings.xml`; do not hard-code it in layouts or
  Java files.
- Use descriptive, lower_snake_case resource names.
- Preserve placeholders and markup when editing existing strings.

## Testing

Build the Android app after changing resources:

```bash
./gradlew assembleDebug
```

## Adding Another Language Later

If a new localization is required, create a matching `values-<locale>`
directory and translate every applicable string while preserving resource names
and placeholders.
