import 'dart:async';
import 'dart:io';

import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';

import 'app_paths.dart';
import 'local_session.dart';
import 'product.dart';
import 'settings.dart';
import 'tool_registry.dart';
import '../core/editor/editor_document.dart';
import '../core/editor/find_replace.dart';
import '../core/git/git_service.dart';
import '../core/storage/atomic_file.dart';
import '../core/storage/document_vault.dart';
import '../core/window/session_transfer.dart';
import '../features/json/json_engine.dart';
import '../features/json/json_path.dart';
import '../features/quick_note/note_attachments.dart';
import '../features/quick_note/note_frontmatter.dart';
import '../features/quick_note/quick_note_session.dart';
import '../features/quick_note/quick_replace.dart';
import '../l10n/strings.dart';

const jsonSample = '''
{
  "name": "MooTool Next Flutter",
  "stack": ["Flutter", "Dart"],
  "desktop": {
    "style": "modern desktop workspace",
    "theme": "light"
  }
}
''';

class HistoryRecord {
  HistoryRecord(
      {required this.id,
      required this.toolId,
      required this.title,
      required this.input,
      required this.output,
      DateTime? at})
      : at = at ?? DateTime.now();

  final String id;
  final String toolId;
  final String title;
  final String input;
  final String output;
  final DateTime at;

  Map<String, Object?> toJson() => {
        'id': id,
        'toolId': toolId,
        'title': title,
        'input': input,
        'output': output,
        'at': at.toIso8601String(),
      };

  factory HistoryRecord.fromJson(Map<String, Object?> json) => HistoryRecord(
        id: json['id'] as String,
        toolId: json['toolId'] as String,
        title: json['title'] as String? ?? '',
        input: json['input'] as String? ?? '',
        output: json['output'] as String? ?? '',
        at: DateTime.tryParse(json['at'] as String? ?? '') ?? DateTime.now(),
      );
}

class JsonSession {
  JsonSession()
      : document = EditorDocument(id: 'json-draft', text: jsonSample),
        formatOptions = const JsonFormatOptions();

  final EditorDocument document;
  JsonFormatOptions formatOptions;
  bool wrap = true;
  bool inspectorOpen = true;
  bool findOpen = false;
  bool historyOpen = false;
  bool pathPickerOpen = false;
  bool gitOpen = false;
  String findQuery = '';
  String replaceText = '';
  FindReplaceOptions findOptions = const FindReplaceOptions();
  String jsonPath = r'$';
  String className = 'Root';
  String notice = '';
  String conversionInput = '';
  String outputTitle = '';
  String outputBody = '';
  double vaultWidth = 220;
  double inspectorWidth = 280;
  String? documentId;

  Map<String, Object?> toJson() => {
        'text': document.text,
        'selectionStart': document.selectionStart,
        'selectionEnd': document.selectionEnd,
        'scrollX': document.scrollX,
        'scrollY': document.scrollY,
        'revision': document.revision,
        'savedRevision': document.savedRevision,
        'formatOptions': formatOptions.toJson(),
        'wrap': wrap,
        'inspectorOpen': inspectorOpen,
        'findOpen': findOpen,
        'jsonPath': jsonPath,
        'className': className,
        'notice': notice,
        'vaultWidth': vaultWidth,
        'inspectorWidth': inspectorWidth,
        'documentId': documentId,
        'findQuery': findQuery,
        'replaceText': replaceText,
        'findOptions': findOptions.toJson(),
      };

  void restore(Map<String, Object?> json) {
    document.apply(json['text'] as String? ?? jsonSample,
        selectionStart: json['selectionStart'] as int? ?? 0,
        selectionEnd: json['selectionEnd'] as int? ?? 0,
        recordUndo: false);
    document.scrollX = (json['scrollX'] as num?)?.toDouble() ?? 0;
    document.scrollY = (json['scrollY'] as num?)?.toDouble() ?? 0;
    document.savedRevision = json['savedRevision'] as int? ?? document.revision;
    formatOptions = JsonFormatOptions.fromJson(
        Map<String, Object?>.from(json['formatOptions'] as Map? ?? {}));
    wrap = json['wrap'] as bool? ?? true;
    inspectorOpen = json['inspectorOpen'] as bool? ?? true;
    findOpen = json['findOpen'] as bool? ?? false;
    jsonPath = json['jsonPath'] as String? ?? r'$';
    className = json['className'] as String? ?? 'Root';
    notice = json['notice'] as String? ?? '';
    vaultWidth = (json['vaultWidth'] as num?)?.toDouble() ?? 220;
    inspectorWidth = (json['inspectorWidth'] as num?)?.toDouble() ?? 280;
    documentId = json['documentId'] as String?;
    findQuery = json['findQuery'] as String? ?? '';
    replaceText = json['replaceText'] as String? ?? '';
    findOptions = FindReplaceOptions.fromJson(
        Map<String, Object?>.from(json['findOptions'] as Map? ?? {}));
  }
}

