# Implementation Plan

## Phase 1: Offline Text Translation

1. Choose the first multilingual translation model.
2. Download it into `models/text/`.
3. Convert or quantize it for the target mobile runtime.
4. Connect `mobile/lib/src/platform/offline_translator_bridge.dart` to the
   native runtime.
5. Apply `data/glossary/dprk_glossary.tsv` and
   `data/rules/dprk_postedit_rules.tsv` after translation when the target is
   `ko_kp`.

## Phase 2: Voice Translation

1. Add microphone permission and recording in Flutter.
2. Send audio frames to the native ASR engine.
3. Translate recognized text through the same text pipeline.
4. Add optional offline TTS when a good local voice exists.

## Phase 3: Image Translation

1. Add camera/gallery selection.
2. Run offline OCR on the image.
3. Translate extracted text through the same text pipeline.
4. Later, add text-box overlays on top of the original image.

## Phase 4: Quality

1. Expand `data/evaluation/phrase_pairs.tsv`.
2. Add automated scoring for phrase coverage.
3. Add manual review fields for DPRK vocabulary and political/geographic terms.
