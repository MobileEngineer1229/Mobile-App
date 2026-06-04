import 'package:flutter/material.dart';

import '../core/language.dart';

class LanguagePairPicker extends StatelessWidget {
  const LanguagePairPicker({
    super.key,
    required this.source,
    required this.target,
    required this.onSourceChanged,
    required this.onTargetChanged,
  });

  final TranslatorLanguage source;
  final TranslatorLanguage target;
  final ValueChanged<TranslatorLanguage> onSourceChanged;
  final ValueChanged<TranslatorLanguage> onTargetChanged;

  @override
  Widget build(BuildContext context) {
    return Row(
      children: [
        Expanded(
          child: _LanguageMenu(
            value: source,
            onChanged: onSourceChanged,
          ),
        ),
        IconButton(
          tooltip: 'Swap',
          onPressed: () {
            onSourceChanged(target);
            onTargetChanged(source);
          },
          icon: const Icon(Icons.swap_horiz),
        ),
        Expanded(
          child: _LanguageMenu(
            value: target,
            onChanged: onTargetChanged,
          ),
        ),
      ],
    );
  }
}

class _LanguageMenu extends StatelessWidget {
  const _LanguageMenu({required this.value, required this.onChanged});

  final TranslatorLanguage value;
  final ValueChanged<TranslatorLanguage> onChanged;

  @override
  Widget build(BuildContext context) {
    return DropdownButtonFormField<TranslatorLanguage>(
      value: value,
      decoration: const InputDecoration(border: OutlineInputBorder()),
      items: translatorLanguages
          .map(
            (language) => DropdownMenuItem(
              value: language,
              child: Text(language.label),
            ),
          )
          .toList(),
      onChanged: (value) {
        if (value != null) onChanged(value);
      },
    );
  }
}
