import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import '../../app/app_controller.dart';
import '../../app/settings.dart';
import '../../app/tool_registry.dart';
import '../../design/theme.dart';
import '../../design/widgets.dart';
import '../hardware/hardware_page.dart';
import '../home/home_page.dart';
import '../host/host_page.dart';
import '../http/http_page.dart';
import '../json/json_tool.dart';
import '../local/local_tool_pages.dart';
import '../net/net_page.dart';
import '../quick_note/quick_note_page.dart';
import '../runtime/runtime_page.dart';
import '../settings/settings_page.dart';
import '../tools/pending_tool_page.dart';
import '../translation/translation_page.dart';
import '../variables/variables_page.dart';

class Workbench extends StatelessWidget {
  const Workbench({super.key, required this.controller});
  final AppController controller;

  @override
  Widget build(BuildContext context) {
    final tokens = tokensOf(context);
    return Shortcuts(
      shortcuts: {
        LogicalKeySet(LogicalKeyboardKey.meta, LogicalKeyboardKey.keyK):
            const _SearchIntent(),
        LogicalKeySet(LogicalKeyboardKey.control, LogicalKeyboardKey.keyK):
            const _SearchIntent(),
        LogicalKeySet(LogicalKeyboardKey.meta, LogicalKeyboardKey.comma):
            const _SettingsIntent(),
        LogicalKeySet(LogicalKeyboardKey.control, LogicalKeyboardKey.comma):
            const _SettingsIntent(),
        LogicalKeySet(LogicalKeyboardKey.escape): const _EscapeIntent(),
      },
      child: Actions(
        actions: {
          _SearchIntent: CallbackAction<_SearchIntent>(onInvoke: (_) {
            controller.openSearch();
            return null;
          }),
          _SettingsIntent: CallbackAction<_SettingsIntent>(onInvoke: (_) {
            controller.openSettings();
            return null;
          }),
          _EscapeIntent: CallbackAction<_EscapeIntent>(onInvoke: (_) {
            controller.closeOverlays();
            return null;
          }),
        },
        child: Column(
          children: [
            Expanded(
              child: Row(
                children: [
                  _sidebar(tokens),
                  Expanded(child: _workspace(tokens)),
                ],
              ),
            ),
            if (controller.toast.isNotEmpty)
              Material(
                color: tokens.toolbar,
                child: ListTile(
                  dense: true,
                  title: Text(controller.toast),
                  trailing: IconButton(
                      icon: const Icon(Icons.close, size: 16),
                      onPressed: () {
                        controller.toast = '';
                        controller.refresh();
                      }),
                ),
              ),
          ],
        ),
      ),
    );
  }

