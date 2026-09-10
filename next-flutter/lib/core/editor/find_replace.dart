class FindReplaceOptions {
  const FindReplaceOptions(
      {this.matchCase = false, this.wholeWord = false, this.regex = false});

  final bool matchCase;
  final bool wholeWord;
  final bool regex;

  FindReplaceOptions copyWith(
          {bool? matchCase, bool? wholeWord, bool? regex}) =>
      FindReplaceOptions(
        matchCase: matchCase ?? this.matchCase,
        wholeWord: wholeWord ?? this.wholeWord,
        regex: regex ?? this.regex,
      );

  Map<String, Object?> toJson() =>
      {'matchCase': matchCase, 'wholeWord': wholeWord, 'regex': regex};

  factory FindReplaceOptions.fromJson(Map<String, Object?> json) =>
      FindReplaceOptions(
        matchCase: json['matchCase'] as bool? ?? false,
        wholeWord: json['wholeWord'] as bool? ?? false,
        regex: json['regex'] as bool? ?? false,
      );
}

class FindMatch {
  const FindMatch(this.start, this.end);
  final int start;
  final int end;
  int get length => end - start;
}

final _literalEscape = RegExp(r'[.*+?^${}()|[\]\\]');

RegExp? buildSearchRegExp(String query, FindReplaceOptions options) {
  if (query.isEmpty) return null;
  try {
    final source = options.regex
        ? query
        : query.replaceAllMapped(_literalEscape, (match) => '\\${match[0]}');
    final wrapped = options.wholeWord ? '\\b(?:$source)\\b' : source;
    return RegExp(wrapped, caseSensitive: options.matchCase, unicode: true);
  } catch (_) {
    return null;
  }
}

List<FindMatch> findAllMatches(
    String content, String query, FindReplaceOptions options) {
  final expression = buildSearchRegExp(query, options);
  if (expression == null) return const [];
  final matches = <FindMatch>[];
  for (final match in expression.allMatches(content)) {
    if (match.start == match.end) continue;
    matches.add(FindMatch(match.start, match.end));
  }
  return matches;
}

FindMatch? findNextMatch(
    String content, String query, FindReplaceOptions options, int fromIndex,
    {required bool forward}) {
  final matches = findAllMatches(content, query, options);
  if (matches.isEmpty) return null;
  if (forward) {
    return matches.cast<FindMatch?>().firstWhere(
        (match) => match!.start >= fromIndex,
        orElse: () => matches.first);
  }
  for (var i = matches.length - 1; i >= 0; i--) {
    if (matches[i].end <= fromIndex) return matches[i];
  }
  return matches.last;
}

String _expandRegexReplacement(String replaceWith) {
  return replaceWith.replaceAllMapped(RegExp(r'\\([nrt\\])'), (match) {
    switch (match[1]) {
      case 'n':
        return '\n';
      case 'r':
        return '\r';
      case 't':
        return '\t';
      default:
        return '\\';
    }
  });
}

class ReplaceCurrentResult {
  const ReplaceCurrentResult(
      {required this.content,
      required this.nextFrom,
      required this.replaced,
      this.match});
  final String content;
  final int nextFrom;
  final bool replaced;
  final FindMatch? match;
}

ReplaceCurrentResult replaceCurrentMatch({
  required String content,
  required String query,
  required String replaceWith,
  required FindReplaceOptions options,
  FindMatch? selection,
}) {
  if (query.isEmpty)
    return ReplaceCurrentResult(
        content: content, nextFrom: selection?.end ?? 0, replaced: false);
  final expression = buildSearchRegExp(query, options);
  if (expression == null)
    return ReplaceCurrentResult(
        content: content, nextFrom: selection?.end ?? 0, replaced: false);
  final replacement =
      options.regex ? _expandRegexReplacement(replaceWith) : replaceWith;
  if (selection != null && selection.end > selection.start) {
    final selected = content.substring(selection.start, selection.end);
    final selectedMatches = findAllMatches(selected, query, options);
    final exact = selectedMatches.length == 1 &&
        selectedMatches.first.start == 0 &&
        selectedMatches.first.end == selected.length;
    if (exact) {
      final replacedSlice = selected.replaceFirst(expression, replacement);
      return ReplaceCurrentResult(
        content:
            '${content.substring(0, selection.start)}$replacedSlice${content.substring(selection.end)}',
        nextFrom: selection.start + replacedSlice.length,
        replaced: true,
        match:
            FindMatch(selection.start, selection.start + replacedSlice.length),
      );
    }
  }
  final from = selection?.end ?? 0;
  final match = findNextMatch(content, query, options, from, forward: true);
  if (match == null)
    return ReplaceCurrentResult(
        content: content, nextFrom: from, replaced: false);
  final slice = content.substring(match.start, match.end);
  final replacedSlice = slice.replaceFirst(expression, replacement);
  return ReplaceCurrentResult(
    content:
        '${content.substring(0, match.start)}$replacedSlice${content.substring(match.end)}',
    nextFrom: match.start + replacedSlice.length,
    replaced: true,
    match: FindMatch(match.start, match.start + replacedSlice.length),
  );
}

({String content, int count}) replaceAllMatches(String content, String query,
    String replaceWith, FindReplaceOptions options) {
  final expression = buildSearchRegExp(query, options);
  if (expression == null) return (content: content, count: 0);
  final replacement =
      options.regex ? _expandRegexReplacement(replaceWith) : replaceWith;
  var count = 0;
  final next = content.replaceAllMapped(expression, (match) {
    if (match[0]!.isEmpty) return match[0]!;
    count += 1;
    return replacement;
  });
  return (content: next, count: count);
}
