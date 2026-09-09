import 'dart:io';

import 'package:file_selector/file_selector.dart';
import 'package:flutter/material.dart';
import 'package:url_launcher/url_launcher.dart';

import '../../app/app_controller.dart';
import '../../app/product.dart';
import '../../app/settings.dart';
import '../../core/update/update_models.dart';
import '../../design/theme.dart';
import '../../design/widgets.dart';

class SettingsPage extends StatelessWidget {
  const SettingsPage(
      {super.key, required this.controller, required this.onBack});

  final AppController controller;
  final VoidCallback onBack;

  static const categories = [
    ('general', 'settings.category.general', Icons.settings_outlined),
    ('appearance', 'settings.category.appearance', Icons.wb_sunny_outlined),
    ('layout', 'settings.category.layout', Icons.view_sidebar_outlined),
    ('editor', 'settings.category.editor', Icons.code),
    ('network', 'settings.category.network', Icons.wifi),
    ('data', 'settings.category.data', Icons.folder_outlined),
    ('vault', 'settings.category.vault', Icons.lock_outline),
    ('runtime', 'settings.category.runtime', Icons.terminal),
    ('tools', 'settings.category.tools', Icons.build_outlined),
    ('shortcuts', 'settings.category.shortcuts', Icons.keyboard_outlined),
    ('about', 'settings.category.about', Icons.info_outline),
  ];

