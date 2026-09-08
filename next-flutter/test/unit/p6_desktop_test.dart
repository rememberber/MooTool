import 'dart:io';
import 'dart:typed_data';

import 'package:flutter_test/flutter_test.dart';
import 'package:mootool_next_flutter/app/app_controller.dart';
import 'package:mootool_next_flutter/app/app_paths.dart';
import 'package:mootool_next_flutter/app/product.dart';
import 'package:mootool_next_flutter/app/settings.dart';
import 'package:mootool_next_flutter/core/desktop/desktop_host.dart';
import 'package:mootool_next_flutter/core/storage/snapshot_backup.dart';
import 'package:mootool_next_flutter/features/http/http_client.dart';
import 'package:mootool_next_flutter/features/http/http_models.dart';

final _png1x1 = Uint8List.fromList(const [
  0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0x00, 0x00, 0x00, 0x0D,
  0x49, 0x48, 0x44, 0x52, 0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x01,
  0x08, 0x06, 0x00, 0x00, 0x00, 0x1F, 0x15, 0xC4, 0x89, 0x00, 0x00, 0x00,
  0x0A, 0x49, 0x44, 0x41, 0x54, 0x78, 0x9C, 0x63, 0x00, 0x01, 0x00, 0x00,
  0x05, 0x00, 0x01, 0x0D, 0x0A, 0x2D, 0xB4, 0x00, 0x00, 0x00, 0x00, 0x49,
  0x45, 0x4E, 0x44, 0xAE, 0x42, 0x60, 0x82,
]);

void main() {
  test('settings round-trip keeps new categories and unknown enums fall back',
      () {
    final settings = AppSettings(
      language: AppLanguage.enUS,
      closeBehavior: CloseBehavior.hide,
      accentColor: 'purple',
      proxyEnabled: true,
      proxyHost: '127.0.0.1',
      proxyPort: '8888',
      httpTimeoutMs: 5000,
      javaPath: '/opt/java/bin/java',
      gitToken: 'secret-token',
    );
    final restored = AppSettings.fromJson(settings.toJson());
    expect(restored.language, AppLanguage.enUS);
    expect(restored.closeBehavior, CloseBehavior.hide);
    expect(restored.accentColor, 'purple');
    expect(resolveAccentHex('purple'), '#8a72b5');
    expect(restored.proxyEnabled, isTrue);
    expect(restored.proxyHost, '127.0.0.1');
    expect(restored.httpTimeoutMs, 5000);
    expect(restored.javaPath, '/opt/java/bin/java');
    expect(restored.gitToken, 'secret-token');
    expect(settings.toJson().containsKey('gitToken'), isTrue);
    expect(AppSettings().toJson().containsKey('gitToken'), isFalse);
    expect(AppSettings().toJson().containsKey('proxyPassword'), isFalse);
    expect(
        AppSettings.fromJson(<String, Object?>{'theme': 'neon'}).theme,
        ThemePreference.system);
    expect(resolveAccentHex('#4f83cc'), '#4f83cc');
  });

  test('snapshot backup restores workspace without touching cache', () async {
    final root = Directory.systemTemp.createTempSync('mootool-backup-');
    addTearDown(() => root.deleteSync(recursive: true));
    final paths = AppPaths(root);
    await paths.ensure();
    await File('${root.path}/settings.json')
        .writeAsString('{"productId":"${Product.id}"}');
    await File('${root.path}/workspace.json')
        .writeAsString('{"activeToolId":"json"}');
    final cache = File('${root.path}/cache/tmp.txt');
    cache.createSync(recursive: true);
    cache.writeAsStringSync('scratch');
    final snapshot = await SnapshotBackup.create(paths);
    await File('${root.path}/workspace.json')
        .writeAsString('{"activeToolId":"changed"}');
    await SnapshotBackup.restore(snapshot, paths);
    expect(File('${root.path}/workspace.json').readAsStringSync(),
        contains('"json"'));
    expect(File('${root.path}/cache/tmp.txt').readAsStringSync(), 'scratch');

    final missing = Directory('${root.path}/missing-backup');
    await expectLater(SnapshotBackup.restore(missing, paths),
        throwsA(isA<FormatException>()));

    final foreign = Directory('${root.path}/foreign-backup')..createSync();
    await File('${foreign.path}/manifest.json')
        .writeAsString('{"productId":"other-product"}');
    await expectLater(SnapshotBackup.restore(foreign, paths),
        throwsA(isA<FormatException>()));
  });

  test('message board presenting uses desktop host and does not fake sleep',
      () async {
    final root = Directory.systemTemp.createTempSync('mootool-sleep-');
    addTearDown(() => root.deleteSync(recursive: true));
    final host = MemoryDesktopHost(sleepWorks: false);
    final controller = AppController(AppPaths(root), desktopHost: host);
    addTearDown(controller.dispose);
    await controller.load();
    await controller.setMessageBoardPresenting(true);
    expect(controller.messageBoard.presenting, isTrue);
    expect(controller.messageBoard.displayAwake, isFalse);
    expect(host.sleepHeld, isFalse);

    final working = MemoryDesktopHost();
    final second = AppController(AppPaths(root), desktopHost: working);
    addTearDown(second.dispose);
    await second.setMessageBoardPresenting(true);
    expect(working.sleepHeld, isTrue);
    expect(second.messageBoard.displayAwake, isTrue);
    await second.setMessageBoardPresenting(false);
    expect(working.sleepHeld, isFalse);
  });

  test('HTTP proxy directive is DIRECT-equivalent when disabled', () {
    expect(
        httpFindProxy(
            HttpProxyConfig(enabled: false, host: '127.0.0.1', port: '9')),
        isNull);
    expect(
        httpFindProxy(
            HttpProxyConfig(enabled: true, host: '127.0.0.1', port: '8888')),
        'PROXY 127.0.0.1:8888');
    expect(
        httpFindProxy(
            HttpProxyConfig(enabled: true, host: '127.0.0.1', port: 'bad')),
        isNull);
  });

  test('HTTP send still works when proxy config is disabled', () async {
    final server = await HttpServer.bind(InternetAddress.loopbackIPv4, 0);
    addTearDown(() => server.close(force: true));
    server.listen((request) async {
      request.response.write('ok');
      await request.response.close();
    });
    final result = await HttpSender().send(
      requestId: 'proxy-off',
      request: HttpRequestDraft(url: 'http://127.0.0.1:${server.port}/'),
      timeoutMs: 3000,
      proxy: HttpProxyConfig(enabled: false, host: '127.0.0.1', port: '9'),
    );
    expect(result.ok, isTrue);
    expect(result.body, 'ok');
  });

  test('screenshot host can import captured bytes into the product library',
      () async {
    final root = Directory.systemTemp.createTempSync('mootool-shot-');
    addTearDown(() => root.deleteSync(recursive: true));
    final host = MemoryDesktopHost(screenshotBytes: _png1x1);
    final controller = AppController(AppPaths(root), desktopHost: host);
    addTearDown(controller.dispose);
    await controller.load();
    await controller.captureScreenshotToLibrary();
    expect(controller.screenshotDraft, isNotNull);
    await controller.confirmScreenshotDraft(crop: false);
    expect(controller.imageAssets, isNotEmpty);
    expect(controller.screenshotDraft, isNull);

    final colorHost = MemoryDesktopHost(pickedColor: '#FF00AA');
    final colors = AppController(AppPaths(root), desktopHost: colorHost);
    addTearDown(colors.dispose);
    await colors.pickScreenColorInto('colorBoard');
    expect(colors.localFor('colorBoard').left, '#FF00AA');
  });
}
