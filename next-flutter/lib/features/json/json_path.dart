import 'dart:convert';

import 'json_engine.dart';

class JsonPathQuery {
  JsonPathQuery(this.t);
  final Translate t;

  String query(String input, String path) {
    if (path.trim().isEmpty) throw JsonException(t('json.error.emptyPath'));
    final json = _decode(input, t);
    return formatJsonPathValue(evaluate(json, path.trim()));
  }

  List<JsonPathEntry> list(String input) {
    final entries = <JsonPathEntry>[];
    collect(_decode(input, t), r'$', r'$', 0, entries);
    return entries;
  }
}

Object? _decode(String input, Translate t) {
  if (input.trim().isEmpty) throw JsonException(t('json.error.empty'));
  try {
    return jsonDecode(input);
  } catch (error) {
    throw JsonException(error.toString());
  }
}

Object? evaluate(Object? json, String path) {
  final tokens = tokenize(path);
  var current = <Object?>[json];
  for (final token in tokens) {
    current = token(current);
  }
  if (current.length == 1) return current.first;
  return current;
}

String formatJsonPathValue(Object? value) {
  if (value is String) return jsonEncode(value);
  if (identical(value, null) && value == null) {
    // jsonpath-plus wrap:false can yield undefined; Dart uses null.
  }
  return const JsonEncoder.withIndent('  ').convert(value);
}

void collect(Object? value, String path, String label, int depth,
    List<JsonPathEntry> entries) {
  entries
      .add(JsonPathEntry(path: path, label: label, value: value, depth: depth));
  if (value is List) {
    for (var i = 0; i < value.length; i++) {
      collect(value[i], '$path[$i]', '[$i]', depth + 1, entries);
    }
    return;
  }
  if (value is Map) {
    for (final entry in value.entries) {
      final key = '${entry.key}';
      final childPath = RegExp(r'^[a-zA-Z_$][\w$]*$').hasMatch(key)
          ? '$path.$key'
          : '$path[${jsonEncode(key)}]';
      collect(entry.value, childPath, key, depth + 1, entries);
    }
  }
}

typedef PathStep = List<Object?> Function(List<Object?> current);

List<PathStep> tokenize(String path) {
  var index = 0;
  final steps = <PathStep>[];
  if (path.startsWith(r'$')) index = 1;
  while (index < path.length) {
    final char = path[index];
    if (char == '.') {
      if (index + 1 < path.length && path[index + 1] == '.') {
        index += 2;
        final name = readIdentifier(path, index);
        index = name.next;
        final key = name.value.isEmpty || name.value == '*' ? null : name.value;
        steps.add((current) => recursive(current, key));
      } else {
        index++;
        if (index < path.length && path[index] == '*') {
          index++;
          steps.add(wildcard);
        } else {
          final name = readIdentifier(path, index);
          index = name.next;
          steps.add(child(name.value));
        }
      }
    } else if (char == '[') {
      index++;
      if (index < path.length && (path[index] == "'" || path[index] == '"')) {
        final quote = path[index++];
        final start = index;
        while (index < path.length && path[index] != quote) {
          index++;
        }
        final name = path.substring(start, index);
        index += 2;
        steps.add(child(name));
      } else if (index < path.length && path[index] == '*') {
        index += 2;
        steps.add(wildcard);
      } else if (index < path.length && path[index] == '?') {
        final end = path.indexOf(']', index);
        final expr = path.substring(index + 1, end);
        index = end + 1;
        steps.add(filterStep(expr));
      } else {
        final end = path.indexOf(']', index);
        final body = path.substring(index, end);
        index = end + 1;
        if (body.contains(':')) {
          steps.add(sliceStep(body));
        } else if (body.contains(',')) {
          steps.add(
              unionStep([for (final part in body.split(',')) part.trim()]));
        } else {
          steps.add(indexStep(int.parse(body.trim())));
        }
      }
    } else {
      index++;
    }
  }
  return steps;
}

