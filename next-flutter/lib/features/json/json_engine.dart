import 'dart:convert';

class JsonException implements Exception {
  JsonException(this.message);
  final String message;
  @override
  String toString() => message;
}

typedef Translate = String Function(String key, [Map<String, String>? params]);

class JsonStatus {
  const JsonStatus(this.kind, this.message);
  final String kind;
  final String message;
}

class JsonFormatOptions {
  const JsonFormatOptions(
      {this.spaces = 2,
      this.sortKeys = false,
      this.ignoreCase = false,
      this.checkDuplicateKeys = true});
  final int spaces;
  final bool sortKeys;
  final bool ignoreCase;
  final bool checkDuplicateKeys;

  JsonFormatOptions copyWith(
          {int? spaces,
          bool? sortKeys,
          bool? ignoreCase,
          bool? checkDuplicateKeys}) =>
      JsonFormatOptions(
        spaces: spaces ?? this.spaces,
        sortKeys: sortKeys ?? this.sortKeys,
        ignoreCase: ignoreCase ?? this.ignoreCase,
        checkDuplicateKeys: checkDuplicateKeys ?? this.checkDuplicateKeys,
      );

  Map<String, Object?> toJson() => {
        'spaces': spaces,
        'sortKeys': sortKeys,
        'ignoreCase': ignoreCase,
        'checkDuplicateKeys': checkDuplicateKeys,
      };

  factory JsonFormatOptions.fromJson(Map<String, Object?> json) =>
      JsonFormatOptions(
        spaces: json['spaces'] as int? ?? 2,
        sortKeys: json['sortKeys'] as bool? ?? false,
        ignoreCase: json['ignoreCase'] as bool? ?? false,
        checkDuplicateKeys: json['checkDuplicateKeys'] as bool? ?? true,
      );
}

class JsonPathEntry {
  const JsonPathEntry(
      {required this.path,
      required this.label,
      required this.value,
      required this.depth});
  final String path;
  final String label;
  final Object? value;
  final int depth;
}

class JsonEngine {
  JsonEngine(this.t);
  final Translate t;

  String format(String input, {int spaces = 2}) =>
      _encode(_parse(input), spaces);

  String compress(String input) => _encode(_parse(input), null);

  String formatAdvanced(String input, JsonFormatOptions options) {
    if (options.checkDuplicateKeys) {
      final duplicates =
          findDuplicateJsonKeys(input, ignoreCase: options.ignoreCase);
      if (duplicates.isNotEmpty) {
        throw JsonException(
            t('json.error.duplicateKeys', {'paths': duplicates.join(', ')}));
      }
    }
    var value = _parse(input);
    if (options.sortKeys) value = sortJsonKeys(value, options.ignoreCase);
    return _encode(value, options.spaces);
  }

  JsonStatus validate(String input) {
    if (input.trim().isEmpty) return JsonStatus('idle', t('json.valid.idle'));
    try {
      final value = _parse(input);
      final label = switch (value) {
        List() => 'Array',
        Map() => 'Object',
        String() => 'string',
        num() => 'number',
        bool() => 'boolean',
        _ => 'null',
      };
      return JsonStatus('valid', t('json.valid.ok', {'type': label}));
    } catch (error) {
      return JsonStatus('error',
          error is JsonException ? error.message : t('json.valid.error'));
    }
  }

  String escapeJsonString(String input) => _encode(input, null);

  String unescapeJsonString(String input) {
    final parsed = _parse(input);
    if (parsed is! String) throw JsonException(t('json.error.notString'));
    return parsed;
  }

  String escapeJavaString(String input) => input
      .replaceAll('\\', r'\\')
      .replaceAll('\b', r'\b')
      .replaceAll('\f', r'\f')
      .replaceAll('\n', r'\n')
      .replaceAll('\r', r'\r')
      .replaceAll('\t', r'\t')
      .replaceAll('"', r'\"');

