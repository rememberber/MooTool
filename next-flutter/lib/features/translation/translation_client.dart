import 'dart:convert';
import 'dart:io';

const translationLanguageCodes = [
  'auto',
  'zh-CN',
  'en',
  'yue',
  'wyw',
  'jp',
  'kor',
  'fra',
  'spa',
  'th',
  'ara',
  'ru',
  'pt',
  'de',
  'it',
  'el',
  'nl',
  'pl',
  'bul',
  'est',
  'dan',
  'fin',
  'cs',
  'rom',
  'slo',
  'swe',
  'hu',
  'cht',
  'vie',
];

class TranslationResult {
  TranslationResult(
      {required this.requestId,
      required this.text,
      required this.provider,
      this.fallbackUsed = false});
  final String requestId;
  final String text;
  final String provider;
  final bool fallbackUsed;
}

class TranslationSession {
  String source = '';
  String target = '';
  String sourceLang = 'auto';
  String targetLang = 'zh-CN';
  String provider = 'google';
  String notice = '';
  bool translating = false;
  String requestId = '';
  final List<Map<String, String>> history = [];

  Map<String, Object?> toJson() => {
        'source': source,
        'target': target,
        'sourceLang': sourceLang,
        'targetLang': targetLang,
        'provider': provider,
        'history': history,
      };

  void restore(Map<String, Object?> json) {
    source = json['source'] as String? ?? '';
    target = json['target'] as String? ?? '';
    sourceLang = json['sourceLang'] as String? ?? 'auto';
    targetLang = json['targetLang'] as String? ?? 'zh-CN';
    provider = json['provider'] as String? ?? 'google';
    history
      ..clear()
      ..addAll([
        for (final item in json['history'] as List? ?? [])
          if (item is Map)
            {for (final entry in item.entries) '${entry.key}': '${entry.value}'}
      ]);
  }
}

List<String> splitTranslationText(String text, int maxLength) {
  if (maxLength < 2)
    throw const FormatException('Invalid translation chunk size');
  if (text.length <= maxLength) return [text];
  final chunks = <String>[];
  var offset = 0;
  while (offset < text.length) {
    var end = (offset + maxLength).clamp(0, text.length);
    if (end < text.length) {
      final minimum = offset + (maxLength ~/ 2);
      for (var cursor = end; cursor > minimum; cursor -= 1) {
        if (RegExp(r'[\s.!?。！？,，;；:：]').hasMatch(text[cursor - 1])) {
          end = cursor;
          break;
        }
      }
      if (end > 0 &&
          end < text.length &&
          text.codeUnitAt(end - 1) >= 0xD800 &&
          text.codeUnitAt(end - 1) <= 0xDBFF &&
          text.codeUnitAt(end) >= 0xDC00 &&
          text.codeUnitAt(end) <= 0xDFFF) {
        end -= 1;
      }
    }
    chunks.add(text.substring(offset, end));
    offset = end;
  }
  return chunks;
}

String googleLanguage(String code) {
  const mapped = {
    'wyw': 'lzh',
    'jp': 'ja',
    'kor': 'ko',
    'fra': 'fr',
    'spa': 'es',
    'ara': 'ar',
    'bul': 'bg',
    'est': 'et',
    'dan': 'da',
    'fin': 'fi',
    'rom': 'ro',
    'slo': 'sl',
    'swe': 'sv',
    'cht': 'zh-TW',
    'vie': 'vi',
  };
  if (code.isEmpty) return 'auto';
  return mapped[code] ?? code;
}

class TranslationClient {
  TranslationClient({this.fetch});

  final Future<String> Function(Uri uri, int timeoutMs)? fetch;
  final Set<String> _cancelled = {};

  void cancel(String requestId) => _cancelled.add(requestId);

  Future<TranslationResult> translate({
    required String requestId,
    required String text,
    required String sourceLang,
    required String targetLang,
    int timeoutMs = 10000,
  }) async {
    _cancelled.remove(requestId);
    if (text.trim().isEmpty) {
      return TranslationResult(
          requestId: requestId, text: '', provider: 'google');
    }
    final chunks = splitTranslationText(text, 1800);
    final parts = <String>[];
    for (final chunk in chunks) {
      if (_cancelled.contains(requestId)) {
        throw const FormatException('ABORTED');
      }
      parts.add(await _googleChunk(chunk, sourceLang, targetLang, timeoutMs));
      if (_cancelled.contains(requestId)) {
        throw const FormatException('ABORTED');
      }
    }
    return TranslationResult(
        requestId: requestId, text: parts.join(), provider: 'google');
  }

  Future<String> _googleChunk(
      String chunk, String sourceLang, String targetLang, int timeoutMs) async {
    final uri = Uri.https('translate.googleapis.com', '/translate_a/single', {
      'client': 'gtx',
      'sl': googleLanguage(sourceLang),
      'tl': googleLanguage(targetLang),
      'dt': 't',
      'q': chunk,
    });
    final body = await (fetch ?? _defaultFetch)(uri, timeoutMs);
    final decoded = jsonDecode(body);
    if (decoded is! List || decoded.isEmpty || decoded.first is! List) {
      throw const FormatException('Google returned no translation');
    }
    final translated = [
      for (final part in decoded.first as List)
        if (part is List && part.isNotEmpty) '${part.first}'
    ].join();
    if (translated.isEmpty) {
      throw const FormatException('Google returned no translation');
    }
    return translated;
  }

  Future<String> _defaultFetch(Uri uri, int timeoutMs) async {
    final client = HttpClient();
    try {
      final request = await client
          .getUrl(uri)
          .timeout(Duration(milliseconds: timeoutMs.clamp(1000, 120000)));
      request.headers.set('user-agent',
          'Mozilla/5.0 (MooTool Next Flutter) AppleWebKit/537.36 Chrome/138 Safari/537.36');
      final response = await request
          .close()
          .timeout(Duration(milliseconds: timeoutMs.clamp(1000, 120000)));
      if (response.statusCode < 200 || response.statusCode >= 300) {
        throw FormatException('Google HTTP ${response.statusCode}');
      }
      return await response.transform(utf8.decoder).join();
    } finally {
      client.close(force: true);
    }
  }
}
