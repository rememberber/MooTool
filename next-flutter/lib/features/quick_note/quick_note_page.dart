import 'dart:convert';
import 'dart:io';

import 'package:file_selector/file_selector.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import '../../app/app_controller.dart';
import '../../core/editor/find_replace.dart';
import '../../core/storage/document_vault.dart';
import '../../design/theme.dart';
import '../../design/widgets.dart';
import 'markdown_preview.dart';
import 'note_frontmatter.dart';
import 'quick_replace.dart';

class QuickNotePage extends StatelessWidget {
  const QuickNotePage({super.key, required this.controller});
  final AppController controller;

  @override
  Widget build(BuildContext context) {
    final width = MediaQuery.sizeOf(context).width;
    final compact = width < 1080;
    final session = controller.note;
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
        LogicalKeySet(LogicalKeyboardKey.meta, LogicalKeyboardKey.keyZ):
            const UndoIntent(),
        LogicalKeySet(LogicalKeyboardKey.control, LogicalKeyboardKey.keyZ):
            const UndoIntent(),
      },
      child: Actions(
        actions: {
          _FindIntent: CallbackAction<_FindIntent>(onInvoke: (_) {
            session.findOpen = !session.findOpen;
            controller.refresh();
            return null;
          }),
          _SaveIntent: CallbackAction<_SaveIntent>(onInvoke: (_) {
            controller.saveNoteDocument();
            return null;
          }),
        },
        child: Column(
          children: [
            _toolbar(tokens, compact),
            if (session.findOpen) _findBar(tokens),
            Expanded(
              child: Row(
                children: [
                  if (session.treeOpen &&
                      (!compact || session.viewMode != 'preview'))
                    SizedBox(
                      width: session.vaultWidth.clamp(180.0, 320.0).toDouble(),
                      child: _vault(tokens),
                    ),
                  Expanded(child: _center(tokens, compact)),
                  if (session.quickReplaceOpen && !compact)
                    SizedBox(
                      width:
                          session.replaceWidth.clamp(180.0, 280.0).toDouble(),
                      child: _replacePane(tokens),
                    ),
                ],
              ),
            ),
            _status(tokens),
          ],
        ),
      ),
    );
  }

  Widget _toolbar(MooTokens tokens, bool compact) {
    final session = controller.note;
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
                label: controller.t('note.save'),
                icon: Icons.save_outlined,
                primary: true,
                onPressed: controller.saveNoteDocument),
            CompactButton(
                label: controller.t('note.mode.editor'),
                primary: session.viewMode == 'editor',
                onPressed: () => _setMode('editor')),
            CompactButton(
                label: controller.t('note.mode.split'),
                primary: session.viewMode == 'split',
                onPressed: () => _setMode('split')),
            CompactButton(
                label: controller.t('note.mode.preview'),
                primary: session.viewMode == 'preview',
                onPressed: () => _setMode('preview')),
            CompactButton(
                label: session.wrap
                    ? controller.t('json.action.wrap')
                    : controller.t('json.action.nowrap'),
                onPressed: () {
                  session.wrap = !session.wrap;
                  session.metadata.lineWrap = session.wrap;
                  controller.scheduleSave();
                  controller.refresh();
                }),
            CompactButton(
                label: controller.t('json.action.find'),
                icon: Icons.search,
                onPressed: () {
                  session.findOpen = !session.findOpen;
                  controller.refresh();
                }),
            CompactButton(
                label: controller.t('json.action.copy'),
                onPressed: () => controller.copyNote()),
            CompactButton(
                label: controller.t('json.action.import'),
                onPressed: () => _importMarkdown()),
            CompactButton(
                label: controller.t('json.action.export'),
                onPressed: () => _exportMarkdown()),
            CompactButton(
                label: controller.t('note.image'),
                onPressed: () => _importImage()),
            CompactButton(
                label: controller.t('note.clipboardImage'),
                onPressed: controller.pasteNoteImageFromClipboard),
            CompactButton(
                label: controller.t('note.list'),
                onPressed: () => controller.wrapNoteLines('- ')),
            CompactButton(
                label: controller.t('note.column'),
                onPressed: controller.markNoteColumn),
            CompactButton(
                label: controller.t('note.columnDelete'),
                onPressed: controller.deleteNoteColumn),
            SizedBox(
              width: 88,
              child: TextField(
                decoration: InputDecoration(
                    isDense: true, hintText: controller.t('note.columnInsert')),
                onSubmitted: (value) {
                  if (value.isNotEmpty) controller.insertNoteColumn(value);
                },
              ),
            ),
            CompactButton(
                label: controller.t('note.tree'),
                onPressed: () {
                  session.treeOpen = !session.treeOpen;
                  controller.scheduleSave();
                  controller.refresh();
                }),
            CompactButton(
                label: controller.t('note.replacePane'),
                onPressed: () {
                  session.quickReplaceOpen = !session.quickReplaceOpen;
                  controller.scheduleSave();
                  controller.refresh();
                }),
            if (compact)
              Text(controller.t('note.replace.hint'),
                  style: TextStyle(fontSize: 11, color: tokens.textSecondary)),
          ],
        ),
      ),
    );
  }

  void _setMode(String mode) {
    controller.note.viewMode = mode;
    controller.scheduleSave();
    controller.refresh();
  }

  Widget _findBar(MooTokens tokens) {
    final session = controller.note;
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
                    if (match != null) {
                      session.document
                          .restoreView(start: match.start, end: match.end);
                    }
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
                    if (result.replaced) {
                      controller.setNoteText(result.content,
                          selectionStart: result.match?.start,
                          selectionEnd: result.match?.end);
                    }
                  }),
              CompactButton(
                  label: controller.t('findReplace.replaceAll'),
                  onPressed: () {
                    final result = replaceAllMatches(
                        session.document.text,
                        session.findQuery,
                        session.replaceText,
                        session.findOptions);
                    controller.setNoteText(result.content);
                  }),
            ]),
      ),
    );
  }

  Widget _center(MooTokens tokens, bool compact) {
    final session = controller.note;
    final editor = MooCodeEditor(
      document: session.document,
      wrap: session.wrap,
      fontSize: session.metadata.fontSize,
      fontFamily:
          session.metadata.fontName.isEmpty ? null : session.metadata.fontName,
      onChanged: (value) {
        controller.scheduleSave();
        controller.refresh();
      },
    );
    final preview = MarkdownPreview(
      source: session.document.text,
      noteId: session.documentId,
      attachments: controller.attachments,
    );
    if (session.viewMode == 'preview') return preview;
    if (session.viewMode == 'split' && !compact) {
      return Row(children: [
        Expanded(child: editor),
        VerticalDivider(width: 1, color: tokens.border),
        Expanded(child: preview),
      ]);
    }
    return editor;
  }

  Widget _vault(MooTokens tokens) {
    final prefs = controller.noteVaultPrefs;
    final nodes = controller.vault.tree('quickNote',
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
                  isDense: true, hintText: controller.t('note.vault.search')),
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
                label: controller.t('note.vault.new'),
                onPressed: () => controller.createNoteDocument()),
            CompactButton(
                label: controller.t('json.vault.newFolder'),
                onPressed: () => controller.createNoteFolder()),
            CompactButton(
                label: controller.t('json.vault.save'),
                onPressed: controller.saveNoteDocument),
          ]),
          Expanded(
            child: nodes.isEmpty
                ? Center(
                    child: Text(controller.t('note.vault.empty'),
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

  Widget _vaultRow(({VaultNode node, int depth}) row, MooTokens tokens) {
    final selected = controller.noteVaultPrefs.selectedEntryId == row.node.id;
    return InkWell(
      onTap: () {
        if (row.node.isFolder) {
          final expanded = controller.noteVaultPrefs.expanded;
          if (!expanded.add(row.node.id)) expanded.remove(row.node.id);
          controller.refresh();
        } else {
          controller.openNoteDocument(row.node.id);
        }
      },
      onSecondaryTap: () => _vaultMenu(row.node),
      child: Container(
        color: selected ? tokens.accent.withValues(alpha: 0.12) : null,
        padding: EdgeInsets.fromLTRB(8.0 + row.depth * 12, 6, 8, 6),
        child: Row(children: [
          Icon(row.node.isFolder ? Icons.folder_outlined : Icons.edit_note,
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
    controller.duplicateVaultEntry(node.id);
  }

  Widget _replacePane(MooTokens tokens) {
    return ColoredBox(
      color: tokens.surface,
      child: ListView(
        padding: const EdgeInsets.all(8),
        children: [
          Text(controller.t('note.replace.title'),
              style:
                  const TextStyle(fontWeight: FontWeight.w600, fontSize: 12)),
          const SizedBox(height: 6),
          Text(controller.t('note.replace.hint'),
              style: TextStyle(color: tokens.textSecondary, fontSize: 11)),
          const SizedBox(height: 8),
          for (final action in quickReplaceActionIds)
            Padding(
              padding: const EdgeInsets.only(bottom: 4),
              child: CompactButton(
                label: quickReplaceLabels[action] ?? action,
                onPressed: () => controller.applyQuickReplace(action),
              ),
            ),
        ],
      ),
    );
  }

  Widget _status(MooTokens tokens) {
    final session = controller.note;
    final file = session.documentId == null
        ? null
        : controller.vault.documents.cast<VaultDocument?>().firstWhere(
            (item) => item!.id == session.documentId,
            orElse: () => null);
    final path = file == null
        ? controller.t('note.unsaved')
        : controller.vault.pathOf(file.id);
    final column = session.document.column;
    return ColoredBox(
      color: tokens.toolbar,
      child: SizedBox(
        height: 26,
        child: Padding(
          padding: const EdgeInsets.symmetric(horizontal: 10),
          child: Row(children: [
            Expanded(
                child: Text(
                    [
                      path,
                      if (session.document.dirty) '•',
                      session.viewMode,
                      if (column != null)
                        'col ${column.top}-${column.bottom}:${column.left}',
                      if (session.notice.isNotEmpty) session.notice,
                    ].join('  '),
                    overflow: TextOverflow.ellipsis,
                    style: const TextStyle(fontSize: 11))),
          ]),
        ),
      ),
    );
  }

  Future<void> _importMarkdown() async {
    final file = await openFile(acceptedTypeGroups: [
      const XTypeGroup(label: 'Markdown', extensions: ['md', 'txt', 'markdown'])
    ]);
    if (file == null) return;
    controller.importNoteMarkdown(await file.readAsString(),
        name: file.name.isEmpty ? 'imported.md' : file.name);
  }

  Future<void> _exportMarkdown() async {
    final path = await getSaveLocation(suggestedName: 'note.md');
    if (path == null) return;
    final packed = serializeNoteDocument(NoteDocument(
      metadata: controller.note.metadata,
      body: controller.note.document.text,
    ));
    final file = XFile.fromData(Uint8List.fromList(utf8.encode(packed)),
        mimeType: 'text/markdown', name: 'note.md');
    await file.saveTo(path.path);
  }

  Future<void> _importImage() async {
    final file = await openFile(acceptedTypeGroups: [
      const XTypeGroup(
          label: 'Image',
          extensions: ['png', 'jpg', 'jpeg', 'gif', 'webp', 'bmp'])
    ]);
    if (file == null) return;
    await controller.importNoteImage(File(file.path));
  }
}

class _FindIntent extends Intent {
  const _FindIntent();
}

class _SaveIntent extends Intent {
  const _SaveIntent();
}
