import 'dart:convert';
import 'dart:io';

Future<void> writeAtomicFile(File file, String contents,
    {int permission = 384}) async {
  await file.parent.create(recursive: true);
  final temp = File('${file.path}.tmp');
  await temp.writeAsString(contents, flush: true);
  if (!Platform.isWindows) {
    await Process.run('chmod', [permission.toRadixString(8), temp.path]);
  }
  if (await file.exists()) {
    await file.copy('${file.path}.prev');
    await file.delete();
  }
  await temp.rename(file.path);
}

Future<void> writeAtomicJson(File file, Object value) async {
  await writeAtomicFile(
      file, const JsonEncoder.withIndent('  ').convert(value));
}

class CorruptStoreException implements Exception {
  CorruptStoreException(this.path, this.cause);
  final String path;
  final Object cause;
  @override
  String toString() => 'Failed to read $path: $cause';
}

Future<Map<String, Object?>?> readJsonObject(File file) async {
  if (!await file.exists()) return null;
  try {
    final decoded = jsonDecode(await file.readAsString());
    if (decoded is Map<String, Object?>) return decoded;
    if (decoded is Map) return Map<String, Object?>.from(decoded);
    throw const FormatException('JSON root is not an object');
  } catch (error) {
    if (error is CorruptStoreException) rethrow;
    throw CorruptStoreException(file.path, error);
  }
}