class AppController extends ChangeNotifier {
  AppController(this.paths);

  final AppPaths paths;
  final GitService git = GitService();
  final SessionCoordinator coordinator = SessionCoordinator();
  AppSettings settings = AppSettings();
  String activeToolId = 'mootool';
  List<String> recentToolIds = [];
  bool sidebarCollapsed = false;
  bool searchOpen = false;
  String? settingsCategory;
  bool groupManagerOpen = false;
  String searchQuery = '';
  String toast = '';
  String? storeError;
  final Set<String> detachedToolIds = {};
  final Map<String, EditorDocument> drafts = {};
  final Map<String, LocalSession> locals = {};
  final List<FavoriteRecord> favorites = [];
  final JsonSession json = JsonSession();
  final QuickNoteSession note = QuickNoteSession();
  late final NoteAttachmentStore attachments = NoteAttachmentStore(paths.noteVaultDir);
  DocumentVault vault = DocumentVault();
  VaultPreferences jsonVaultPrefs = VaultPreferences();
  VaultPreferences noteVaultPrefs = VaultPreferences();
  final List<HistoryRecord> histories = [];
  Timer? _saveTimer;
  bool _pauseAutosave = false;
  Future<void> _writeQueue = Future.value();

  L10n get l10n => L10n(settings.language);
  String t(String key, [Map<String, String>? params]) => l10n.t(key, params);
  JsonEngine get jsonEngine => JsonEngine(t);
  JsonPathQuery get jsonPath => JsonPathQuery(t);
  bool get settingsOpen => settingsCategory != null;
  bool get overlayOpen => searchOpen || settingsOpen || groupManagerOpen;

  void refresh() => notifyListeners();

  Future<void> load() async {
    await paths.ensure();
    paths.verifyIsolation();
    if (!await paths.productFile.exists()) {
      await writeAtomicJson(paths.productFile, {
        'productId': Product.id,
        'schemaVersion': Product.schemaVersion,
        'createdAt': DateTime.now().toUtc().toIso8601String(),
      });
    }
    try {
      final settingsJson = await readJsonObject(paths.settingsFile);
      if (settingsJson != null) settings = AppSettings.fromJson(settingsJson);
    } on CorruptStoreException catch (error) {
      storeError = error.toString();
      _pauseAutosave = true;
    }
    try {
      final workspace = await readJsonObject(paths.workspaceFile);
      if (workspace != null) _restoreWorkspace(workspace);
    } on CorruptStoreException catch (error) {
      storeError = error.toString();
      _pauseAutosave = true;
    }
    coordinator.claim('json');
    coordinator.claim('quickNote');
    notifyListeners();
  }

  void _restoreWorkspace(Map<String, Object?> workspace) {
    activeToolId = workspace['activeToolId'] as String? ?? 'mootool';
    recentToolIds = [...?workspace['recentToolIds'] as List?];
    sidebarCollapsed = workspace['sidebarCollapsed'] as bool? ?? false;
    if (workspace['json'] is Map)
      json.restore(Map<String, Object?>.from(workspace['json'] as Map));
    if (workspace['note'] is Map)
      note.restore(Map<String, Object?>.from(workspace['note'] as Map));
    if (workspace['vault'] is Map)
      vault = DocumentVault.fromJson(
          Map<String, Object?>.from(workspace['vault'] as Map));
    if (workspace['jsonVaultPrefs'] is Map)
      jsonVaultPrefs = VaultPreferences.fromJson(
          Map<String, Object?>.from(workspace['jsonVaultPrefs'] as Map));
    if (workspace['noteVaultPrefs'] is Map)
      noteVaultPrefs = VaultPreferences.fromJson(
          Map<String, Object?>.from(workspace['noteVaultPrefs'] as Map));
    histories
      ..clear()
      ..addAll([
        for (final item in workspace['histories'] as List? ?? const [])
          if (item is Map)
            HistoryRecord.fromJson(Map<String, Object?>.from(item)),
      ]);
    if (workspace['drafts'] is Map) {
      final raw = Map<String, Object?>.from(workspace['drafts'] as Map);
      raw.forEach((id, value) {
        if (value is Map) {
          drafts[id] =
              EditorDocument(id: id, text: value['text'] as String? ?? '');
        }
      });
    }
    if (workspace['locals'] is Map) {
      final raw = Map<String, Object?>.from(workspace['locals'] as Map);
      raw.forEach((id, value) {
        if (value is Map) {
          locals[id] = LocalSession.fromJson(Map<String, Object?>.from(value));
        }
      });
    }
    if (workspace['favorites'] is List) {
      favorites
        ..clear()
        ..addAll([
          for (final item in workspace['favorites'] as List)
            if (item is Map)
              FavoriteRecord.fromJson(Map<String, Object?>.from(item)),
        ]);
    }
  }

