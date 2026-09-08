class CalculatorEngine {
  String evaluateExpression(String expression) {
    final source = expression.trim().replaceFirst(RegExp(r'=$'), '');
    if (source.isEmpty ||
        source.length > 500 ||
        !RegExp(r'^[\d+\-*/().\s]+$').hasMatch(source)) {
      throw const FormatException('Invalid expression');
    }
    final result = _ArithmeticParser(source).parse();
    if (!result.isFinite) {
      throw const FormatException('Expression did not produce a finite number');
    }
    final formatted = double.parse(result.toStringAsPrecision(14));
    if (formatted % 1 == 0 && formatted.abs() < 1e15) {
      return formatted.toInt().toString();
    }
    return formatted.toString();
  }

  String convertBase(String value, int from, int to) {
    final normalized = value.trim();
    if (normalized.isEmpty) throw const FormatException('A value is required');
    final negative = normalized.startsWith('-');
    final unsigned = normalized.replaceFirst(RegExp(r'^[+-]'), '');
    final valid = from == 2
        ? RegExp(r'^[01]+$')
        : from == 10
            ? RegExp(r'^\d+$')
            : RegExp(r'^[\da-f]+$', caseSensitive: false);
    if (!valid.hasMatch(unsigned)) {
      throw const FormatException('Invalid value for the selected base');
    }
    final parsed = from == 10
        ? BigInt.parse(unsigned)
        : BigInt.parse(unsigned, radix: from);
    final converted = parsed.toRadixString(to);
    return '${negative ? '-' : ''}$converted';
  }

  String gcd(String left, String right) {
    var a = _abs(_parseInteger(left));
    var b = _abs(_parseInteger(right));
    while (b != BigInt.zero) {
      final next = a % b;
      a = b;
      b = next;
    }
    return a.toString();
  }

  String lcm(String left, String right) {
    final a = _parseInteger(left);
    final b = _parseInteger(right);
    if (a == BigInt.zero || b == BigInt.zero) return '0';
    return (_abs(a ~/ BigInt.parse(gcd(left, right)) * b)).toString();
  }

  String permutation(String nValue, String mValue) {
    final pair = _parseCountPair(nValue, mValue);
    var result = BigInt.one;
    for (var value = pair.$1 - pair.$2 + 1; value <= pair.$1; value++) {
      result *= BigInt.from(value);
    }
    return result.toString();
  }

  String combination(String nValue, String mValue) {
    final pair = _parseCountPair(nValue, mValue);
    final n = pair.$1;
    final requested = pair.$2;
    final m = requested < n - requested ? requested : n - requested;
    var result = BigInt.one;
    for (var index = 1; index <= m; index++) {
      result = result * BigInt.from(n - m + index) ~/ BigInt.from(index);
    }
    return result.toString();
  }

  BigInt _parseInteger(String value) {
    if (!RegExp(r'^[+-]?\d+$').hasMatch(value.trim())) {
      throw const FormatException('An integer is required');
    }
    return BigInt.parse(value.trim());
  }

  (int, int) _parseCountPair(String nValue, String mValue) {
    final n = int.parse(nValue);
    final m = int.parse(mValue);
    if (n < 0 || m < 0 || m > n || n > 5000) {
      throw const FormatException('Require 0 <= m <= n <= 5000');
    }
    return (n, m);
  }

  BigInt _abs(BigInt value) => value < BigInt.zero ? -value : value;
}

class _ArithmeticParser {
  _ArithmeticParser(this.source);
  final String source;
  var position = 0;

  double parse() {
    final result = _parseAddition();
    _skipWhitespace();
    if (position != source.length)
      throw const FormatException('Invalid expression');
    return result;
  }

  double _parseAddition() {
    var result = _parseMultiplication();
    while (true) {
      if (_consume('+')) {
        result += _parseMultiplication();
      } else if (_consume('-')) {
        result -= _parseMultiplication();
      } else {
        return result;
      }
    }
  }

  double _parseMultiplication() {
    var result = _parseUnary();
    while (true) {
      if (_consume('*')) {
        result *= _parseUnary();
      } else if (_consume('/')) {
        result /= _parseUnary();
      } else {
        return result;
      }
    }
  }

  double _parseUnary() {
    if (_consume('+')) return _parseUnary();
    if (_consume('-')) return -_parseUnary();
    return _parsePrimary();
  }

  double _parsePrimary() {
    if (_consume('(')) {
      final result = _parseAddition();
      if (!_consume(')')) throw const FormatException('Invalid expression');
      return result;
    }
    _skipWhitespace();
    final match = RegExp(r'^(?:\d+(?:\.\d*)?|\.\d+)')
        .matchAsPrefix(source.substring(position));
    if (match == null) throw const FormatException('Invalid expression');
    position += match.end;
    return double.parse(match.group(0)!);
  }

  bool _consume(String token) {
    _skipWhitespace();
    if (position >= source.length || source[position] != token) return false;
    position += 1;
    return true;
  }

  void _skipWhitespace() {
    while (
        position < source.length && RegExp(r'\s').hasMatch(source[position])) {
      position += 1;
    }
  }
}