  String unescapeJsonText(String input) {
    try {
      final parsed = _parse('"${input.replaceAll('"', r'\"')}"');
      if (parsed is String) return parsed;
    } catch (_) {}
    throw JsonException(t('json.valid.error'));
  }

  String jsonToXml(String input) {
    final value = _parse(input);
    final buffer = StringBuffer();
    _writeXml(buffer, 'root', value, 0);
    return buffer.toString();
  }

  String xmlToJson(String input) {
    if (input.trim().isEmpty) throw JsonException(t('json.error.emptyXml'));
    return _encode(_parseXml(input.trim()), 2);
  }

  String swapKeysAndValues(String input) {
    final value = _parse(input);
    if (value is! Map) throw JsonException(t('json.error.objectRequired'));
    return _encode(_swapObject(Map<String, Object?>.from(value)), 2);
  }

  String javaBeanToJson(String input) {
    if (input.trim().isEmpty)
      throw JsonException(t('json.error.emptyJavaBean'));
    final result = <String, Object?>{};
    final fieldPattern = RegExp(
      r'^(?:(?:public|protected|private)\s+)?(?:(?:static|final|transient|volatile)\s+)*([\w$.<>?, \[\]]+?)\s+(\w+)\s*(?:=.*)?$',
    );
    for (final statement in input.split(';')) {
      final boundary = [
        statement.lastIndexOf('{'),
        statement.lastIndexOf('}'),
      ].reduce((a, b) => a > b ? a : b);
      final match =
          fieldPattern.firstMatch(statement.substring(boundary + 1).trim());
      if (match == null) continue;
      final type = match.group(1)!.trim();
      final name = match.group(2)!;
      if (name == 'serialVersionUID') continue;
      result[name] = _mockJavaValue(type);
    }
    if (result.isEmpty) throw JsonException(t('json.error.noJavaFields'));
    return _encode(result, 2);
  }

  String jsonToJavaBean(String input, {String rootClassName = 'Root'}) {
    final value = _parse(input);
    if (value is! Map) throw JsonException(t('json.error.objectRequired'));
    return _buildJavaClass(_toPascalCase(rootClassName),
        Map<String, Object?>.from(value), 0, true);
  }

  Object? _parse(String input) {
    if (input.trim().isEmpty) throw JsonException(t('json.error.empty'));
    try {
      return const JsonCodec().decode(input);
    } catch (error) {
      throw JsonException(error.toString());
    }
  }
}

const _codec = JsonCodec();

String _encode(Object? value, int? spaces) {
  if (spaces == null) return _codec.encode(value);
  return JsonEncoder.withIndent(' ' * spaces).convert(value);
}

List<String> findDuplicateJsonKeys(String input, {bool ignoreCase = false}) {
  _codec.decode(input);
  return _DuplicateKeyParser(input, ignoreCase).parse();
}

Object? sortJsonKeys(Object? value, bool ignoreCase) {
  if (value is List)
    return [for (final item in value) sortJsonKeys(item, ignoreCase)];
  if (value is! Map) return value;
  final keys = value.keys.map((key) => key.toString()).toList()
    ..sort((left, right) {
      final compared = ignoreCase
          ? left.toLowerCase().compareTo(right.toLowerCase())
          : left.compareTo(right);
      return compared != 0 ? compared : left.compareTo(right);
    });
  return {for (final key in keys) key: sortJsonKeys(value[key], ignoreCase)};
}

Map<String, Object?> _swapObject(Map<String, Object?> value) {
  final result = <String, Object?>{};
  for (final entry in value.entries) {
    final item = entry.value;
    if (item is Map) {
      result[entry.key] = _swapObject(Map<String, Object?>.from(item));
      continue;
    }
    final swappedKey = item is List ? _encode(item, null) : '$item';
    result[swappedKey] = entry.key;
  }
  return result;
}

void _writeXml(StringBuffer buffer, String tag, Object? value, int depth) {
  final indent = '  ' * depth;
  if (value is List) {
    if (value.isEmpty) {
      buffer.writeln('$indent<$tag></$tag>');
      return;
    }
    for (final item in value) {
      _writeXml(buffer, tag, item, depth);
    }
    return;
  }
  if (value is Map) {
    buffer.writeln('$indent<$tag>');
    for (final entry in value.entries) {
      _writeXml(buffer, _xmlTag(entry.key.toString()), entry.value, depth + 1);
    }
    buffer.writeln('$indent</$tag>');
    return;
  }
  if (value == null) {
    buffer.writeln('$indent<$tag></$tag>');
    return;
  }
  buffer.writeln('$indent<$tag>${_escapeXml('$value')}</$tag>');
}

String _xmlTag(String key) {
  final cleaned = key.replaceAll(RegExp(r'[^A-Za-z0-9_\-.]'), '_');
  return cleaned.isEmpty ? 'item' : cleaned;
}

String _escapeXml(String value) => value
    .replaceAll('&', '&amp;')
    .replaceAll('<', '&lt;')
    .replaceAll('>', '&gt;')
    .replaceAll('"', '&quot;');

Object? _parseXml(String source) {
  final parser = _XmlMiniParser(source);
  return parser.parse();
}

Object? _mockJavaValue(String type) {
  final normalized = type.replaceAll(' ', '');
  if (normalized.endsWith('[]') ||
      RegExp(r'^(List|Set|Collection|Iterable)<').hasMatch(normalized))
    return <Object?>[];
  if (RegExp(r'^(Map|HashMap|LinkedHashMap)<').hasMatch(normalized))
    return <String, Object?>{};
  if (RegExp(r'^(boolean|Boolean)$').hasMatch(normalized)) return false;
  if (RegExp(
          r'^(byte|short|int|long|float|double|Byte|Short|Integer|Long|Float|Double|BigDecimal|BigInteger)$')
      .hasMatch(normalized)) {
    return 0;
  }
  if (RegExp(r'^(char|Character|String|CharSequence)$').hasMatch(normalized))
    return '';
  return null;
}

String _buildJavaClass(
    String className, Map<String, Object?> value, int depth, bool root) {
  final fields = <String>[];
  final childClasses = <({String name, Map<String, Object?> value})>[];
  final indent = '    ' * depth;
  final bodyIndent = '    ' * (depth + 1);
  for (final entry in value.entries) {
    final type = _inferJavaType(entry.key, entry.value, childClasses);
    fields.add('$bodyIndent private $type ${_toJavaIdentifier(entry.key)};');
  }
  final declaration =
      root ? 'public class $className' : 'public static class $className';
  final children = [
    for (final child in childClasses)
      _buildJavaClass(child.name, child.value, depth + 1, false)
  ];
  final members = [...fields, ...children];
  return '$indent$declaration {\n${members.join('\n\n')}\n$indent}';
}

String _inferJavaType(String key, Object? value,
    List<({String name, Map<String, Object?> value})> childClasses) {
  if (value == null) return 'Object';
  if (value is String) return 'String';
  if (value is bool) return 'Boolean';
  if (value is int) return 'Long';
  if (value is num) return 'Double';
  if (value is List) {
    final first = value
        .cast<Object?>()
        .firstWhere((item) => item != null, orElse: () => null);
    if (first == null) return 'List<Object>';
    if (first is Map) {
      final name = _toPascalCase(_singularize(key));
      childClasses.add((name: name, value: Map<String, Object?>.from(first)));
      return 'List<$name>';
    }
    return 'List<${_inferJavaType(key, first, childClasses)}>';
  }
  if (value is Map) {
    final name = _toPascalCase(key);
    childClasses.add((name: name, value: Map<String, Object?>.from(value)));
    return name;
  }
  return 'Object';
}

String _toPascalCase(String value) {
  final normalized = value.replaceAllMapped(
      RegExp(r'[^a-zA-Z0-9]+(.)'), (match) => match.group(1)!.toUpperCase());
  final result = normalized.isEmpty
      ? 'Root'
      : '${normalized[0].toUpperCase()}${normalized.substring(1)}';
  return RegExp(r'^\d').hasMatch(result) ? 'Type$result' : result;
}

String _toJavaIdentifier(String value) {
  final normalized = value.replaceAll(RegExp(r'[^a-zA-Z0-9_$]'), '_');
  final ident = normalized.isEmpty ? 'value' : normalized;
  return RegExp(r'^\d').hasMatch(ident) ? '_$ident' : ident;
}

String _singularize(String value) {
  if (value.endsWith('ies')) return '${value.substring(0, value.length - 3)}y';
  if (value.endsWith('s')) return value.substring(0, value.length - 1);
  return value;
}

class _DuplicateKeyParser {
  _DuplicateKeyParser(this.source, this.ignoreCase);
  final String source;
  final bool ignoreCase;
  int index = 0;
  final duplicates = <String>[];

