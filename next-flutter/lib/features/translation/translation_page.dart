import 'package:flutter/material.dart';

import '../../app/app_controller.dart';
import '../../design/theme.dart';
import '../../design/widgets.dart';
import 'translation_client.dart';

class TranslationToolPage extends StatelessWidget {
  const TranslationToolPage({super.key, required this.controller});
  final AppController controller;

  @override
  Widget build(BuildContext context) {
    final session = controller.translation;
    final tokens = tokensOf(context);
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
                DropdownButton<String>(
                  value: translationLanguageCodes.contains(session.sourceLang)
                      ? session.sourceLang
                      : 'auto',
                  items: [
                    for (final code in translationLanguageCodes)
                      DropdownMenuItem(value: code, child: Text(code))
                  ],
                  onChanged: (value) {
                    if (value == null) return;
                    session.sourceLang = value;
                    controller.refresh();
                  },
                ),
                CompactButton(
                    label: '↔',
                    onPressed: () {
                      if (session.sourceLang == 'auto') return;
                      final source = session.sourceLang;
                      session.sourceLang = session.targetLang;
                      session.targetLang = source;
                      controller.refresh();
                    }),
                DropdownButton<String>(
                  value: session.targetLang == 'auto'
                      ? 'zh-CN'
                      : session.targetLang,
                  items: [
                    for (final code in translationLanguageCodes)
                      if (code != 'auto')
                        DropdownMenuItem(value: code, child: Text(code))
                  ],
                  onChanged: (value) {
                    if (value == null) return;
                    session.targetLang = value;
                    controller.refresh();
                  },
                ),
                CompactButton(
                    label: controller.t('translation.run'),
                    primary: true,
                    onPressed: session.translating
                        ? null
                        : () => controller.translateText()),
                CompactButton(
                    label: controller.t('http.cancel'),
                    onPressed: session.translating
                        ? () => controller.cancelTranslation()
                        : null),
                CompactButton(
                    label: controller.t('json.action.copy'),
                    onPressed: () => controller.copyText(session.target)),
              ],
            ),
          ),
        ),
        Expanded(
          child: Row(
            children: [
              Expanded(
                child: TextField(
                  maxLines: null,
                  expands: true,
                  controller: TextEditingController(text: session.source)
                    ..selection =
                        TextSelection.collapsed(offset: session.source.length),
                  decoration: InputDecoration(
                      hintText: controller.t('translation.source'),
                      border: InputBorder.none,
                      contentPadding: const EdgeInsets.all(12)),
                  onChanged: (value) {
                    session.source = value;
                    controller.scheduleSave();
                  },
                ),
              ),
              VerticalDivider(width: 1, color: tokens.border),
              Expanded(
                child: SelectableText(
                  session.target,
                  style: const TextStyle(fontSize: 14, height: 1.45),
                ),
              ),
            ],
          ),
        ),
        ColoredBox(
          color: tokens.toolbar,
          child: SizedBox(
            height: 26,
            child: Padding(
              padding: const EdgeInsets.symmetric(horizontal: 10),
              child: Text(
                  session.notice.isEmpty
                      ? (session.translating
                          ? controller.t('translation.running')
                          : session.provider)
                      : session.notice,
                  style: const TextStyle(fontSize: 11)),
            ),
          ),
        ),
      ],
    );
  }
}
