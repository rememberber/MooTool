import 'package:flutter_test/flutter_test.dart';
import 'package:mootool_next_flutter/core/samples/dynamic_proto.dart';
import 'package:mootool_next_flutter/core/samples/simple_pdf.dart';
import 'package:mootool_next_flutter/core/storage/document_vault.dart';
import 'package:mootool_next_flutter/core/window/session_transfer.dart';
import 'package:mootool_next_flutter/app/tool_registry.dart';

void main() {
  test('registry exposes home plus 25 tools', () {
    expect(toolRegistry, hasLength(26));
    expect(searchTools('json').single.id, 'json');
    expect(searchTools('随手记').single.id, 'quickNote');
    expect(searchTools('ホーム').single.id, 'mootool');
  });

  test('vault create rename move duplicate and delete', () {
    final vault = DocumentVault();
    final folder = vault.createFolder(toolId: 'json', name: 'drafts');
    final id = vault.createDocument(
        toolId: 'json', name: 'a.json', content: '{"a":1}', parent: folder);
    vault.rename(id, 'b.json');
    final copy = vault.duplicate(id);
    expect(vault.documents, hasLength(2));
    vault.move(copy, null);
    expect(vault.parentOf(copy), isNull);
    vault.delete(folder);
    expect(vault.documents.where((item) => item.id == id), isEmpty);
  });

  test('session transfer requires matching revision', () {
    final coordinator = SessionCoordinator();
    coordinator.claim('json');
    final begin = coordinator.beginTransfer(TransferRequest(
      sessionId: 'json',
      sourceWindowId: 'main',
      targetWindowId: 'other',
      revision: 3,
      text: '{}',
      selectionStart: 0,
      selectionEnd: 0,
      undoDepth: 1,
    ));
    expect(begin.ok, isTrue);
    expect(coordinator.ack('json', 'other', 2).ok, isFalse);
    expect(coordinator.abort('json').ok, isTrue);
    expect(coordinator.ownerOf('json'), 'main');
  });

  test('dynamic proto encodes a message that was not generated at compile time',
      () {
    const source = '''
syntax = "proto3";
message Person {
  string name = 1;
  int32 id = 2;
  repeated string tags = 3;
}
''';
    final proto = DynamicProto.parse(source);
    final bytes = proto.encodeJson('Person', {
      'name': 'Moo',
      'id': 7,
      'tags': ['a', 'b']
    });
    final decoded = proto.decode('Person', bytes);
    expect(decoded['name'], 'Moo');
    expect(decoded['id'], 7);
    expect(decoded['tags'], ['a', 'b']);
  });

  test('simple PDF split and merge keep extractable page text', () {
    final source = SimplePdf(['one', 'two', 'three', 'four', 'five']).encode();
    final first = SimplePdf.split(source, [0, 2, 4]);
    final second = SimplePdf.split(source, [1, 3]);
    final merged = SimplePdf.merge([first, second]);
    expect(
        SimplePdf.parse(merged).pages, ['one', 'three', 'five', 'two', 'four']);
    expect(() => SimplePdf.parse(source.sublist(0, 12)), throwsFormatException);
  });
}
