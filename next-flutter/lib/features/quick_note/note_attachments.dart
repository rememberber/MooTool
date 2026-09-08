import 'dart:io';

import 'package:path/path.dart' as p;

class TextSelectionRange {
  const TextSelectionRange({required this.start, required this.end});
  final int start;
  final int end;
}

class MarkdownImageInsertion {
  const MarkdownImageInsertion(
      {required this.start,
      required this.end,
      required this.text,
      required this.caret});
  final int start;
  final int end;
  final String text;
  final int caret;
}

MarkdownImageInsertion prepareMarkdownImageInsertion(
    String content, TextSelectionRange requested, String markdown) {
  final start = requested.start.clamp(0, content.length);
  final end = requested.end.clamp(start, content.length);
  final leadingBreak = start > 0 && content[start - 1] != '\n' ? '\n' : '';
  final trailingBreak = end < content.length && content[end] != '\n' ? '\n' : '';
  final text = '$leadingBreak$markdown$trailingBreak';
  return MarkdownImageInsertion(
      start: start, end: end, text: text, caret: start + text.length);
}

class NoteAttachmentStore {
  NoteAttachmentStore(this.root);
  final Directory root;

  Directory directoryFor(String noteId) =>
      Directory(p.join(root.path, 'attachments', _safeId(noteId)));

  Future<String> importFile(String noteId, File source) async {
    return importBytes(noteId,
        name: p.basename(source.path), bytes: await source.readAsBytes());
  }

  Future<String> importBytes(String noteId,
      {required String name, required List<int> bytes}) async {
    if (!_isImageName(name)) {
      throw const FormatException('Only image attachments are accepted');
    }
    final dir = directoryFor(noteId);
    await dir.create(recursive: true);
    final targetName = _uniqueName(dir, name);
    final target = File(p.join(dir.path, targetName));
    if (!_isInside(dir, target)) {
      throw const FormatException('Attachment path escaped the note directory');
    }
    await target.writeAsBytes(bytes, flush: true);
    return 'attachments/$targetName';
  }

  File? resolve(String noteId, String relative) {
    final cleaned = relative.replaceAll('\\', '/');
    if (cleaned.contains('..') || cleaned.startsWith('/')) return null;
    final file = File(p.join(directoryFor(noteId).path, p.basename(cleaned)));
    if (!file.existsSync()) return null;
    if (!_isInside(directoryFor(noteId), file)) return null;
    return file;
  }

  Future<void> deleteOrphans(String noteId, String markdown) async {
    final dir = directoryFor(noteId);
    if (!await dir.exists()) return;
    final referenced = RegExp(r'!\[[^\]]*\]\(([^)]+)\)')
        .allMatches(markdown)
        .map((match) => p.basename(match[1]!.split('?').first))
        .toSet();
    await for (final entity in dir.list()) {
      if (entity is File && !referenced.contains(p.basename(entity.path))) {
        await entity.delete();
      }
    }
  }

  bool _isImageName(String name) =>
      RegExp(r'\.(?:png|jpe?g|gif|bmp|webp)$', caseSensitive: false)
          .hasMatch(name);

  String _safeId(String id) => id.replaceAll(RegExp(r'[^A-Za-z0-9._-]'), '_');

  String _uniqueName(Directory dir, String name) {
    final base = p.basename(name);
    var candidate = base;
    var index = 2;
    while (File(p.join(dir.path, candidate)).existsSync()) {
      final stem = p.basenameWithoutExtension(base);
      final ext = p.extension(base);
      candidate = '$stem-$index$ext';
      index += 1;
    }
    return candidate;
  }

  bool _isInside(Directory root, File file) {
    final rootPath = p.normalize(root.absolute.path);
    final filePath = p.normalize(file.absolute.path);
    return filePath == rootPath || filePath.startsWith('$rootPath${p.separator}');
  }
}
