import 'package:flutter/material.dart';

import '../../core/language.dart';
import '../../platform/offline_translator_bridge.dart';
import '../../widgets/language_pair_picker.dart';

class TextTranslationView extends StatefulWidget {
  const TextTranslationView({super.key});

  @override
  State<TextTranslationView> createState() => _TextTranslationViewState();
}

class _TextTranslationViewState extends State<TextTranslationView> {
  final _controller = TextEditingController();
  final _bridge = const OfflineTranslatorBridge();
  TranslatorLanguage _source = translatorLanguages[1];
  TranslatorLanguage _target = translatorLanguages[0];
  String _result = '';
  bool _busy = false;

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  Future<void> _translate() async {
    setState(() => _busy = true);
    final result = await _bridge.translateText(
      source: _source,
      target: _target,
      text: _controller.text.trim(),
    );
    if (!mounted) return;
    setState(() {
      _result = result;
      _busy = false;
    });
  }

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
        const SizedBox(height: 16),
        TextField(
          controller: _controller,
          minLines: 6,
          maxLines: 10,
          decoration: const InputDecoration(
            border: OutlineInputBorder(),
            labelText: 'Text',
          ),
        ),
        const SizedBox(height: 12),
        FilledButton.icon(
          onPressed: _busy ? null : _translate,
          icon: _busy
              ? const SizedBox.square(
                  dimension: 18,
                  child: CircularProgressIndicator(strokeWidth: 2),
                )
              : const Icon(Icons.offline_bolt),
          label: const Text('Translate'),
        ),
        const SizedBox(height: 16),
        SelectableText(_result),
      ],
    );
  }
}
