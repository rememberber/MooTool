import 'dart:convert';
import 'dart:typed_data';

import 'package:gbk_codec/gbk_codec.dart';

enum UrlCharset { utf8, gb2312 }

enum AsciiFormat { decimal, hex }

class EncodeEngine {
  String toUnicode(String value) {
    final buffer = StringBuffer();
    for (final codePoint in value.runes) {
      if (codePoint <= 0x7f) {
        buffer.writeCharCode(codePoint);
      } else if (codePoint <= 0xffff) {
        buffer.write('\\u${codePoint.toRadixString(16).padLeft(4, '0')}');
      } else {
        final offset = codePoint - 0x10000;
        final high = 0xd800 + (offset >> 10);
        final low = 0xdc00 + (offset & 0x3ff);
        buffer.write('\\u${high.toRadixString(16)}\\u${low.toRadixString(16)}');
      }
    }
    return buffer.toString();
  }

  String fromUnicode(String value) {
    return value.replaceAllMapped(RegExp(r'\\u([\da-fA-F]{4})'), (match) {
      return String.fromCharCode(int.parse(match.group(1)!, radix: 16));
    });
  }

  String urlEncode(String value, UrlCharset charset) {
    final bytes = _encodeCharset(value, charset);
    final buffer = StringBuffer();
    for (final byte in bytes) {
      if (_isUnreserved(byte)) {
        buffer.writeCharCode(byte);
      } else {
        buffer
            .write('%${byte.toRadixString(16).toUpperCase().padLeft(2, '0')}');
      }
    }
    return buffer.toString();
  }

  String urlDecode(String value, UrlCharset charset) {
    final bytes = <int>[];
    final source = value.replaceAll('+', ' ');
    var index = 0;
    while (index < source.length) {
      if (source[index] == '%' &&
          index + 2 < source.length &&
          RegExp(r'^[\da-fA-F]{2}$')
              .hasMatch(source.substring(index + 1, index + 3))) {
        bytes.add(int.parse(source.substring(index + 1, index + 3), radix: 16));
        index += 3;
      } else {
        bytes.addAll(_encodeCharset(source[index], charset));
        index += 1;
      }
    }
    return _decodeCharset(Uint8List.fromList(bytes), charset);
  }

  String textToHex(String value) {
    return utf8
        .encode(value)
        .map((byte) => byte.toRadixString(16).padLeft(2, '0'))
        .join();
  }

  String hexToText(String value) {
    final normalized = value.replaceAll(RegExp(r'[\s:_-]+'), '');
    if (normalized.isEmpty ||
        normalized.length.isOdd ||
        !RegExp(r'^[\da-fA-F]+$').hasMatch(normalized)) {
      throw const FormatException('Invalid hexadecimal input');
    }
    final bytes = Uint8List(normalized.length ~/ 2);
    for (var i = 0; i < bytes.length; i++) {
      bytes[i] = int.parse(normalized.substring(i * 2, i * 2 + 2), radix: 16);
    }
    return utf8.decode(bytes);
  }

  String textToAscii(String value, AsciiFormat format) {
    return [
      for (final codePoint in value.runes)
        format == AsciiFormat.hex
            ? codePoint.toRadixString(16).toUpperCase()
            : '$codePoint'
    ].join(' ');
  }

  String asciiToText(String value) {
    if (value.trim().isEmpty) return '';
    return value.trim().split(RegExp(r'[\s,;]+')).map((part) {
      final radix = RegExp(r'^0x', caseSensitive: false).hasMatch(part) ||
              RegExp(r'[a-f]', caseSensitive: false).hasMatch(part)
          ? 16
          : 10;
      final normalized =
          part.replaceFirst(RegExp(r'^0x', caseSensitive: false), '');
      final codePoint = int.parse(normalized, radix: radix);
      if (codePoint < 0 || codePoint > 0x10ffff) {
        throw FormatException('Invalid code point: $part');
      }
      return String.fromCharCodes([codePoint]);
    }).join();
  }

  List<int> _encodeCharset(String value, UrlCharset charset) {
    if (charset == UrlCharset.utf8) return utf8.encode(value);
    final bytes = gbk_bytes.encode(value);
    if (gbk_bytes.decode(bytes) != value) {
      throw const FormatException('Character cannot be represented in GB2312');
    }
    return bytes;
  }

  String _decodeCharset(List<int> bytes, UrlCharset charset) {
    if (charset == UrlCharset.utf8) return utf8.decode(bytes);
    return gbk_bytes.decode(bytes);
  }

  bool _isUnreserved(int byte) {
    return (byte >= 0x41 && byte <= 0x5a) ||
        (byte >= 0x61 && byte <= 0x7a) ||
        (byte >= 0x30 && byte <= 0x39) ||
        byte == 0x2d ||
        byte == 0x2e ||
        byte == 0x5f ||
        byte == 0x7e;
  }
}
