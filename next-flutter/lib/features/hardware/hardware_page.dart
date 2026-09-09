import 'package:flutter/material.dart';

import '../../app/app_controller.dart';
import '../../design/theme.dart';
import '../../design/widgets.dart';
import 'system_info.dart';

class HardwareToolPage extends StatelessWidget {
  const HardwareToolPage({super.key, required this.controller});
  final AppController controller;

  @override
  Widget build(BuildContext context) {
    final tokens = tokensOf(context);
    final session = controller.localFor('hardware');
    if (session.tab.isEmpty) session.tab = 'system';
    final snapshot = controller.hardwareSnapshot;
    final groups = snapshot?.sections[session.tab] ?? [];
    return Column(
      children: [
        ColoredBox(
          color: tokens.toolbar,
          child: Padding(
            padding: const EdgeInsets.all(8),
            child: Wrap(
              spacing: 6,
              children: [
                for (final tab in [
                  'system',
                  'cpu',
                  'memory',
                  'storage',
                  'network'
                ])
                  CompactButton(
                    label: controller.t('hardware.tab.$tab'),
                    primary: session.tab == tab,
                    onPressed: () {
                      session.tab = tab;
                      controller.refresh();
                    },
                  ),
                CompactButton(
                    label: controller.t('variables.refresh'),
                    onPressed: () => controller.refreshHardware()),
                CompactButton(
                    label: controller.t('json.action.copy'),
                    onPressed: () => controller.copyText(_plain(groups))),
              ],
            ),
          ),
        ),
        Expanded(
          child: snapshot == null
              ? Center(child: Text(controller.t('hardware.loading')))
              : ListView(
                  padding: const EdgeInsets.all(12),
                  children: [
                    for (final group in groups) ...[
                      Text(group.title,
                          style: const TextStyle(fontWeight: FontWeight.w600)),
                      const SizedBox(height: 6),
                      for (final item in group.items)
                        Padding(
                          padding: const EdgeInsets.only(bottom: 4),
                          child: Text('${item.label}: ${item.value}',
                              style: const TextStyle(
                                  fontFamily: 'monospace', fontSize: 12)),
                        ),
                      const SizedBox(height: 12),
                    ],
                  ],
                ),
        ),
      ],
    );
  }

  String _plain(List<HardwareGroup> groups) => [
        for (final group in groups) ...[
          '========== ${group.title} ==========',
          for (final item in group.items) '${item.label}: ${item.value}',
          '',
        ]
      ].join('\n');
}