  List<String> parse() {
    parseValue(r'$');
    return duplicates;
  }

  void parseValue(String path) {
    skipWhitespace();
    if (index >= source.length) return;
    final token = source[index];
    if (token == '{') {
      parseObject(path);
    } else if (token == '[') {
      parseArray(path);
    } else if (token == '"') {
      parseString();
    } else {
      parsePrimitive();
    }
  }

  void parseObject(String path) {
    index++;
    skipWhitespace();
    final keys = <String>{};
    if (index < source.length && source[index] == '}') {
      index++;
      return;
    }
    while (index < source.length) {
      skipWhitespace();
      final key = parseString();
      final normalized = ignoreCase ? key.toLowerCase() : key;
      final keyPath = RegExp(r'^[a-zA-Z_$][\w$]*$').hasMatch(key)
          ? '$path.$key'
          : '$path[${_encode(key, null)}]';
      if (keys.contains(normalized)) duplicates.add(keyPath);
      keys.add(normalized);
      skipWhitespace();
      index++;
      parseValue(keyPath);
      skipWhitespace();
      if (index >= source.length) return;
      final next = source[index++];
      if (next == '}') return;
    }
  }

  void parseArray(String path) {
    index++;
    skipWhitespace();
    if (index < source.length && source[index] == ']') {
      index++;
      return;
    }
    var itemIndex = 0;
    while (index < source.length) {
      parseValue('$path[${itemIndex++}]');
      skipWhitespace();
      if (index >= source.length) return;
      final next = source[index++];
      if (next == ']') return;
    }
  }

