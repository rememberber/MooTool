import 'dart:io';

import 'package:flutter_test/flutter_test.dart';
import 'package:mootool_next_flutter/app/app_controller.dart';
import 'package:mootool_next_flutter/app/app_paths.dart';
import 'package:mootool_next_flutter/core/editor/editor_document.dart';
import 'package:mootool_next_flutter/features/quick_note/markdown_preview.dart';
import 'package:mootool_next_flutter/features/quick_note/note_attachments.dart';
import 'package:mootool_next_flutter/features/quick_note/note_frontmatter.dart';
import 'package:mootool_next_flutter/features/quick_note/quick_replace.dart';

void main() {
  test('quick replace matches Electron fixtures', () {
    expect(runQuickReplace(' a \n\n b ', 'removeBlankLines'), ' a \n b ');
    expect(runQuickReplace('a\na\nb', 'deduplicateWithCount'), 'a\t2\nb\t1');
    expect(runQuickReplace('b\na', 'sortAscending'), 'a\nb');
    expect(runQuickReplace('hello_world', 'underscoreToCamel'), 'helloWorld');
    expect(runQuickReplace('helloWorld', 'camelToUnderscore'), 'hello_world');
    expect(runQuickReplace('1.25e3', 'scientificToNormal'), '1250');
    expect(runQuickReplace('1234567.5', 'normalToThousands'), '1,234,567.5');
    expect(runQuickReplace('a\nb', 'linesToSingleQuoted'), "'a','b'");
    expect(runQuickReplace('"a", b', 'commaToLines'), 'a\nb');
    final escaped = runQuickReplace('a\nb', 'escape');
    expect(runQuickReplace(escaped, 'unescape'), 'a\nb');
    expect(quickReplaceActionIds, hasLength(24));
  });

  test('frontmatter round-trips Java-compatible keys and hides them from body',
      () {
    const source = '''---
title: daily
syntax: text/markdown
font_name: PingFang SC
font_size: "15"
line_spacing: "1.0"
line_wrap: "0"
---
# body
second
''';
    final parsed = parseNoteDocument(source);
    expect(parsed.body, '# body\nsecond\n');
    expect(parsed.metadata.title, 'daily');
    expect(parsed.metadata.fontName, 'PingFang SC');
    expect(parsed.metadata.fontSize, 15);
    expect(parsed.metadata.lineSpacing, 1.0);
    expect(parsed.metadata.lineWrap, isFalse);
    final packed = serializeNoteDocument(parsed);
    final again = parseNoteDocument(packed);
    expect(again.body, parsed.body);
    expect(again.metadata.title, 'daily');
    expect(again.metadata.lineWrap, isFalse);
    expect(packed.contains('# body'), isTrue);
    expect(packed.startsWith('---\n'), isTrue);
  });

  test('selection quick replace is a single undo', () {
    final document = EditorDocument(text: 'aaaBBB');
    document.restoreView(start: 3, end: 6);
    document.transformSelectionOrAll(
        (value) => runQuickReplace(value, 'lowercase'));
    expect(document.text, 'aaabbb');
    expect(document.undo(), isTrue);
    expect(document.text, 'aaaBBB');
    expect(document.canUndo, isFalse);
  });

  test('column insert/delete handles Chinese and tab as UTF-16 columns', () {
    final document = EditorDocument(text: '中文\nA\tB');
    document.column = const ColumnSelection(
        startLine: 0, startColumn: 1, endLine: 1, endColumn: 1);
    document.insertInColumn('>>');
    expect(document.text, '中>>文\nA>>\tB');
    expect(document.undo(), isTrue);
    expect(document.text, '中文\nA\tB');
    document.column = const ColumnSelection(
        startLine: 0, startColumn: 1, endLine: 1, endColumn: 2);
    document.deleteInColumn();
    expect(document.text, '中\nAB');
  });

  test('prepareMarkdownImageInsertion keeps surrounding newlines', () {
    const content = 'before\nafter';
    final insertion = prepareMarkdownImageInsertion(
        content,
        const TextSelectionRange(start: 7, end: 7),
        '![shot.png](attachments/shot.png)');
    expect(insertion.text, '![shot.png](attachments/shot.png)\n');
    final next =
        '${content.substring(0, insertion.start)}${insertion.text}${content.substring(insertion.end)}';
    expect(next, 'before\n![shot.png](attachments/shot.png)\nafter');
  });

  test('attachment store rejects path escape and keeps referenced files',
      () async {
    final root = Directory.systemTemp.createTempSync('mootool-note-att-');
    addTearDown(() => root.deleteSync(recursive: true));
    final store = NoteAttachmentStore(root);
    expect(store.resolve('note-1', '../secret.png'), isNull);
    expect(store.resolve('note-1', '/tmp/x.png'), isNull);
    final source = File('${root.path}/shot.png')..writeAsBytesSync([1, 2, 3]);
    final relative = await store.importFile('note-1', source);
    expect(relative.endsWith('shot.png'), isTrue);
    final markdown = '![shot]($relative)';
    await store.deleteOrphans('note-1', markdown);
    expect(store.resolve('note-1', relative), isNotNull);
    await store.deleteOrphans('note-1', 'no image');
    expect(store.resolve('note-1', relative), isNull);
  });

  test('preview sanitizer strips script and iframe', () {
    final cleaned = sanitizeMarkdownSource(
        '# hi<script>alert(1)</script>\n<iframe src="x"></iframe>ok');
    expect(cleaned.contains('script'), isFalse);
    expect(cleaned.contains('iframe'), isFalse);
    expect(cleaned.contains('ok'), isTrue);
  });

  test('note vault save reopen restores body not frontmatter', () async {
    final root = Directory.systemTemp.createTempSync('mootool-flutter-note-');
    addTearDown(() => root.deleteSync(recursive: true));
    final first = AppController(AppPaths(root));
    addTearDown(first.dispose);
    await first.load();
    first.setNoteText('# hello\nworld');
    first.note.document.restoreView(start: 2, end: 7);
    first.note.viewMode = 'split';
    first.createNoteDocument(name: 'daily.md');
    await first.persist();

    final packed =
        File('${root.path}/vaults/quick-note/${first.note.documentId}.md')
            .readAsStringSync();
    expect(packed.startsWith('---\n'), isTrue);
    expect(packed.contains('title: daily.md'), isTrue);
    expect(packed.contains('# hello'), isTrue);
    expect(first.note.document.text.startsWith('---\n'), isFalse);

    final second = AppController(AppPaths(root));
    addTearDown(second.dispose);
    await second.load();
    expect(second.note.document.text, '# hello\nworld');
    expect(second.note.viewMode, 'split');
    expect(second.note.document.selectionStart, 2);
    expect(second.note.document.selectionEnd, 7);
    expect(second.vault.documents.single.title, 'daily.md');
  });

  test('switching notes saves first and isolates undo', () async {
    final root = Directory.systemTemp.createTempSync('mootool-flutter-switch-');
    addTearDown(() => root.deleteSync(recursive: true));
    final controller = AppController(AppPaths(root));
    addTearDown(controller.dispose);
    await controller.load();
    controller.setNoteText('first-body');
    final firstId = controller.createNoteDocument(name: 'one.md');
    controller.setNoteText('first-edited');
    controller.saveNoteDocument();

    controller.note.documentId = null;
    controller.setNoteText('second-body');
    final secondId = controller.createNoteDocument(name: 'two.md');
    expect(secondId, isNot(firstId));

    controller.openNoteDocument(firstId);
    expect(controller.note.document.text, 'first-edited');
    expect(controller.note.document.canUndo, isFalse);
    controller.setNoteText('keep-me');
    controller.openNoteDocument(secondId);
    expect(controller.note.document.text, 'second-body');
    expect(controller.note.document.canUndo, isFalse);
    controller.note.document.apply('mutated');
    expect(controller.note.document.undo(), isTrue);
    expect(controller.note.document.text, 'second-body');

    controller.openNoteDocument(firstId);
    expect(controller.note.document.text, 'keep-me');
  });
}
