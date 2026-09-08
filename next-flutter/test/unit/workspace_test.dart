import 'dart:io';

import 'package:flutter_test/flutter_test.dart';
import 'package:mootool_next_flutter/app/app_controller.dart';
import 'package:mootool_next_flutter/app/app_paths.dart';
import 'package:mootool_next_flutter/app/product.dart';
import 'package:mootool_next_flutter/features/encode/encode_engine.dart';

void main() {
  test('JSON format save reopen restores text', () async {
    final root = Directory.systemTemp.createTempSync('mootool-flutter-ws-');
    addTearDown(() => root.deleteSync(recursive: true));
    final first = AppController(AppPaths(root));
    addTearDown(first.dispose);
    await first.load();
    first.setJsonText('{"z":1,"a":2}');
    first.formatJson(advanced: true);
    first.createJsonDocument(name: 'sample.json');
    await first.persist();

    final second = AppController(AppPaths(root));
    addTearDown(second.dispose);
    await second.load();
    expect(second.json.document.text.contains('"a": 2'), isTrue);
    expect(second.vault.documents, isNotEmpty);
    expect(File('${root.path}/product.json').readAsStringSync(),
        contains(Product.id));
  });

  test('corrupt settings does not overwrite with empty defaults', () async {
    final root = Directory.systemTemp.createTempSync('mootool-flutter-bad-');
    addTearDown(() => root.deleteSync(recursive: true));
    File('${root.path}/settings.json')
      ..createSync(recursive: true)
      ..writeAsStringSync('{not-json');
    final controller = AppController(AppPaths(root));
    addTearDown(controller.dispose);
    await controller.load();
    expect(controller.storeError, isNotNull);
    await File('${root.path}/settings.json').readAsString();
    expect(File('${root.path}/settings.json').readAsStringSync(), '{not-json');
  });

  test('local tool draft survives persist and reload', () async {
    final root = Directory.systemTemp.createTempSync('mootool-flutter-local-');
    addTearDown(() => root.deleteSync(recursive: true));
    final first = AppController(AppPaths(root));
    addTearDown(first.dispose);
    await first.load();
    first.runLocal('encode', (session) {
      session.tab = 'unicode';
      session.left = 'Moo 编码';
      session.right = EncodeEngine().toUnicode('Moo 编码');
    });
    await first.persist();

    final second = AppController(AppPaths(root));
    addTearDown(second.dispose);
    await second.load();
    expect(second.localFor('encode').left, 'Moo 编码');
    expect(second.localFor('encode').right, isNotEmpty);
  });
}
