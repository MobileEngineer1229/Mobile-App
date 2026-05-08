# Internationalization (i18n) Setup Guide

## Overview
The Smart Home Android app supports **English** (default) and **Korean** languages.

## Project Structure

```
Smart-Home/app/src/main/res/
├── values/
│   └── strings.xml          # English strings (default)
└── values-ko/
    └── strings.xml          # Korean strings
```

## How It Works

Android automatically selects the appropriate language based on the device's locale:
- **English (en_US)**: Uses `res/values/strings.xml`
- **Korean (ko_KR)**: Uses `res/values-ko/strings.xml`

## Adding New Strings

### Step 1: Add to English strings.xml
```xml
<!-- res/values/strings.xml -->
<string name="new_feature">New Feature</string>
```

### Step 2: Add Korean translation
```xml
<!-- res/values-ko/strings.xml -->
<string name="new_feature">새 기능</string>
```

### Step 3: Use in code
```java
// Java/Kotlin
String text = getString(R.string.new_feature);
```

```xml
<!-- XML Layout -->
<TextView android:text="@string/new_feature" />
```

## Important Rules

1. **NEVER hardcode text** - Always use string resources
2. **Always add both languages** - When adding a new string, add it to both English and Korean files
3. **Use descriptive names** - String names should be clear and descriptive
4. **Parameterized strings** - Use `%1$s`, `%2$d` for dynamic content:
   ```xml
   <string name="welcome_user">Welcome, %1$s!</string>
   ```
   ```java
   String message = getString(R.string.welcome_user, userName);
   ```

## Language Selection

Users can change the app language via:
- **Settings** → **Account** → **App Appearance** → **App Language**

The app will use the device's system language by default, but users can override it in settings.

## Testing

1. Change device language to Korean in Android Settings
2. Launch the app - it should display Korean text
3. Change back to English - app should display English text
4. Test all screens to ensure proper layout with both languages

## Current Status

✅ **English**: Complete (all strings translated)
✅ **Korean**: Complete (all strings translated)

## Adding More Languages

To add a new language (e.g., Japanese):

1. Create directory: `res/values-ja/`
2. Copy `strings.xml` to the new directory
3. Translate all strings to Japanese
4. Update `AppLanguageActivity.java` to include the new language option

## Notes

- String resource names must match exactly between language files
- Missing translations will fall back to the default (English)
- Test UI layouts with longer text (Korean can be more compact than English)
- Consider RTL (Right-to-Left) languages if adding Arabic or Hebrew in the future
