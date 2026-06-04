# Third-Party Offline Engines

Place source checkouts or prebuilt native libraries here when the mobile
runtime is chosen.

Suggested layout:

```text
third_party/
  whisper.cpp/
  paddleocr/
  piper/
  onnxruntime-mobile/
```

Do not commit large generated binaries unless the repository policy explicitly
allows it. Keep repeatable download and conversion commands in `scripts/`.
