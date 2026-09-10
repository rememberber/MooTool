import 'package:flutter/material.dart';

import '../../app/app_controller.dart';
import '../../design/theme.dart';

class PendingToolPage extends StatelessWidget {
  const PendingToolPage(
      {super.key, required this.controller, required this.toolId});

  final AppController controller;
  final String toolId;

  @override
  Widget build(BuildContext context) {
    final tokens = tokensOf(context);
    final name = controller.t('app.nav.$toolId');
    return ColoredBox(
      color: tokens.workspace,
      child: Center(
        child: ConstrainedBox(
          constraints: const BoxConstraints(maxWidth: 480),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              Icon(Icons.handyman_outlined,
                  size: 28, color: tokens.textSecondary),
              const SizedBox(height: 12),
              Text(controller.t('tool.pending.title'),
                  style: const TextStyle(
                      fontSize: 16, fontWeight: FontWeight.w600)),
              const SizedBox(height: 8),
              Text(controller.t('tool.pending.body', {'name': name}),
                  textAlign: TextAlign.center,
                  style: TextStyle(color: tokens.textSecondary)),
            ],
          ),
        ),
      ),
    );
  }
}
