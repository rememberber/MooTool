import 'package:flutter/material.dart';

import '../../app/app_controller.dart';
import '../../design/theme.dart';
import '../../design/widgets.dart';
import 'environment_store.dart';

class VariablesToolPage extends StatelessWidget {
  const VariablesToolPage({super.key, required this.controller});
  final AppController controller;

  @override
  Widget build(BuildContext context) {
    final tokens = tokensOf(context);
    final session = controller.localFor('variables');
    if (session.tab.isEmpty) session.tab = 'process';
    return Column(
      children: [
        ColoredBox(
          color: tokens.toolbar,
          child: Padding(
            padding: const EdgeInsets.all(8),
            child: Wrap(
              spacing: 6,
              runSpacing: 6,
              children: [
                for (final tab in ['process', 'runtime', 'user'])
                  CompactButton(
                    label: controller.t('variables.tab.$tab'),
                    primary: session.tab == tab,
                    onPressed: () {
                      session.tab = tab;
                      controller.refresh();
                    },
                  ),
                CompactButton(
                    label: controller.t('variables.refresh'),
                    onPressed: () => controller.refreshEnvironment()),
                CompactButton(
                    label: controller.t('json.action.copy'),
                    onPressed: () {
                      final rows = _visible(
                          session.tab, session.options['query'] ?? '');
                      controller.copyText([
                        for (final entry in rows)
                          '${entry.key}=${entry.value}'
                      ].join('\n'));
                    }),
                if (session.tab == 'user') ...[
                  SizedBox(
                    width: 140,
                    child: TextField(
                      decoration: InputDecoration(
                          isDense: true,
                          hintText: controller.t('variables.key')),
                      controller: TextEditingController(
                          text: session.options['userKey'] ?? ''),
                      onChanged: (value) => session.options['userKey'] = value,
                    ),
                  ),
                  SizedBox(
                    width: 180,
                    child: TextField(
                      decoration: InputDecoration(
                          isDense: true,
                          hintText: controller.t('variables.value')),
                      controller: TextEditingController(
                          text: session.options['userValue'] ?? ''),
                      onChanged: (value) =>
                          session.options['userValue'] = value,
                    ),
                  ),
                  CompactButton(
                      label: controller.t('variables.save'),
                      onPressed: () => controller.saveUserVariable()),
                ],
              ],
            ),
          ),
        ),
        Padding(
          padding: const EdgeInsets.fromLTRB(8, 0, 8, 8),
          child: TextField(
            decoration: InputDecoration(
                isDense: true, hintText: controller.t('variables.search')),
            onChanged: (value) {
              session.options['query'] = value;
              controller.refresh();
            },
          ),
        ),
        if (session.tab == 'user')
          Padding(
            padding: const EdgeInsets.symmetric(horizontal: 8),
            child: Text(controller.t('variables.userHint'),
                style: TextStyle(fontSize: 11, color: tokens.textSecondary)),
          ),
        Expanded(
          child: ListView(
            children: [
              for (final entry in _visible(session.tab, session.options['query'] ?? ''))
                ListTile(
                  dense: true,
                  title: Text(entry.key,
                      style: const TextStyle(fontFamily: 'monospace', fontSize: 12)),
                  subtitle: Text(entry.value,
                      maxLines: 2, overflow: TextOverflow.ellipsis),
                  onTap: session.tab == 'user'
                      ? () => controller.editUserVariable(entry.key, entry.value)
                      : null,
                  trailing: session.tab == 'user'
                      ? IconButton(
                          icon: const Icon(Icons.delete_outline, size: 16),
                          onPressed: () =>
                              controller.deleteUserVariable(entry.key),
                        )
                      : null,
                ),
            ],
          ),
        ),
      ],
    );
  }

  List<EnvironmentEntry> _visible(String tab, String query) {
    final haystack = query.toLowerCase();
    final source = switch (tab) {
      'runtime' => controller.environmentRuntime,
      'user' => controller.environmentUser,
      _ => controller.environmentProcess,
    };
    if (haystack.isEmpty) return source;
    return [
      for (final entry in source)
        if ('${entry.key}\n${entry.value}'.toLowerCase().contains(haystack))
          entry
    ];
  }
}
