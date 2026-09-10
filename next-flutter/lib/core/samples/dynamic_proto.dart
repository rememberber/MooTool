import 'dart:convert';
import 'dart:typed_data';

/// Minimal proto3 subset: runtime schema from pasted text, JSON <-> binary.
/// Does not execute plugins or require generated Dart messages.
class DynamicProto {
  DynamicProto(this.messages);
  final Map<String, ProtoMessage> messages;

  static DynamicProto parse(String source) {
    final messages = <String, ProtoMessage>{};
    final messagePattern =
        RegExp(r'message\s+(\w+)\s*\{([^}]*)\}', dotAll: true);
    for (final match in messagePattern.allMatches(source)) {
      final name = match.group(1)!;
      final fields = <ProtoField>[];
      final fieldPattern =
          RegExp(r'(repeated\s+)?(optional\s+)?(\w+)\s+(\w+)\s*=\s*(\d+)\s*;');
      for (final field in fieldPattern.allMatches(match.group(2)!)) {
        fields.add(ProtoField(
          repeated: field.group(1) != null,
          type: field.group(3)!,
          name: field.group(4)!,
          number: int.parse(field.group(5)!),
        ));
      }
      messages[name] = ProtoMessage(name, fields);
    }
    if (messages.isEmpty)
      throw const FormatException('No proto3 message found');
    return DynamicProto(messages);
  }

  Uint8List encodeJson(String messageName, Map<String, Object?> json) {
    final message = messages[messageName];
    if (message == null) throw FormatException('Unknown message $messageName');
    final builder = BytesBuilder();
    for (final field in message.fields) {
      final value = json[field.name];
      if (value == null) continue;
      if (field.repeated && value is List) {
        for (final item in value) {
          _writeField(builder, field, item);
        }
      } else {
        _writeField(builder, field, value);
      }
    }
    return builder.toBytes();
  }

  Map<String, Object?> decode(String messageName, Uint8List bytes) {
    final message = messages[messageName];
    if (message == null) throw FormatException('Unknown message $messageName');
    final byNumber = {for (final field in message.fields) field.number: field};
    final result = <String, Object?>{
      for (final field in message.fields)
        field.name: field.repeated ? <Object?>[] : null
    };
    var offset = 0;
    while (offset < bytes.length) {
      final key = _readVarint(bytes, offset);
      offset = key.next;
      final number = key.value >> 3;
      final wire = key.value & 7;
      final field = byNumber[number];
      if (field == null) {
        offset = _skip(bytes, offset, wire);
        continue;
      }
      final decoded = _readValue(bytes, offset, wire, field.type);
      offset = decoded.next;
      if (field.repeated) {
        (result[field.name] as List).add(decoded.value);
      } else {
        result[field.name] = decoded.value;
      }
    }
    return result;
  }

  void _writeField(BytesBuilder builder, ProtoField field, Object? value) {
    switch (field.type) {
      case 'int32' || 'int64' || 'uint32' || 'bool':
        _writeVarint(builder, (field.number << 3) | 0);
        _writeVarint(
            builder,
            field.type == 'bool'
                ? ((value == true) ? 1 : 0)
                : (value as num).toInt());
      case 'string' || 'bytes':
        final data = value is List<int>
            ? Uint8List.fromList(value)
            : Uint8List.fromList(utf8.encode('$value'));
        _writeVarint(builder, (field.number << 3) | 2);
        _writeVarint(builder, data.length);
        builder.add(data);
      default:
        throw FormatException('Unsupported proto field type ${field.type}');
    }
  }
}

class ProtoMessage {
  ProtoMessage(this.name, this.fields);
  final String name;
  final List<ProtoField> fields;
}

class ProtoField {
  ProtoField(
      {required this.repeated,
      required this.type,
      required this.name,
      required this.number});
  final bool repeated;
  final String type;
  final String name;
  final int number;
}

void _writeVarint(BytesBuilder builder, int value) {
  var current = value;
  while (current > 0x7f) {
    builder.addByte((current & 0x7f) | 0x80);
    current >>= 7;
  }
  builder.addByte(current & 0x7f);
}

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

({Object? value, int next}) _readValue(
    Uint8List bytes, int offset, int wire, String type) {
  if (wire == 0) {
    final varint = _readVarint(bytes, offset);
    if (type == 'bool') return (value: varint.value != 0, next: varint.next);
    return (value: varint.value, next: varint.next);
  }
  if (wire == 2) {
    final length = _readVarint(bytes, offset);
    final end = length.next + length.value;
    final slice = bytes.sublist(length.next, end);
    if (type == 'string') return (value: utf8.decode(slice), next: end);
    return (value: slice, next: end);
  }
  throw FormatException('Unsupported wire type $wire');
}

int _skip(Uint8List bytes, int offset, int wire) {
  if (wire == 0) return _readVarint(bytes, offset).next;
  if (wire == 1) return offset + 8;
  if (wire == 5) return offset + 4;
  if (wire == 2) {
    final length = _readVarint(bytes, offset);
    return length.next + length.value;
  }
  throw FormatException('Unknown wire type $wire');
}