  Map<String, Object?> _workspaceJson() => {
        'schemaVersion': Product.schemaVersion,
        'productId': Product.id,
        'activeToolId': activeToolId,
        'recentToolIds': recentToolIds,
        'sidebarCollapsed': sidebarCollapsed,
        'json': json.toJson(),
        'note': note.toJson(),
        'vault': vault.toJson(),
        'jsonVaultPrefs': jsonVaultPrefs.toJson(),
        'noteVaultPrefs': noteVaultPrefs.toJson(),
        'histories': [for (final item in histories.take(100)) item.toJson()],
        'drafts': {
          for (final entry in drafts.entries)
            entry.key: {'text': entry.value.text},
        },
        'locals': {
          for (final entry in locals.entries) entry.key: entry.value.toJson(),
        },
        'favorites': [for (final item in favorites) item.toJson()],
      };

  void scheduleSave() {
    if (_pauseAutosave) return;
    _saveTimer?.cancel();
    _saveTimer = Timer(const Duration(milliseconds: 400), () {
      unawaited(persist());
    });
  }

  Future<void> persist() {
    _saveTimer?.cancel();
    _saveTimer = null;
    if (_pauseAutosave) return Future.value();
    _writeQueue = _writeQueue.then((_) => _persistNow());
    return _writeQueue;
  }

  Future<void> _persistNow() async {
    if (_pauseAutosave) return;
    try {
      await writeAtomicJson(paths.settingsFile, settings.toJson());
      await writeAtomicJson(paths.workspaceFile, _workspaceJson());
      if (json.documentId != null) {
        final file = _document(json.documentId);
        if (file != null && file.content != json.document.text) {
          file.content = json.document.text;
          file.query = json.jsonPath;
          file.modified = DateTime.now();
          json.document.markSaved();
        }
      }
      if (note.documentId != null) {
        final file = _document(note.documentId);
        if (file != null && file.content != note.document.text) {
          file.content = note.document.text;
          file.metadata.addAll(note.metadata.toJson());
          file.modified = DateTime.now();
          await _exportNoteFile(file);
          note.document.markSaved();
        }
      }
    } catch (error) {
      storeError = error.toString();
      notifyListeners();
      rethrow;
    }
  }

  VaultDocument? _document(String? id) {
    if (id == null) return null;
    for (final item in vault.documents) {
      if (item.id == id) return item;
    }
    return null;
  }

  void openTool(String id) {
    if (!isToolId(id)) return;
    settingsCategory = null;
    searchOpen = false;
    groupManagerOpen = false;
    activeToolId = id;
    if (id != 'mootool') {
      recentToolIds =
          [id, ...recentToolIds.where((item) => item != id)].take(5).toList();
    }
    scheduleSave();
    notifyListeners();
  }

  void setLanguage(AppLanguage language) {
    settings.language = language;
    scheduleSave();
    notifyListeners();
  }

  void setTheme(ThemePreference theme) {
    settings.theme = theme;
    scheduleSave();
    notifyListeners();
  }

  void setNavigationStyle(NavigationStyle style) {
    settings.navigationStyle = style;
    scheduleSave();
    notifyListeners();
  }

  void toggleSidebar() {
    sidebarCollapsed = !sidebarCollapsed;
    scheduleSave();
    notifyListeners();
  }

  void openSearch() {
    searchOpen = true;
    notifyListeners();
  }

  void closeOverlays() {
    searchOpen = false;
    settingsCategory = null;
    groupManagerOpen = false;
    notifyListeners();
  }

