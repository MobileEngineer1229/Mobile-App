# Mobile App

Flutter mobile app skeleton for the offline translator.

The first screen should support three tabs:

1. Text
2. Voice
3. Image

The real offline model inference should be connected through platform channels
to native Android/iOS engines.

## Flutter Modules

```text
lib/
  main.dart
  src/
    app.dart
    core/
      language.dart
    features/
      text_translation/
      voice_translation/
      image_translation/
    platform/
      offline_translator_bridge.dart
    widgets/
      language_pair_picker.dart
```

## Native Boundary

Flutter calls:

```dart
OfflineTranslatorBridge.translateText(...)
OfflineTranslatorBridge.transcribeAudio(...)     // next native method
OfflineTranslatorBridge.recognizeImageText(...)  // next native method
```

Native code owns:

- model file loading
- ASR/OCR/translation inference
- memory-sensitive quantized runtime
