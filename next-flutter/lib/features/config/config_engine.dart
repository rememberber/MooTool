import 'package:yaml/yaml.dart';

class YamlValidation {
  const YamlValidation({required this.valid, required this.message});
  final bool valid;
  final String message;
}

class ConfigEngine {
  String propertiesToYaml(String source) {
    final root = <String, Object?>{};
    for (final rawLine in source.split(RegExp(r'\r?\n'))) {
      final line = rawLine.trim();
      if (line.isEmpty || line.startsWith('#') || line.startsWith('!')) {
        continue;
      }
      final separator = _findSeparator(rawLine);
      final key =
          (separator < 0 ? rawLine : rawLine.substring(0, separator)).trim();
      final value =
          (separator < 0 ? '' : rawLine.substring(separator + 1)).trim();
      _assignPath(root, _tokenizePath(key), _decodeProperty(value));
    }
    return _emitYaml(root, indentSize: 4);
  }

  String yamlToProperties(String source) {
    final value = loadYaml(source);
    if (value == null || value is! Map) {
      throw const FormatException('YAML root must be an object');
    }
    final lines = <String>[];
    _flattenYaml(_toPlain(value), '', lines);
    return lines.join('\n');
  }

  String formatYaml(String source) {
    final value = loadYaml(source);
    return _emitYaml(_toPlain(value), indentSize: 2);
  }

  YamlValidation validateYaml(String source) {
    try {
      loadYaml(source);
      return const YamlValidation(valid: true, message: '');
    } catch (error) {
      return YamlValidation(valid: false, message: '$error');
    }
  }

  int _findSeparator(String line) {
    var escaped = false;
    for (var index = 0; index < line.length; index++) {
      if (!escaped && (line[index] == '=' || line[index] == ':')) return index;
      escaped = !escaped && line[index] == r'\';
      if (line[index] != r'\') escaped = false;
    }
    return -1;
  }

  List<Object> _tokenizePath(String key) {
    final tokens = <Object>[];
    for (final part in key.split('.')) {
      final expression = RegExp(r'([^\[\]]+)|\[(\d+)\]');
      for (final match in expression.allMatches(part)) {
        if (match.group(2) == null) {
          tokens.add(match.group(1)!);
        } else {
          tokens.add(int.parse(match.group(2)!));
        }
      }
    }
    return tokens;
  }

  void _assignPath(
      Map<String, Object?> root, List<Object> tokens, String value) {
    Object current = root;
    for (var index = 0; index < tokens.length; index++) {
      final token = tokens[index];
      final last = index == tokens.length - 1;
      if (last) {
        if (current is List && token is int) {
          _ensureListLength(current, token);
          current[token] = value;
        } else if (current is Map && token is String) {
          current[token] = value;
        }
        return;
      }
      final nextIsArray = tokens[index + 1] is int;
      if (current is List && token is int) {
        _ensureListLength(current, token);
        current[token] ??= nextIsArray ? <Object?>[] : <String, Object?>{};
        current = current[token] as Object;
      } else if (current is Map && token is String) {
        current[token] ??= nextIsArray ? <Object?>[] : <String, Object?>{};
        current = current[token] as Object;
      }
    }
  }

  void _ensureListLength(List<Object?> list, int index) {
    while (list.length <= index) {
      list.add(null);
    }
  }

  void _flattenYaml(Object? value, String prefix, List<String> lines) {
    if (value is List) {
      if (value
          .every((item) => item == null || item is! Map && item is! List)) {
        lines.add('$prefix=${value.map(_propertyValue).join(',')}');
      } else {
        for (var index = 0; index < value.length; index++) {
          _flattenYaml(value[index], '$prefix[$index]', lines);
        }
      }
    } else if (value is Map) {
      for (final entry in value.entries) {
        final next = prefix.isEmpty ? '${entry.key}' : '$prefix.${entry.key}';
        _flattenYaml(entry.value, next, lines);
      }
    } else {
      lines.add('$prefix=${_propertyValue(value)}');
    }
  }

  String _propertyValue(Object? value) {
    return '${value ?? ' '}'.replaceAll(r'\', r'\\').replaceAll('\n', r'\n');
  }

  String _decodeProperty(String value) {
    return value
        .replaceAllMapped(RegExp(r'\\u([\da-fA-F]{4})'), (match) {
          return String.fromCharCode(int.parse(match.group(1)!, radix: 16));
        })
        .replaceAll(r'\n', '\n')
        .replaceAll(r'\t', '\t')
        .replaceAllMapped(RegExp(r'\\([:= ])'), (match) => match.group(1)!);
  }

  Object? _toPlain(Object? value) {
    if (value is YamlMap) {
      return {
        for (final entry in value.entries) '${entry.key}': _toPlain(entry.value)
      };
    }
    if (value is YamlList) {
      return [for (final item in value) _toPlain(item)];
    }
    return value;
  }

  String _emitYaml(Object? value, {required int indentSize}) {
    final buffer = StringBuffer();
    _writeYaml(buffer, value, 0, indentSize, true);
    return buffer.toString();
  }

  void _writeYaml(StringBuffer buffer, Object? value, int depth, int indentSize,
      bool isRoot) {
    final indent = ' ' * (depth * indentSize);
    if (value is Map) {
      if (value.isEmpty) {
        buffer.writeln('{}');
        return;
      }
      var first = true;
      for (final entry in value.entries) {
        if (!first || !isRoot) {
          if (!first) {
            // keep newline from previous
          }
        }
        first = false;
        buffer.write('$indent${entry.key}:');
        final child = entry.value;
        if (child is Map || child is List) {
          buffer.writeln();
          _writeYaml(buffer, child, depth + 1, indentSize, false);
        } else {
          buffer.writeln(' ${_scalar(child)}');
        }
      }
    } else if (value is List) {
      if (value.isEmpty) {
        buffer.writeln('$indent[]');
        return;
      }
      for (final item in value) {
        if (item is Map || item is List) {
          buffer.writeln('$indent-');
          _writeYaml(buffer, item, depth + 1, indentSize, false);
        } else {
          buffer.writeln('$indent- ${_scalar(item)}');
        }
      }
    } else {
      buffer.writeln('${isRoot ? '' : indent}${_scalar(value)}');
    }
  }

  String _scalar(Object? value) {
    if (value == null) return 'null';
    final text = '$value';
    if (text.contains(':') || text.contains('#') || text.contains('\n')) {
      return '"${text.replaceAll('"', r'\"')}"';
    }
    return text;
  }
}
