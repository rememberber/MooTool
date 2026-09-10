import 'dart:convert';
import 'dart:io';

import 'package:flutter_test/flutter_test.dart';
import 'package:mootool_next_flutter/app/app_controller.dart';
import 'package:mootool_next_flutter/app/app_paths.dart';
import 'package:mootool_next_flutter/app/settings.dart';
import 'package:mootool_next_flutter/core/desktop/desktop_host.dart';
import 'package:mootool_next_flutter/core/samples/simple_pdf.dart';
import 'package:mootool_next_flutter/core/storage/atomic_file.dart';
import 'package:mootool_next_flutter/features/regex/regex_engine.dart';

Future<AppController> fresh() async {
  final root = Directory.systemTemp.createTempSync('mootool-review-data-');
  final c = AppController(AppPaths(root), desktopHost: MemoryDesktopHost());
  await c.load();
  addTearDown(() async {
    c.dispose();
    await Future<void>.delayed(const Duration(milliseconds: 100));
    if (root.existsSync()) root.deleteSync(recursive: true);
  });
  return c;
}

void main() {
  test('R02 note auto-save must complete rather than await its own queue',
      () async {
    final c = await fresh();
    c.setNoteText('ORIGINAL');
    c.createNoteDocument(name: 'original.md');
    await c.persist();
    c.setNoteText('CHANGED BY TYPING');
    await c.persist().timeout(const Duration(seconds: 2));
  });

  test('R03 backup restore must replace in-memory state before next save',
      () async {
    final c = await fresh();
    c.settings.theme = ThemePreference.light;
    c.setJsonText('{"snapshot":true}');
    await c.persist();
    await c.createBackup();
    final snapshot = Directory(c.lastBackupPath!);
    c.settings.theme = ThemePreference.dark;
    c.setJsonText('{"snapshot":false}');
    await c.persist();
    await c.restoreBackup(snapshot);
    await c.persist();
    final disk = jsonDecode(await c.paths.settingsFile.readAsString());
    expect(disk['theme'], 'light');
  });

  test('R04 importing a note must not overwrite the selected existing note',
      () async {
    final c = await fresh();
    c.setNoteText('ORIGINAL NOTE');
    final old = c.createNoteDocument(name: 'old.md');
    await c.persist();
    c.importNoteMarkdown('IMPORTED NOTE', name: 'imported.md');
    await c.persist();
    expect(c.vault.documents.singleWhere((d) => d.id == old).content,
        'ORIGINAL NOTE');
  });

  test('R05 JSON undo after document switch must not insert another document',
      () async {
    final c = await fresh();
    final a = c.vault
        .createDocument(toolId: 'json', name: 'a.json', content: '{"a":1}');
    final b = c.vault
        .createDocument(toolId: 'json', name: 'b.json', content: '{"b":1}');
    c.openJsonDocument(a);
    c.setJsonText('{"a":2}');
    c.openJsonDocument(b);
    c.json.document.undo();
    expect(c.json.document.text, '{"b":1}');
  });

  test('R06 a transient settings write failure must not poison future saves',
      () async {
    final c = await fresh();
    final blocker = Directory(c.paths.settingsFile.path);
    blocker.createSync();
    await expectLater(c.persist(), throwsA(isA<FileSystemException>()));
    blocker.deleteSync();
    await c.persist();
    expect(c.paths.settingsFile.existsSync(), isTrue);
  });

  test('R07 credentials must not be serialized into settings JSON', () {
    final s = AppSettings(
        proxyPassword: 'review-only-dummy-password',
        gitToken: 'review-only-dummy-token');
    expect(s.toJson().containsKey('proxyPassword'), isFalse);
    expect(s.toJson().containsKey('gitToken'), isFalse);
  });

  test('R07 persist and backup omit credentials', () async {
    final c = await fresh();
    c.settings.gitToken = 'review-only-dummy-token';
    c.settings.proxyPassword = 'review-only-dummy-password';
    await c.persist();
    final settingsDisk = jsonDecode(await c.paths.settingsFile.readAsString())
        as Map<String, dynamic>;
    expect(settingsDisk.containsKey('gitToken'), isFalse);
    expect(settingsDisk.containsKey('proxyPassword'), isFalse);
    final secrets = jsonDecode(await c.paths.secretsFile.readAsString())
        as Map<String, dynamic>;
    expect(secrets['gitToken'], 'review-only-dummy-token');
    await c.createBackup();
    final snapshot = Directory(c.lastBackupPath!);
    expect(File('${snapshot.path}/secrets.json').existsSync(), isFalse);
    expect(File('${snapshot.path}/settings.json').readAsStringSync(),
        isNot(contains('review-only-dummy')));
  });

  test('R08 valid SimplePdf text containing parentheses must round-trip', () {
    final bytes = SimplePdf(['Hello (world)']).encode();
    expect(SimplePdf.parse(bytes).pages, ['Hello (world)']);
  });

  test('R09 a start anchor must not match again at every substring', () {
    final hits = RegexEngine().matchRegex(r'^a', 'aaa', const RegexOptions());
    expect(hits.map((h) => h.index).toList(), [0]);
  });

  test(
      'R10 final JSON autosave must persist latest Vault content before marking saved',
      () async {
    final c = await fresh();
    c.setJsonText('{"version":1}');
    final id = c.createJsonDocument(name: 'version.json');
    await c.persist();
    c.setJsonText('{"version":2}');
    await c.persist();
    final second = AppController(c.paths, desktopHost: MemoryDesktopHost());
    addTearDown(second.dispose);
    await second.load();
    expect(second.vault.documents.singleWhere((d) => d.id == id).content,
        '{"version":2}');
  });

  test('R14 missing primary file falls back to previous copy', () async {
    final root = Directory.systemTemp.createTempSync('mootool-atomic-');
    addTearDown(() => root.deleteSync(recursive: true));
    final file = File('${root.path}/settings.json');
    await writeAtomicJson(file, {'v': 1});
    await writeAtomicJson(file, {'v': 2});
    await file.writeAsString('{not-json', flush: true);
    expect(await readJsonObject(file), {'v': 1});
  });
}