  void openSettings([String category = 'general']) {
    settingsCategory = category;
    searchOpen = false;
    notifyListeners();
  }

  EditorDocument draftFor(String toolId) => drafts.putIfAbsent(
      toolId, () => EditorDocument(id: toolId, text: localFor(toolId).left));

  LocalSession localFor(String toolId) =>
      locals.putIfAbsent(toolId, () => LocalSession(tab: _defaultTab(toolId)));

  String _defaultTab(String toolId) => switch (toolId) {
        'encode' => 'unicode',
        'crypto' => 'symmetric',
        'calculator' => 'expr',
        'reformat' => 'nginx',
        'qrCode' => 'generate',
        'protobuf' => 'json',
        'ymlProperties' => 'properties',
        'colorBoard' => 'hex',
        _ => '',
      };

  void setLocalTab(String toolId, String tab) {
    final session = localFor(toolId);
    if (toolId == 'encode' && session.tab.isNotEmpty) {
      session.pairs[session.tab] = {
        'left': session.left,
        'right': session.right
      };
    }
    session.tab = tab;
    if (toolId == 'encode') {
      final pair = session.pairs[tab];
      session.left = pair?['left'] ?? session.left;
      session.right = pair?['right'] ?? '';
    }
    scheduleSave();
    notifyListeners();
  }

  void runLocal(String toolId, void Function(LocalSession session) action) {
    final session = localFor(toolId);
    try {
      action(session);
      session.notice = '';
    } catch (error) {
      session.notice = error.toString().replaceFirst('FormatException: ', '');
    }
    draftFor(toolId).apply(session.left, recordUndo: false);
    scheduleSave();
    notifyListeners();
  }

  void recordToolHistory(
      {required String toolId,
      required String title,
      required String input,
      required String output}) {
    histories.insert(
        0,
        HistoryRecord(
            id: 'h-${DateTime.now().microsecondsSinceEpoch}',
            toolId: toolId,
            title: title,
            input: input,
            output: output));
    if (histories.length > 200) histories.removeRange(200, histories.length);
  }

  Future<void> copyText(String text) async {
    await Clipboard.setData(ClipboardData(text: text));
    toast = t('json.action.copied');
    notifyListeners();
  }

  void addFavorite(String toolId, String name, String value) {
    favorites.insert(
        0,
        FavoriteRecord(
            id: 'f-${DateTime.now().microsecondsSinceEpoch}',
            toolId: toolId,
            name: name,
            value: value));
    scheduleSave();
    notifyListeners();
  }

  void removeFavorite(String id) {
    favorites.removeWhere((item) => item.id == id);
    scheduleSave();
    notifyListeners();
  }

  void setJsonText(String value,
      {int? selectionStart, int? selectionEnd, bool recordUndo = true}) {
    json.document.apply(value,
        selectionStart: selectionStart ?? json.document.selectionStart,
        selectionEnd: selectionEnd ?? json.document.selectionEnd,
        recordUndo: recordUndo);
    json.notice = jsonEngine.validate(value).message;
    scheduleSave();
    notifyListeners();
  }

  void runJson(void Function() action) {
    try {
      action();
    } catch (error) {
      json.notice = error.toString();
    }
    scheduleSave();
    notifyListeners();
  }

  void formatJson({bool advanced = false}) => runJson(() {
        final next = advanced
            ? jsonEngine.formatAdvanced(json.document.text, json.formatOptions)
            : jsonEngine.format(json.document.text,
                spaces: json.formatOptions.spaces);
        json.document.apply(next, selectionStart: 0, selectionEnd: 0);
        json.notice = jsonEngine.validate(next).message;
        _recordHistory('format', next);
      });

  void compressJson() => runJson(() {
        final next = jsonEngine.compress(json.document.text);
        json.document.apply(next, selectionStart: 0, selectionEnd: 0);
        json.notice = jsonEngine.validate(next).message;
        _recordHistory('compress', next);
      });

  void convertJson(String title, String Function(String input) convert,
          {String? source}) =>
      runJson(() {
        json.outputTitle = title;
        json.outputBody = convert(source ?? json.document.text);
      });

  void queryJsonPath() => convertJson(t('json.panel.jsonPath'),
      (input) => jsonPath.query(input, json.jsonPath));

  void applyJsonPath(String path) {
    json.jsonPath = path;
    json.pathPickerOpen = false;
    notifyListeners();
  }

