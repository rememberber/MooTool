List<int> parsePageSelection(String expression, int maxPage) {
  if (maxPage < 1) throw const FormatException('PDF has no pages');
  final value =
      expression.trim().replaceAll(RegExp('[，,]'), ';').replaceAll(RegExp(r'\s+'), '');
  if (value.isEmpty ||
      !RegExp(r'^\d+(?:-\d+)?(?:;\d+(?:-\d+)?)*$').hasMatch(value)) {
    throw const FormatException('Use page ranges such as 1-5;8;10-12');
  }
  final pages = <int>{};
  for (final token in value.split(';')) {
    final parts = token.split('-');
    final start = int.parse(parts[0]);
    final end = int.parse(parts.length > 1 ? parts[1] : parts[0]);
    if (start < 1 || end < start || end > maxPage) {
      throw FormatException('Page range must stay between 1 and $maxPage');
    }
    for (var page = start; page <= end; page += 1) {
      pages.add(page);
    }
  }
  return pages.toList();
}

List<int> selectSplitPages(
    String pageRange, String rule, String customRule, int maxPage) {
  final candidates = parsePageSelection(pageRange, maxPage);
  if (rule == 'odd') {
    return [for (final page in candidates) if (page.isOdd) page];
  }
  if (rule == 'even') {
    return [for (final page in candidates) if (page.isEven) page];
  }
  final selected = parsePageSelection(customRule, maxPage).toSet();
  return [for (final page in candidates) if (selected.contains(page)) page];
}
