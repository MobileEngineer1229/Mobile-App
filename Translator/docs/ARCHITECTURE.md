# Architecture

`Translator` is mobile-first. The app should not depend on a remote server for
normal translation.

## Runtime Layers

```text
Flutter UI
  |
  |-- TextTranslationController
  |     |
  |     |-- NativeTranslationEngine
  |     |-- DprkNormalizer
  |     |-- GlossaryPostEditor
  |
  |-- VoiceTranslationController
  |     |
  |     |-- NativeASREngine
  |     |-- TextTranslationController
  |     |-- NativeTTSEngine optional
  |
  |-- ImageTranslationController
        |
        |-- NativeOCREngine
        |-- TextTranslationController
        |-- OverlayRenderer
```

## Development Layers

```text
scripts/
  download model candidates
  verify model coverage
  export/quantize model
  prepare OCR/ASR assets

python_lab/
  local-only model testing
  quality experiments
  not required by final mobile app
```

## Why Flutter + Native Engines

Flutter is fast for building the UI, history screen, camera screen, and audio
screen. Offline ML inference is still better handled by native engines:

- Android: Kotlin/C++ bridge for ONNX Runtime, whisper.cpp, OCR libraries
- iOS: Swift/C++ bridge for Core ML, whisper.cpp, OCR libraries

The Flutter app should call a small platform interface rather than directly
owning heavy model logic.

## Translation Pipeline

```text
Input text
  -> language detection or selected language
  -> normalization
  -> model translation
  -> glossary replacement
  -> DPRK Korean style post-editing when target is ko_kp
  -> output
```

## Voice Pipeline

```text
Microphone
  -> local ASR
  -> source text
  -> translation pipeline
  -> optional local TTS
```

## Image Pipeline

```text
Camera/gallery image
  -> local OCR
  -> line/block grouping
  -> translation pipeline
  -> translated text list or overlay
```

## Offline Rule

Runtime code must work with airplane mode enabled after the model files are
packaged into the app.
