import 'dart:convert';
import 'dart:io';
import 'dart:typed_data';

import 'package:crypto/crypto.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mootool_next_flutter/app/app_controller.dart';
import 'package:mootool_next_flutter/app/app_paths.dart';
import 'package:mootool_next_flutter/app/product.dart';
import 'package:mootool_next_flutter/app/tool_registry.dart';
import 'package:mootool_next_flutter/core/update/package_names.dart';
import 'package:mootool_next_flutter/core/update/semver.dart';
import 'package:mootool_next_flutter/core/update/update_downloader.dart';
import 'package:mootool_next_flutter/core/update/update_models.dart';
import 'package:mootool_next_flutter/core/update/update_service.dart';
import 'package:mootool_next_flutter/features/json/json_engine.dart';

void main() {
  test('package names match the product contract', () {
    expect(
        PackageNames.artifact(
            version: '0.1.0',
            platform: 'darwin',
            architecture: 'arm64',
            packageType: 'dmg'),
        'MooTool-Next-Flutter-0.1.0-mac-arm64.dmg');
    expect(
        PackageNames.artifact(
            version: '0.1.0',
            platform: 'win32',
            architecture: 'amd64',
            packageType: 'nsis'),
        'MooTool-Next-Flutter-0.1.0-win-x64-setup.exe');
    expect(
        PackageNames.artifact(
            version: '0.1.0',
            platform: 'windows',
            architecture: 'x64',
            packageType: 'portable'),
        'MooTool-Next-Flutter-0.1.0-win-x64-portable.zip');
    expect(
        PackageNames.artifact(
            version: '0.1.0',
            platform: 'linux',
            architecture: 'x64',
            packageType: 'appimage'),
        'MooTool-Next-Flutter-0.1.0-linux-x64.AppImage');
    expect(
        PackageNames.artifact(
            version: '0.1.0',
            platform: 'linux',
            architecture: 'x64',
            packageType: 'deb'),
        'MooTool-Next-Flutter-0.1.0-linux-x64.deb');
    expect(
        () => PackageNames.artifact(
            version: '0.1.0',
            platform: 'darwin',
            architecture: 'arm64',
            packageType: 'zip'),
        throwsA(isA<FormatException>()));
  });

  test('compareVersions matches Electron fixtures', () {
    expect(compareVersions('1.7.9', '1.7.8'), 1);
    expect(compareVersions('2.0.0-beta.2', '2.0.0-beta.1'), 1);
    expect(compareVersions('2.0.0-beta.10', '2.0.0-beta.2'), 1);
    expect(compareVersions('2.0.0', '2.0.0-beta.2'), 1);
    expect(compareVersions('2.0.0+build.2', '2.0.0+build.1'), 0);
    expect(compareVersions('v1.7', '1.7.0'), 0);
    expect(() => compareVersions('2.0.0-beta.01', '2.0.0-beta.1'),
        throwsA(isA<FormatException>()));
  });

  test('missing next-flutter node is unpublished, not another product',
      () async {
    final service = UpdateService(
      feedUrl: 'https://feed.test/manifest.json',
      identity:
          const UpdateClientIdentity(platform: 'darwin', architecture: 'arm64'),
      fetcher: (_) async => UpdateTextResponse(
        statusCode: 200,
        body: jsonEncode({
          'schemaVersion': 1,
          'products': {
            'next-electron': {
              'displayName': 'MooTool Next Electron',
              'status': 'active',
              'releases': [
                release('99.0.0', 'Electron only', '', [
                  asset('darwin', 'arm64', 'dmg',
                      'MooTool-Next-Electron-99.0.0-mac-arm64.dmg', 10)
                ])
              ]
            }
          }
        }),
      ),
    );
    final result = await service.check('0.1.0');
    expect(result.status, UpdateCheckStatus.unpublished);
    expect(result.productId, Product.id);
    expect(result.download, isNull);
    expect(result.latestVersion, '0.1.0');
  });

  test('selects next-flutter macOS dmg and ignores other products', () async {
    final service = UpdateService(
      feedUrl: 'https://feed.test/manifest.json',
      identity:
          const UpdateClientIdentity(platform: 'darwin', architecture: 'arm64'),
      fetcher: (_) async => UpdateTextResponse(
        statusCode: 200,
        body: jsonEncode({
          'schemaVersion': 1,
          'products': {
            'next-electron': {
              'displayName': 'MooTool Next Electron',
              'status': 'active',
              'releases': [release('9.9.9', 'Other', '', [])]
            },
            'next-flutter': {
              'displayName': 'MooTool Next Flutter',
              'status': 'active',
              'releases': [
                release('0.1.0', 'Current', ''),
                release('0.2.0', 'Next', 'rewritten', [
                  asset('darwin', 'x64', 'dmg',
                      'MooTool-Next-Flutter-0.2.0-mac-x64.dmg', 1),
                  asset('darwin', 'arm64', 'zip',
                      'MooTool-Next-Flutter-0.2.0-mac-arm64.zip', 20),
                  asset('darwin', 'arm64', 'dmg',
                      'MooTool-Next-Flutter-0.2.0-mac-arm64.dmg', 10),
                ]),
              ]
            }
          }
        }),
      ),
    );
    final result = await service.check('0.1.0');
    expect(result.status, UpdateCheckStatus.available);
    expect(result.productId, 'next-flutter');
    expect(result.latestVersion, '0.2.0');
    expect(
        result.download?.fileName, 'MooTool-Next-Flutter-0.2.0-mac-arm64.dmg');
    expect(result.releaseNotes, contains('rewritten'));
  });

  test('stable clients skip prerelease; prerelease can move to stable',
      () async {
    Future<UpdateCheckResult> check(String current) {
      return UpdateService(
        feedUrl: 'https://feed.test/manifest.json',
        identity: const UpdateClientIdentity(
            platform: 'darwin', architecture: 'arm64'),
        fetcher: (_) async => UpdateTextResponse(
          statusCode: 200,
          body: jsonEncode({
            'schemaVersion': 1,
            'products': {
              'next-flutter': {
                'displayName': 'MooTool Next Flutter',
                'status': 'active',
                'releases': [
                  release('1.0.0', 'Stable', 'stable changes'),
                  release('1.1.0-beta.1', 'Beta', 'beta changes'),
                  release('1.1.0-beta.2', 'Beta 2', 'beta fixes'),
                  release('1.1.0', 'Next', 'next stable'),
                ]
              }
            }
          }),
        ),
      ).check(current);
    }

    final stable = await check('1.0.0');
    expect(stable.latestVersion, '1.1.0');
    expect(stable.releaseNotes, isNot(contains('beta changes')));
    final beta = await check('1.1.0-beta.1');
    expect(beta.latestVersion, '1.1.0');
    expect(beta.releaseNotes, contains('beta fixes'));
    expect(beta.releaseNotes, contains('next stable'));
  });

  test('no matching arch opens product release instead of other packages',
      () async {
    final result = await UpdateService(
      feedUrl: 'https://feed.test/manifest.json',
      identity:
          const UpdateClientIdentity(platform: 'linux', architecture: 'arm64'),
      fetcher: (_) async => UpdateTextResponse(
        statusCode: 200,
        body: jsonEncode({
          'schemaVersion': 1,
          'products': {
            'next-flutter': {
              'displayName': 'MooTool Next Flutter',
              'status': 'active',
              'releases': [
                release('0.2.0', 'Linux', '', [
                  asset('linux', 'x64', 'appimage',
                      'MooTool-Next-Flutter-0.2.0-linux-x64.AppImage', 10)
                ])
              ]
            }
          }
        }),
      ),
    ).check('0.1.0');
    expect(result.status, UpdateCheckStatus.available);
    expect(result.download, isNull);
    expect(result.releaseUrl,
        'https://github.com/rememberber/MooTool/releases/tag/next-flutter-v0.2.0');
  });

  test('rejects insecure, invalid, and oversized feeds', () async {
    await expectLater(
        UpdateService(
          feedUrl: 'https://feed.test/manifest.json',
          identity: const UpdateClientIdentity(
              platform: 'darwin', architecture: 'arm64'),
          fetcher: (_) async =>
              const UpdateTextResponse(statusCode: 503, body: ''),
        ).check('0.1.0'),
        throwsA(isA<FormatException>()));
    await expectLater(
        UpdateService(
          feedUrl: 'https://feed.test/manifest.json',
          identity: const UpdateClientIdentity(
              platform: 'darwin', architecture: 'arm64'),
          fetcher: (_) async =>
              const UpdateTextResponse(statusCode: 200, body: '{'),
        ).check('0.1.0'),
        throwsA(isA<FormatException>()));
    await expectLater(
        UpdateService(
          feedUrl: 'http://feed.test/manifest.json',
          identity: const UpdateClientIdentity(
              platform: 'darwin', architecture: 'arm64'),
          fetcher: (_) async =>
              const UpdateTextResponse(statusCode: 200, body: '{}'),
        ).check('0.1.0'),
        throwsA(isA<FormatException>()));
    await expectLater(
        UpdateService(
          feedUrl: 'https://feed.test/manifest.json',
          identity: const UpdateClientIdentity(
              platform: 'darwin', architecture: 'arm64'),
          fetcher: (_) async => UpdateTextResponse(
            statusCode: 200,
            body: jsonEncode({
              'schemaVersion': 1,
              'products': {
                'next-flutter': {
                  'displayName': 'MooTool Next Flutter',
                  'status': 'active',
                  'releases': [
                    release('0.2.0', 'Unsafe', '', [
                      {
                        ...asset('darwin', 'arm64', 'dmg', 'Unsafe.dmg', 1),
                        'url': 'http://example.test/Unsafe.dmg',
                      }
                    ])
                  ]
                }
              }
            }),
          ),
        ).check('0.1.0'),
        throwsA(isA<FormatException>()));
  });

  test('download verifies size and sha512 before ready', () async {
    final bytes = Uint8List.fromList('mootool-next-flutter'.codeUnits);
    final digest = base64Encode(sha512.convert(bytes).bytes);
    final root = Directory.systemTemp.createTempSync('mootool-update-');
    addTearDown(() => root.deleteSync(recursive: true));
    final downloader = UpdateDownloader(
      directory: root,
      fetcher: (_) async => UpdateBytesResponse(statusCode: 200, body: bytes),
      opener: (path) async => path.endsWith('.dmg'),
    );
    final file = await downloader.download(UpdateDownload(
      fileName: 'MooTool-Next-Flutter-0.2.0-mac-x64.dmg',
      packageType: 'dmg',
      url: 'https://example.test/MooTool-Next-Flutter-0.2.0-mac-x64.dmg',
      sha512: digest,
      size: bytes.length,
    ));
    expect(file.existsSync(), isTrue);
    expect(file.readAsBytesSync(), bytes);

    await expectLater(
        downloader.download(UpdateDownload(
          fileName: 'bad.dmg',
          packageType: 'dmg',
          url: 'https://example.test/bad.dmg',
          sha512: digest,
          size: bytes.length + 1,
        )),
        throwsA(isA<FormatException>()));
    expect(File('${root.path}/bad.dmg').existsSync(), isFalse);
  });

  test('controller check and download stay on next-flutter and stay unsigned',
      () async {
    final root = Directory.systemTemp.createTempSync('mootool-p7-');
    addTearDown(() => root.deleteSync(recursive: true));
    final bytes = Uint8List.fromList('package'.codeUnits);
    final digest = base64Encode(sha512.convert(bytes).bytes);
    final controller = AppController(
      AppPaths(root),
      updateService: UpdateService(
        feedUrl: 'https://feed.test/manifest.json',
        identity:
            const UpdateClientIdentity(platform: 'darwin', architecture: 'x64'),
        fetcher: (_) async => UpdateTextResponse(
          statusCode: 200,
          body: jsonEncode({
            'schemaVersion': 1,
            'products': {
              'next-flutter': {
                'displayName': 'MooTool Next Flutter',
                'status': 'active',
                'releases': [
                  release('0.2.0', 'Next', 'notes', [
                    asset('darwin', 'x64', 'dmg',
                        'MooTool-Next-Flutter-0.2.0-mac-x64.dmg', bytes.length,
                        sha512: digest)
                  ])
                ]
              }
            }
          }),
        ),
      ),
    );
    controller.updateBytesFetcher =
        (_) async => UpdateBytesResponse(statusCode: 200, body: bytes);
    var opened = '';
    controller.updateOpener = (path) async {
      opened = path;
      return true;
    };
    await controller.load();
    await controller.checkForUpdates();
    expect(controller.updateResult?.status, UpdateCheckStatus.available);
    expect(controller.updateResult?.productId, Product.id);
    expect(controller.updateNotice, contains('0.2.0'));
    await controller.downloadUpdate();
    expect(controller.updateDownloadStatus, UpdateDownloadStatus.ready);
    expect(controller.updateNotice, contains('不签名'));
    await controller.openDownloadedUpdate();
    expect(opened, endsWith('MooTool-Next-Flutter-0.2.0-mac-x64.dmg'));
  });

  test('searching 26 tools stays within the local budget', () {
    final watch = Stopwatch()..start();
    var hits = 0;
    for (var i = 0; i < 200; i++) {
      hits += searchTools('json').length;
    }
    watch.stop();
    expect(hits, greaterThan(0));
    expect(watch.elapsedMilliseconds, lessThan(1000));
  });

  test('json format of 100 KiB stays interactive on this host', () {
    final payload = '{"k":"${'x' * 100000}"}';
    final watch = Stopwatch()..start();
    final out = JsonEngine((key, [params]) => key).format(payload);
    watch.stop();
    expect(out.contains('"k"'), isTrue);
    expect(watch.elapsedMilliseconds, lessThan(3000));
  });
}

Map<String, Object?> release(String version, String title, String notes,
    [List<Map<String, Object?>> assets = const []]) {
  return {
    'version': version,
    'title': title,
    'notes': notes,
    'prerelease': version.contains('-'),
    'releaseUrl':
        'https://github.com/rememberber/MooTool/releases/tag/next-flutter-v$version',
    'assets': assets,
  };
}

Map<String, Object?> asset(String platform, String architecture,
    String packageType, String fileName, int size,
    {String sha512 =
        'AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=='}) {
  return {
    'platform': platform,
    'architecture': architecture,
    'packageType': packageType,
    'priority': 10,
    'fileName': fileName,
    'url':
        'https://github.com/rememberber/MooTool/releases/download/next-flutter-v0.2.0/$fileName',
    'sha512': sha512,
    'size': size,
  };
}
