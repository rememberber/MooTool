class RegexOptions {
  const RegexOptions({
    this.global = true,
    this.ignoreCase = false,
    this.multiline = false,
    this.dotAll = false,
  });
  final bool global;
  final bool ignoreCase;
  final bool multiline;
  final bool dotAll;
}

class RegexMatchHit {
  const RegexMatchHit(
      {required this.index, required this.value, required this.groups});
  final int index;
  final String value;
  final List<String> groups;
}

const commonRegexes = [
  ('phone', r'1[3-9]\d{9}'),
  ('email', r'^[a-zA-Z0-9_-]+@[a-zA-Z0-9_-]+(\.[a-zA-Z0-9_-]+)+$'),
  (
    'domain',
    r'^((http:\/\/)|(https:\/\/))?([a-zA-Z0-9]([a-zA-Z0-9\-]{0,61}[a-zA-Z0-9])?\.)+[a-zA-Z]{2,6}(\/)'
  ),
  (
    'ipv4',
    r'((?:(?:25[0-5]|2[0-4]\d|[01]?\d?\d)\.){3}(?:25[0-5]|2[0-4]\d|[01]?\d?\d))'
  ),
  ('account', r'^[a-zA-Z][a-zA-Z0-9_]{4,15}$'),
  ('htmlId', r'(?<=id=")[\s\S]*?(?=")'),
  ('color', r'#([a-fA-F0-9]{6})'),
  ('jpg', r'''http[s:]{1,2}//[^\s'"<>]*?.jpg'''),
  ('magnet', r'magnet:\?xt=urn:btih:[0-9a-fA-F]{40,}'),
  ('chinese', r'^[\u4e00-\u9fa5]{0,}$'),
  ('alnum', r'^[A-Za-z0-9]+$'),
  ('len3to20', r'^.{3,20}$'),
  ('letters26', r'^[A-Za-z]+$'),
  ('wordUnderscore', r'^\w+$'),
  ('cnEnNum', r'^[\u4E00-\u9FA5A-Za-z0-9_]+$'),
  ('noSpecial', r'''[^%&',;=?$\x22]+'''),
  ('integer', r'^-?[1-9]\d*$'),
  ('positiveInt', r'^[1-9]\d*$'),
  ('negativeInt', r'^-[1-9]\d*$'),
  ('nonNegativeInt', r'^(?:[1-9]\d*|0)$'),
  ('float', r'^-?([1-9]\d*\.\d*|0\.\d*[1-9]\d*|0?\.0+|0)$'),
];

class RegexEngine {
  List<RegexMatchHit> matchRegex(
      String pattern, String source, RegexOptions options) {
    final expression = RegExp(
      pattern,
      caseSensitive: !options.ignoreCase,
      multiLine: options.multiline,
      dotAll: options.dotAll,
    );
    if (!options.global) {
      final match = expression.firstMatch(source);
      return match == null ? const [] : [_hit(match)];
    }
    return [for (final match in expression.allMatches(source)) _hit(match)];
  }

  RegexMatchHit _hit(RegExpMatch match) {
    return RegexMatchHit(
      index: match.start,
      value: match.group(0) ?? '',
      groups: [
        for (var i = 1; i <= match.groupCount; i++) match.group(i) ?? ''
      ],
    );
  }
}
