import 'dart:convert';
import 'dart:math' as math;
import 'dart:typed_data';

import 'package:image/image.dart' as img;

import 'image_tools.dart';

Uint8List compressImageBytes(Uint8List bytes, CompressImageOptions options) {
  final decoded = _decode(bytes);
  final size = scaledDimensions(decoded.width, decoded.height, options.scale);
  final resized = img.copyResize(decoded,
      width: size.width,
      height: size.height,
      interpolation: img.Interpolation.linear);
  return _encode(resized, options.format, bytes, options.quality);
}

Uint8List watermarkImageBytes(Uint8List bytes, WatermarkImageOptions options) {
  final text = options.text.trim();
  if (text.isEmpty) throw const FormatException('Watermark text is required');
  final decoded = _decode(bytes);
  final font = _watermarkFont(decoded.width, decoded.height, options.fontSize);
  final color = _color(options.color, options.opacity);
  final metricsWidth = text.length * (font.size * 0.6);
  final textHeight = font.size.toDouble();
  final margin = math.max(8, (math.min(decoded.width, decoded.height) * 0.02).round());
  if (options.position == 'tile') {
    final stepX = (metricsWidth + margin * 3).round();
    final stepY = (textHeight + margin * 3).round();
    for (var y = -decoded.height; y < decoded.height * 2; y += stepY) {
      for (var x = -decoded.width; x < decoded.width * 2; x += stepX) {
        _stamp(decoded, text, font, color, x.toDouble(), y.toDouble(),
            options.diagonal);
      }
    }
  } else {
    final point = watermarkAnchor(decoded.width, decoded.height, metricsWidth,
        textHeight, options.position, margin.toDouble());
    _stamp(decoded, text, font, color, point.x, point.y, options.diagonal);
  }
  final jpeg = _looksJpeg(bytes);
  return jpeg
      ? Uint8List.fromList(img.encodeJpg(decoded, quality: 92))
      : Uint8List.fromList(img.encodePng(decoded));
}

String vectorizeImage(Uint8List bytes, VectorizeOptions options) {
  if (!{'poster', 'photo', 'bw'}.contains(options.preset)) {
    throw const FormatException('Invalid image vectorization options');
  }
  if (!{'low', 'medium', 'high'}.contains(options.detail)) {
    throw const FormatException('Invalid image vectorization options');
  }
  if (options.colorCount < 2 || options.colorCount > 64) {
    throw const FormatException('Invalid color count');
  }
  if (options.filterSpeckle < 0 || options.filterSpeckle > 128) {
    throw const FormatException('Invalid speckle filter');
  }
  final decoded = _decode(bytes);
  final simplify = options.detail == 'low'
      ? 3
      : options.detail == 'high'
          ? 1
          : 2;
  final paths = <String>[];
  if (options.preset == 'bw') {
    paths.addAll(_traceColor(decoded, const _Rgb(0, 0, 0), true,
        options.filterSpeckle, simplify));
  } else {
    final palette = _quantize(decoded, options.colorCount);
    for (final color in palette) {
      paths.addAll(_traceColor(
          decoded, color, false, options.filterSpeckle, simplify));
    }
  }
  if (paths.isEmpty) throw const FormatException('Vectorizer returned invalid SVG output');
  final svg =
      '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 ${decoded.width} ${decoded.height}" width="${decoded.width}" height="${decoded.height}">\n${paths.join('\n')}\n</svg>';
  if (!svg.contains('<path') ||
      svg.contains('<image') ||
      svg.contains('data:image/')) {
    throw const FormatException('Vectorizer returned an embedded bitmap instead of vector paths');
  }
  return svg;
}

img.Image _decode(Uint8List bytes) {
  final decoded = img.decodeImage(bytes);
  if (decoded == null) throw const FormatException('Unable to load image');
  if (decoded.width * decoded.height > 4096 * 4096) {
    throw const FormatException('Image exceeds 16 megapixel limit');
  }
  return decoded;
}

