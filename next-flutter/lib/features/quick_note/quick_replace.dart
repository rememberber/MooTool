import 'dart:convert';

const quickReplaceLabels = {
  'trim': '去首尾空格',
  'removeBlankLines': '删除空白行',
  'removeTabs': '删除 Tab',
  'scientificToNormal': '科学计数→普通',
  'normalToScientific': '普通→科学计数',
  'thousandsToNormal': '去掉千分位',
  'normalToThousands': '添加千分位',
  'underscoreToCamel': '下划线→驼峰',
  'camelToUnderscore': '驼峰→下划线',
  'uppercase': '转大写',
  'lowercase': '转小写',
  'linesToComma': '多行→逗号',
  'linesToSingleQuoted': "多行→'列表'",
  'linesToDoubleQuoted': '多行→"列表"',
  'commaToLines': '逗号→多行',
  'tabsToLines': 'Tab→多行',
  'clearNewlines': '去掉换行',
  'deduplicateLines': '去重',
  'deduplicateWithCount': '去重并计数',
  'escape': '转义',
  'unescape': '反转义',
  'reverseLines': '行倒序',
  'sortAscending': '行升序',
  'sortDescending': '行降序',
};

const quickReplaceActionIds = [
  'trim',
  'removeBlankLines',
  'removeTabs',
  'scientificToNormal',
  'normalToScientific',
  'thousandsToNormal',
  'normalToThousands',
  'underscoreToCamel',
  'camelToUnderscore',
  'uppercase',
  'lowercase',
  'linesToComma',
  'linesToSingleQuoted',
  'linesToDoubleQuoted',
  'commaToLines',
  'tabsToLines',
  'clearNewlines',
  'deduplicateLines',
  'deduplicateWithCount',
  'escape',
  'unescape',
  'reverseLines',
  'sortAscending',
  'sortDescending',
];

String runQuickReplace(String input, String action) {
  final lines = _normalizeLines(input);
  switch (action) {
    case 'trim':
      return lines.map((line) => line.trim()).join('\n');
    case 'removeBlankLines':
      return lines.where((line) => line.trim().isNotEmpty).join('\n');
    case 'removeTabs':
      return input.replaceAll('\t', '');
    case 'scientificToNormal':
      return _replaceNumbers(input, _scientificToNormal);
    case 'normalToScientific':
      return _replaceNumbers(input, (value) {
        final parsed = num.parse(value.replaceAll(',', ''));
        return parsed.toStringAsExponential();
      });
    case 'thousandsToNormal':
      return input.replaceAllMapped(
          RegExp(r'(?<=\d),(?=\d{3}(?:\D|$))'), (_) => '');
    case 'normalToThousands':
      return _replaceNumbers(input, _addThousands);
    case 'underscoreToCamel':
      return input.replaceAllMapped(
          RegExp(r'_([a-zA-Z0-9])'), (match) => match[1]!.toUpperCase());
    case 'camelToUnderscore':
      return input.replaceAllMapped(
          RegExp(r'([a-z0-9])([A-Z])'), (match) => '${match[1]}_${match[2]}').toLowerCase();
    case 'uppercase':
      return input.toUpperCase();
    case 'lowercase':
      return input.toLowerCase();
    case 'linesToComma':
      return _nonEmpty(lines).join(',');
    case 'linesToSingleQuoted':
      return _nonEmpty(lines)
          .map((line) => "'${line.replaceAll("'", r"\'")}'")
          .join(',');
    case 'linesToDoubleQuoted':
      return _nonEmpty(lines)
          .map((line) => '"${line.replaceAll('"', r'\"')}"')
          .join(',');
    case 'commaToLines':
      return input
          .split(',')
          .map((item) => _unquote(item.trim()))
          .where((item) => item.isNotEmpty)
          .join('\n');
    case 'tabsToLines':
      return input.split('\t').join('\n');
    case 'clearNewlines':
      return lines.join();
    case 'deduplicateLines':
      return {...lines}.join('\n');
    case 'deduplicateWithCount':
      final counts = <String, int>{};
      for (final line in lines) {
        counts[line] = (counts[line] ?? 0) + 1;
      }
      return [
        for (final entry in counts.entries) '${entry.key}\t${entry.value}'
      ].join('\n');
    case 'escape':
      final encoded = jsonEncode(input);
      return encoded.substring(1, encoded.length - 1);
    case 'unescape':
      return jsonDecode('"${input.replaceAll('"', r'\"')}"') as String;
    case 'reverseLines':
      return lines.reversed.join('\n');
    case 'sortAscending':
      final sorted = [...lines]..sort();
      return sorted.join('\n');
    case 'sortDescending':
      final sorted = [...lines]..sort((a, b) => b.compareTo(a));
      return sorted.join('\n');
    default:
      throw FormatException('Unknown quick replace $action');
  }
}

List<String> _normalizeLines(String value) =>
    value.replaceAll('\r\n', '\n').replaceAll('\r', '\n').split('\n');

List<String> _nonEmpty(List<String> lines) =>
    [for (final line in lines) line.trim()].where((line) => line.isNotEmpty).toList();

String _replaceNumbers(String input, String Function(String value) transform) {
  return input.replaceAllMapped(
      RegExp(r'[-+]?(?:\d[\d,]*\.?\d*|\.\d+)(?:e[-+]?\d+)?', caseSensitive: false),
      (match) {
    final value = match[0]!;
    final parsed = num.tryParse(value.replaceAll(',', ''));
    return parsed != null && parsed.isFinite ? transform(value) : value;
  });
}

String _scientificToNormal(String value) {
  final normalized = value.replaceAll(',', '');
  if (!RegExp(r'[eE]').hasMatch(normalized)) return normalized;
  final parts = normalized.toLowerCase().split('e');
  final exponent = int.parse(parts[1]);
  final coefficient = parts[0];
  final negative = coefficient.startsWith('-');
  final unsigned = coefficient.replaceFirst(RegExp(r'^[+-]'), '');
  final pieces = unsigned.split('.');
  final integer = pieces[0];
  final fraction = pieces.length > 1 ? pieces[1] : '';
  final digits = '$integer$fraction';
  final decimalIndex = integer.length + exponent;
  final result = decimalIndex <= 0
      ? '0.${'0' * (-decimalIndex)}$digits'
      : decimalIndex >= digits.length
          ? '$digits${'0' * (decimalIndex - digits.length)}'
          : '${digits.substring(0, decimalIndex)}.${digits.substring(decimalIndex)}';
  return negative ? '-$result' : result;
}

String _addThousands(String value) {
  final normalized = value.replaceAll(',', '');
  if (RegExp(r'[eE]').hasMatch(normalized)) return normalized;
  final pieces = normalized.split('.');
  final integer = pieces[0];
  final fraction = pieces.length > 1 ? pieces[1] : null;
  final sign = integer.startsWith('-') || integer.startsWith('+') ? integer[0] : '';
  final digits = sign.isEmpty ? integer : integer.substring(1);
  final grouped = digits.replaceAllMapped(
      RegExp(r'\B(?=(\d{3})+(?!\d))'), (match) => ',');
  return '$sign$grouped${fraction == null ? '' : '.$fraction'}';
}

String _unquote(String value) {
  if (value.length >= 2 &&
      ((value.startsWith('"') && value.endsWith('"')) ||
          (value.startsWith("'") && value.endsWith("'")))) {
    return value.substring(1, value.length - 1);
  }
  return value;
}
