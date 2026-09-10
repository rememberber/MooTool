import 'package:flutter_test/flutter_test.dart';
import 'package:mootool_next_flutter/features/json/json_engine.dart';
import 'package:mootool_next_flutter/features/json/json_path.dart';

const translations = {
  'json.valid.idle': 'idle',
  'json.valid.ok': 'valid {type}',
  'json.valid.error': 'invalid',
  'json.error.empty': 'empty',
  'json.error.notString': 'not string',
  'json.error.duplicateKeys': 'duplicates {paths}',
  'json.error.objectRequired': 'object required',
  'json.error.emptyJavaBean': 'empty bean',
  'json.error.noJavaFields': 'no fields',
  'json.error.emptyXml': 'empty xml',
  'json.error.emptyPath': 'empty path',
};

String t(String key, [Map<String, String>? params]) {
  var message = translations[key] ?? key;
  params?.forEach((name, value) {
    message = message.replaceAll('{$name}', value);
  });
  return message;
}

void main() {
  final engine = JsonEngine(t);
  final paths = JsonPathQuery(t);

  test('formats and compresses JSON without changing its value', () {
    const input = '{"name":"MooTool","items":[1,2]}';
    expect(engine.format(input, spaces: 2),
        '{\n  "name": "MooTool",\n  "items": [\n    1,\n    2\n  ]\n}');
    expect(engine.compress(engine.format(input)), input);
  });

  test('escapes and restores JSON strings', () {
    const input = 'line one\nline two';
    expect(engine.unescapeJsonString(engine.escapeJsonString(input)), input);
  });

  test('reports idle, valid, and invalid input', () {
    expect(engine.validate('').kind, 'idle');
    expect(engine.validate('[]').message, 'valid Array');
    expect(engine.validate('{').kind, 'error');
  });

  test('sorts keys recursively and detects duplicate keys', () {
    const input = '{"z":{"B":1,"a":2},"A":0}';
    expect(
      engine.formatAdvanced(
          input,
          const JsonFormatOptions(
              spaces: 2,
              sortKeys: true,
              ignoreCase: true,
              checkDuplicateKeys: true)),
      '{\n  "A": 0,\n  "z": {\n    "a": 2,\n    "B": 1\n  }\n}',
    );
    expect(
        findDuplicateJsonKeys('{"a":1,"A":2,"child":{"x":1,"x":2}}',
            ignoreCase: true),
        ['\$.A', '\$.child.x']);
  });

  test('converts JSON and XML in both directions', () {
    final xml = engine.jsonToXml('{"name":"MooTool","enabled":true}');
    expect(xml.contains('<name>MooTool</name>'), isTrue);
    expect(
        engine.xmlToJson(
            '<tool><name>MooTool</name><enabled>true</enabled></tool>'),
        contains('"tool"'));
    expect(
        engine.xmlToJson(
            '<tool><name>MooTool</name><enabled>true</enabled></tool>'),
        contains('"name": "MooTool"'));
    expect(
        engine.xmlToJson(
            '<tool><name>MooTool</name><enabled>true</enabled></tool>'),
        contains('"enabled": true'));
  });

  test('queries and enumerates JSON paths', () {
    const input = '{"store":{"books":[{"title":"One"},{"title":"Two"}]}}';
    expect(paths.query(input, r'$.store.books[1].title'), '"Two"');
    expect(paths.list(input).map((entry) => entry.path),
        contains(r'$.store.books[0].title'));
  });

  test('filters JSONPath arrays without evaluating JavaScript', () {
    const input = '{"items":[{"n":1},{"n":2},{"n":3}]}';
    expect(paths.query(input, r'$.items[?(@.n>1)]'), contains('"n": 2'));
  });

  test('swaps object keys and values', () {
    expect(engine.swapKeysAndValues('{"first":"one","second":2}'),
        contains('"one": "first"'));
  });

  test('converts JavaBean fields and emits nested Java classes', () {
    expect(
        engine.javaBeanToJson(
            'public class User { private String name; private int age; private List<String> tags; }'),
        contains('"name": ""'));
    final source = engine.jsonToJavaBean(
        '{"name":"MooTool","profile":{"active":true}}',
        rootClassName: 'ToolConfig');
    expect(source, contains('public class ToolConfig'));
    expect(source, contains('private Profile profile;'));
    expect(source, contains('public static class Profile'));
    expect(source.trim().endsWith('}'), isTrue);
  });

  test('invalid JSON does not destroy the original text contract', () {
    expect(engine.validate('{').kind, 'error');
    expect(() => engine.format('{'), throwsA(isA<JsonException>()));
  });
}
