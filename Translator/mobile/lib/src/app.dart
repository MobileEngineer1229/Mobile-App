import 'package:flutter/material.dart';

import 'features/image_translation/image_translation_view.dart';
import 'features/text_translation/text_translation_view.dart';
import 'features/voice_translation/voice_translation_view.dart';

class TranslatorApp extends StatelessWidget {
  const TranslatorApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      debugShowCheckedModeBanner: false,
      title: 'Translator',
      theme: ThemeData(
        colorScheme: ColorScheme.fromSeed(seedColor: const Color(0xFF1F6F5B)),
        useMaterial3: true,
      ),
      home: const TranslatorHomePage(),
    );
  }
}

class TranslatorHomePage extends StatefulWidget {
  const TranslatorHomePage({super.key});

  @override
  State<TranslatorHomePage> createState() => _TranslatorHomePageState();
}

class _TranslatorHomePageState extends State<TranslatorHomePage> {
  int _index = 0;

  @override
  Widget build(BuildContext context) {
    final pages = const [
      TextTranslationView(),
      VoiceTranslationView(),
      ImageTranslationView(),
    ];

    return Scaffold(
      appBar: AppBar(title: const Text('Translator')),
      body: pages[_index],
      bottomNavigationBar: NavigationBar(
        selectedIndex: _index,
        onDestinationSelected: (value) => setState(() => _index = value),
        destinations: const [
          NavigationDestination(icon: Icon(Icons.translate), label: 'Text'),
          NavigationDestination(icon: Icon(Icons.mic), label: 'Voice'),
          NavigationDestination(icon: Icon(Icons.image_search), label: 'Image'),
        ],
      ),
    );
  }
}