  void _recordHistory(String title, String output) {
    recordToolHistory(
        toolId: 'json',
        title: title,
        input: json.document.text,
        output: output);
  }

  void restoreHistory(HistoryRecord record) {
    if (record.toolId == 'json') {
      json.document.apply(record.input);
      json.outputBody = record.output;
      json.historyOpen = false;
    } else {
      final session = localFor(record.toolId);
      session.left = record.input;
      session.right = record.output;
      session.historyOpen = false;
      activeToolId = record.toolId;
    }
    scheduleSave();
    notifyListeners();
  }

  Future<void> copyJson() async {
    await Clipboard.setData(ClipboardData(text: json.document.text));
    toast = t('json.action.copied');
    notifyListeners();
  }

  void clearJson() => runJson(() {
        json.document.apply('');
        json.notice = jsonEngine.validate('').message;
      });

  String createJsonDocument({String name = 'untitled.json', String? parent}) {
    final id = vault.createDocument(
        toolId: 'json',
        name: name,
        content: json.document.text,
        parent: parent);
    json.documentId = id;
    jsonVaultPrefs.selectedEntryId = id;
    json.document.markSaved();
    scheduleSave();
    notifyListeners();
    return id;
  }

  String createJsonFolder({String name = 'folder', String? parent}) {
    final id = vault.createFolder(toolId: 'json', name: name, parent: parent);
    jsonVaultPrefs.expanded.add(id);
    scheduleSave();
    notifyListeners();
    return id;
  }

  void openJsonDocument(String id) {
    final file = _document(id);
    if (file == null) return;
    if (json.documentId != id &&
        json.document.dirty &&
        json.documentId != null) {
      final current = _document(json.documentId);
      current?.content = json.document.text;
    }
    json.documentId = id;
    json.document.apply(file.content, recordUndo: false);
    json.document.markSaved();
    json.jsonPath = file.query.isEmpty ? json.jsonPath : file.query;
    jsonVaultPrefs.selectedEntryId = id;
    jsonVaultPrefs.expanded.addAll(vault.ancestorsOf(id));
    scheduleSave();
    notifyListeners();
  }

  void saveJsonDocument() {
    if (json.documentId == null) {
      createJsonDocument();
      return;
    }
    final file = _document(json.documentId);
    if (file == null) return;
    file.content = json.document.text;
    file.query = json.jsonPath;
    file.modified = DateTime.now();
    json.document.markSaved();
    scheduleSave();
    notifyListeners();
  }

  void deleteVaultEntry(String id) {
    final removed = vault.delete(id);
    if (json.documentId != null && removed.contains(json.documentId)) {
      json.documentId = null;
    }
    if (note.documentId != null && removed.contains(note.documentId)) {
      note.documentId = null;
    }
    jsonVaultPrefs.expanded.removeAll(removed);
    noteVaultPrefs.expanded.removeAll(removed);
    for (final removedId in removed) {
      final dir = attachments.directoryFor(removedId);
      if (dir.existsSync()) {
        unawaited(dir.delete(recursive: true));
      }
    }
    scheduleSave();
    notifyListeners();
  }

  String createNoteDocument({String name = 'untitled.md', String? parent}) {
    if (note.documentId != null && note.document.dirty) {
      saveNoteDocument();
    }
    final id = vault.createDocument(
        toolId: 'quickNote',
        name: name,
        content: note.document.text,
        parent: parent);
    final file = _document(id);
    if (file == null) throw StateError('Failed to create note $id');
    file.metadata.addAll(note.metadata.toJson());
    note.metadata.title = name;
    file.metadata['title'] = name;
    note.documentId = id;
    noteVaultPrefs.selectedEntryId = id;
    note.document.markSaved();
    unawaited(_exportNoteFile(file));
    scheduleSave();
    notifyListeners();
    return id;
  }

  String createNoteFolder({String name = 'folder', String? parent}) {
    final id =
        vault.createFolder(toolId: 'quickNote', name: name, parent: parent);
    noteVaultPrefs.expanded.add(id);
    scheduleSave();
    notifyListeners();
    return id;
  }