  String parseString() {
    final start = index++;
    var escaped = false;
    while (index < source.length) {
      final character = source[index++];
      if (escaped) {
        escaped = false;
      } else if (character == r'\') {
        escaped = true;
      } else if (character == '"') {
        break;
      }
    }
    return _codec.decode(source.substring(start, index)) as String;
  }

  void parsePrimitive() {
    while (
        index < source.length && !RegExp(r'[\s,}\]]').hasMatch(source[index])) {
      index++;
    }
  }

  void skipWhitespace() {
    while (index < source.length && RegExp(r'\s').hasMatch(source[index])) {
      index++;
    }
  }
}

class _XmlMiniParser {
  _XmlMiniParser(this.source);
  final String source;
  int index = 0;

  Object? parse() {
    skip();
    final value = parseNode();
    return value;
  }

  Object? parseNode() {
    skip();
    if (index >= source.length || source[index] != '<') return null;
    if (source.startsWith('<?', index) || source.startsWith('<!', index)) {
      final end = source.indexOf('>', index);
      index = end + 1;
      return parseNode();
    }
    index++;
    final name = readName();
    skip();
    if (index < source.length && source[index] == '/') {
      index += 2;
      return {name: ''};
    }
    index++;
    final children = <String, Object?>{};
    final texts = <String>[];
    while (index < source.length) {
      skip();
      if (source.startsWith('</', index)) {
        index += 2;
        readName();
        if (index < source.length && source[index] == '>') index++;
        break;
      }
      if (source[index] == '<') {
        final child = parseNode();
        if (child is Map<String, Object?>) {
          child.forEach((key, value) => _push(children, key, value));
        }
      } else {
        texts.add(readText());
      }
    }
    if (children.isEmpty) {
      final text = texts.join().trim();
      return {name: _coerce(text)};
    }
    return {name: children};
  }

  void _push(Map<String, Object?> target, String key, Object? value) {
    if (!target.containsKey(key)) {
      target[key] = value;
      return;
    }
    final existing = target[key];
    if (existing is List) {
      existing.add(value);
    } else {
      target[key] = [existing, value];
    }
  }

  String readName() {
    final start = index;
    while (index < source.length &&
        RegExp(r'[A-Za-z0-9_\-.:]').hasMatch(source[index])) {
      index++;
    }
    return source.substring(start, index);
  }

  String readText() {
    final start = index;
    while (index < source.length && source[index] != '<') {
      index++;
    }
    return source
        .substring(start, index)
        .replaceAll('&lt;', '<')
        .replaceAll('&gt;', '>')
        .replaceAll('&quot;', '"')
        .replaceAll('&amp;', '&');
  }

  Object? _coerce(String text) {
    if (text == 'true') return true;
    if (text == 'false') return false;
    if (text == 'null' || text.isEmpty) return text.isEmpty ? '' : null;
    final asInt = int.tryParse(text);
    if (asInt != null) return asInt;
    final asDouble = double.tryParse(text);
    if (asDouble != null) return asDouble;
    return text;
  }

  void skip() {
    while (index < source.length && RegExp(r'\s').hasMatch(source[index])) {
      index++;
    }
  }
}
