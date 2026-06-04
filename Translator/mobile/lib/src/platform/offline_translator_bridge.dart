import '../core/language.dart';

class OfflineTranslatorBridge {
  const OfflineTranslatorBridge();

  Future<String> translateText({
    required TranslatorLanguage source,
    required TranslatorLanguage target,
    required String text,
  }) async {
    // Native Android/iOS inference will be connected here after model conversion.
    await Future<void>.delayed(const Duration(milliseconds: 120));
    return '[${source.label} -> ${target.label}]\n$text';
  }
}