  void openNoteDocument(String id) {
    final file = _document(id);
    if (file == null) return;
    if (note.documentId != id && note.documentId != null) {
      if (note.document.dirty) {
        final current = _document(note.documentId);
        if (current == null) {
          note.notice = '当前笔记保存失败，未切换。';
          notifyListeners();
          return;
        }
        saveNoteDocument();
      }
    }
    note.documentId = id;
    final parsed = parseNoteDocument(file.content);
    final packed = file.content.startsWith('---\n');
    final body = packed ? parsed.body : file.content;
    note.metadata = packed
        ? parsed.metadata
        : NoteMetadata.fromJson({
            ...file.metadata,
            'title': file.title,
          });
    note.document.apply(body, recordUndo: false);
    note.document.resetHistory();
    note.document.markSaved();
    noteVaultPrefs.selectedEntryId = id;
    noteVaultPrefs.expanded.addAll(vault.ancestorsOf(id));
    scheduleSave();
    notifyListeners();
  }

  void saveNoteDocument() {
    if (note.documentId == null) {
      createNoteDocument();
      return;
    }
    final file = _document(note.documentId);
    if (file == null) return;
    file.content = note.document.text;
    file.metadata
      ..clear()
      ..addAll(note.metadata.toJson());
    file.modified = DateTime.now();
    note.document.markSaved();
    unawaited(_exportNoteFile(file));
    unawaited(attachments.deleteOrphans(file.id, file.content));
    scheduleSave();
    notifyListeners();
  }

  void setNoteText(String value,
      {int? selectionStart, int? selectionEnd, bool recordUndo = true}) {
    note.document.apply(value,
        selectionStart: selectionStart ?? note.document.selectionStart,
        selectionEnd: selectionEnd ?? note.document.selectionEnd,
        recordUndo: recordUndo);
    scheduleSave();
    notifyListeners();
  }

  void applyQuickReplace(String action) {
    try {
      note.document.transformSelectionOrAll(
          (value) => runQuickReplace(value, action));
      note.notice = action;
    } catch (error) {
      note.notice = error.toString();
    }
    scheduleSave();
    notifyListeners();
  }

  Future<void> importNoteImage(File source) async {
    final id = note.documentId ?? createNoteDocument();
    final relative = await attachments.importFile(id, source);
    final alt = source.uri.pathSegments.isEmpty
        ? relative
        : source.uri.pathSegments.last;
    final insertion = prepareMarkdownImageInsertion(
      note.document.text,
      TextSelectionRange(
          start: note.document.selectionStart,
          end: note.document.selectionEnd),
      '![$alt]($relative)',
    );
    note.document.apply(
      '${note.document.text.substring(0, insertion.start)}${insertion.text}${note.document.text.substring(insertion.end)}',
      selectionStart: insertion.caret,
      selectionEnd: insertion.caret,
    );
    saveNoteDocument();
  }

  void pasteNoteImageFromClipboard() {
    note.notice = '剪贴板图片需要平台通道，本轮未实现。普通文本粘贴仍走编辑器，不会被拦截成图片。';
    notifyListeners();
  }

  void importNoteMarkdown(String source, {String name = 'imported.md'}) {
    final parsed = parseNoteDocument(source);
    if (source.startsWith('---\n')) {
      note.metadata = parsed.metadata;
      note.document.apply(parsed.body);
    } else {
      note.document.apply(source);
    }
    createNoteDocument(name: name);
  }

  Future<void> copyNote() async {
    await Clipboard.setData(ClipboardData(text: note.document.text));
    toast = t('json.action.copied');
    notifyListeners();
  }

  void wrapNoteLines(String prefix) {
    note.document.transformSelectionOrAll((value) => value
        .split('\n')
        .map((line) => line.trim().isEmpty ? line : '$prefix$line')
        .join('\n'));
    scheduleSave();
    notifyListeners();
  }

  void insertNoteColumn(String insertion) {
    if (note.document.column == null) {
      note.document.setColumnFromSelection();
    }
    note.document.insertInColumn(insertion);
    scheduleSave();
    notifyListeners();
  }

  void deleteNoteColumn() {
    if (note.document.column == null) {
      note.document.setColumnFromSelection();
    }
    note.document.deleteInColumn();
    scheduleSave();
    notifyListeners();
  }

  void markNoteColumn() {
    note.document.setColumnFromSelection();
    notifyListeners();
  }

  void renameVaultEntry(String id, String name) {
    vault.rename(id, name);
    final file = _document(id);
    if (file?.toolId == 'quickNote' && note.documentId == id) {
      note.metadata.title = name;
    }
    scheduleSave();
    notifyListeners();
  }

