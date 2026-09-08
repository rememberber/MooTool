import 'dart:convert';
import 'dart:io';

import 'package:path/path.dart' as p;

import '../../app/app_paths.dart';
import '../../app/product.dart';

const backupNames = [
  'product.json',
  'settings.json',
  'workspace.json',
  'vaults',
  'images',
  'environment',
  'database',
  'workspace',
];

class SnapshotBackup {
  static Future<Directory> create(AppPaths paths, {Directory? into}) async {
    final root = into ?? Directory(p.join(paths.dataRoot.path, 'backups'));
    final stamp = DateTime.now().toUtc().toIso8601String().replaceAll(':', '');
    final dest = Directory(p.join(root.path, 'backup-$stamp'));
    await dest.create(recursive: true);
    for (final name in backupNames) {
      final source = File(p.join(paths.dataRoot.path, name));
      final directory = Directory(p.join(paths.dataRoot.path, name));
      if (await source.exists()) {
        await File(p.join(dest.path, name)).parent.create(recursive: true);
        await source.copy(p.join(dest.path, name));
      } else if (await directory.exists()) {
        await _copyDirectory(directory, Directory(p.join(dest.path, name)));
      }
    }
    await File(p.join(dest.path, 'manifest.json')).writeAsString(
        const JsonEncoder.withIndent('  ').convert({
      'productId': Product.id,
      'schemaVersion': Product.schemaVersion,
      'createdAt': DateTime.now().toUtc().toIso8601String(),
    }));
    return dest;
  }

  static Future<void> restore(Directory snapshot, AppPaths paths) async {
    final manifestFile = File(p.join(snapshot.path, 'manifest.json'));
    if (!await manifestFile.exists()) {
      throw const FormatException('Backup manifest is missing');
    }
    final manifest = jsonDecode(await manifestFile.readAsString());
    if (manifest is! Map || manifest['productId'] != Product.id) {
      throw const FormatException('Backup does not belong to this product');
    }
    for (final name in backupNames) {
      final sourceFile = File(p.join(snapshot.path, name));
      final sourceDir = Directory(p.join(snapshot.path, name));
      final targetFile = File(p.join(paths.dataRoot.path, name));
      final targetDir = Directory(p.join(paths.dataRoot.path, name));
      if (await sourceFile.exists()) {
        await targetFile.parent.create(recursive: true);
        await sourceFile.copy(targetFile.path);
      } else if (await sourceDir.exists()) {
        if (await targetDir.exists()) {
          await targetDir.delete(recursive: true);
        }
        await _copyDirectory(sourceDir, targetDir);
      }
    }
  }

  static Future<void> _copyDirectory(Directory from, Directory to) async {
    await to.create(recursive: true);
    await for (final entity in from.list(recursive: false, followLinks: false)) {
      final name = p.basename(entity.path);
      if (name == '.' || name == '..') continue;
      if (entity is File) {
        await entity.copy(p.join(to.path, name));
      } else if (entity is Directory) {
        await _copyDirectory(entity, Directory(p.join(to.path, name)));
      }
    }
  }
}