  @override
  Widget build(BuildContext context) {
    final tokens = tokensOf(context);
    final category = controller.settingsCategory ?? 'general';
    return Material(
      color: tokens.workspace,
      child: Row(
        children: [
          SizedBox(
            width: 220,
            child: Material(
              color: tokens.sidebar,
              child: ListView(
                children: [
                  ListTile(
                      leading: const Icon(Icons.arrow_back, size: 18),
                      title: Text(controller.t('common.close')),
                      onTap: onBack),
                  for (final item in categories)
                    ListTile(
                      selected: category == item.$1,
                      leading: Icon(item.$3, size: 16),
                      title: Text(controller.t(item.$2)),
                      onTap: () => controller.openSettings(item.$1),
                    ),
                ],
              ),
            ),
          ),
          Expanded(
            child: Padding(
              padding: const EdgeInsets.all(24),
              child: ListView(
                children: [
                  switch (category) {
                    'appearance' => _appearance(),
                    'layout' => _layout(),
                    'editor' => _editor(),
                    'network' => _network(),
                    'data' => _data(),
                    'vault' => _vault(),
                    'runtime' => _runtime(),
                    'tools' => _tools(),
                    'shortcuts' => _shortcuts(),
                    'about' => _about(tokens),
                    _ => _general(),
                  },
                ],
              ),
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
      const SizedBox(height: 16),
      Text(controller.t('settings.closeBehavior')),
      Wrap(spacing: 8, children: [
        for (final behavior in CloseBehavior.values)
          ChoiceChip(
            label: Text(controller.t('settings.close.${behavior.name}')),
            selected: controller.settings.closeBehavior == behavior,
            onSelected: (_) {
              controller.settings.closeBehavior = behavior;
              controller.scheduleSave();
              controller.applyDesktopPolicy();
              controller.refresh();
            },
          ),
      ]),
      SwitchListTile(
        contentPadding: EdgeInsets.zero,
        title: Text(controller.t('settings.autoCheckUpdates')),
        value: controller.settings.autoCheckUpdates,
        onChanged: (value) =>
            _set(() => controller.settings.autoCheckUpdates = value),
      ),
      SwitchListTile(
        contentPadding: EdgeInsets.zero,
        title: Text(controller.t('settings.autoDownloadUpdates')),
        subtitle: Text(controller.t('settings.updateUnsignedHint'),
            style: const TextStyle(fontSize: 12)),
        value: controller.settings.autoDownloadUpdates,
        onChanged: (value) =>
            _set(() => controller.settings.autoDownloadUpdates = value),
      ),
      Wrap(spacing: 8, runSpacing: 8, children: [
        FilledButton(
          onPressed: () => controller.checkForUpdates(),
          child: Text(controller.t('settings.updateCheck')),
        ),
        OutlinedButton(
          onPressed: controller.updateResult?.download == null
              ? null
              : () => controller.downloadUpdate(),
          child: Text(controller.t('settings.updateDownload')),
        ),
        OutlinedButton(
          onPressed: controller.updateLocalPath == null
              ? null
              : () => controller.openDownloadedUpdate(),
          child: Text(controller.t('settings.updateOpen')),
        ),
        TextButton(
          onPressed: () async {
            final url = controller.updateResult?.releaseUrl;
            if (url == null || url.isEmpty) return;
            await launchUrl(Uri.parse(url),
                mode: LaunchMode.externalApplication);
          },
          child: Text(controller.t('settings.updateRelease')),
        ),
      ]),
      if (controller.updateNotice.isNotEmpty) ...[
        const SizedBox(height: 8),
        Text(controller.updateNotice, style: const TextStyle(fontSize: 12)),
      ],
      if (controller.updateDownloadStatus == UpdateDownloadStatus.downloading)
        Padding(
          padding: const EdgeInsets.only(top: 8),
          child: LinearProgressIndicator(
              value: controller.updatePercent <= 0
                  ? null
                  : (controller.updatePercent / 100).clamp(0, 1)),
        ),
      SwitchListTile(
        contentPadding: EdgeInsets.zero,
        title: Text(controller.t('settings.startMaximized')),
        value: controller.settings.startMaximized,
        onChanged: (value) =>
            _set(() => controller.settings.startMaximized = value),
      ),
      SwitchListTile(
        contentPadding: EdgeInsets.zero,
        title: Text(controller.t('settings.trayEnabled')),
        subtitle: Text(
            controller.t(controller.desktopCaps.tray
                ? 'settings.trayReady'
                : 'settings.trayPending'),
            style: const TextStyle(fontSize: 12)),
        value: controller.settings.trayEnabled,
        onChanged: (value) =>
            _set(() => controller.settings.trayEnabled = value),
      ),
      Text(controller.t('settings.hideNeedsTray'),
          style: const TextStyle(fontSize: 12)),
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
      const SizedBox(height: 16),
      Text(controller.t('settings.style')),
      Wrap(spacing: 8, runSpacing: 8, children: [
        for (final style in InterfaceStyle.values)
          ChoiceChip(
            label: Text(controller.t('settings.style.${style.name}')),
            selected: controller.settings.interfaceStyle == style,
            onSelected: (_) =>
                _set(() => controller.settings.interfaceStyle = style),
          ),
      ]),
      const SizedBox(height: 16),
      Text(controller.t('settings.accentColor')),
      const SizedBox(height: 8),
      Wrap(spacing: 8, children: [
        for (final entry in accentColorPresets.entries)
          InkWell(
            onTap: () =>
                _set(() => controller.settings.accentColor = entry.key),
            child: Container(
              width: 28,
              height: 28,
              decoration: BoxDecoration(
                color: Color(
                    int.parse('FF${entry.value.substring(1)}', radix: 16)),
                shape: BoxShape.circle,
                border: Border.all(
                    width:
                        controller.settings.accentColor == entry.key ? 3 : 1),
              ),
            ),
          ),
      ]),
      const SizedBox(height: 16),
      Text(
          '${controller.t('settings.uiFontSize')} ${controller.settings.uiFontSize.round()}'),
      Slider(
        min: 11,
        max: 18,
        divisions: 7,
        value: controller.settings.uiFontSize.clamp(11, 18),
        onChanged: (value) =>
            _set(() => controller.settings.uiFontSize = value),
      ),
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
        contentPadding: EdgeInsets.zero,
        title: Text(controller.t('settings.showRecent')),
        value: controller.settings.showRecent,
        onChanged: (value) =>
            _set(() => controller.settings.showRecent = value),
      ),
      SwitchListTile(
        contentPadding: EdgeInsets.zero,
        title: Text(controller.t('settings.hideTitles')),
        value: controller.settings.hideNavigationTitles,
        onChanged: (value) =>
            _set(() => controller.settings.hideNavigationTitles = value),
      ),
      SwitchListTile(
        contentPadding: EdgeInsets.zero,
        title: Text(controller.t('settings.compactNavigation')),
        value: controller.settings.compactNavigation,
        onChanged: (value) =>
            _set(() => controller.settings.compactNavigation = value),
      ),
      SwitchListTile(
        contentPadding: EdgeInsets.zero,
        title: Text(controller.t('settings.showSidebarDivider')),
        value: controller.settings.showSidebarDivider,
        onChanged: (value) =>
            _set(() => controller.settings.showSidebarDivider = value),
      ),
    ]);
  }

  Widget _editor() {
    return Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
      SwitchListTile(
        contentPadding: EdgeInsets.zero,
        title: Text(controller.t('settings.softWrap')),
        value: controller.settings.softWrap,
        onChanged: (value) => _set(() => controller.settings.softWrap = value),
      ),
      _field('settings.jsonFont', controller.settings.jsonFontName,
          (value) => controller.settings.jsonFontName = value),
      _field('settings.noteFont', controller.settings.noteFontName,
          (value) => controller.settings.noteFontName = value),
      _field('settings.sqlDialect', controller.settings.sqlDialect,
          (value) => controller.settings.sqlDialect = value),
      Text(
          '${controller.t('settings.editorFontSize')} ${controller.settings.editorFontSize.round()}'),
      Slider(
        min: 11,
        max: 22,
        divisions: 11,
        value: controller.settings.editorFontSize.clamp(11, 22),
        onChanged: (value) =>
            _set(() => controller.settings.editorFontSize = value),
      ),
    ]);
  }

