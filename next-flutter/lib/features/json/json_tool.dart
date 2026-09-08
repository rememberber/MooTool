import 'dart:convert';

import 'package:file_selector/file_selector.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import '../../app/app_controller.dart';
import '../../core/editor/find_replace.dart';
import '../../core/storage/document_vault.dart';
import '../../design/theme.dart';
import '../../design/widgets.dart';
import 'json_engine.dart';

class JsonToolPage extends StatelessWidget {
  const JsonToolPage({super.key, required this.controller});
  final AppController controller;

  @override
  Widget build(BuildContext context) {
    final width = MediaQuery.sizeOf(context).width;
    final compact = width < 1080;
    final session = controller.json;
    final tokens = tokensOf(context);
    return Shortcuts(
      shortcuts: {
        LogicalKeySet(LogicalKeyboardKey.meta, LogicalKeyboardKey.keyF):
            const _FindIntent(),
        LogicalKeySet(LogicalKeyboardKey.control, LogicalKeyboardKey.keyF):
            const _FindIntent(),
        LogicalKeySet(LogicalKeyboardKey.meta, LogicalKeyboardKey.keyS):
            const _SaveIntent(),
        LogicalKeySet(LogicalKeyboardKey.control, LogicalKeyboardKey.keyS):
            const _SaveIntent(),
        LogicalKeySet(LogicalKeyboardKey.meta, LogicalKeyboardKey.shift,
            LogicalKeyboardKey.keyF): const _FormatIntent(),
        LogicalKeySet(LogicalKeyboardKey.meta, LogicalKeyboardKey.enter):
            const _FormatIntent(),
        LogicalKeySet(LogicalKeyboardKey.control, LogicalKeyboardKey.enter):
            const _FormatIntent(),
      },
      child: Actions(
        actions: {
          _FindIntent: CallbackAction<_FindIntent>(onInvoke: (_) {
            session.findOpen = !session.findOpen;
            controller.refresh();
            return null;
          }),
          _SaveIntent: CallbackAction<_SaveIntent>(onInvoke: (_) {
            controller.saveJsonDocument();
            return null;
          }),
          _FormatIntent: CallbackAction<_FormatIntent>(onInvoke: (_) {
            controller.formatJson();
            return null;
          }),
        },
        child: Column(
          children: [
            _toolbar(context, tokens),
            if (session.findOpen) _findBar(tokens),
            Expanded(
              child: Row(
                children: [
                  if (!compact || session.inspectorOpen == false)
                    SizedBox(
                      width: session.vaultWidth.clamp(180.0, 320.0).toDouble(),
                      child: _vault(tokens, compact),
                    ),
                  Expanded(
                    child: MooCodeEditor(
                      document: session.document,
                      wrap: session.wrap,
                      fontSize: controller.settings.editorFontSize,
                      onChanged: (value) {
                        session.notice =
                            controller.jsonEngine.validate(value).message;
                        controller.scheduleSave();
                        controller.refresh();
                      },
                    ),
                  ),
                  if (!compact && session.inspectorOpen)
                    SizedBox(
                        width: session.inspectorWidth
                            .clamp(240.0, 360.0)
                            .toDouble(),
                        child: _inspector(tokens)),
                ],
              ),
            ),
            _status(tokens),
            if (session.outputBody.isNotEmpty) _output(tokens),
            if (session.pathPickerOpen) _pathPicker(tokens),
            if (session.historyOpen) _history(tokens),
          ],
        ),
      ),
    );
  }