Uint8List _encode(
    img.Image image, String format, Uint8List source, double quality) {
  final jpeg = format == 'jpeg' ||
      (format == 'auto' && _looksJpeg(source));
  if (jpeg) {
    return Uint8List.fromList(
        img.encodeJpg(image, quality: (quality.clamp(0.01, 1) * 100).round()));
  }
  return Uint8List.fromList(img.encodePng(image));
}

({int width, int height}) imagePixelSize(Uint8List bytes) {
  final decoded = _decode(bytes);
  return (width: decoded.width, height: decoded.height);
}

Uint8List cropImageBytes(Uint8List bytes,
    {required int left,
    required int top,
    required int width,
    required int height}) {
  final decoded = _decode(bytes);
  if (width <= 0 || height <= 0) {
    throw const FormatException('Crop region is empty');
  }
  final x = left.clamp(0, decoded.width);
  final y = top.clamp(0, decoded.height);
  final maxW = decoded.width - x;
  final maxH = decoded.height - y;
  if (maxW <= 0 || maxH <= 0) {
    throw const FormatException('Crop region is outside the image');
  }
  final w = width.clamp(1, maxW);
  final h = height.clamp(1, maxH);
  final cropped =
      img.copyCrop(decoded, x: x, y: y, width: w, height: h);
  return Uint8List.fromList(img.encodePng(cropped));
}

bool _looksJpeg(Uint8List bytes) =>
    bytes.length >= 3 && bytes[0] == 0xFF && bytes[1] == 0xD8 && bytes[2] == 0xFF;

img.BitmapFont _watermarkFont(int width, int height, String mode) {
  final base = (math.min(width, height) * 0.05).round();
  if (mode == 'small' || base <= 24) return img.arial14;
  if (mode == 'large' && base >= 32) return img.arial48;
  return img.arial24;
}

img.ColorRgba8 _color(String hex, double opacity) {
  final value = hex.replaceAll('#', '');
  if (!RegExp(r'^[0-9a-fA-F]{6}$').hasMatch(value)) {
    return img.ColorRgba8(255, 255, 255, (opacity.clamp(0.01, 1) * 255).round());
  }
  return img.ColorRgba8(
    int.parse(value.substring(0, 2), radix: 16),
    int.parse(value.substring(2, 4), radix: 16),
    int.parse(value.substring(4, 6), radix: 16),
    (opacity.clamp(0.01, 1) * 255).round(),
  );
}

void _stamp(img.Image image, String text, img.BitmapFont font, img.Color color,
    double x, double y, bool diagonal) {
  if (!diagonal) {
    img.drawString(image, text,
        font: font, x: x.round(), y: (y - font.size).round(), color: color);
    return;
  }
  final stamp = img.Image(width: font.size * (text.length + 2), height: font.size * 3);
  img.drawString(stamp, text, font: font, x: font.size, y: font.size, color: color);
  final rotated = img.copyRotate(stamp, angle: -45);
  img.compositeImage(image, rotated, dstX: x.round(), dstY: (y - font.size).round());
}

class _Rgb {
  const _Rgb(this.r, this.g, this.b);
  final int r, g, b;
  String get hex =>
      '#${r.toRadixString(16).padLeft(2, '0')}${g.toRadixString(16).padLeft(2, '0')}${b.toRadixString(16).padLeft(2, '0')}';
  @override
  bool operator ==(Object other) =>
      other is _Rgb && other.r == r && other.g == g && other.b == b;
  @override
  int get hashCode => Object.hash(r, g, b);
}

List<_Rgb> _quantize(img.Image image, int count) {
  final buckets = <int, _Rgb>{};
  final tallies = <int, int>{};
  final shift = count <= 8 ? 5 : count <= 16 ? 4 : 3;
  for (final pixel in image) {
    final key = ((pixel.r.toInt() >> shift) << 10) |
        ((pixel.g.toInt() >> shift) << 5) |
        (pixel.b.toInt() >> shift);
    buckets.putIfAbsent(
        key,
        () => _Rgb(pixel.r.toInt(), pixel.g.toInt(), pixel.b.toInt()));
    tallies[key] = (tallies[key] ?? 0) + 1;
  }
  final ranked = buckets.keys.toList()
    ..sort((a, b) => (tallies[b] ?? 0).compareTo(tallies[a] ?? 0));
  return [for (final key in ranked.take(count)) buckets[key]!];
}