  Widget _network() {
    return Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
      SwitchListTile(
        contentPadding: EdgeInsets.zero,
        title: Text(controller.t('settings.proxyEnabled')),
        value: controller.settings.proxyEnabled,
        onChanged: (value) =>
            _set(() => controller.settings.proxyEnabled = value),
      ),
      _field('settings.proxyHost', controller.settings.proxyHost,
          (value) => controller.settings.proxyHost = value),
      _field('settings.proxyPort', controller.settings.proxyPort,
          (value) => controller.settings.proxyPort = value),
      _field('settings.proxyUsername', controller.settings.proxyUsername,
          (value) => controller.settings.proxyUsername = value),
      _field('settings.proxyPassword', controller.settings.proxyPassword,
          (value) => controller.settings.proxyPassword = value,
          obscure: true),
      _field(
          'settings.httpTimeout',
          '${controller.settings.httpTimeoutMs}',
          (value) => controller.settings.httpTimeoutMs =
              int.tryParse(value) ?? controller.settings.httpTimeoutMs),
      _field(
          'settings.translationTimeout',
          '${controller.settings.translationTimeoutMs}',
          (value) => controller.settings.translationTimeoutMs =
              int.tryParse(value) ?? controller.settings.translationTimeoutMs),
    ]);
  }

  Widget _data() {
    return Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
      Text(controller.t('settings.dataPath')),
      SelectableText(controller.paths.dataRoot.path,
          style: const TextStyle(fontFamily: 'monospace', fontSize: 12)),
      const SizedBox(height: 12),
      Wrap(spacing: 8, children: [
        CompactButton(
            label: controller.t('settings.backup'),
            onPressed: () => controller.createBackup()),
        CompactButton(
            label: controller.t('settings.restore'),
            onPressed: () async {
              final directory = await getDirectoryPath();
              if (directory == null) return;
              await controller.restoreBackup(Directory(directory));
            }),
      ]),
      if (controller.lastBackupPath != null)
        Padding(
          padding: const EdgeInsets.only(top: 8),
          child: SelectableText(controller.lastBackupPath!,
              style: const TextStyle(fontSize: 12)),
        ),
      if (controller.dataNotice.isNotEmpty)
        Padding(
          padding: const EdgeInsets.only(top: 8),
          child: Text(controller.dataNotice),
        ),
    ]);
  }

  Widget _vault() {
    return Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
      _field('settings.gitRemote', controller.settings.gitRemote,
          (value) => controller.settings.gitRemote = value),
      _field('settings.gitUser', controller.settings.gitUser,
          (value) => controller.settings.gitUser = value),
      _field('settings.gitToken', controller.settings.gitToken,
          (value) => controller.settings.gitToken = value,
          obscure: true),
      SwitchListTile(
        contentPadding: EdgeInsets.zero,
        title: Text(controller.t('settings.gitAutoCommit')),
        value: controller.settings.gitAutoCommit,
        onChanged: (value) =>
            _set(() => controller.settings.gitAutoCommit = value),
      ),
      SwitchListTile(
        contentPadding: EdgeInsets.zero,
        title: Text(controller.t('settings.gitAutoPull')),
        value: controller.settings.gitAutoPull,
        onChanged: (value) =>
            _set(() => controller.settings.gitAutoPull = value),
      ),
    ]);
  }

  Widget _runtime() {
    return Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
      _field('settings.javaPath', controller.settings.javaPath,
          (value) => controller.settings.javaPath = value),
      _field('settings.groovyPath', controller.settings.groovyPath,
          (value) => controller.settings.groovyPath = value),
      _field('settings.pythonPath', controller.settings.pythonPath,
          (value) => controller.settings.pythonPath = value),
      _field('settings.nodePath', controller.settings.nodePath,
          (value) => controller.settings.nodePath = value),
      CompactButton(
          label: controller.t('runtime.detect'),
          onPressed: () => controller.detectRuntimes()),
    ]);
  }

  Widget _tools() {
    return Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
      _field(
          'settings.qrSize',
          '${controller.settings.qrSize}',
          (value) => controller.settings.qrSize =
              int.tryParse(value) ?? controller.settings.qrSize),
      _field('settings.qrLevel', controller.settings.qrLevel,
          (value) => controller.settings.qrLevel = value),
      _field(
          'settings.randomLength',
          '${controller.settings.randomLength}',
          (value) => controller.settings.randomLength =
              int.tryParse(value) ?? controller.settings.randomLength),
      _field('settings.exportDirectory', controller.settings.exportDirectory,
          (value) => controller.settings.exportDirectory = value),
      _field(
          'settings.defaultTranslator',
          controller.settings.defaultTranslator,
          (value) => controller.settings.defaultTranslator = value),
    ]);
  }

  Widget _shortcuts() {
    return Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
      Text(controller.t('settings.shortcutSearch')),
      const SizedBox(height: 8),
      const Text('⌘/Ctrl + K'),
      const SizedBox(height: 16),
      Text(controller.t('settings.shortcutSettings')),
      const SizedBox(height: 8),
      const Text('⌘/Ctrl + ,'),
      const SizedBox(height: 16),
      Text(controller.t('settings.shortcutHelp'),
          style: const TextStyle(fontSize: 12)),
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
      const SizedBox(height: 12),
      Text(controller.t('settings.updateUnsignedHint')),
      if (controller.updateNotice.isNotEmpty) ...[
        const SizedBox(height: 8),
        Text(controller.updateNotice, style: const TextStyle(fontSize: 12)),
      ],
      const SizedBox(height: 12),
      FilledButton(
        onPressed: () => controller.checkForUpdates(),
        child: Text(controller.t('settings.updateCheck')),
      ),
    ]);
  }

  Widget _field(String labelKey, String value, ValueChanged<String> onChanged,
      {bool obscure = false}) {
    final label =
        labelKey.startsWith('settings.') || labelKey.startsWith('runtime.')
            ? controller.t(labelKey)
            : labelKey;
    return Padding(
      padding: const EdgeInsets.only(bottom: 10),
      child: TextField(
        obscureText: obscure,
        controller: TextEditingController(text: value)
          ..selection = TextSelection.collapsed(offset: value.length),
        decoration: InputDecoration(isDense: true, labelText: label),
        onChanged: (next) {
          onChanged(next);
          controller.scheduleSave();
        },
      ),
    );
  }

  void _set(VoidCallback action) {
    action();
    controller.scheduleSave();
    controller.applyDesktopPolicy();
    controller.refresh();
  }
}
