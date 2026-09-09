import 'package:flutter/material.dart';

import '../../app/app_controller.dart';
import '../../design/theme.dart';
import '../../design/widgets.dart';

class HostToolPage extends StatelessWidget {
  const HostToolPage({super.key, required this.controller});
  final AppController controller;

  @override
  Widget build(BuildContext context) {
    final tokens = tokensOf(context);
    final session = controller.host;
    final selected = session.selected;
    return Row(
      children: [
        SizedBox(
          width: 220,
          child: ColoredBox(
            color: tokens.sidebar,
            child: Column(
              children: [
                Wrap(children: [
                  CompactButton(
                      label: controller.t('host.new'),
                      onPressed: controller.createHostProfile),
                ]),
                Expanded(
                  child: ListView(
                    children: [
                      for (final item in session.profiles)
                        ListTile(
                          dense: true,
                          selected: item.id == session.selectedId,
                          title: Text(item.name),
                          onTap: () {
                            session.selectedId = item.id;
                            controller.refresh();
                          },
                        ),
                    ],
                  ),
                ),
              ],
            ),
          ),
        ),
        Expanded(
          child: Column(
            children: [
              ColoredBox(
                color: tokens.toolbar,
                child: Padding(
                  padding: const EdgeInsets.all(8),
                  child: Wrap(spacing: 6, children: [
                    CompactButton(
                        label: controller.t('host.save'),
                        onPressed: controller.saveHostProfile),
                    CompactButton(
                        label: controller.t('host.delete'),
                        onPressed: controller.deleteHostProfile),
                    CompactButton(
                        label: controller.t('host.apply'),
                        onPressed: controller.applyHostToSystem),
                  ]),
                ),
              ),
              if (selected != null)
                Padding(
                  padding: const EdgeInsets.all(8),
                  child: TextField(
                    decoration:
                        InputDecoration(labelText: controller.t('host.name')),
                    controller: TextEditingController(text: selected.name),
                    onChanged: (value) => selected.name = value,
                  ),
                ),
              Expanded(
                child: selected == null
                    ? Center(child: Text(controller.t('host.empty')))
                    : TextField(
                        maxLines: null,
                        expands: true,
                        controller:
                            TextEditingController(text: selected.content)
                              ..selection = TextSelection.collapsed(
                                  offset: selected.content.length),
                        style: const TextStyle(
                            fontFamily: 'monospace', fontSize: 13),
                        decoration: const InputDecoration(
                            border: InputBorder.none,
                            contentPadding: EdgeInsets.all(12)),
                        onChanged: (value) => selected.content = value,
                      ),
              ),
              ColoredBox(
                color: tokens.toolbar,
                child: SizedBox(
                  height: 26,
                  child: Padding(
                    padding: const EdgeInsets.symmetric(horizontal: 10),
                    child: Text(session.notice,
                        overflow: TextOverflow.ellipsis,
                        style: const TextStyle(fontSize: 11)),
                  ),
                ),
              ),
            ],
          ),
        ),
      ],
    );
  }
}