  Widget _sidebar(tokens) {
    final collapsed = controller.sidebarCollapsed;
    final width = collapsed
        ? 84.0
        : controller.settings.sidebarWidth.clamp(220.0, 280.0).toDouble();
    final hidden = controller.settings.hiddenNavigationToolIds.toSet();
    return SizedBox(
      width: width,
      child: ColoredBox(
        color: tokens.sidebar,
        child: Column(
          children: [
            Padding(
              padding: const EdgeInsets.fromLTRB(8, 10, 8, 8),
              child: Row(children: [
                if (!collapsed)
                  Expanded(
                      child: Text(controller.t('app.nav.tools'),
                          style: const TextStyle(fontWeight: FontWeight.w600))),
                IconButton(
                  tooltip: controller
                      .t(collapsed ? 'app.nav.expand' : 'app.nav.collapse'),
                  icon: Icon(
                      collapsed ? Icons.chevron_right : Icons.chevron_left,
                      size: 18),
                  onPressed: controller.toggleSidebar,
                ),
              ]),
            ),
            Padding(
              padding: const EdgeInsets.symmetric(horizontal: 8),
              child: CompactButton(
                label: collapsed ? '' : controller.t('app.nav.search'),
                icon: Icons.search,
                tooltip: '${controller.t('app.nav.search')} ⌘K',
                onPressed: controller.openSearch,
              ),
            ),
            const SizedBox(height: 8),
            Expanded(
              child: ListView(
                children: [
                  for (final group in controller.settings.customGroups)
                    if (group.toolIds.isNotEmpty) ...[
                      if (!collapsed &&
                          controller.settings.navigationStyle ==
                              NavigationStyle.grouped)
                        Padding(
                            padding: const EdgeInsets.fromLTRB(12, 8, 8, 4),
                            child: Text(group.name,
                                style: TextStyle(
                                    fontSize: 11,
                                    color: tokens.textSecondary))),
                      for (final id in group.toolIds)
                        if (toolById.containsKey(id) && !hidden.contains(id))
                          _navTile(toolById[id]!, tokens, collapsed),
                    ],
                  for (final group in toolGroups) ...[
                    if (!collapsed &&
                        controller.settings.navigationStyle ==
                            NavigationStyle.grouped)
                      Padding(
                          padding: const EdgeInsets.fromLTRB(12, 10, 8, 4),
                          child: Text(controller.t(group.titleKey),
                              style: TextStyle(
                                  fontSize: 11, color: tokens.textSecondary))),
                    for (final id in group.toolIds)
                      if (!hidden.contains(id))
                        _navTile(toolById[id]!, tokens, collapsed),
                  ],
                  if (controller.settings.showRecent &&
                      controller.recentToolIds.isNotEmpty) ...[
                    if (!collapsed)
                      Padding(
                          padding: const EdgeInsets.fromLTRB(12, 10, 8, 4),
                          child: Text(controller.t('app.nav.recent'),
                              style: TextStyle(
                                  fontSize: 11, color: tokens.textSecondary))),
                    for (final id in controller.recentToolIds)
                      if (toolById.containsKey(id))
                        _navTile(toolById[id]!, tokens, collapsed),
                  ],
                ],
              ),
            ),
            ListTile(
              dense: true,
              leading: Image.asset('assets/brand/mootool-logo.png',
                  width: 20, height: 20),
              title: collapsed
                  ? null
                  : const Text('MooTool', style: TextStyle(fontSize: 12)),
              trailing: collapsed
                  ? null
                  : Row(mainAxisSize: MainAxisSize.min, children: [
                      _langButton('中', AppLanguage.zhCN),
                      _langButton('EN', AppLanguage.enUS),
                      _langButton('日', AppLanguage.jaJP),
                      IconButton(
                          tooltip: controller.t('app.nav.settings'),
                          icon: const Icon(Icons.settings_outlined, size: 16),
                          onPressed: controller.openSettings),
                    ]),
            ),
          ],
        ),
      ),
    );
  }

  Widget _langButton(String label, AppLanguage language) {
    final selected = controller.settings.language == language;
    return TextButton(
      onPressed: () => controller.setLanguage(language),
      style: TextButton.styleFrom(
          minimumSize: const Size(28, 28),
          padding: EdgeInsets.zero,
          foregroundColor: selected ? null : Colors.grey),
      child: Text(label, style: const TextStyle(fontSize: 11)),
    );
  }

  Widget _navTile(ToolDefinition tool, tokens, bool collapsed) {
    final selected =
        controller.activeToolId == tool.id && !controller.settingsOpen;
    final detached = controller.detachedToolIds.contains(tool.id);
    return ListTile(
      selected: selected,
      dense: true,
      minLeadingWidth: 20,
      leading: Icon(_iconFor(tool.icon), size: 16),
      title: collapsed || controller.settings.hideNavigationTitles
          ? null
          : Text(controller.t(tool.titleKey), overflow: TextOverflow.ellipsis),
      trailing: detached ? const Icon(Icons.open_in_new, size: 12) : null,
      onTap: () => controller.openTool(tool.id),
      onLongPress: tool.id == 'mootool'
          ? null
          : () {
              if (detached) {
                controller.dockTool(tool.id);
              } else {
                controller.detachTool(tool.id);
              }
            },
    );
  }

