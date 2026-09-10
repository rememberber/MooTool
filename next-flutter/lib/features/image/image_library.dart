import 'dart:convert';
import 'dart:io';
import 'dart:typed_data';

import 'package:image/image.dart' as img;
import 'package:path/path.dart' as p;

class ImageAssetSummary {
  ImageAssetSummary(
      {required this.name,
      required this.width,
      required this.height,
      required this.size});
  final String name;
  final int width;
  final int height;
  final int size;
}

class ImageAsset {
  ImageAsset(
      {required this.name,
      required this.width,
      required this.height,
      required this.bytes});
  final String name;
  final int width;
  final int height;
  final Uint8List bytes;
  int get size => bytes.length;
}

class ImageLibrary {
  ImageLibrary(this.directory);
  final Directory directory;

  static const maxBytes = 20 * 1024 * 1024;
  static const maxPixels = 4096 * 4096;

  Future<List<ImageAssetSummary>> list() async {
    await directory.create(recursive: true);
    final items = <ImageAssetSummary>[];
    await for (final entity in directory.list()) {
      if (entity is! File) continue;
      final name = p.basename(entity.path);
      if (name.startsWith('.') || name == 'library.json') continue;
      if (!_allowedName(name)) continue;
      if (name.toLowerCase().endsWith('.svg')) {
        items.add(ImageAssetSummary(
            name: name, width: 0, height: 0, size: await entity.length()));
        continue;
      }
      try {
        final bytes = await entity.readAsBytes();
        final decoded = img.decodeImage(bytes);
        if (decoded == null) continue;
        items.add(ImageAssetSummary(
            name: name,
            width: decoded.width,
            height: decoded.height,
            size: bytes.length));
      } catch (_) {}
    }
    items.sort((a, b) => a.name.compareTo(b.name));
    return items;
  }

  Future<ImageAsset> read(String name) async {
    final file = _file(_safeName(name));
    if (!await file.exists()) throw FormatException('Image not found: $name');
    final bytes = await file.readAsBytes();
    if (name.toLowerCase().endsWith('.svg')) {
      return ImageAsset(
          name: _safeName(name),
          width: 0,
          height: 0,
          bytes: Uint8List.fromList(bytes));
    }
    final decoded = img.decodeImage(bytes);
    if (decoded == null) throw const FormatException('Unable to load image');
    return ImageAsset(
        name: _safeName(name),
        width: decoded.width,
        height: decoded.height,
        bytes: Uint8List.fromList(bytes));
  }

  Future<ImageAsset> save(
      {required String name, required Uint8List bytes}) async {
    _validateBytes(bytes);
    final safe = _safeName(name);
    await directory.create(recursive: true);
    if (safe.toLowerCase().endsWith('.svg')) {
      await _file(safe).writeAsBytes(bytes, flush: true);
      return ImageAsset(name: safe, width: 0, height: 0, bytes: bytes);
    }
    final decoded = img.decodeImage(bytes);
    if (decoded == null) throw const FormatException('Unable to load image');
    if (decoded.width * decoded.height > maxPixels) {
      throw const FormatException('Image exceeds 16 megapixel limit');
    }
    await _file(safe).writeAsBytes(bytes, flush: true);
    return ImageAsset(
        name: safe, width: decoded.width, height: decoded.height, bytes: bytes);
  }

  Future<ImageAsset> saveDataUrl(
      {required String name, required String dataUrl}) async {
    final payload = dataUrl.contains(',') ? dataUrl.split(',').last : dataUrl;
    return save(name: name, bytes: Uint8List.fromList(base64Decode(payload)));
  }

  Future<ImageAsset> rename(String name, String nextName) async {
    final from = _file(_safeName(name));
    final toName = _safeName(nextName);
    final to = _file(toName);
    if (!await from.exists()) throw FormatException('Image not found: $name');
    if (await to.exists() && to.path != from.path) {
      throw FormatException('Image already exists: $toName');
    }
    await from.rename(to.path);
    return read(toName);
  }

  Future<void> delete(List<String> names) async {
    for (final name in names) {
      final file = _file(_safeName(name));
      if (await file.exists()) await file.delete();
    }
  }

  Future<String> exportTo(List<String> names, Directory destination) async {
    await destination.create(recursive: true);
    for (final name in names) {
      final asset = await read(name);
      await File(p.join(destination.path, asset.name))
          .writeAsBytes(asset.bytes, flush: true);
    }
    return destination.path;
  }

  File _file(String name) => File(p.join(directory.path, name));

  String _safeName(String name) {
    final trimmed = name.trim();
    if (trimmed.contains('..') ||
        trimmed.contains('/') ||
        trimmed.contains('\\')) {
      throw const FormatException('Invalid image name');
    }
    final base = p.basename(trimmed);
    if (!_allowedName(base)) {
      throw const FormatException('Invalid image name');
    }
    return base;
  }

  bool _allowedName(String name) {
    if (name.isEmpty ||
        name.contains('..') ||
        name.contains('/') ||
        name.contains('\\')) {
      return false;
    }
    return RegExp(r'^[\w .+\-()\[\]]{1,180}\.(png|jpe?g|gif|webp|svg)$',
            caseSensitive: false)
        .hasMatch(name);
  }

  void _validateBytes(Uint8List bytes) {
    if (bytes.isEmpty) throw const FormatException('Image is empty');
    if (bytes.length > maxBytes) {
      throw const FormatException('Image exceeds 20 MB limit');
    }
  }
}
