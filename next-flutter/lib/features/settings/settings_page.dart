import 'package:flutter/material.dart';

import '../../app/app_controller.dart';
import '../../app/settings.dart';
import '../../app/product.dart';
import '../../design/theme.dart';

class SettingsPage extends StatelessWidget {
  const SettingsPage(
      {super.key, required this.controller, required this.onBack});

  final AppController controller;
  final VoidCallback onBack;

  @override
  Widget build(BuildContext context) {
    final tokens = tokensOf(context);
    final category = controller.settingsCategory ?? 'general';
    final items = [
      (
        'general',
        controller.t('settings.category.general'),
        Icons.settings_outlined
      ),
      (
        'appearance',
        controller.t('settings.category.appearance'),
        Icons.wb_sunny_outlined
      ),
      (
        'layout',
        controller.t('settings.category.layout'),
        Icons.view_sidebar_outlined
      ),
      ('editor', controller.t('settings.category.editor'), Icons.code),
      ('about', controller.t('settings.category.about'), Icons.info_outline),
    ];
    return ColoredBox(
      color: tokens.workspace,
      child: Row(
        children: [
          SizedBox(
            width: 220,
            child: ColoredBox(
              color: tokens.sidebar,
              child: ListView(
                children: [
                  ListTile(
                      leading: const Icon(Icons.arrow_back, size: 18),
                      title: Text(controller.t('common.close')),
                      onTap: onBack),
                  for (final item in items)
                    ListTile(
                      selected: category == item.$1,
                      leading: Icon(item.$3, size: 16),
                      title: Text(item.$2),
                      onTap: () => controller.openSettings(item.$1),
                    ),
                ],
              ),
            ),
          ),
          Expanded(
            child: Padding(
              padding: const EdgeInsets.all(24),
              child: switch (category) {
                'appearance' => _appearance(),
                'layout' => _layout(),
                'about' => _about(tokens),
                _ => _general(),
              },
            ),
          ),
        ],
      ),
    );
  }

  Widget _general() {
    return Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
      Text(controller.t('settings.language'),
          style: const TextStyle(fontWeight: FontWeight.w600)),
      const SizedBox(height: 8),
      Wrap(spacing: 8, children: [
        for (final language in AppLanguage.values)
          ChoiceChip(
            label: Text(switch (language) {
              AppLanguage.zhCN => '中',
              AppLanguage.enUS => 'EN',
              AppLanguage.jaJP => '日'
            }),
            selected: controller.settings.language == language,
            onSelected: (_) => controller.setLanguage(language),
          ),
      ]),
    ]);
  }

  Widget _appearance() {
    return Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
      Text(controller.t('settings.theme'),
          style: const TextStyle(fontWeight: FontWeight.w600)),
      const SizedBox(height: 8),
      Wrap(spacing: 8, children: [
        for (final theme in ThemePreference.values)
          ChoiceChip(
            label: Text(controller.t('settings.theme.${theme.name}')),
            selected: controller.settings.theme == theme,
            onSelected: (_) => controller.setTheme(theme),
          ),
      ]),
    ]);
  }

  Widget _layout() {
    return Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
      Text(controller.t('settings.navigation'),
          style: const TextStyle(fontWeight: FontWeight.w600)),
      const SizedBox(height: 8),
      Wrap(spacing: 8, children: [
        for (final style in NavigationStyle.values)
          ChoiceChip(
            label: Text(controller.t('settings.navigation.${style.name}')),
            selected: controller.settings.navigationStyle == style,
            onSelected: (_) => controller.setNavigationStyle(style),
          ),
      ]),
      SwitchListTile(
        title: Text(controller.t('settings.showRecent')),
        value: controller.settings.showRecent,
        onChanged: (value) {
          controller.settings.showRecent = value;
          controller.scheduleSave();
          controller.refresh();
        },
      ),
      SwitchListTile(
        title: Text(controller.t('settings.hideTitles')),
        value: controller.settings.hideNavigationTitles,
        onChanged: (value) {
          controller.settings.hideNavigationTitles = value;
          controller.scheduleSave();
          controller.refresh();
        },
      ),
    ]);
  }

  Widget _about(tokens) {
    return Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
      Image.asset('assets/brand/mootool-logo.png', width: 52, height: 52),
      const SizedBox(height: 12),
      Text(Product.displayName,
          style: const TextStyle(fontSize: 20, fontWeight: FontWeight.w600)),
      Text('v${Product.version}+${Product.buildNumber}'),
      const SizedBox(height: 12),
      Text(controller.t('settings.about.independent')),
      Text(Product.applicationId,
          style: TextStyle(color: tokens.textSecondary, fontSize: 12)),
    ]);
  }
}