  String duplicateVaultEntry(String id) {
    final copyId = vault.duplicate(id);
    scheduleSave();
    notifyListeners();
    return copyId;
  }

  Future<void> _exportNoteFile(VaultDocument file) async {
    await paths.noteVaultDir.create(recursive: true);
    final target = File('${paths.noteVaultDir.path}/${file.id}.md');
    if (!target.path.startsWith(paths.noteVaultDir.path)) return;
    final packed = serializeNoteDocument(NoteDocument(
      metadata: NoteMetadata.fromJson({
        ...file.metadata,
        'title': file.title,
      }),
      body: file.content.startsWith('---\n')
          ? parseNoteDocument(file.content).body
          : file.content,
    ));
    await writeAtomicFile(target, packed);
  }

  void detachTool(String id) {
    if (id == 'mootool') return;
    final session = id == 'json'
        ? json.document
        : id == 'quickNote'
            ? note.document
            : draftFor(id);
    final begin = coordinator.beginTransfer(TransferRequest(
      sessionId: id,
      sourceWindowId: 'main',
      targetWindowId: 'detached-$id',
      revision: session.revision,
      text: session.text,
      selectionStart: session.selectionStart,
      selectionEnd: session.selectionEnd,
      undoDepth: 0,
    ));
    if (!begin.ok) {
      toast = begin.error ?? '';
      notifyListeners();
      return;
    }
    final ack = coordinator.ack(id, 'detached-$id', session.revision);
    if (!ack.ok) {
      coordinator.abort(id);
      toast = ack.error ?? '';
      notifyListeners();
      return;
    }
    detachedToolIds.add(id);
    notifyListeners();
  }

  void dockTool(String id) {
    final session = id == 'json'
        ? json.document
        : id == 'quickNote'
            ? note.document
            : draftFor(id);
    final begin = coordinator.beginTransfer(TransferRequest(
      sessionId: id,
      sourceWindowId: 'detached-$id',
      targetWindowId: 'main',
      revision: session.revision,
      text: session.text,
      selectionStart: session.selectionStart,
      selectionEnd: session.selectionEnd,
      undoDepth: 0,
    ));
    if (begin.ok) coordinator.ack(id, 'main', session.revision);
    detachedToolIds.remove(id);
    notifyListeners();
  }

  void addCustomGroup() {
    final number = settings.customGroups.length + 1;
    settings.customGroups.add(CustomToolGroup(
        id: 'group-$number-${DateTime.now().millisecondsSinceEpoch}',
        name: t('groups.defaultName', {'number': '$number'}),
        toolIds: []));
    scheduleSave();
    notifyListeners();
  }

  void hideTool(String id) {
    if (!settings.hiddenNavigationToolIds.contains(id))
      settings.hiddenNavigationToolIds.add(id);
    scheduleSave();
    notifyListeners();
  }

  void showTool(String id) {
    settings.hiddenNavigationToolIds.remove(id);
    scheduleSave();
    notifyListeners();
  }

  Future<GitStatus> jsonGitStatus() => git.status(paths.jsonVaultDir);

  Future<void> initJsonGit() async {
    await persist();
    await _exportVaultFiles();
    await git.init(paths.jsonVaultDir);
    notifyListeners();
  }

  Future<void> commitJsonGit(String message) async {
    await persist();
    await _exportVaultFiles();
    await git.commitAll(paths.jsonVaultDir, message);
    notifyListeners();
  }

  Future<GitStatus> noteGitStatus() => git.status(paths.noteVaultDir);

  Future<void> initNoteGit() async {
    await persist();
    await _exportNoteFiles();
    await git.init(paths.noteVaultDir);
    notifyListeners();
  }

  Future<void> commitNoteGit(String message) async {
    await persist();
    await _exportNoteFiles();
    await git.commitAll(paths.noteVaultDir, message);
    notifyListeners();
  }

  Future<void> _exportNoteFiles() async {
    for (final file
        in vault.documents.where((item) => item.toolId == 'quickNote')) {
      await _exportNoteFile(file);
    }
  }

  Future<void> _exportVaultFiles() async {
    final root = Directory('${paths.jsonVaultDir.path}/files');
    await root.create(recursive: true);
    for (final file in vault.documents.where((item) => item.toolId == 'json')) {
      final target = File('${root.path}/${file.id}.json');
      await writeAtomicFile(target, file.content);
    }
  }

  @override
  void dispose() {
    _saveTimer?.cancel();
    super.dispose();
  }
}
