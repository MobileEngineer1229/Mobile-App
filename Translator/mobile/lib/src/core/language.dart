class TranslatorLanguage {
  const TranslatorLanguage({
    required this.code,
    required this.label,
    required this.modelCode,
  });

  final String code;
  final String label;
  final String modelCode;
}

const translatorLanguages = [
  TranslatorLanguage(code: 'ko_kp', label: '조선말', modelCode: 'kor_Hang'),
  TranslatorLanguage(code: 'en', label: 'English', modelCode: 'eng_Latn'),
  TranslatorLanguage(code: 'zh', label: 'Chinese', modelCode: 'zho_Hans'),
  TranslatorLanguage(code: 'ru', label: 'Russian', modelCode: 'rus_Cyrl'),
];
