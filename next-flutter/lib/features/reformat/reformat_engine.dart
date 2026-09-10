class ReformatEngine {
  String formatCode(String input, String type, [int indent = 4]) {
    if (input.trim().isEmpty) return '';
    final tabWidth = indent.round().clamp(1, 8);
    switch (type) {
      case 'nginx':
        return formatNginx(input, tabWidth);
      case 'xml':
      case 'html':
        return _formatMarkup(input, tabWidth);
      case 'java':
        return _formatBraces(input, tabWidth);
      default:
        throw FormatException('Unsupported reformat type $type');
    }
  }

  String formatNginx(String input, [int indent = 4]) {
    final tokens = _tokenizeNginx(input);
    final lines = <String>[];
    var level = 0;
    for (final token in tokens) {
      if (token == '}') level = level > 0 ? level - 1 : 0;
      if (token.isNotEmpty) lines.add('${' ' * (level * indent)}$token');
      if (token.endsWith('{')) level += 1;
    }
    return lines.join('\n');
  }

  List<String> _tokenizeNginx(String input) {
    final tokens = <String>[];
    var current = '';
    var quote = '';
    var escaped = false;
    var comment = false;

    void flush() {
      final value = current.trim();
      if (value.isNotEmpty) tokens.add(value);
      current = '';
    }

    for (var index = 0; index < input.length; index++) {
      final char = input[index];
      if (comment) {
        current += char;
        if (char == '\n') {
          flush();
          comment = false;
        }
        continue;
      }
      if (escaped) {
        current += char;
        escaped = false;
        continue;
      }
      if (char == r'\') {
        current += char;
        escaped = true;
        continue;
      }
      if (quote.isNotEmpty) {
        current += char;
        if (char == quote) quote = '';
        continue;
      }
      if (char == '"' || char == "'") {
        quote = char;
        current += char;
        continue;
      }
      if (char == '#') {
        comment = true;
        current += char;
        continue;
      }
      if (char == '{') {
        current = '${current.trimRight()} {';
        flush();
      } else if (char == '}') {
        flush();
        tokens.add('}');
      } else if (char == ';') {
        current = '${current.trimRight()};';
        flush();
      } else if (char == '\n' || char == '\r') {
        flush();
      } else {
        current += char;
      }
    }
    flush();
    return tokens;
  }

  String _formatMarkup(String input, int indent) {
    final tokens = <String>[];
    final expression = RegExp(r'<[^>]+>|[^<]+');
    for (final match in expression.allMatches(input.trim())) {
      final token = match.group(0)!.trim();
      if (token.isNotEmpty) tokens.add(token);
    }
    final lines = <String>[];
    var level = 0;
    for (final token in tokens) {
      final closing = RegExp(r'^</').hasMatch(token);
      final selfClosing = RegExp(r'/>$').hasMatch(token) ||
          RegExp(r'^<(?:\?|!|area|br|hr|img|input|meta|link)\b',
                  caseSensitive: false)
              .hasMatch(token);
      if (!token.startsWith('<')) {
        if (lines.isEmpty) {
          lines.add(token);
        } else {
          lines[lines.length - 1] += token;
        }
        continue;
      }
      if (closing) {
        level = level > 0 ? level - 1 : 0;
        if (lines.isNotEmpty &&
            !lines.last.trimLeft().startsWith('</') &&
            lines.last.contains('<') &&
            !lines.last.contains('</')) {
          lines[lines.length - 1] += token;
          continue;
        }
        lines.add('${' ' * (level * indent)}$token');
        continue;
      }
      lines.add('${' ' * (level * indent)}$token');
      if (!selfClosing) {
        level += 1;
      }
    }
    return lines.join('\n');
  }

  String _formatBraces(String input, int indent) {
    final buffer = StringBuffer();
    var level = 0;
    var quote = '';
    var escaped = false;
    var pendingNewline = false;
    void writeIndent() {
      buffer
        ..writeln()
        ..write(' ' * (level * indent));
    }

    for (var index = 0; index < input.length; index++) {
      final char = input[index];
      if (quote.isNotEmpty) {
        buffer.write(char);
        if (escaped) {
          escaped = false;
        } else if (char == r'\') {
          escaped = true;
        } else if (char == quote) {
          quote = '';
        }
        continue;
      }
      if (char == '"' || char == "'") {
        if (pendingNewline) {
          writeIndent();
          pendingNewline = false;
        }
        quote = char;
        buffer.write(char);
        continue;
      }
      if (char == '{') {
        buffer.write(' {');
        level += 1;
        pendingNewline = true;
        continue;
      }
      if (char == '}') {
        level = level > 0 ? level - 1 : 0;
        writeIndent();
        buffer.write('}');
        pendingNewline = true;
        continue;
      }
      if (char == ';') {
        buffer.write(';');
        pendingNewline = true;
        continue;
      }
      if (char.trim().isEmpty) {
        if (!pendingNewline && buffer.isNotEmpty) buffer.write(' ');
        continue;
      }
      if (pendingNewline) {
        writeIndent();
        pendingNewline = false;
      }
      buffer.write(char);
    }
    return buffer.toString().trim();
  }
}
