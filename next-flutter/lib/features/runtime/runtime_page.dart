import 'package:flutter/material.dart';

import '../../app/app_controller.dart';
import '../../design/theme.dart';
import '../../design/widgets.dart';
import 'runtime_tools.dart';

class RuntimeToolPage extends StatelessWidget {
  const RuntimeToolPage({super.key, required this.controller});
  final AppController controller;

  @override
  Widget build(BuildContext context) {
    final session = controller.runtime;
    final tokens = tokensOf(context);
    final runtime = session.runtime;
    RuntimeStatus? status;
    for (final item in session.statuses) {
      if (item.id == runtime) {
        status = item;
        break;
      }
    }
    return Column(
      children: [
        ColoredBox(
          color: tokens.toolbar,
          child: Padding(
            padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 6),
            child: Wrap(
              spacing: 6,
              runSpacing: 6,
              crossAxisAlignment: WrapCrossAlignment.center,
              children: [
                for (final tab in ['java', 'python', 'node'])
                  CompactButton(
                    label:
                        tab == 'java' ? 'Java/Groovy' : runtimeDisplayName(tab),
                    primary: session.tab == tab,
                    onPressed: () {
                      session.tab = tab;
                      controller.refresh();
                    },
                  ),
                if (session.tab == 'java')
                  DropdownButton<String>(
                    value: session.javaMode,
                    items: const [
                      DropdownMenuItem(value: 'java', child: Text('Java')),
                      DropdownMenuItem(value: 'groovy', child: Text('Groovy')),
                    ],
                    onChanged: (value) {
                      if (value == null) return;
                      session.javaMode = value;
                      controller.refresh();
                    },
                  ),
                CompactButton(
                    label: controller.t('runtime.run'),
                    icon: Icons.play_arrow,
                    primary: true,
                    onPressed:
                        session.running ? null : () => controller.runRuntime()),
                CompactButton(
                    label: controller.t('runtime.stop'),
                    icon: Icons.stop,
                    onPressed: session.running
                        ? () => controller.stopRuntime()
                        : null),
                CompactButton(
                    label: controller.t('runtime.format'),
                    onPressed: () {
                      session.code =
                          formatRuntimeSource(session.code, session.runtime);
                      controller.scheduleSave();
                      controller.refresh();
                    }),
                CompactButton(
                    label: controller.t('runtime.detect'),
                    onPressed: () => controller.detectRuntimes()),
                Text(
                    status == null
                        ? controller.t('runtime.detectHint')
                        : status.available
                            ? '${status.command} ${status.version}'
                            : controller.t('runtime.missing',
                                {'name': runtimeDisplayName(runtime)}),
                    style:
                        TextStyle(fontSize: 12, color: tokens.textSecondary)),
              ],
            ),
          ),
        ),
        Padding(
          padding: const EdgeInsets.fromLTRB(8, 0, 8, 8),
          child: Row(
            children: [
              Expanded(
                child: TextField(
                  decoration: InputDecoration(
                      isDense: true,
                      labelText: controller.t('runtime.arguments')),
                  controller: TextEditingController(
                      text: session.arguments[runtime] ?? ''),
                  onChanged: (value) => session.arguments[runtime] = value,
                ),
              ),
              const SizedBox(width: 8),
              Expanded(
                child: TextField(
                  decoration: InputDecoration(
                      isDense: true, labelText: controller.t('runtime.cwd')),
                  controller: TextEditingController(
                      text: session.workingDirectories[runtime] ?? ''),
                  onChanged: (value) =>
                      session.workingDirectories[runtime] = value,
                ),
              ),
            ],
          ),
        ),
        Expanded(
          child: TextField(
            maxLines: null,
            expands: true,
            controller: TextEditingController(text: session.code)
              ..selection =
                  TextSelection.collapsed(offset: session.code.length),
            style: const TextStyle(fontFamily: 'monospace', fontSize: 13),
            decoration: const InputDecoration(
                border: InputBorder.none, contentPadding: EdgeInsets.all(12)),
            onChanged: (value) {
              session.code = value;
              controller.scheduleSave();
            },
          ),
        ),
        ColoredBox(
          color: tokens.surface,
          child: SizedBox(
            height: 180,
            child: Padding(
              padding: const EdgeInsets.all(8),
              child: SelectableText(
                [
                  if (session.result != null)
                    '${session.result!.command}  exit=${session.result!.exitCode}  ${session.result!.durationMs}ms',
                  if (session.stdout.isNotEmpty) session.stdout,
                  if (session.stderr.isNotEmpty) session.stderr,
                  if (session.notice.isNotEmpty) session.notice,
                ].join('\n'),
                style: const TextStyle(fontFamily: 'monospace', fontSize: 12),
              ),
            ),
          ),
        ),
      ],
    );
  }
}
