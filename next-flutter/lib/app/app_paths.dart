import 'dart:io';

import 'package:path/path.dart' as p;

import 'product.dart';
import '../core/storage/atomic_file.dart';

class AppPaths {
  AppPaths(this.dataRoot);

  final Directory dataRoot;

  Directory get cacheRoot => Directory(p.join(dataRoot.path, 'cache'));
  Directory get updatesDir => Directory(p.join(cacheRoot.path, 'updates'));
  Directory get databaseDir => Directory(p.join(dataRoot.path, 'database'));
  Directory get workspaceDir => Directory(p.join(dataRoot.path, 'workspace'));
  Directory get jsonVaultDir =>
      Directory(p.join(dataRoot.path, 'vaults', 'json'));
  Directory get noteVaultDir =>
      Directory(p.join(dataRoot.path, 'vaults', 'quick-note'));
  Directory get imagesDir => Directory(p.join(dataRoot.path, 'images'));
  Directory get importsDir => Directory(p.join(dataRoot.path, 'imports'));
  Directory get migrationsDir => Directory(p.join(dataRoot.path, 'migrations'));
  File get productFile => File(p.join(dataRoot.path, 'product.json'));
  File get settingsFile => File(p.join(dataRoot.path, 'settings.json'));
  File get workspaceFile => File(p.join(dataRoot.path, 'workspace.json'));
  File get secretsFile => File(p.join(dataRoot.path, 'secrets.json'));
  File get previousSettingsFile =>
      File(p.join(dataRoot.path, 'settings.json.prev'));

  static AppPaths resolve({String? override}) {
    if (override != null && override.trim().isNotEmpty) {
      return AppPaths(Directory(p.normalize(override)).absolute);
    }
    return AppPaths(defaultDataRoot());
  }

  static Directory defaultDataRoot() {
    if (Platform.isMacOS) {
      final home = Platform.environment['HOME'] ?? Directory.current.path;
      return Directory(p.join(
          home, 'Library', 'Application Support', Product.applicationId));
    }
    if (Platform.isWindows) {
      final appData = Platform.environment['APPDATA'] ?? Directory.current.path;
      return Directory(p.join(appData, 'MooToolNextFlutter'));
    }
    final xdg = Platform.environment['XDG_DATA_HOME'];
    if (xdg != null && xdg.isNotEmpty) {
      return Directory(p.join(xdg, 'mootool-next-flutter'));
    }
    final home = Platform.environment['HOME'] ?? Directory.current.path;
    return Directory(p.join(home, '.local', 'share', 'mootool-next-flutter'));
  }

  Future<void> ensure() async {
    for (final directory in [
      dataRoot,
      cacheRoot,
      updatesDir,
      databaseDir,
      workspaceDir,
      jsonVaultDir,
      noteVaultDir,
      imagesDir,
      importsDir,
      migrationsDir,
    ]) {
      await directory.create(recursive: true);
      if (!Platform.isWindows) {
        await Process.run('chmod', ['700', directory.path]);
      }
    }
  }

  Future<void> verifyIsolation() async {
    final path = await _canonicalPath(dataRoot);
    const foreign = [
      'com.rememberber.mootool.next.macos-native',
      'MooToolNextElectron',
      'mootool-next-electron',
      'MooToolNextTauri',
      '.MooTool',
      'MooTool',
    ];
    final base = p.basename(path);
    for (final marker in foreign) {
      if (base == marker ||
          path.endsWith('${Platform.pathSeparator}$marker') ||
          path.contains(
              '${Platform.pathSeparator}$marker${Platform.pathSeparator}')) {
        throw StateError(
            'Flutter data path must not reuse another product directory: $path');
      }
    }
    final marker = File(p.join(path, 'product.json'));
    if (await marker.exists()) {
      final json = await readJsonObject(marker);
      final productId = json?['productId'] as String?;
      if (productId != null && productId != Product.id) {
        throw StateError(
            'Data directory belongs to $productId, not ${Product.id}: $path');
      }
    }
  }

  static Future<String> _canonicalPath(Directory directory) async {
    final absolute = p.normalize(directory.absolute.path);
    if (await directory.exists()) {
      return p.normalize(await directory.resolveSymbolicLinks());
    }
    return absolute;
  }
}

class LaunchArgs {
  LaunchArgs({this.dataDir});

  final String? dataDir;

  factory LaunchArgs.parse(List<String> args) {
    String? dataDir;
    for (var i = 0; i < args.length; i++) {
      final arg = args[i];
      if (arg.startsWith('--data-dir=')) {
        dataDir = arg.substring('--data-dir='.length);
      } else if (arg == '--data-dir' && i + 1 < args.length) {
        dataDir = args[++i];
      }
    }
    return LaunchArgs(dataDir: dataDir);
  }
}
