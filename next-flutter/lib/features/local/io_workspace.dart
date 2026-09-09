import 'package:flutter/material.dart';

import '../../app/app_controller.dart';
import '../../design/theme.dart';
import '../../design/widgets.dart';

class IoWorkspace extends StatelessWidget {
  const IoWorkspace({
    super.key,
    required this.controller,
    required this.toolId,
    required this.leftLabel,
    required this.rightLabel,
    required this.actions,
    this.tabs = const [],
    this.onClear,
    this.headerActions,
  });

  final AppController controller;
  final String toolId;
  final String leftLabel;
  final String rightLabel;
  final List<Widget> actions;
  final List<(String id, String label)> tabs;
  final VoidCallback? onClear;
  final List<Widget>? headerActions;

  @override
  Widget build(BuildContext context) {
    final tokens = tokensOf(context);
    final session = controller.localFor(toolId);
    return Material(
      color: tokens.workspace,
      child: Column(
        children: [
          Padding(
            padding: const EdgeInsets.fromLTRB(12, 8, 12, 8),
            child: Row(
              children: [
                if (tabs.isNotEmpty)
                  Expanded(
                    child: Wrap(
                      spacing: 6,
                      runSpacing: 6,
                      children: [
                        for (final tab in tabs)
                          CompactButton(
                            label: tab.$2,
                            primary: session.tab == tab.$1,
                            onPressed: () {
                              controller.setLocalTab(toolId, tab.$1);
                            },
                          ),
                      ],
                    ),
                  )
                else
                  const Spacer(),
                ...?headerActions,
                CompactButton(
                  label: controller.t('json.action.history'),
                  icon: Icons.history,
                  onPressed: () {
                    session.historyOpen = !session.historyOpen;
                    controller.refresh();
                  },
                ),
                const SizedBox(width: 6),
                CompactButton(
                  label: controller.t('json.action.copy'),
                  icon: Icons.copy,
                  onPressed: () => controller.copyText(session.right),
                ),
                const SizedBox(width: 6),
                CompactButton(
                  label: controller.t('json.action.clear'),
                  onPressed: onClear ??
                      () {
                        session.left = '';
                        session.right = '';
                        session.notice = '';
                        controller.scheduleSave();
                        controller.refresh();
                      },
                ),
              ],
            ),
          ),
          if (session.notice.isNotEmpty)
            Padding(
              padding: const EdgeInsets.symmetric(horizontal: 12),
              child: Align(
                alignment: Alignment.centerLeft,
                child: Text(session.notice,
                    style:
                        TextStyle(color: tokens.textSecondary, fontSize: 12)),
              ),
            ),
          Expanded(
            child: Row(
              children: [
                Expanded(
                    child: IoTextPane(
                        label: leftLabel,
                        value: session.left,
                        fontSize: controller.settings.editorFontSize,
                        onChanged: (value) {
                          session.left = value;
                          controller.scheduleSave();
                        })),
                SizedBox(
                  width: 220,
                  child: Padding(
                    padding:
                        const EdgeInsets.symmetric(horizontal: 8, vertical: 12),
                    child: Column(
                      mainAxisAlignment: MainAxisAlignment.center,
                      children: [
                        for (final action in actions) ...[
                          SizedBox(width: double.infinity, child: action),
                          const SizedBox(height: 8),
                        ],
                      ],
                    ),
                  ),
                ),
                Expanded(
                    child: IoTextPane(
                        label: rightLabel,
                        value: session.right,
                        fontSize: controller.settings.editorFontSize,
                        onChanged: (value) {
                          session.right = value;
                          controller.scheduleSave();
                        })),
              ],
            ),
          ),
          if (session.historyOpen) _history(tokens),
        ],
      ),
    );
  }

  Widget _history(MooTokens tokens) {
    final items = [
      for (final item in controller.histories)
        if (item.toolId == toolId) item
    ];
    return SizedBox(
      height: 160,
      child: Material(
        color: tokens.surface,
        child: ListView(
          children: [
            if (items.isEmpty)
              ListTile(
                  dense: true, title: Text(controller.t('tool.history.empty'))),
            for (final item in items)
              ListTile(
                dense: true,
                title: Text(item.title),
                subtitle: Text(item.at.toIso8601String()),
                onTap: () => controller.restoreHistory(item),
              ),
          ],
        ),
      ),
    );
  }
}

class IoTextPane extends StatefulWidget {
  const IoTextPane({
    super.key,
    required this.label,
    required this.value,
    required this.onChanged,
    this.fontSize = 14,
  });

  final String label;
  final String value;
  final ValueChanged<String> onChanged;
  final double fontSize;

  @override
  State<IoTextPane> createState() => _IoTextPaneState();
}

class _IoTextPaneState extends State<IoTextPane> {
  late final TextEditingController _controller;

  @override
  void initState() {
    super.initState();
    _controller = TextEditingController(text: widget.value);
  }

  @override
  void didUpdateWidget(covariant IoTextPane oldWidget) {
    super.didUpdateWidget(oldWidget);
    if (widget.value != _controller.text &&
        !_controller.value.composing.isValid) {
      _controller.value = TextEditingValue(
        text: widget.value,
        selection: TextSelection.collapsed(
            offset: widget.value.length.clamp(0, widget.value.length)),
      );
    }
  }

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final tokens = tokensOf(context);
    return Padding(
      padding: const EdgeInsets.all(8),
      child: DecoratedBox(
        decoration: BoxDecoration(
          color: tokens.surface,
          border: Border.all(color: tokens.border),
          borderRadius: BorderRadius.circular(8),
        ),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            Padding(
              padding: const EdgeInsets.fromLTRB(12, 8, 12, 4),
              child: Text(widget.label,
                  style: TextStyle(fontSize: 12, color: tokens.textSecondary)),
            ),
            Expanded(
              child: TextField(
                controller: _controller,
                maxLines: null,
                expands: true,
                style: TextStyle(
                    fontFamily: 'monospace', fontSize: widget.fontSize),
                decoration: const InputDecoration(
                    border: InputBorder.none,
                    contentPadding: EdgeInsets.all(12)),
                onChanged: widget.onChanged,
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class ToolActionButton extends StatelessWidget {
  const ToolActionButton(
      {super.key, required this.label, required this.onPressed, this.icon});
  final String label;
  final VoidCallback onPressed;
  final IconData? icon;

  @override
  Widget build(BuildContext context) {
    return SizedBox(
      width: double.infinity,
      child: CompactButton(
          label: label, icon: icon, onPressed: onPressed, primary: true),
    );
  }
}
