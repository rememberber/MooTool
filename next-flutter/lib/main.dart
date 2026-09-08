import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import 'app/app_controller.dart';
import 'app/app_paths.dart';
import 'app/product.dart';
import 'app/settings.dart';
import 'core/desktop/window_policy.dart';
import 'design/theme.dart';
import 'features/workbench/workbench.dart';

Future<void> main(List<String> args) async {
  WidgetsFlutterBinding.ensureInitialized();
  final launch = LaunchArgs.parse(args);
  final paths = AppPaths.resolve(override: launch.dataDir);
  final controller = AppController(paths);
  await controller.load();
  runApp(MooToolApp(controller: controller));
}

class MooToolApp extends StatelessWidget {
  const MooToolApp({super.key, required this.controller});
  final AppController controller;

  @override
  Widget build(BuildContext context) {
    return AnimatedBuilder(
      animation: controller,
      builder: (context, _) {
        return MaterialApp(
          title: Product.displayName,
          debugShowCheckedModeBanner: false,
          theme: buildTheme(
              brightness: Brightness.light,
              uiFontSize: controller.settings.uiFontSize,
              accent: _accent(controller.settings.accentColor)),
          darkTheme: buildTheme(
              brightness: Brightness.dark,
              uiFontSize: controller.settings.uiFontSize,
              accent: _accent(controller.settings.accentColor)),
          themeMode: switch (controller.settings.theme) {
            ThemePreference.light => ThemeMode.light,
            ThemePreference.dark => ThemeMode.dark,
            ThemePreference.system => ThemeMode.system,
          },
          home: CallbackShortcuts(
            bindings: {
              const SingleActivator(LogicalKeyboardKey.keyK, meta: true):
                  controller.openSearch,
              const SingleActivator(LogicalKeyboardKey.keyK, control: true):
                  controller.openSearch,
            },
            child: Stack(
              children: [
                Scaffold(
                  body: SafeArea(child: Workbench(controller: controller)),
                ),
                if (controller.closePrompt)
                  ColoredBox(
                    color: const Color(0x66000000),
                    child: Center(
                      child: ConstrainedBox(
                        constraints: const BoxConstraints(maxWidth: 420),
                        child: Material(
                          borderRadius: BorderRadius.circular(8),
                          child: Padding(
                            padding: const EdgeInsets.all(20),
                            child: Column(
                              mainAxisSize: MainAxisSize.min,
                              crossAxisAlignment: CrossAxisAlignment.start,
                              children: [
                                Text(controller.t('settings.closeBehavior'),
                                    style: const TextStyle(
                                        fontWeight: FontWeight.w600,
                                        fontSize: 16)),
                                const SizedBox(height: 8),
                                Text(controller.t('settings.close.askBody')),
                                const SizedBox(height: 16),
                                Wrap(
                                  spacing: 8,
                                  children: [
                                    TextButton(
                                        onPressed: controller.cancelClosePrompt,
                                        child: Text(
                                            controller.t('common.cancel'))),
                                    TextButton(
                                        onPressed: () => controller
                                            .confirmClose(CloseDecision.hide),
                                        child: Text(
                                            controller.t('settings.close.hide'))),
                                    FilledButton(
                                        onPressed: () => controller
                                            .confirmClose(CloseDecision.quit),
                                        child: Text(
                                            controller.t('settings.close.quit'))),
                                  ],
                                ),
                              ],
                            ),
                          ),
                        ),
                      ),
                    ),
                  ),
              ],
            ),
          ),
        );
      },
    );
  }
}

Color _accent(String value) {
  final hex = resolveAccentHex(value).substring(1);
  return Color(int.parse('FF$hex', radix: 16));
}
