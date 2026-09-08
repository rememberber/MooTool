import '../../core/editor/editor_document.dart';
import '../../core/editor/find_replace.dart';
import 'note_frontmatter.dart';

const noteSample = '''# MooTool Next Flutter

- 随手记正文不包含 frontmatter
- 支持 **Markdown** 预览
- 任务：[ ] 待办

```
code
```
''';

class QuickNoteSession {
  QuickNoteSession()
      : document = EditorDocument(id: 'quick-note-draft', text: noteSample),
        metadata = NoteMetadata(title: 'untitled', syntax: 'text/markdown');

  final EditorDocument document;
  NoteMetadata metadata;
  String viewMode = 'editor';
  bool wrap = true;
  bool treeOpen = true;
  bool quickReplaceOpen = true;
  bool findOpen = false;
  bool gitOpen = false;
  String findQuery = '';
  String replaceText = '';
  FindReplaceOptions findOptions = const FindReplaceOptions();
  String notice = '';
  double vaultWidth = 220;
  double replaceWidth = 220;
  String? documentId;
  int replacedCount = 0;

  Map<String, Object?> toJson() => {
        'text': document.text,
        'selectionStart': document.selectionStart,
        'selectionEnd': document.selectionEnd,
        'scrollX': document.scrollX,
        'scrollY': document.scrollY,
        'savedRevision': document.savedRevision,
        'metadata': metadata.toJson(),
        'viewMode': viewMode,
        'wrap': wrap,
        'treeOpen': treeOpen,
        'quickReplaceOpen': quickReplaceOpen,
        'findOpen': findOpen,
        'findQuery': findQuery,
        'replaceText': replaceText,
        'findOptions': findOptions.toJson(),
        'notice': notice,
        'vaultWidth': vaultWidth,
        'replaceWidth': replaceWidth,
        'documentId': documentId,
      };

  void restore(Map<String, Object?> json) {
    document.apply(json['text'] as String? ?? noteSample,
        selectionStart: json['selectionStart'] as int? ?? 0,
        selectionEnd: json['selectionEnd'] as int? ?? 0,
        recordUndo: false);
    document.scrollX = (json['scrollX'] as num?)?.toDouble() ?? 0;
    document.scrollY = (json['scrollY'] as num?)?.toDouble() ?? 0;
    document.savedRevision = json['savedRevision'] as int? ?? document.revision;
    metadata = NoteMetadata.fromJson(
        Map<String, Object?>.from(json['metadata'] as Map? ?? {}));
    viewMode = json['viewMode'] as String? ?? 'editor';
    wrap = json['wrap'] as bool? ?? true;
    treeOpen = json['treeOpen'] as bool? ?? true;
    quickReplaceOpen = json['quickReplaceOpen'] as bool? ?? true;
    findOpen = json['findOpen'] as bool? ?? false;
    findQuery = json['findQuery'] as String? ?? '';
    replaceText = json['replaceText'] as String? ?? '';
    findOptions = FindReplaceOptions.fromJson(
        Map<String, Object?>.from(json['findOptions'] as Map? ?? {}));
    notice = json['notice'] as String? ?? '';
    vaultWidth = (json['vaultWidth'] as num?)?.toDouble() ?? 220;
    replaceWidth = (json['replaceWidth'] as num?)?.toDouble() ?? 220;
    documentId = json['documentId'] as String?;
  }
}
