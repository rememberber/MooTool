ParsedVersion parseVersion(String raw) {
  var value = raw.trim();
  if (value.startsWith('v') || value.startsWith('V')) {
    value = value.substring(1);
  }
  final plus = value.indexOf('+');
  if (plus >= 0) value = value.substring(0, plus);
  final dash = value.indexOf('-');
  final core = dash >= 0 ? value.substring(0, dash) : value;
  final pre = dash >= 0 ? value.substring(dash + 1) : '';
  final parts = core.split('.');
  if (parts.isEmpty || parts.length > 3) {
    throw FormatException('Invalid version: $raw');
  }
  final numbers = List<int>.filled(3, 0);
  for (var i = 0; i < parts.length; i++) {
    if (!RegExp(r'^\d+$').hasMatch(parts[i])) {
      throw FormatException('Invalid version: $raw');
    }
    numbers[i] = int.parse(parts[i]);
  }
  final prerelease = <String>[];
  if (pre.isNotEmpty) {
    for (final token in pre.split('.')) {
      if (token.isEmpty) throw FormatException('Invalid version: $raw');
      if (RegExp(r'^\d+$').hasMatch(token) &&
          token.length > 1 &&
          token.startsWith('0')) {
        throw FormatException('Invalid version: $raw');
      }
      prerelease.add(token);
    }
  }
  return ParsedVersion(numbers, prerelease);
}

class ParsedVersion {
  ParsedVersion(this.numbers, this.prerelease);
  final List<int> numbers;
  final List<String> prerelease;
}

int compareVersions(String left, String right) {
  final a = parseVersion(left);
  final b = parseVersion(right);
  for (var index = 0; index < 3; index++) {
    if (a.numbers[index] != b.numbers[index]) {
      return a.numbers[index] > b.numbers[index] ? 1 : -1;
    }
  }
  if (a.prerelease.isEmpty && b.prerelease.isEmpty) return 0;
  if (a.prerelease.isEmpty) return 1;
  if (b.prerelease.isEmpty) return -1;
  final max = a.prerelease.length > b.prerelease.length
      ? a.prerelease.length
      : b.prerelease.length;
  for (var index = 0; index < max; index++) {
    if (index >= a.prerelease.length) return -1;
    if (index >= b.prerelease.length) return 1;
    final leftPart = a.prerelease[index];
    final rightPart = b.prerelease[index];
    if (leftPart == rightPart) continue;
    final leftNumeric = RegExp(r'^\d+$').hasMatch(leftPart);
    final rightNumeric = RegExp(r'^\d+$').hasMatch(rightPart);
    if (leftNumeric && rightNumeric) {
      return int.parse(leftPart) > int.parse(rightPart) ? 1 : -1;
    }
    if (leftNumeric != rightNumeric) return leftNumeric ? -1 : 1;
    return leftPart.compareTo(rightPart);
  }
  return 0;
}

String normalizeVersion(String raw) {
  final parsed = parseVersion(raw);
  final core = parsed.numbers.join('.');
  if (parsed.prerelease.isEmpty) return core;
  return '$core-${parsed.prerelease.join('.')}';
}