List<String> _traceColor(img.Image image, _Rgb color, bool bw, int speckle,
    int simplify) {
  final width = image.width;
  final height = image.height;
  final mask = List<bool>.filled(width * height, false);
  for (var y = 0; y < height; y++) {
    for (var x = 0; x < width; x++) {
      final pixel = image.getPixel(x, y);
      final match = bw
          ? ((pixel.r.toInt() + pixel.g.toInt() + pixel.b.toInt()) / 3) < 128
          : (pixel.r.toInt() - color.r).abs() < 24 &&
              (pixel.g.toInt() - color.g).abs() < 24 &&
              (pixel.b.toInt() - color.b).abs() < 24;
      mask[y * width + x] = match;
    }
  }
  final visited = List<bool>.filled(mask.length, false);
  final paths = <String>[];
  for (var y = 0; y < height; y++) {
    for (var x = 0; x < width; x++) {
      final index = y * width + x;
      if (!mask[index] || visited[index]) continue;
      final blob = <int>[];
      final stack = [index];
      visited[index] = true;
      while (stack.isNotEmpty) {
        final current = stack.removeLast();
        blob.add(current);
        final cx = current % width;
        final cy = current ~/ width;
        for (final dy in [-1, 0, 1]) {
          for (final dx in [-1, 0, 1]) {
            if (dx == 0 && dy == 0) continue;
            final nx = cx + dx;
            final ny = cy + dy;
            if (nx < 0 || ny < 0 || nx >= width || ny >= height) continue;
            final next = ny * width + nx;
            if (!mask[next] || visited[next]) continue;
            visited[next] = true;
            stack.add(next);
          }
        }
      }
      if (blob.length <= speckle) continue;
      final start = blob.reduce((a, b) {
        final ay = a ~/ width;
        final by = b ~/ width;
        if (ay != by) return ay < by ? a : b;
        return (a % width) <= (b % width) ? a : b;
      });
      final contour = _moore(mask, width, height, start % width, start ~/ width);
      if (contour.length < 3) continue;
      final simplified = [
        for (var i = 0; i < contour.length; i += simplify) contour[i]
      ];
      if (simplified.last != contour.last) simplified.add(contour.last);
      final d = [
        'M ${simplified.first.$1} ${simplified.first.$2}',
        for (final point in simplified.skip(1)) 'L ${point.$1} ${point.$2}',
        'Z'
      ].join(' ');
      paths.add(
          '<path fill="${bw ? '#000000' : color.hex}" fill-rule="evenodd" d="$d"/>');
    }
  }
  return paths;
}

List<(int, int)> _moore(List<bool> mask, int width, int height, int startX, int startY) {
  const dirs = [
    (-1, 0),
    (-1, -1),
    (0, -1),
    (1, -1),
    (1, 0),
    (1, 1),
    (0, 1),
    (-1, 1),
  ];
  bool at(int x, int y) =>
      x >= 0 && y >= 0 && x < width && y < height && mask[y * width + x];
  final contour = <(int, int)>[(startX, startY)];
  var x = startX;
  var y = startY;
  var dir = 0;
  for (var step = 0; step < width * height; step++) {
    var found = false;
    for (var offset = 0; offset < 8; offset++) {
      final index = (dir + offset) % 8;
      final nx = x + dirs[index].$1;
      final ny = y + dirs[index].$2;
      if (!at(nx, ny)) continue;
      x = nx;
      y = ny;
      dir = (index + 6) % 8;
      found = true;
      break;
    }
    if (!found) break;
    contour.add((x, y));
    if (x == startX && y == startY && contour.length > 2) break;
  }
  return contour;
}

String imageToDataUrl(Uint8List bytes, {String mime = 'image/png'}) =>
    'data:$mime;base64,${base64Encode(bytes)}';
