import 'dart:io';

import 'package:file_selector/file_selector.dart';
import 'package:flutter/material.dart';

import '../../app/app_controller.dart';
import '../../design/theme.dart';
import '../../design/widgets.dart';
import 'pdf_session.dart';

class PdfToolPage extends StatelessWidget {
  const PdfToolPage({super.key, required this.controller});
  final AppController controller;

  @override
  Widget build(BuildContext context) {
    final session = controller.pdf;
    final tokens = tokensOf(context);
    final rows = session.tab == 'split' ? session.splitRows : session.mergeRows;
    return Column(
      children: [
        ColoredBox(
          color: tokens.toolbar,
          child: Padding(
            padding: const EdgeInsets.all(8),
            child: Wrap(
              spacing: 6,
              runSpacing: 6,
              crossAxisAlignment: WrapCrossAlignment.center,
              children: [
                CompactButton(
                    label: controller.t('pdf.tab.split'),
                    primary: session.tab == 'split',
                    onPressed: () {
                      session.tab = 'split';
                      controller.scheduleSave();
                      controller.refresh();
                    }),
                CompactButton(
                    label: controller.t('pdf.tab.merge'),
                    primary: session.tab == 'merge',
                    onPressed: () {
                      session.tab = 'merge';
                      controller.scheduleSave();
                      controller.refresh();
                    }),
                CompactButton(
                    label: session.tab == 'split'
                        ? controller.t('pdf.addTask')
                        : controller.t('pdf.addFile'),
                    onPressed: session.busy
                        ? null
                        : () async {
                            final files = await openFiles(acceptedTypeGroups: [
                              const XTypeGroup(
                                  label: 'PDF', extensions: ['pdf'])
                            ]);
                            await controller.addPdfFiles(
                                [for (final file in files) File(file.path)]);
                          }),
                CompactButton(
                    label: session.tab == 'split'
                        ? controller.t('pdf.startSplit')
                        : controller.t('pdf.startMerge'),
                    primary: true,
                    onPressed: session.busy
                        ? null
                        : () => session.tab == 'split'
                            ? controller.splitSelectedPdfs()
                            : controller.mergeSelectedPdfs()),
                Text(controller.t('pdf.simpleHint'),
                    style: TextStyle(fontSize: 11, color: tokens.textSecondary)),
              ],
            ),
          ),
        ),
        if (session.notice.isNotEmpty)
          Padding(
            padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
            child: Align(
                alignment: Alignment.centerLeft,
                child: Text(session.notice,
                    style: const TextStyle(color: Color(0xFFB42318)))),
          ),
        Expanded(
          child: rows.isEmpty
              ? Center(child: Text(controller.t('pdf.empty')))
              : ListView.builder(
                  itemCount: rows.length,
                  itemBuilder: (context, index) =>
                      _row(tokens, session, rows[index]),
                ),
        ),
        ColoredBox(
          color: tokens.toolbar,
          child: Padding(
            padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 6),
            child: Align(
              alignment: Alignment.centerLeft,
              child: Text(
                session.lastOutputs.isEmpty
                    ? '${controller.t('pdf.output')} —'
                    : '${controller.t('pdf.output')} ${session.lastOutputs.join(' · ')}',
                maxLines: 2,
                overflow: TextOverflow.ellipsis,
                style: const TextStyle(fontSize: 11),
              ),
            ),
          ),
        ),
      ],
    );
  }

  Widget _row(tokens, PdfSession session, PdfTaskRow row) {
    return ListTile(
      dense: true,
      leading: Checkbox(
        value: row.selected,
        onChanged: (value) {
          row.selected = value ?? false;
          controller.refresh();
        },
      ),
      title: Text(row.name),
      subtitle: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text('${row.pageCount} ${controller.t('pdf.pages')} · ${_bytes(row.size)}'),
          Wrap(
            spacing: 8,
            runSpacing: 4,
            crossAxisAlignment: WrapCrossAlignment.center,
            children: [
              SizedBox(
                width: 140,
                child: TextField(
                  controller: TextEditingController(text: row.pageRange)
                    ..selection = TextSelection.collapsed(
                        offset: row.pageRange.length),
                  decoration: InputDecoration(
                      isDense: true,
                      labelText: session.tab == 'split'
                          ? controller.t('pdf.pageRange')
                          : controller.t('pdf.mergeRange')),
                  onChanged: (value) => row.pageRange = value,
                ),
              ),
              if (session.tab == 'split') ...[
                DropdownButton<String>(
                  value: row.rule,
                  items: [
                    for (final rule in ['odd', 'even', 'custom'])
                      DropdownMenuItem(
                          value: rule,
                          child: Text(controller.t('pdf.rule.$rule'))),
                  ],
                  onChanged: (value) {
                    if (value == null) return;
                    row.rule = value;
                    controller.refresh();
                  },
                ),
                if (row.rule == 'custom')
                  SizedBox(
                    width: 120,
                    child: TextField(
                      controller: TextEditingController(text: row.customRule),
                      decoration: InputDecoration(
                          isDense: true,
                          labelText: controller.t('pdf.customRule')),
                      onChanged: (value) => row.customRule = value,
                    ),
                  ),
              ],
              Text(controller.t('pdf.status.${row.status}')),
            ],
          ),
        ],
      ),
      trailing: IconButton(
        icon: const Icon(Icons.delete_outline, size: 16),
        onPressed: () {
          session.splitRows.removeWhere((item) => item.path == row.path);
          session.mergeRows.removeWhere((item) => item.path == row.path);
          controller.scheduleSave();
          controller.refresh();
        },
      ),
    );
  }

  String _bytes(int bytes) {
    if (bytes < 1024) return '$bytes B';
    if (bytes < 1024 * 1024) return '${(bytes / 1024).toStringAsFixed(1)} KB';
    return '${(bytes / (1024 * 1024)).toStringAsFixed(1)} MB';
  }
}