({String value, int next}) readIdentifier(String path, int index) {
  final start = index;
  while (
      index < path.length && RegExp(r'[A-Za-z0-9_$]').hasMatch(path[index])) {
    index++;
  }
  return (value: path.substring(start, index), next: index);
}

PathStep child(String name) => (current) {
      final result = <Object?>[];
      for (final value in current) {
        if (value is Map && value.containsKey(name)) result.add(value[name]);
      }
      return result;
    };

List<Object?> wildcard(List<Object?> current) {
  final result = <Object?>[];
  for (final value in current) {
    if (value is List) result.addAll(value);
    if (value is Map) result.addAll(value.values);
  }
  return result;
}

PathStep indexStep(int n) => (current) {
      final result = <Object?>[];
      for (final value in current) {
        if (value is List) {
          final index = n < 0 ? value.length + n : n;
          if (index >= 0 && index < value.length) result.add(value[index]);
        }
      }
      return result;
    };

PathStep sliceStep(String body) => (current) {
      final parts = body.split(':');
      int? parseAt(int i) => i < parts.length && parts[i].trim().isNotEmpty
          ? int.parse(parts[i].trim())
          : null;
      final start = parseAt(0);
      final end = parseAt(1);
      final step = parseAt(2) ?? 1;
      final result = <Object?>[];
      for (final value in current) {
        if (value is! List) continue;
        var i = start ?? (step >= 0 ? 0 : value.length - 1);
        final stop = end ?? (step >= 0 ? value.length : -1);
        if (step > 0) {
          for (; i < stop && i < value.length; i += step) {
            if (i >= 0) result.add(value[i]);
          }
        } else if (step < 0) {
          for (; i > stop && i >= 0; i += step) {
            if (i < value.length) result.add(value[i]);
          }
        }
      }
      return result;
    };

PathStep unionStep(List<String> parts) => (current) {
      final result = <Object?>[];
      for (final part in parts) {
        final asInt = int.tryParse(part);
        if (asInt != null) {
          result.addAll(indexStep(asInt)(current));
        } else {
          result.addAll(
              child(part.replaceAll('"', '').replaceAll("'", ''))(current));
        }
      }
      return result;
    };

List<Object?> recursive(List<Object?> current, String? name) {
  final result = <Object?>[];
  void walk(Object? value) {
    if (value is List) {
      for (final item in value) {
        if (name == null) result.add(item);
        walk(item);
      }
    } else if (value is Map) {
      for (final entry in value.entries) {
        if (name == null || entry.key == name) result.add(entry.value);
        walk(entry.value);
      }
    }
  }

  for (final value in current) {
    walk(value);
  }
  return result;
}

PathStep filterStep(String expr) {
  final match =
      RegExp(r'^\(\s*@\.([A-Za-z0-9_]+)\s*(==|!=|>=|<=|>|<)\s*(.+)\s*\)$')
          .firstMatch(expr.trim());
  if (match == null) {
    throw JsonException('Unsupported JSONPath filter: $expr');
  }
  final key = match.group(1)!;
  final op = match.group(2)!;
  final raw = match.group(3)!.trim();
  late final Object? expected;
  if ((raw.startsWith("'") && raw.endsWith("'")) ||
      (raw.startsWith('"') && raw.endsWith('"'))) {
    expected = raw.substring(1, raw.length - 1);
  } else if (raw == 'true' || raw == 'false') {
    expected = raw == 'true';
  } else {
    expected = num.parse(raw);
  }
  return (current) {
    final result = <Object?>[];
    for (final value in current) {
      final items = value is List ? value : [value];
      for (final item in items) {
        if (item is Map && compare(item[key], op, expected)) result.add(item);
      }
    }
    return result;
  };
}

bool compare(Object? left, String op, Object? right) {
  if (op == '==') return left == right;
  if (op == '!=') return left != right;
  if (left is num && right is num) {
    return switch (op) {
      '>' => left > right,
      '<' => left < right,
      '>=' => left >= right,
      '<=' => left <= right,
      _ => false,
    };
  }
  return false;
}
