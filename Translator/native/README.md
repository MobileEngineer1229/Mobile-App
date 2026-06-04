# Native Offline Engines

This folder is for Android/iOS native inference integrations.

## Android

Recommended bridge:

```text
Flutter MethodChannel
  -> Kotlin TranslatorPlugin
  -> C++/ONNX/whisper/OCR runtime
```

## iOS

Recommended bridge:

```text
Flutter MethodChannel
  -> Swift TranslatorPlugin
  -> Core ML / C++ runtime
```

## Required Native Methods

```text
translateText(sourceLanguage, targetLanguage, text)
transcribeAudio(sourceLanguage, audioPath)
recognizeImageText(sourceLanguage, imagePath)
```

## Model Files

Large model files should be copied during build from:

```text
Translator/models/
```

Do not commit generated model binaries.
