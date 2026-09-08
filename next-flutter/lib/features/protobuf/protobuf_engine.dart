import 'dart:convert';
import 'dart:typed_data';

import '../../core/samples/dynamic_proto.dart';

class ProtobufEngine {
  String jsonToProtobuf(
      String proto, String messageName, String jsonText, String format) {
    final bytes = DynamicProto.parse(proto)
        .encodeJson(messageName, _asMap(jsonDecode(jsonText)));
    return format == 'Base64' ? base64.encode(bytes) : _toHex(bytes);
  }

  String protobufToJson(
      String proto, String messageName, String payload, String format) {
    final bytes = _decodePayload(payload, format);
    final decoded = DynamicProto.parse(proto).decode(messageName, bytes);
    decoded.removeWhere((key, value) => value == null);
    return const JsonEncoder.withIndent('  ').convert(decoded);
  }

  String convertBinary(String payload, String from, String to) {
    final bytes = _decodePayload(payload, from);
    return to == 'Base64' ? base64.encode(bytes) : _toHex(bytes);
  }

  String decodeWire(String payload, String format) {
    final bytes = _decodePayload(payload, format);
    final lines = <String>[];
    var offset = 0;
    while (offset < bytes.length) {
      final key = _readVarint(bytes, offset);
      offset = key.next;
      final number = key.value >> 3;
      final wire = key.value & 7;
      final decoded = _readWire(bytes, offset, wire);
      offset = decoded.next;
      lines.add('field=$number wire=$wire value=${decoded.value}');
    }
    return lines.join('\n');
  }

  String formatProtoDefinition(String source) {
    final buffer = StringBuffer();
    var level = 0;
    var quote = '';
    var escaped = false;
    var current = StringBuffer();
    void flushStatement({bool closing = false, bool opening = false}) {
      final text = current.toString().trim();
      current = StringBuffer();
      if (closing) {
        level = level > 0 ? level - 1 : 0;
        buffer.writeln('${'  ' * level}}');
      }
      if (text.isNotEmpty) {
        buffer.writeln('${'  ' * level}$text${opening ? ' {' : ';'}');
      } else if (opening) {
        buffer.writeln('${'  ' * level}{');
      }
      if (opening) level += 1;
    }

    for (var index = 0; index < source.length; index++) {
      final char = source[index];
      if (quote.isNotEmpty) {
        current.write(char);
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
        quote = char;
        current.write(char);
        continue;
      }
      if (char == '{') {
        flushStatement(opening: true);
        continue;
      }
      if (char == '}') {
        flushStatement(closing: true);
        continue;
      }
      if (char == ';') {
        flushStatement();
        continue;
      }
      current.write(char);
    }
    final leftover = current.toString().trim();
    if (leftover.isNotEmpty) buffer.writeln('${'  ' * level}$leftover');
    return buffer.toString().trimRight();
  }

  Map<String, Object?> _asMap(Object? value) {
    if (value is Map<String, Object?>) return value;
    if (value is Map) {
      return {for (final entry in value.entries) '${entry.key}': entry.value};
    }
    throw const FormatException('JSON root must be an object');
  }

  Uint8List _decodePayload(String payload, String format) {
    final cleaned = payload.replaceAll(RegExp(r'\s+'), '');
    if (format == 'Base64') return Uint8List.fromList(base64.decode(cleaned));
    if (cleaned.isEmpty || cleaned.length.isOdd) {
      throw const FormatException('Invalid hexadecimal content');
    }
    final bytes = Uint8List(cleaned.length ~/ 2);
    for (var i = 0; i < bytes.length; i++) {
      bytes[i] = int.parse(cleaned.substring(i * 2, i * 2 + 2), radix: 16);
    }
    return bytes;
  }

  String _toHex(List<int> bytes) =>
      bytes.map((byte) => byte.toRadixString(16).padLeft(2, '0')).join();

  ({int value, int next}) _readVarint(Uint8List bytes, int offset) {
    var result = 0;
    var shift = 0;
    var index = offset;
    while (index < bytes.length) {
      final byte = bytes[index++];
      result |= (byte & 0x7f) << shift;
      if (byte < 0x80) return (value: result, next: index);
      shift += 7;
    }
    throw const FormatException('Truncated varint');
  }

  ({Object? value, int next}) _readWire(Uint8List bytes, int offset, int wire) {
    if (wire == 0) {
      final varint = _readVarint(bytes, offset);
      return (value: varint.value, next: varint.next);
    }
    if (wire == 1)
      return (
        value: _toHex(bytes.sublist(offset, offset + 8)),
        next: offset + 8
      );
    if (wire == 5)
      return (
        value: _toHex(bytes.sublist(offset, offset + 4)),
        next: offset + 4
      );
    if (wire == 2) {
      final length = _readVarint(bytes, offset);
      final end = length.next + length.value;
      final slice = bytes.sublist(length.next, end);
      try {
        final text = utf8.decode(slice);
        if (RegExp(r'^[\u0009\u000a\u000d\u0020-\u007e\u00a0-\uFFFF]+$')
            .hasMatch(text)) {
          return (value: jsonEncode(text), next: end);
        }
      } catch (_) {}
      return (value: _toHex(slice), next: end);
    }
    throw FormatException('Unknown wire type $wire');
  }
}