  Widget _workspace(tokens) {
    if (controller.storeError != null) {
      return Center(
          child: Padding(
              padding: const EdgeInsets.all(24),
              child: Text(controller.storeError!)));
    }
    if (controller.searchOpen) return _search(tokens);
    if (controller.settingsOpen)
      return SettingsPage(
          controller: controller, onBack: controller.closeOverlays);
    if (controller.groupManagerOpen) return _groups(tokens);
    final tool = toolById[controller.activeToolId] ?? toolById['mootool']!;
    if (controller.detachedToolIds.contains(tool.id) && tool.id != 'mootool') {
      return Center(
        child: Column(mainAxisSize: MainAxisSize.min, children: [
          Text(controller.t('tool.detached')),
          const SizedBox(height: 12),
          CompactButton(
              label: controller.t('tool.dock'),
              onPressed: () => controller.dockTool(tool.id)),
        ]),
      );
    }
    return switch (tool.id) {
      'mootool' => HomePage(l10n: controller.l10n),
      'quickNote' => QuickNotePage(controller: controller),
      'json' => JsonToolPage(controller: controller),
      'encode' => EncodeToolPage(controller: controller),
      'calculator' => CalculatorToolPage(controller: controller),
      'timeConvert' => TimeConvertToolPage(controller: controller),
      'regex' => RegexToolPage(controller: controller),
      'cron' => CronToolPage(controller: controller),
      'uaParse' => UaParseToolPage(controller: controller),
      'ymlProperties' => ConfigConvertToolPage(controller: controller),
      'crypto' => CryptoToolPage(controller: controller),
      'colorBoard' => ColorBoardToolPage(controller: controller),
      'textDiff' => TextDiffToolPage(controller: controller),
      'qrCode' => QrCodeToolPage(controller: controller),
      'reformat' => ReformatToolPage(controller: controller),
      'protobuf' => ProtobufToolPage(controller: controller),
      'http' => HttpToolPage(controller: controller),
      'host' => HostToolPage(controller: controller),
      'net' => NetToolPage(controller: controller),
      'java' => RuntimeToolPage(controller: controller),
      'variables' => VariablesToolPage(controller: controller),
      'translation' => TranslationToolPage(controller: controller),
      'hardware' => HardwareToolPage(controller: controller),
      _ => PendingToolPage(controller: controller, toolId: tool.id),
    };
  }

  Widget _search(tokens) {
    final hits = searchTools(controller.searchQuery);
    return ColoredBox(
      color: tokens.workspace,
      child: Padding(
        padding: const EdgeInsets.all(24),
        child: Column(
          children: [
            TextField(
              autofocus: true,
              decoration: InputDecoration(
                  prefixIcon: const Icon(Icons.search),
                  hintText: controller.t('app.search.placeholder')),
              onChanged: (value) {
                controller.searchQuery = value;
                controller.refresh();
              },
              onSubmitted: (value) {
                final first = searchTools(value);
                if (first.isNotEmpty) controller.openTool(first.first.id);
              },
            ),
            const SizedBox(height: 12),
            Expanded(
              child: hits.isEmpty
                  ? Center(child: Text(controller.t('app.search.empty')))
                  : ListView(
                      children: [
                        for (final tool in hits)
                          ListTile(
                            leading: Icon(_iconFor(tool.icon)),
                            title: Text(controller.t(tool.titleKey)),
                            subtitle: Text(tool.id),
                            onTap: () => controller.openTool(tool.id),
                          ),
                      ],
                    ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _groups(tokens) {
    return ColoredBox(
      color: tokens.workspace,
      child: Padding(
        padding: const EdgeInsets.all(24),
        child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
          Text(controller.t('groups.title'),
              style:
                  const TextStyle(fontSize: 18, fontWeight: FontWeight.w600)),
          CompactButton(
              label: controller.t('groups.new'),
              onPressed: controller.addCustomGroup),
          if (controller.settings.customGroups.isEmpty)
            Text(controller.t('groups.empty')),
          for (final group in controller.settings.customGroups)
            ListTile(title: Text(group.name)),
        ]),
      ),
    );
  }
}

IconData _iconFor(String id) => switch (id) {
      'home' => Icons.home_outlined,
      'note' => Icons.edit_note,
      'diff' => Icons.difference_outlined,
      'format' => Icons.format_paint_outlined,
      'json' => Icons.data_object,
      'runtime' => Icons.terminal,
      'config' => Icons.settings_applications_outlined,
      'proto' => Icons.account_tree_outlined,
      'env' => Icons.functions,
      'http' => Icons.language,
      'host' => Icons.dns_outlined,
      'net' => Icons.hub_outlined,
      'ua' => Icons.devices,
      'encode' => Icons.swap_horiz,
      'crypto' => Icons.lock_outline,
      'regex' => Icons.manage_search,
      'cron' => Icons.schedule,
      'qr' => Icons.qr_code,
      'time' => Icons.access_time,
      'board' => Icons.campaign_outlined,
      'translate' => Icons.translate,
      'calc' => Icons.calculate_outlined,
      'color' => Icons.palette_outlined,
      'image' => Icons.image_outlined,
      'pdf' => Icons.picture_as_pdf_outlined,
      'cpu' => Icons.memory,
      _ => Icons.extension_outlined,
    };

class _SearchIntent extends Intent {
  const _SearchIntent();
}

class _SettingsIntent extends Intent {
  const _SettingsIntent();
}

class _EscapeIntent extends Intent {
  const _EscapeIntent();
}
