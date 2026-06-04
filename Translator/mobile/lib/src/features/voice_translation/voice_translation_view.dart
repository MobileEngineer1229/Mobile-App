import 'package:flutter/material.dart';

import '../../core/language.dart';
import '../../widgets/language_pair_picker.dart';

class VoiceTranslationView extends StatefulWidget {
  const VoiceTranslationView({super.key});

  @override
  State<VoiceTranslationView> createState() => _VoiceTranslationViewState();
}

class _VoiceTranslationViewState extends State<VoiceTranslationView> {
  TranslatorLanguage _source = translatorLanguages[1];
  TranslatorLanguage _target = translatorLanguages[0];
  bool _recording = false;
  String _recognizedText = '';
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
        Center(
          child: FilledButton.tonalIcon(
            onPressed: () {
              setState(() {
                _recording = !_recording;
                _recognizedText = _recording ? '' : 'Offline ASR output';
                _result = _recording ? '' : 'Offline translated speech';
              });
            },
            icon: Icon(_recording ? Icons.stop : Icons.mic),
            label: Text(_recording ? 'Stop' : 'Record'),
          ),
        ),
        const SizedBox(height: 24),
        SelectableText(_recognizedText),
        const SizedBox(height: 12),
        SelectableText(_result),
      ],
    );
  }
}