  Widget _toolbar(BuildContext context, tokens) {
    final session = controller.json;
    return ColoredBox(
      color: tokens.toolbar,
      child: Padding(
        padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 6),
        child: Wrap(
          spacing: 6,
          runSpacing: 6,
          crossAxisAlignment: WrapCrossAlignment.center,
          children: [
            CompactButton(
                label: controller.t('json.action.format'),
                icon: Icons.auto_fix_high,
                primary: true,
                onPressed: () => controller.formatJson()),
            CompactButton(
                label: controller.t('json.action.compress'),
                icon: Icons.compress,
                onPressed: controller.compressJson),
            CompactButton(
                label: session.wrap
                    ? controller.t('json.action.wrap')
                    : controller.t('json.action.nowrap'),
                onPressed: () {
                  session.wrap = !session.wrap;
                  controller.refresh();
                }),
            CompactButton(
                label: controller.t('json.action.copy'),
                onPressed: () => controller.copyJson()),
            CompactButton(
                label: controller.t('json.action.find'),
                icon: Icons.search,
                onPressed: () {
                  session.findOpen = !session.findOpen;
                  controller.refresh();
                }),
            CompactButton(
                label: controller.t('json.action.import'),
                onPressed: () => _import()),
            CompactButton(
                label: controller.t('json.action.export'),
                onPressed: () => _export()),
            CompactButton(
                label: controller.t('json.action.history'),
                onPressed: () {
                  session.historyOpen = !session.historyOpen;
                  controller.refresh();
                }),
            CompactButton(
                label: controller.t('json.action.more'),
                onPressed: () {
                  session.inspectorOpen = !session.inspectorOpen;
                  controller.refresh();
                }),
            CompactButton(
                label: controller.t('json.action.clear'),
                onPressed: controller.clearJson),
          ],
        ),
      ),
    );
  }

  Widget _findBar(tokens) {
    final session = controller.json;
    final matches = findAllMatches(
        session.document.text, session.findQuery, session.findOptions);
    return ColoredBox(
      color: tokens.toolbar,
      child: Padding(
        padding: const EdgeInsets.fromLTRB(8, 0, 8, 8),
        child: Wrap(
            spacing: 8,
            crossAxisAlignment: WrapCrossAlignment.center,
            children: [
              SizedBox(
                width: 180,
                child: TextField(
                  decoration: InputDecoration(
                      isDense: true,
                      labelText: controller.t('findReplace.find')),
                  onChanged: (value) {
                    session.findQuery = value;
                    controller.refresh();
                  },
                ),
              ),
              SizedBox(
                width: 160,
                child: TextField(
                  decoration: InputDecoration(
                      isDense: true,
                      labelText: controller.t('findReplace.replace')),
                  onChanged: (value) => session.replaceText = value,
                ),
              ),
              FilterChip(
                  label: Text(controller.t('findReplace.matchCase')),
                  selected: session.findOptions.matchCase,
                  onSelected: (value) {
                    session.findOptions =
                        session.findOptions.copyWith(matchCase: value);
                    controller.refresh();
                  }),
              FilterChip(
                  label: Text(controller.t('findReplace.wholeWord')),
                  selected: session.findOptions.wholeWord,
                  onSelected: (value) {
                    session.findOptions =
                        session.findOptions.copyWith(wholeWord: value);
                    controller.refresh();
                  }),
              FilterChip(
                  label: Text(controller.t('findReplace.regex')),
                  selected: session.findOptions.regex,
                  onSelected: (value) {
                    session.findOptions =
                        session.findOptions.copyWith(regex: value);
                    controller.refresh();
                  }),
              Text(
                  '${controller.t('findReplace.foundPrefix')} ${matches.length}'),
              CompactButton(
                  label: controller.t('findReplace.next'),
                  onPressed: () {
                    final match = findNextMatch(
                        session.document.text,
                        session.findQuery,
                        session.findOptions,
                        session.document.selectionEnd,
                        forward: true);
                    if (match != null)
                      session.document
                          .restoreView(start: match.start, end: match.end);
                    controller.refresh();
                  }),
              CompactButton(
                  label: controller.t('findReplace.replace'),
                  onPressed: () {
                    final range = session.document.selection;
                    final result = replaceCurrentMatch(
                        content: session.document.text,
                        query: session.findQuery,
                        replaceWith: session.replaceText,
                        options: session.findOptions,
                        selection: FindMatch(range.start, range.end));
                    if (result.replaced)
                      controller.setJsonText(result.content,
                          selectionStart: result.match?.start,
                          selectionEnd: result.match?.end);
                  }),
              CompactButton(
                  label: controller.t('findReplace.replaceAll'),
                  onPressed: () {
                    final result = replaceAllMatches(
                        session.document.text,
                        session.findQuery,
                        session.replaceText,
                        session.findOptions);
                    controller.setJsonText(result.content);
                  }),
            ]),
      ),
    );
  }

  Widget _vault(tokens, bool compact) {
    final prefs = controller.jsonVaultPrefs;
    final nodes = controller.vault.tree('json',
        sort: prefs.sort,
        query: prefs.query,
        includeContent: prefs.includeContent);
    return ColoredBox(
      color: tokens.sidebar,
      child: Column(
        children: [
          Padding(
            padding: const EdgeInsets.all(8),
            child: TextField(
              decoration: InputDecoration(
                  isDense: true, hintText: controller.t('json.vault.search')),
              onChanged: (value) {
                prefs.query = value;
                controller.refresh();
              },
            ),
          ),
          CheckboxListTile(
            dense: true,
            title: Text(controller.t('json.vault.searchContent')),
            value: prefs.includeContent,
            onChanged: (value) {
              prefs.includeContent = value ?? true;
              controller.refresh();
            },
          ),
          Wrap(spacing: 4, children: [
            CompactButton(
                label: controller.t('json.vault.new'),
                onPressed: () => controller.createJsonDocument()),
            CompactButton(
                label: controller.t('json.vault.newFolder'),
                onPressed: () => controller.createJsonFolder()),
            CompactButton(
                label: controller.t('json.vault.save'),
                onPressed: controller.saveJsonDocument),
          ]),
          Expanded(
            child: nodes.isEmpty
                ? Center(
                    child: Text(controller.t('json.vault.empty'),
                        style: TextStyle(
                            color: tokens.textSecondary, fontSize: 12)))
                : ListView(children: [
                    for (final node in _flatten(nodes, 0, prefs.expanded))
                      _vaultRow(node, tokens)
                  ]),
          ),
        ],
      ),
    );
  }

  List<({VaultNode node, int depth})> _flatten(
      List<VaultNode> nodes, int depth, Set<String> expanded) {
    final rows = <({VaultNode node, int depth})>[];
    for (final node in nodes) {
      rows.add((node: node, depth: depth));
      if (node.isFolder && expanded.contains(node.id)) {
        rows.addAll(_flatten(node.children, depth + 1, expanded));
      }
    }
    return rows;
  }

  Widget _vaultRow(({VaultNode node, int depth}) row, tokens) {
    final selected = controller.jsonVaultPrefs.selectedEntryId == row.node.id;
    return InkWell(
      onTap: () {
        if (row.node.isFolder) {
          final expanded = controller.jsonVaultPrefs.expanded;
          if (!expanded.add(row.node.id)) expanded.remove(row.node.id);
          controller.refresh();
        } else {
          controller.openJsonDocument(row.node.id);
        }
      },
      onSecondaryTap: () => _vaultMenu(row.node),
      child: Container(
        color: selected ? tokens.accent.withValues(alpha: 0.12) : null,
        padding: EdgeInsets.fromLTRB(8.0 + row.depth * 12, 6, 8, 6),
        child: Row(children: [
          Icon(row.node.isFolder ? Icons.folder_outlined : Icons.data_object,
              size: 14, color: tokens.textSecondary),
          const SizedBox(width: 6),
          Expanded(
              child: Text(row.node.title,
                  overflow: TextOverflow.ellipsis,
                  style: const TextStyle(fontSize: 12))),
        ]),
      ),
    );
  }

  Future<void> _vaultMenu(VaultNode node) async {
    if (node.isFolder) return;
    controller.vault.duplicate(node.id);
    controller.scheduleSave();
    controller.refresh();
  }

  Widget _inspector(tokens) {
    final session = controller.json;
    final options = session.formatOptions;
    return ColoredBox(
      color: tokens.surface,
      child: ListView(
        padding: const EdgeInsets.all(12),
        children: [
          Text(controller.t('json.panel.format'),
              style: const TextStyle(fontWeight: FontWeight.w600)),
          DropdownButton<int>(
            value: options.spaces,
            items: const [
              DropdownMenuItem(value: 2, child: Text('2')),
              DropdownMenuItem(value: 4, child: Text('4'))
            ],
            onChanged: (value) {
              session.formatOptions = options.copyWith(spaces: value ?? 2);
              controller.refresh();
            },
          ),
          CheckboxListTile(
              dense: true,
              title: Text(controller.t('json.format.sortKeys')),
              value: options.sortKeys,
              onChanged: (value) {
                session.formatOptions = options.copyWith(sortKeys: value);
                controller.refresh();
              }),
          CheckboxListTile(
              dense: true,
              title: Text(controller.t('json.format.ignoreCase')),
              value: options.ignoreCase,
              onChanged: (value) {
                session.formatOptions = options.copyWith(ignoreCase: value);
                controller.refresh();
              }),
          CheckboxListTile(
              dense: true,
              title: Text(controller.t('json.format.duplicateKeys')),
              value: options.checkDuplicateKeys,
              onChanged: (value) {
                session.formatOptions =
                    options.copyWith(checkDuplicateKeys: value);
                controller.refresh();
              }),
          CompactButton(
              label: controller.t('json.format.apply'),
              primary: true,
              onPressed: () => controller.formatJson(advanced: true)),
          const SizedBox(height: 16),
          Text(controller.t('json.panel.convert'),
              style: const TextStyle(fontWeight: FontWeight.w600)),
          Wrap(spacing: 6, runSpacing: 6, children: [
            CompactButton(
                label: controller.t('json.action.jsonToXml'),
                onPressed: () => controller.convertJson(
                    controller.t('json.action.jsonToXml'),
                    controller.jsonEngine.jsonToXml)),
            CompactButton(
                label: controller.t('json.action.xmlToJson'),
                onPressed: () => _promptConvert(
                    controller.t('json.action.xmlToJson'),
                    controller.jsonEngine.xmlToJson)),
            CompactButton(
                label: controller.t('json.action.beanToJson'),
                onPressed: () => _promptConvert(
                    controller.t('json.action.beanToJson'),
                    controller.jsonEngine.javaBeanToJson)),
            CompactButton(
                label: controller.t('json.action.jsonToBean'),
                onPressed: () => controller.convertJson(
                    controller.t('json.action.jsonToBean'),
                    (input) => controller.jsonEngine.jsonToJavaBean(input,
                        rootClassName: session.className))),
            CompactButton(
                label: controller.t('json.action.swap'),
                onPressed: () => controller.convertJson(
                    controller.t('json.action.swap'),
                    controller.jsonEngine.swapKeysAndValues)),
            CompactButton(
                label: controller.t('json.action.escape'),
                onPressed: () => controller.setJsonText(controller.jsonEngine
                    .escapeJsonString(session.document.text))),
            CompactButton(
                label: controller.t('json.action.unescape'),
                onPressed: () => controller.runJson(() =>
                    controller.setJsonText(controller.jsonEngine
                        .unescapeJsonString(session.document.text)))),
            CompactButton(
                label: controller.t('json.action.escapeText'),
                onPressed: () => controller.setJsonText(controller.jsonEngine
                    .escapeJavaString(session.document.text))),
            CompactButton(
                label: controller.t('json.action.unescapeText'),
                onPressed: () => controller.runJson(() =>
                    controller.setJsonText(controller.jsonEngine
                        .unescapeJsonText(session.document.text)))),
          ]),
          TextField(
            decoration: InputDecoration(
                labelText: controller.t('json.dialog.className')),
            controller: TextEditingController(text: session.className),
            onChanged: (value) => session.className = value,
          ),
          const SizedBox(height: 16),
          Text(controller.t('json.panel.jsonPath'),
              style: const TextStyle(fontWeight: FontWeight.w600)),
          TextField(
            decoration: InputDecoration(
                hintText: controller.t('json.path.placeholder')),
            controller: TextEditingController(text: session.jsonPath),
            onChanged: (value) => session.jsonPath = value,
          ),
          Wrap(spacing: 6, children: [
            CompactButton(
                label: controller.t('json.path.query'),
                primary: true,
                onPressed: controller.queryJsonPath),
            CompactButton(
                label: controller.t('json.path.pick'),
                onPressed: () {
                  session.pathPickerOpen = true;
                  controller.refresh();
                }),
          ]),
        ],
      ),
    );
  }

  Widget _status(tokens) {
    final status =
        controller.jsonEngine.validate(controller.json.document.text);
    return ColoredBox(
      color: tokens.toolbar,
      child: SizedBox(
        height: 26,
        child: Padding(
          padding: const EdgeInsets.symmetric(horizontal: 10),
          child: Row(children: [
            Icon(
                status.kind == 'error'
                    ? Icons.error_outline
                    : Icons.check_circle_outline,
                size: 14,
                color: status.kind == 'error' ? Colors.red : tokens.accent),
            const SizedBox(width: 6),
            Expanded(
                child: Text(
                    controller.json.notice.isEmpty
                        ? status.message
                        : controller.json.notice,
                    overflow: TextOverflow.ellipsis,
                    style: const TextStyle(fontSize: 11))),
            Text(controller.json.document.dirty ? '•' : '',
                style: TextStyle(color: tokens.textSecondary)),
          ]),
        ),
      ),
    );
  }

  Widget _output(tokens) {
    return Material(
      color: tokens.surface,
      child: ConstrainedBox(
        constraints: const BoxConstraints(maxHeight: 180),
        child: Column(children: [
          ListTile(
            dense: true,
            title: Text(controller.json.outputTitle),
            trailing: IconButton(
                icon: const Icon(Icons.close, size: 16),
                onPressed: () {
                  controller.json.outputBody = '';
                  controller.refresh();
                }),
          ),
          Expanded(
              child: SingleChildScrollView(
                  padding: const EdgeInsets.all(12),
                  child: SelectableText(controller.json.outputBody,
                      style: const TextStyle(
                          fontFamily: 'monospace', fontSize: 12)))),
        ]),
      ),
    );
  }

  Widget _pathPicker(tokens) {
    List<JsonPathEntry> entries = const [];
    try {
      entries = controller.jsonPath.list(controller.json.document.text);
    } catch (_) {}
    return Material(
      color: tokens.surface,
      child: SizedBox(
        height: 220,
        child: ListView(
          children: [
            for (final entry in entries)
              ListTile(
                dense: true,
                contentPadding:
                    EdgeInsets.only(left: 12.0 + entry.depth * 12, right: 12),
                title: Text(entry.label,
                    style:
                        const TextStyle(fontFamily: 'monospace', fontSize: 12)),
                subtitle:
                    Text(entry.path, style: const TextStyle(fontSize: 10)),
                onTap: () => controller.applyJsonPath(entry.path),
              ),
          ],
        ),
      ),
    );
  }

  Widget _history(tokens) {
    final items = controller.histories.where((item) => item.toolId == 'json');
    return Material(
      child: SizedBox(
        height: 160,
        child: ListView(
          children: [
            for (final item in items)
              ListTile(
                dense: true,
                title: Text(item.title),
                subtitle: Text(item.at.toLocal().toString()),
                onTap: () => controller.restoreHistory(item),
              ),
          ],
        ),
      ),
    );
  }

  Future<void> _import() async {
    final file = await openFile(acceptedTypeGroups: [
      const XTypeGroup(label: 'JSON', extensions: ['json', 'txt'])
    ]);
    if (file == null) return;
    controller.setJsonText(await file.readAsString());
  }

  Future<void> _export() async {
    final path = await getSaveLocation(suggestedName: 'mootool.json');
    if (path == null) return;
    final file = XFile.fromData(
        Uint8List.fromList(utf8.encode(controller.json.document.text)),
        mimeType: 'application/json',
        name: 'mootool.json');
    await file.saveTo(path.path);
  }

  void _promptConvert(String title, String Function(String input) convert) {
    controller.json.outputTitle = title;
    controller.json.outputBody = '';
    controller.convertJson(title, convert,
        source: controller.json.conversionInput.isEmpty
            ? controller.json.document.text
            : controller.json.conversionInput);
  }
}

class _FindIntent extends Intent {
  const _FindIntent();
}

class _SaveIntent extends Intent {
  const _SaveIntent();
}

class _FormatIntent extends Intent {
  const _FormatIntent();
}
