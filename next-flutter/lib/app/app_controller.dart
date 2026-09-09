import 'dart:async';
import 'dart:io';

import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';

import 'app_paths.dart';
import 'local_session.dart';
import 'product.dart';
import 'settings.dart';
import 'tool_registry.dart';
import '../core/desktop/desktop_host.dart';
import '../core/desktop/window_policy.dart';
import '../core/update/update_downloader.dart';
import '../core/update/update_models.dart';
import '../core/update/update_service.dart';
import '../core/editor/editor_document.dart';
import '../core/editor/find_replace.dart';
import '../core/git/git_service.dart';
import '../core/storage/atomic_file.dart';
import '../core/storage/document_vault.dart';
import '../core/storage/snapshot_backup.dart';
import '../core/window/session_transfer.dart';
import '../features/hardware/system_info.dart';
import '../features/host/host_session.dart';
import '../features/http/http_client.dart';
import '../features/http/http_models.dart';
import '../features/http/http_session.dart';
import '../features/image/image_library.dart';
import '../features/image/image_process.dart';
import '../features/image/image_tools.dart';
import '../features/json/json_engine.dart';
import '../features/json/json_path.dart';
import '../features/message_board/message_board.dart';
import '../features/pdf/pdf_service.dart';
import '../features/pdf/pdf_session.dart';
import '../features/quick_note/note_attachments.dart';
import '../features/quick_note/note_frontmatter.dart';
import '../features/quick_note/quick_note_session.dart';
import '../features/quick_note/quick_replace.dart';
import '../features/runtime/runtime_service.dart';
import '../features/runtime/runtime_tools.dart';
import '../features/translation/translation_client.dart';
import '../features/variables/environment_store.dart';
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
  AppController(this.paths,
      {DesktopHost? desktopHost, UpdateService? updateService})
      : desktopHost = desktopHost ?? ChannelDesktopHost(),
        updateService = updateService ?? UpdateService() {
    this.desktopHost.onCloseRequested = () {
      unawaited(handleNativeCloseRequested());
    };
    this.desktopHost.onTrayAction = (action) {
      if (action == 'screenshot') {
        unawaited(captureScreenshotToLibrary());
      } else if (action == 'color') {
        openTool('colorBoard');
        unawaited(pickScreenColorInto('colorBoard'));
      }
    };
  }

  final AppPaths paths;
  final DesktopHost desktopHost;
  final UpdateService updateService;
  DesktopCapabilities desktopCaps = DesktopCapabilities();
  bool closePrompt = false;
  String? lastBackupPath;
  String dataNotice = '';
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
  final HttpSession http = HttpSession();
  final HttpSender httpSender = HttpSender();
  final HostSession host = HostSession();
  final RuntimeSession runtime = RuntimeSession();
  final TranslationSession translation = TranslationSession();
  final TranslationClient translationClient = TranslationClient();
  late final EnvironmentStore environmentStore =
      EnvironmentStore(File('${paths.dataRoot.path}/environment/user.json'));
  late final RuntimeExecutionService runtimeService =
      RuntimeExecutionService(Directory('${paths.cacheRoot.path}/runtime'));
  List<EnvironmentEntry> environmentProcess = [];
  List<EnvironmentEntry> environmentRuntime = [];
  List<EnvironmentEntry> environmentUser = [];
  HardwareSnapshot? hardwareSnapshot;
  final MessageBoardSession messageBoard = MessageBoardSession();
  final PdfSession pdf = PdfSession();
  late final ImageLibrary imageLibrary = ImageLibrary(paths.imagesDir);
  List<ImageAssetSummary> imageAssets = [];
  final Set<String> imageSelected = {};
  String? imageCurrentName;
  ImageAsset? imageCurrent;
  bool imageListVisible = true;
  bool imageFit = true;
  String imagePanel = '';
  String imageNotice = '';
  String imageBase64Draft = '';
  String imageRenameDraft = '';
  Uint8List? screenshotDraft;
  int screenshotCropLeft = 0;
  int screenshotCropTop = 0;
  int screenshotCropWidth = 0;
  int screenshotCropHeight = 0;
  late final NoteAttachmentStore attachments =
      NoteAttachmentStore(paths.noteVaultDir);
  DocumentVault vault = DocumentVault();
  VaultPreferences jsonVaultPrefs = VaultPreferences();
  VaultPreferences noteVaultPrefs = VaultPreferences();
  final List<HistoryRecord> histories = [];
  Timer? _saveTimer;
  bool _pauseAutosave = false;
  bool _closed = false;
  Future<void> _writeQueue = Future.value();
  UpdateCheckResult? updateResult;
  UpdateDownloadStatus updateDownloadStatus = UpdateDownloadStatus.idle;
  String updateNotice = '';
  double updatePercent = 0;
  String? updateLocalPath;
  UpdateDownloader? _updateDownloader;
  UpdateBytesFetcher? updateBytesFetcher;
  Future<bool> Function(String path)? updateOpener;

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
    for (final id in detachedToolIds) {
      coordinator.forceOwner(id, 'detached-$id');
    }
    await applyDesktopPolicy(launch: true);
    if (settings.autoCheckUpdates) {
      unawaited(checkForUpdates(quiet: true));
    }
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
    if (workspace['http'] is Map)
      http.restore(Map<String, Object?>.from(workspace['http'] as Map));
    if (workspace['host'] is Map)
      host.restore(Map<String, Object?>.from(workspace['host'] as Map));
    if (workspace['runtime'] is Map)
      runtime.restore(Map<String, Object?>.from(workspace['runtime'] as Map));
    if (workspace['translation'] is Map)
      translation
          .restore(Map<String, Object?>.from(workspace['translation'] as Map));
    if (workspace['messageBoard'] is Map)
      messageBoard
          .restore(Map<String, Object?>.from(workspace['messageBoard'] as Map));
    if (workspace['pdf'] is Map)
      pdf.restore(Map<String, Object?>.from(workspace['pdf'] as Map));
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
    detachedToolIds
      ..clear()
      ..addAll([
        for (final item in workspace['detachedToolIds'] as List? ?? const [])
          if (item is String && item != 'mootool') item
      ]);
    for (final id in detachedToolIds) {
      coordinator.forceOwner(id, 'detached-$id');
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
        'http': http.toJson(),
        'host': host.toJson(),
        'runtime': runtime.toJson(),
        'translation': translation.toJson(),
        'messageBoard': messageBoard.toJson(),
        'pdf': pdf.toJson(),
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
        'detachedToolIds': detachedToolIds.toList(),
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
    if (id != 'messageBoard' && messageBoard.presenting) {
      unawaited(setMessageBoardPresenting(false));
    }
    if (id == 'java' && runtime.statuses.isEmpty) {
      unawaited(detectRuntimes());
    }
    if (id == 'hardware' && hardwareSnapshot == null) {
      unawaited(refreshHardware());
    }
    if (id == 'variables' && environmentProcess.isEmpty) {
      unawaited(refreshEnvironment());
    }
    if (id == 'image' && imageAssets.isEmpty) {
      unawaited(refreshImages());
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
    if (messageBoard.presenting) {
      unawaited(setMessageBoardPresenting(false));
      return;
    }
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
        'net' => 'ipv4',
        'variables' => 'process',
        'hardware' => 'system',
        'pdf' => 'split',
        'image' => 'library',
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
      note.document
          .transformSelectionOrAll((value) => runQuickReplace(value, action));
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
    _insertNoteMarkdownImage(
        source.uri.pathSegments.isEmpty
            ? relative
            : source.uri.pathSegments.last,
        relative);
  }

  Future<void> pasteNoteImageFromClipboard() async {
    final bytes = await desktopHost.readClipboardImage();
    if (bytes == null || bytes.isEmpty) {
      note.notice = t('note.clipboardEmpty');
      notifyListeners();
      return;
    }
    try {
      final id = note.documentId ?? createNoteDocument();
      final name = clipboardImageFileName(bytes);
      final relative =
          await attachments.importBytes(id, name: name, bytes: bytes);
      _insertNoteMarkdownImage(name, relative);
    } catch (error) {
      note.notice = error.toString();
      notifyListeners();
    }
  }

  void _insertNoteMarkdownImage(String alt, String relative) {
    final insertion = prepareMarkdownImageInsertion(
      note.document.text,
      TextSelectionRange(
          start: note.document.selectionStart, end: note.document.selectionEnd),
      '![$alt]($relative)',
    );
    note.document.apply(
      '${note.document.text.substring(0, insertion.start)}${insertion.text}${note.document.text.substring(insertion.end)}',
      selectionStart: insertion.caret,
      selectionEnd: insertion.caret,
    );
    saveNoteDocument();
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

  Future<void> _exportNoteFile(VaultDocument file) {
    final done = _writeQueue.then((_) async {
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
    });
    _writeQueue = done.catchError((_) {});
    return done;
  }

  void newHttpDraft() {
    http.draft = HttpSession.emptyDraft();
    http.response = null;
    http.notice = '';
    notifyListeners();
  }

  void saveHttpDraft() {
    http.draft.id ??= 'http-${DateTime.now().microsecondsSinceEpoch}';
    SavedHttpRequest? existing;
    for (final item in http.collection) {
      if (item.request.id == http.draft.id) {
        existing = item;
        break;
      }
    }
    final snapshot = SavedHttpRequest(
      request: http.draft.copy(),
      responseBody: http.response?.body ?? '',
      responseHeaders: http.response?.headers ?? '',
      responseCookies: http.response?.cookies ?? '',
    );
    if (existing != null) {
      existing.request = snapshot.request;
      existing.responseBody = snapshot.responseBody;
      existing.responseHeaders = snapshot.responseHeaders;
      existing.responseCookies = snapshot.responseCookies;
      existing.modified = DateTime.now();
    } else {
      http.collection.add(snapshot);
    }
    scheduleSave();
    notifyListeners();
  }

  void openHttpSaved(SavedHttpRequest item) {
    http.draft = item.request.copy();
    http.response = HttpResponseResult(
      requestId: 'saved',
      ok: true,
      status: 0,
      statusText: '',
      url: item.request.url,
      durationMs: 0,
      body: item.responseBody,
      headers: item.responseHeaders,
      cookies: item.responseCookies,
    );
    notifyListeners();
  }

  void deleteHttpSaved(String? id) {
    http.collection.removeWhere((item) => item.request.id == id);
    scheduleSave();
    notifyListeners();
  }

  Future<void> sendHttp() async {
    if (http.draft.url.trim().isEmpty) {
      http.notice = t('http.urlRequired');
      notifyListeners();
      return;
    }
    http.activeRequestId = 'http-${DateTime.now().microsecondsSinceEpoch}';
    http.sending = true;
    http.notice = '';
    notifyListeners();
    final requestId = http.activeRequestId;
    final result = await httpSender.send(
      requestId: requestId,
      request: http.draft,
      timeoutMs: http.timeoutMs > 0 ? http.timeoutMs : settings.httpTimeoutMs,
      proxy: HttpProxyConfig(
        enabled: settings.proxyEnabled,
        host: settings.proxyHost,
        port: settings.proxyPort,
        username: settings.proxyUsername,
        password: settings.proxyPassword,
      ),
    );
    if (http.activeRequestId != requestId) return;
    http.response = result;
    http.sending = false;
    if (!result.ok) {
      http.notice = result.statusText;
    } else {
      recordToolHistory(
          toolId: 'http',
          title: '${http.draft.method} ${http.draft.url}',
          input: http.draft.url,
          output: result.body);
    }
    scheduleSave();
    notifyListeners();
  }

  void cancelHttp() {
    httpSender.cancel(http.activeRequestId);
    http.activeRequestId = '';
    http.sending = false;
    http.notice = 'ABORTED';
    notifyListeners();
  }

  void createHostProfile() {
    final profile = HostProfile(
        id: 'host-${DateTime.now().microsecondsSinceEpoch}',
        name: 'untitled',
        content: '127.0.0.1 localhost\n');
    host.profiles.add(profile);
    host.selectedId = profile.id;
    scheduleSave();
    notifyListeners();
  }

  void saveHostProfile() {
    host.selected?.modified = DateTime.now();
    scheduleSave();
    notifyListeners();
  }

  void deleteHostProfile() {
    host.profiles.removeWhere((item) => item.id == host.selectedId);
    host.selectedId = host.profiles.isEmpty ? null : host.profiles.first.id;
    scheduleSave();
    notifyListeners();
  }

  void applyHostToSystem() {
    host.notice = '系统 hosts 写入需要提权助手，本轮未实现。方案只保存在本产品数据目录，拒绝授权时不会标记为已应用到系统。';
    notifyListeners();
  }

  Future<void> refreshEnvironment() async {
    environmentProcess = await environmentStore.processEntries();
    environmentRuntime = environmentStore.runtimeEntries();
    final user = await environmentStore.readUser();
    environmentUser = [
      for (final entry in user.entries)
        EnvironmentEntry(key: entry.key, value: entry.value, scope: 'user')
    ]..sort((a, b) => a.key.compareTo(b.key));
    if (_closed) return;
    notifyListeners();
  }

  void editUserVariable(String key, String value) {
    final session = localFor('variables');
    session.options['userKey'] = key;
    session.options['userValue'] = value;
    notifyListeners();
  }

  Future<void> saveUserVariable() async {
    final session = localFor('variables');
    final key = (session.options['userKey'] ?? '').trim();
    if (key.isEmpty || key.contains('\u0000') || key.contains('=')) {
      session.notice = t('variables.invalidKey');
      notifyListeners();
      return;
    }
    final values = await environmentStore.readUser();
    values[key] = session.options['userValue'] ?? '';
    await environmentStore.writeUser(values);
    await refreshEnvironment();
    scheduleSave();
  }

  Future<void> deleteUserVariable(String key) async {
    final values = await environmentStore.readUser();
    values.remove(key);
    await environmentStore.writeUser(values);
    await refreshEnvironment();
    scheduleSave();
  }

  Future<void> detectRuntimes() async {
    runtime.statuses = await runtimeService.detect(
      javaPath: settings.javaPath,
      groovyPath: settings.groovyPath,
      pythonPath: settings.pythonPath,
      nodePath: settings.nodePath,
    );
    if (_closed) return;
    notifyListeners();
  }

  Future<void> runRuntime() async {
    final session = runtime;
    final requestId =
        'run-${DateTime.now().millisecondsSinceEpoch}-${session.runtime}';
    session.requestId = requestId;
    session.running = true;
    session.notice = '';
    session.stdout = '';
    session.stderr = '';
    notifyListeners();
    try {
      final args =
          parseRuntimeArguments(session.arguments[session.runtime] ?? '');
      final result = await runtimeService.run(
        requestId: requestId,
        runtime: session.runtime,
        code: session.code,
        arguments: args,
        workingDirectory: session.workingDirectories[session.runtime] ?? '',
        javaPath: settings.javaPath,
        groovyPath: settings.groovyPath,
        pythonPath: settings.pythonPath,
        nodePath: settings.nodePath,
      );
      if (session.requestId != requestId) return;
      session.result = result;
      session.stdout = result.stdout;
      session.stderr = result.stderr;
      if (result.timedOut ||
          result.cancelled ||
          result.truncated ||
          result.exitCode != 0) {
        session.notice = [
          if (result.timedOut) 'TIMEOUT',
          if (result.cancelled) 'CANCELLED',
          if (result.truncated) 'TRUNCATED',
          if (result.exitCode != 0) 'exit ${result.exitCode}',
        ].join(' ');
      }
      recordToolHistory(
          toolId: 'java',
          title: '${session.runtime} ${result.exitCode}',
          input: session.code,
          output: '${result.stdout}\n${result.stderr}');
    } catch (error) {
      if (session.requestId == requestId) {
        session.notice = error.toString();
      }
    } finally {
      if (session.requestId == requestId) session.running = false;
      if (!_closed) {
        scheduleSave();
        notifyListeners();
      }
    }
  }

  Future<void> stopRuntime() async {
    await runtimeService.cancel(runtime.requestId);
    runtime.requestId = '';
    runtime.running = false;
    runtime.notice = 'CANCELLED';
    notifyListeners();
  }

  Future<void> translateText() async {
    final session = translation;
    final requestId = 'tr-${DateTime.now().millisecondsSinceEpoch}';
    session.requestId = requestId;
    session.translating = true;
    session.notice = '';
    notifyListeners();
    try {
      final result = await translationClient.translate(
        requestId: requestId,
        text: session.source,
        sourceLang: session.sourceLang,
        targetLang: session.targetLang,
        timeoutMs: settings.translationTimeoutMs,
      );
      if (session.requestId != requestId) return;
      session.target = result.text;
      session.provider = result.provider;
      session.history.insert(0, {
        'source': session.source,
        'target': result.text,
        'from': session.sourceLang,
        'to': session.targetLang,
      });
      if (session.history.length > 50) {
        session.history.removeRange(50, session.history.length);
      }
    } catch (error) {
      if (session.requestId == requestId) {
        session.notice = error.toString();
        session.target = '';
      }
    } finally {
      if (session.requestId == requestId) session.translating = false;
      if (!_closed) {
        scheduleSave();
        notifyListeners();
      }
    }
  }

  void cancelTranslation() {
    translationClient.cancel(translation.requestId);
    translation.requestId = '';
    translation.translating = false;
    translation.notice = 'ABORTED';
    notifyListeners();
  }

  Future<void> refreshHardware() async {
    hardwareSnapshot = await collectSystemInfo();
    if (_closed) return;
    notifyListeners();
  }

  List<String> get imageTargets => imageSelected.isNotEmpty
      ? imageSelected.toList()
      : [if (imageCurrentName != null) imageCurrentName!];

  Future<void> refreshImages({String? preferred}) async {
    imageAssets = await imageLibrary.list();
    final name = preferred ??
        imageCurrentName ??
        (imageAssets.isEmpty ? null : imageAssets.first.name);
    if (name == null) {
      imageCurrent = null;
      imageCurrentName = null;
      imageSelected.clear();
    } else {
      await selectImage(name);
      imageSelected.removeWhere(
          (item) => imageAssets.every((asset) => asset.name != item));
    }
    if (_closed) return;
    notifyListeners();
  }

  Future<void> selectImage(String name) async {
    imageCurrent = await imageLibrary.read(name);
    imageCurrentName = name;
    imageFit = true;
    if (imageSelected.isEmpty) imageSelected.add(name);
    notifyListeners();
  }

  void toggleImageSelection(String name, bool selected) {
    if (selected) {
      imageSelected.add(name);
    } else {
      imageSelected.remove(name);
    }
    notifyListeners();
  }

  Future<void> importImageFiles(List<File> files) async {
    ImageAsset? last;
    for (final file in files) {
      last = await imageLibrary.save(
          name: file.uri.pathSegments.last, bytes: await file.readAsBytes());
    }
    await refreshImages(preferred: last?.name);
  }

  Future<void> importImageBase64(String value) async {
    try {
      final dataUrl = ensureImageDataUrl(value);
      final saved = await imageLibrary.saveDataUrl(
          name: 'Base64-${DateTime.now().millisecondsSinceEpoch}.png',
          dataUrl: dataUrl);
      imagePanel = '';
      imageNotice = '';
      await refreshImages(preferred: saved.name);
    } catch (error) {
      imageNotice = error.toString();
      notifyListeners();
    }
  }

  Future<void> exportCurrentImageBase64() async {
    final current = imageCurrent;
    if (current == null) return;
    imageNotice = imageToDataUrl(current.bytes);
    imagePanel = 'base64';
    notifyListeners();
  }

  Future<void> renameCurrentImage() async {
    final current = imageCurrentName;
    final next = (localFor('image').options['rename'] ?? '').trim();
    if (current == null || next.isEmpty) return;
    final renamed = await imageLibrary.rename(current, next);
    await refreshImages(preferred: renamed.name);
  }

  Future<void> deleteSelectedImages() async {
    await imageLibrary.delete(imageTargets);
    imageCurrent = null;
    imageCurrentName = null;
    imageSelected.clear();
    await refreshImages();
  }

  Future<void> exportImages(Directory directory) async {
    imageNotice = await imageLibrary.exportTo(imageTargets, directory);
    notifyListeners();
  }

  Future<void> setMessageBoardPresenting(bool value) async {
    messageBoard.presenting = value;
    final awake = await desktopHost.setPreventDisplaySleep(value);
    messageBoard.displayAwake = awake;
    if (value && !awake) {
      toast = t('messageBoard.sleepHint');
    }
    notifyListeners();
  }

  Future<void> createBackup() async {
    try {
      final dest = await SnapshotBackup.create(paths);
      lastBackupPath = dest.path;
      dataNotice = t('settings.backupDone');
    } catch (error) {
      dataNotice = error.toString();
    }
    notifyListeners();
  }

  Future<void> restoreBackup(Directory snapshot) async {
    try {
      await persist();
      await SnapshotBackup.restore(snapshot, paths);
      dataNotice = t('settings.restoreDone');
    } catch (error) {
      dataNotice = error.toString();
    }
    notifyListeners();
  }

  Future<void> captureScreenshotToLibrary() async {
    final bytes = await desktopHost.captureScreenRegion();
    if (bytes == null) {
      noteImageDesktopGap(desktopCaps.screenshot
          ? t('image.screenshotDenied')
          : t('image.screenshotPending'));
      return;
    }
    try {
      final size = imagePixelSize(bytes);
      screenshotDraft = bytes;
      screenshotCropLeft = 0;
      screenshotCropTop = 0;
      screenshotCropWidth = size.width;
      screenshotCropHeight = size.height;
      imagePanel = 'screenshot';
      imageNotice = t('image.screenshotCropHint', {
        'width': '${size.width}',
        'height': '${size.height}',
      });
      notifyListeners();
    } catch (error) {
      noteImageDesktopGap(error.toString());
    }
  }

  Future<void> confirmScreenshotDraft({required bool crop}) async {
    final draft = screenshotDraft;
    if (draft == null) return;
    try {
      final bytes = crop
          ? cropImageBytes(draft,
              left: screenshotCropLeft,
              top: screenshotCropTop,
              width: screenshotCropWidth,
              height: screenshotCropHeight)
          : draft;
      final saved = await imageLibrary.save(
          name: 'Screenshot-${DateTime.now().millisecondsSinceEpoch}.png',
          bytes: bytes);
      cancelScreenshotDraft();
      await refreshImages(preferred: saved.name);
    } catch (error) {
      noteImageDesktopGap(error.toString());
    }
  }

  void cancelScreenshotDraft() {
    screenshotDraft = null;
    if (imagePanel == 'screenshot') imagePanel = '';
    notifyListeners();
  }

  Future<void> pickScreenColorInto(String toolId) async {
    final color = await desktopHost.pickScreenColor();
    if (color == null) {
      toast = desktopCaps.screenColor
          ? t('color.pickDenied')
          : t('color.pickPending');
      notifyListeners();
      return;
    }
    runLocal(toolId, (session) {
      session.left = color;
    });
  }

  Future<void> importClipboardImageToLibrary() async {
    final bytes = await desktopHost.readClipboardImage();
    if (bytes == null || bytes.isEmpty) {
      noteImageDesktopGap(t('image.clipboardEmpty'));
      return;
    }
    try {
      final saved = await imageLibrary.save(
          name: clipboardImageFileName(bytes), bytes: bytes);
      await refreshImages(preferred: saved.name);
    } catch (error) {
      noteImageDesktopGap(error.toString());
    }
  }

  void noteImageDesktopGap(String message) {
    imageNotice = message;
    notifyListeners();
  }

  Future<void> applyDesktopPolicy({bool launch = false}) async {
    await desktopHost.applyWindowPolicy(
      closeBehavior: settings.closeBehavior.name,
      trayEnabled: settings.trayEnabled,
      startMaximized: launch && settings.startMaximized,
    );
    desktopCaps = await desktopHost.capabilities();
    notifyListeners();
  }

  CloseBehavior get effectiveCloseBehavior => WindowPolicy.effectiveClose(
        requested: settings.closeBehavior,
        trayEnabled: settings.trayEnabled,
        trayAvailable: desktopCaps.tray,
      );

  Future<void> handleNativeCloseRequested() async {
    switch (effectiveCloseBehavior) {
      case CloseBehavior.ask:
        closePrompt = true;
        notifyListeners();
      case CloseBehavior.hide:
        await confirmClose(CloseDecision.hide);
      case CloseBehavior.quit:
        await confirmClose(CloseDecision.quit);
    }
  }

  Future<void> confirmClose(CloseDecision decision) async {
    closePrompt = false;
    if (decision == CloseDecision.cancel) {
      await desktopHost.performCloseAction('cancel');
      notifyListeners();
      return;
    }
    if (decision == CloseDecision.hide &&
        WindowPolicy.effectiveClose(
              requested: CloseBehavior.hide,
              trayEnabled: settings.trayEnabled,
              trayAvailable: desktopCaps.tray,
            ) !=
            CloseBehavior.hide) {
      toast = t('settings.hideNeedsTray');
      closePrompt = true;
      notifyListeners();
      return;
    }
    await persist();
    await desktopHost.performCloseAction(decision.name);
    notifyListeners();
  }

  void cancelClosePrompt() {
    closePrompt = false;
    unawaited(desktopHost.performCloseAction('cancel'));
    notifyListeners();
  }

  Future<void> compressSelectedImages(
      CompressImageOptions options, String mode) async {
    await _processImages((bytes) => compressImageBytes(bytes, options),
        'compressed', mode, options.format);
  }

  Future<void> watermarkSelectedImages(WatermarkImageOptions options) async {
    await _processImages((bytes) => watermarkImageBytes(bytes, options),
        'watermarked', 'keep', 'auto');
  }

  Future<void> vectorizeSelectedImages(VectorizeOptions options) async {
    try {
      String? preferred;
      for (final name in imageTargets) {
        final asset = await imageLibrary.read(name);
        final svg = vectorizeImage(asset.bytes, options);
        final saved = await imageLibrary.save(
            name: processedImageName(name, 'svg', 'png')
                .replaceAll('.png', '.svg'),
            bytes: Uint8List.fromList(svg.codeUnits));
        preferred ??= saved.name;
      }
      imageNotice = t('image.svgComplete', {'count': '${imageTargets.length}'});
      await refreshImages(preferred: preferred);
    } catch (error) {
      imageNotice = error.toString();
      notifyListeners();
    }
  }

  Future<void> _processImages(Uint8List Function(Uint8List bytes) transform,
      String suffix, String mode, String format) async {
    try {
      String? preferred;
      for (final name in imageTargets) {
        final asset = await imageLibrary.read(name);
        final bytes = transform(asset.bytes);
        final outputName = mode == 'overwrite'
            ? overwriteName(asset.name, format)
            : processedImageName(asset.name, suffix, format);
        final saved = await imageLibrary.save(name: outputName, bytes: bytes);
        if (mode == 'overwrite' && saved.name != asset.name) {
          await imageLibrary.delete([asset.name]);
        }
        preferred ??= saved.name;
      }
      imageNotice =
          t('image.processComplete', {'count': '${imageTargets.length}'});
      await refreshImages(preferred: preferred);
    } catch (error) {
      imageNotice = error.toString();
      notifyListeners();
    }
  }

  Future<void> addPdfFiles(List<File> files) async {
    try {
      final additions = [for (final file in files) inspectPdfFile(file)];
      if (pdf.tab == 'split') {
        final next = appendPdfTasks([...pdf.splitRows], additions);
        pdf.splitRows
          ..clear()
          ..addAll(next);
      } else {
        final next = appendPdfTasks([...pdf.mergeRows], additions);
        pdf.mergeRows
          ..clear()
          ..addAll(next);
      }
      pdf.notice = '';
    } catch (error) {
      pdf.notice = error.toString();
    }
    scheduleSave();
    notifyListeners();
  }

  Future<void> splitSelectedPdfs() async {
    final selected = [
      for (final row in pdf.splitRows)
        if (row.selected) row
    ];
    if (selected.isEmpty) {
      pdf.notice = t('pdf.selectTask');
      notifyListeners();
      return;
    }
    pdf.busy = true;
    notifyListeners();
    try {
      final outputDir = Directory('${paths.dataRoot.path}/pdf-output');
      final result = splitPdfTasks(selected, outputDirectory: outputDir.path);
      pdf.lastOutputs
        ..clear()
        ..addAll(result.outputs);
      for (final row in pdf.splitRows) {
        if (row.selected) row.status = 'done';
      }
      pdf.notice = t('pdf.splitComplete', {'count': '${result.pageCount}'});
    } catch (error) {
      for (final row in pdf.splitRows) {
        if (row.selected) row.status = 'error';
      }
      pdf.notice = error.toString();
    } finally {
      pdf.busy = false;
      scheduleSave();
      notifyListeners();
    }
  }

  Future<void> mergeSelectedPdfs() async {
    final selected = [
      for (final row in pdf.mergeRows)
        if (row.selected) row
    ];
    if (selected.length < 2) {
      pdf.notice = t('pdf.selectTwo');
      notifyListeners();
      return;
    }
    pdf.busy = true;
    notifyListeners();
    try {
      final output = File(
          '${paths.dataRoot.path}/pdf-output/merged_${DateTime.now().millisecondsSinceEpoch}.pdf');
      final result = mergePdfTasks(selected, output.path);
      pdf.lastOutputs
        ..clear()
        ..addAll(result.outputs);
      for (final row in pdf.mergeRows) {
        if (row.selected) row.status = 'done';
      }
      pdf.notice = t('pdf.mergeComplete', {'count': '${result.pageCount}'});
    } catch (error) {
      for (final row in pdf.mergeRows) {
        if (row.selected) row.status = 'error';
      }
      pdf.notice = error.toString();
    } finally {
      pdf.busy = false;
      scheduleSave();
      notifyListeners();
    }
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
    scheduleSave();
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
    scheduleSave();
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

  Future<void> checkForUpdates({bool quiet = false}) async {
    updateNotice = '';
    notifyListeners();
    try {
      final result = await updateService.check(Product.version);
      if (_closed) return;
      updateResult = result;
      if (result.status == UpdateCheckStatus.available &&
          result.download != null) {
        updateDownloadStatus = UpdateDownloadStatus.available;
      } else {
        updateDownloadStatus = UpdateDownloadStatus.idle;
        updateLocalPath = null;
      }
      if (!quiet) {
        updateNotice = switch (result.status) {
          UpdateCheckStatus.unpublished => t('settings.updateUnpublished'),
          UpdateCheckStatus.latest => t('settings.updateLatest'),
          UpdateCheckStatus.available => result.download == null
              ? t('settings.updateNoAsset')
              : t('settings.updateAvailable',
                  {'version': result.latestVersion}),
        };
      }
      if (result.status == UpdateCheckStatus.available &&
          result.download != null &&
          settings.autoDownloadUpdates) {
        await downloadUpdate();
      }
    } catch (error) {
      updateDownloadStatus = UpdateDownloadStatus.error;
      updateNotice = t('settings.updateError', {'error': '$error'});
    }
    notifyListeners();
  }

  Future<void> downloadUpdate() async {
    final download = updateResult?.download;
    if (download == null) {
      updateNotice = t('settings.updateNoAsset');
      notifyListeners();
      return;
    }
    _updateDownloader = UpdateDownloader(
      directory: paths.updatesDir,
      fetcher: updateBytesFetcher ?? defaultUpdateBytesFetcher,
      opener: updateOpener ?? openLocalPath,
    );
    updateDownloadStatus = UpdateDownloadStatus.downloading;
    updatePercent = 0;
    updateNotice = t('settings.updateDownloading');
    notifyListeners();
    try {
      final file =
          await _updateDownloader!.download(download, onProgress: (progress) {
        updatePercent = progress.percent;
        notifyListeners();
      });
      updateLocalPath = file.path;
      updateDownloadStatus = UpdateDownloadStatus.ready;
      updateNotice = t('settings.updateReadyUnsigned');
    } on UpdateCancelled {
      updateDownloadStatus = UpdateDownloadStatus.available;
      updateLocalPath = null;
      updateNotice = t('settings.updateCancelled');
    } catch (error) {
      updateDownloadStatus = UpdateDownloadStatus.error;
      updateLocalPath = null;
      updateNotice = t('settings.updateError', {'error': '$error'});
    }
    notifyListeners();
  }

  void cancelUpdateDownload() {
    _updateDownloader?.cancel();
  }

  Future<void> openDownloadedUpdate() async {
    final path = updateLocalPath;
    if (path == null) {
      updateNotice = t('settings.updateNotReady');
      notifyListeners();
      return;
    }
    final opened = await (_updateDownloader ??
            UpdateDownloader(directory: paths.updatesDir))
        .openInstaller(File(path));
    updateNotice =
        opened ? t('settings.updateOpened') : t('settings.updateOpenFailed');
    notifyListeners();
  }

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
    _closed = true;
    _saveTimer?.cancel();
    super.dispose();
  }
}
