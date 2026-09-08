import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import 'app/app_controller.dart';
import 'app/app_paths.dart';
import 'app/product.dart';
import 'app/settings.dart';
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
            child: Scaffold(
              body: SafeArea(child: Workbench(controller: controller)),
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
