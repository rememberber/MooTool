import 'dart:math' as math;

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import '../../app/app_controller.dart';
import '../../design/theme.dart';
import '../../design/widgets.dart';
import 'message_board.dart';

class MessageBoardPage extends StatelessWidget {
  const MessageBoardPage({super.key, required this.controller});
  final AppController controller;

  @override
  Widget build(BuildContext context) {
    final session = controller.messageBoard;
    final tokens = tokensOf(context);
    final colors =
        messageBoardColors[session.theme] ?? messageBoardColors['sunbeam']!;
    final visible = session.message.trim().isEmpty
        ? controller.t('messageBoard.empty')
        : session.message;
    if (session.presenting) {
      return _stage(session, colors, visible, presenting: true);
    }
    return Row(
      children: [
        SizedBox(
          width: 280,
          child: ColoredBox(
            color: tokens.surface,
            child: ListView(
              padding: const EdgeInsets.all(12),
              children: [
                Row(
                  children: [
                    Text(controller.t('messageBoard.message'),
                        style: const TextStyle(fontWeight: FontWeight.w600)),
                    const Spacer(),
                    Text('${session.message.length}/$maxMessageLength',
                        style: TextStyle(
                            fontSize: 11, color: tokens.textSecondary)),
                  ],
                ),
                const SizedBox(height: 6),
                TextField(
                  maxLines: 3,
                  maxLength: maxMessageLength,
                  controller: TextEditingController(text: session.message)
                    ..selection =
                        TextSelection.collapsed(offset: session.message.length),
                  inputFormatters: [
                    LengthLimitingTextInputFormatter(maxMessageLength)
                  ],
                  decoration: InputDecoration(
                      hintText: controller.t('messageBoard.placeholder'),
                      counterText: ''),
                  onChanged: (value) {
                    session.message = session.clampMessage(value);
                    controller.scheduleSave();
                  },
                ),
                Text(controller.t('messageBoard.messageHint'),
                    style:
                        TextStyle(fontSize: 11, color: tokens.textSecondary)),
                const SizedBox(height: 16),
                Text(controller.t('messageBoard.presets'),
                    style: const TextStyle(fontWeight: FontWeight.w600)),
                const SizedBox(height: 8),
                Wrap(
                  spacing: 6,
                  runSpacing: 6,
                  children: [
                    for (final preset in messageBoardPresets)
                      CompactButton(
                        label: controller.t('messageBoard.preset.${preset.$1}'),
                        primary: session.message ==
                            controller.t('messageBoard.preset.${preset.$1}'),
                        onPressed: () {
                          session.applyPreset(preset.$1,
                              controller.t('messageBoard.preset.${preset.$1}'));
                          controller.scheduleSave();
                          controller.refresh();
                        },
                      ),
                  ],
                ),
                const SizedBox(height: 16),
                Text(controller.t('messageBoard.style'),
                    style: const TextStyle(fontWeight: FontWeight.w600)),
                const SizedBox(height: 8),
                Wrap(
                  spacing: 6,
                  children: [
                    for (final id in messageBoardThemes)
                      _themeSwatch(id, session.theme == id, () {
                        session.theme = id;
                        controller.scheduleSave();
                        controller.refresh();
                      }),
                  ],
                ),
                const SizedBox(height: 12),
                Row(
                  children: [
                    Text(controller.t('messageBoard.size')),
                    const Spacer(),
                    Text('${session.size}%'),
                  ],
                ),
                Slider(
                  min: 70,
                  max: 130,
                  divisions: 12,
                  value: session.size.toDouble(),
                  onChanged: (value) {
                    session.size = value.round();
                    controller.scheduleSave();
                    controller.refresh();
                  },
                ),
                Row(
                  children: [
                    CompactButton(
                        label: controller.t('messageBoard.alignLeft'),
                        primary: session.alignment == 'left',
                        onPressed: () {
                          session.alignment = 'left';
                          controller.scheduleSave();
                          controller.refresh();
                        }),
                    const SizedBox(width: 6),
                    CompactButton(
                        label: controller.t('messageBoard.alignCenter'),
                        primary: session.alignment == 'center',
                        onPressed: () {
                          session.alignment = 'center';
                          controller.scheduleSave();
                          controller.refresh();
                        }),
                  ],
                ),
              ],
            ),
          ),
        ),
        VerticalDivider(width: 1, color: tokens.border),
        Expanded(child: _stage(session, colors, visible, presenting: false)),
      ],
    );
  }

  Widget _themeSwatch(String id, bool active, VoidCallback onTap) {
    final colors = messageBoardColors[id]!;
    return InkWell(
      onTap: onTap,
      child: Container(
        width: 36,
        height: 22,
        decoration: BoxDecoration(
          borderRadius: BorderRadius.circular(4),
          border: Border.all(
              color: active ? Colors.white : Colors.black26,
              width: active ? 2 : 1),
        ),
        child: Row(children: [
          Expanded(child: ColoredBox(color: Color(colors.$1))),
          Expanded(child: ColoredBox(color: Color(colors.$2))),
        ]),
      ),
    );
  }

  Widget _stage(MessageBoardSession session, (int, int) colors, String visible,
      {required bool presenting}) {
    return ColoredBox(
      color: Color(colors.$1),
      child: Column(
        children: [
          Padding(
            padding: const EdgeInsets.fromLTRB(12, 10, 12, 0),
            child: Row(
              children: [
                Text(controller.t('messageBoard.badge'),
                    style: TextStyle(
                        color: Color(colors.$2),
                        fontSize: 12,
                        fontWeight: FontWeight.w600)),
                const Spacer(),
                Text(
                    presenting
                        ? controller.t(session.displayAwake
                            ? 'messageBoard.displayAwake'
                            : 'messageBoard.sleepHint')
                        : controller.t('messageBoard.badgeEnglish'),
                    style: TextStyle(color: Color(colors.$2), fontSize: 11)),
                const SizedBox(width: 8),
                CompactButton(
                  label: presenting
                      ? controller.t('messageBoard.exitHint')
                      : controller.t('messageBoard.display'),
                  onPressed: () {
                    controller.setMessageBoardPresenting(!presenting);
                  },
                ),
              ],
            ),
          ),
          Expanded(
            child: Padding(
              padding: const EdgeInsets.all(24),
              child: LayoutBuilder(builder: (context, constraints) {
                final maxSize =
                    (math.min(constraints.maxHeight, constraints.maxWidth) *
                            0.72 *
                            session.size /
                            100)
                        .clamp(26.0, 280.0);
                return Align(
                  alignment: session.alignment == 'left'
                      ? Alignment.centerLeft
                      : Alignment.center,
                  child: FittedBox(
                    fit: BoxFit.contain,
                    alignment: session.alignment == 'left'
                        ? Alignment.centerLeft
                        : Alignment.center,
                    child: ConstrainedBox(
                      constraints:
                          BoxConstraints(maxWidth: constraints.maxWidth),
                      child: Text(
                        visible,
                        textAlign: session.alignment == 'left'
                            ? TextAlign.left
                            : TextAlign.center,
                        style: TextStyle(
                          color: Color(colors.$2),
                          fontSize: maxSize,
                          fontWeight: FontWeight.w700,
                          height: 1.15,
                        ),
                      ),
                    ),
                  ),
                );
              }),
            ),
          ),
          Padding(
            padding: const EdgeInsets.only(bottom: 12),
            child: Text(controller.t('messageBoard.footer'),
                style: TextStyle(color: Color(colors.$2), fontSize: 11)),
          ),
        ],
      ),
    );
  }
}
