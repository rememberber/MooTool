import 'package:flutter_test/flutter_test.dart';
import 'package:mootool_next_flutter/core/editor/editor_document.dart';
import 'package:mootool_next_flutter/core/editor/find_replace.dart';

void main() {
  test('column insert updates three lines and undoes once', () {
    final document = EditorDocument(text: 'one\ntwo\nthree');
    document.column = const ColumnSelection(
        startLine: 0, startColumn: 0, endLine: 2, endColumn: 0);
    document.insertInColumn('>>');
    expect(document.text, '>>one\n>>two\n>>three');
    expect(document.undo(), isTrue);
    expect(document.text, 'one\ntwo\nthree');
  });

  test('column delete is one undo and keeps a zero-width column', () {
    final document = EditorDocument(text: 'abcd\nefgh');
    document.column = const ColumnSelection(
        startLine: 0, startColumn: 1, endLine: 1, endColumn: 3);
    document.deleteInColumn();
    expect(document.text, 'ad\neh');
    expect(document.undo(), isTrue);
    expect(document.text, 'abcd\nefgh');
  });

  test('switching documents does not share undo', () {
    final first = EditorDocument(id: 'a', text: 'alpha');
    final second = EditorDocument(id: 'b', text: 'beta');
    first.replaceSelection('A');
    first.isolateFrom(second);
    expect(second.canUndo, isFalse);
    expect(first.canUndo, isTrue);
  });

  test('find replace supports case, word and regex', () {
    const content = 'Foo foo food';
    expect(
        findAllMatches(content, 'foo', const FindReplaceOptions()).length, 3);
    expect(
        findAllMatches(
                content, 'foo', const FindReplaceOptions(matchCase: true))
            .length,
        2);
    expect(
        findAllMatches(
                content, 'foo', const FindReplaceOptions(wholeWord: true))
            .map((match) => match.start)
            .toList(),
        [0, 4]);
    final replaced = replaceAllMatches(
        content, r'f\w+', 'x', const FindReplaceOptions(regex: true));
    expect(replaced.count, 3);
  });
}
