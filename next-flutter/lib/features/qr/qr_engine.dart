import 'dart:convert';
import 'dart:io';
import 'dart:typed_data';

import 'package:qr/qr.dart';

class QrEngine {
  Uint8List generatePng(String content,
      {int size = 300, String errorCorrectionLevel = 'M'}) {
    if (content.trim().isEmpty) {
      throw const FormatException('QR content is required');
    }
    final width = normalizeQrSize(size);
    final level = switch (errorCorrectionLevel) {
      'L' => QrErrorCorrectLevel.L,
      'Q' => QrErrorCorrectLevel.Q,
      'H' => QrErrorCorrectLevel.H,
      _ => QrErrorCorrectLevel.M,
    };
    final qr =
        QrImage(QrCode.fromData(data: content, errorCorrectLevel: level));
    const margin = 4;
    final modules = qr.moduleCount + margin * 2;
    final scale = width ~/ modules;
    final pixel = scale < 1 ? 1 : scale;
    final canvas = modules * pixel;
    final pixels = Uint8List(canvas * canvas);
    for (var y = 0; y < canvas; y++) {
      for (var x = 0; x < canvas; x++) {
        final mx = x ~/ pixel - margin;
        final my = y ~/ pixel - margin;
        final dark = mx >= 0 &&
            my >= 0 &&
            mx < qr.moduleCount &&
            my < qr.moduleCount &&
            qr.isDark(my, mx);
        pixels[y * canvas + x] = dark ? 0x11 : 0xff;
      }
    }
    return _encodeGrayPng(pixels, canvas, canvas);
  }

  int normalizeQrSize(num size) {
    if (size.isNaN || size.isInfinite) return 300;
    return size.round().clamp(120, 2000);
  }

  Uint8List _encodeGrayPng(Uint8List pixels, int width, int height) {
    final raw = BytesBuilder();
    for (var y = 0; y < height; y++) {
      raw.addByte(0);
      raw.add(pixels.sublist(y * width, (y + 1) * width));
    }
    final compressed = ZLibEncoder().convert(raw.toBytes());
    final buffer = BytesBuilder();
    buffer.add([137, 80, 78, 71, 13, 10, 26, 10]);
    void chunk(String type, List<int> data) {
      final typeBytes = ascii.encode(type);
      final payload = Uint8List(data.length + 4)..setAll(4, data);
      payload.setAll(0, typeBytes);
      final length = ByteData(4)..setUint32(0, data.length);
      buffer.add(length.buffer.asUint8List());
      buffer.add(payload);
      final crc = ByteData(4)..setUint32(0, _crc32(payload));
      buffer.add(crc.buffer.asUint8List());
    }

    final ihdr = ByteData(13)
      ..setUint32(0, width)
      ..setUint32(4, height)
      ..setUint8(8, 8)
      ..setUint8(9, 0)
      ..setUint8(10, 0)
      ..setUint8(11, 0)
      ..setUint8(12, 0);
    chunk('IHDR', ihdr.buffer.asUint8List());
    chunk('IDAT', compressed);
    chunk('IEND', const []);
    return Uint8List.fromList(buffer.toBytes());
  }
}

// PNG CRC helper kept next to encoder.

int _crc32(List<int> data) {
  var crc = 0xffffffff;
  for (final byte in data) {
    crc ^= byte;
    for (var i = 0; i < 8; i++) {
      crc = (crc & 1) == 1 ? (crc >> 1) ^ 0xedb88320 : crc >> 1;
    }
  }
  return crc ^ 0xffffffff;
}
