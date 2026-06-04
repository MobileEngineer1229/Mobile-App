import 'package:flutter/material.dart';

import '../../core/language.dart';
import '../../widgets/language_pair_picker.dart';

class ImageTranslationView extends StatefulWidget {
  const ImageTranslationView({super.key});

  @override
  State<ImageTranslationView> createState() => _ImageTranslationViewState();
}

class _ImageTranslationViewState extends State<ImageTranslationView> {
  TranslatorLanguage _source = translatorLanguages[1];
  TranslatorLanguage _target = translatorLanguages[0];
  String _ocrText = '';
  String _result = '';

  @override
  Widget build(BuildContext context) {
    return ListView(
      padding: const EdgeInsets.all(16),
      children: [
        LanguagePairPicker(
          source: _source,
          target: _target,
          onSourceChanged: (value) => setState(() => _source = value),
          onTargetChanged: (value) => setState(() => _target = value),
        ),
        const SizedBox(height: 24),
        Row(
          children: [
            Expanded(
              child: OutlinedButton.icon(
                onPressed: () {
                  setState(() {
                    _ocrText = 'Offline OCR output';
                    _result = 'Offline translated image text';
                  });
                },
                icon: const Icon(Icons.photo_camera),
                label: const Text('Camera'),
              ),
            ),
            const SizedBox(width: 12),
            Expanded(
              child: OutlinedButton.icon(
                onPressed: () {
                  setState(() {
                    _ocrText = 'Offline OCR output';
                    _result = 'Offline translated image text';
                  });
                },
                icon: const Icon(Icons.image),
                label: const Text('Gallery'),
              ),
            ),
          ],
        ),
        const SizedBox(height: 24),
        SelectableText(_ocrText),
        const SizedBox(height: 12),
        SelectableText(_result),
      ],
    );
  }
}
