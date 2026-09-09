import 'dart:io';

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mootool_next_flutter/app/app_controller.dart';
import 'package:mootool_next_flutter/app/app_paths.dart';
import 'package:mootool_next_flutter/app/tool_registry.dart';
import 'package:mootool_next_flutter/core/desktop/desktop_host.dart';
import 'package:mootool_next_flutter/core/editor/editor_document.dart';
import 'package:mootool_next_flutter/design/theme.dart';
import 'package:mootool_next_flutter/design/widgets.dart';
import 'package:mootool_next_flutter/main.dart';

void main() {
  test('R17 non-default UI font size must build a usable theme', () {
    expect(() => buildTheme(brightness: Brightness.light, uiFontSize: 14),
        returnsNormally);
    expect(() => buildTheme(brightness: Brightness.dark, uiFontSize: 18),
        returnsNormally);
  });

  testWidgets('R12 selection-only find result reaches visible editor',
      (tester) async {
    final doc = EditorDocument(id: 'probe', text: 'alpha beta gamma');
    Widget view() => MaterialApp(
        theme: buildTheme(brightness: Brightness.light, uiFontSize: 13),
        home: Scaffold(body: MooCodeEditor(document: doc, onChanged: (_) {})));
    await tester.pumpWidget(view());
    doc.restoreView(start: 6, end: 10);
    await tester.pumpWidget(view());
    final field = tester.widget<TextField>(find.byType(TextField));
    expect(field.controller!.selection,
        const TextSelection(baseOffset: 6, extentOffset: 10));
  });

  testWidgets('R16 tool pages do not assert missing Material for navigation',
      (tester) async {
    tester.view.physicalSize = const Size(1440, 900);
    tester.view.devicePixelRatio = 1;
    addTearDown(tester.view.resetPhysicalSize);
    addTearDown(tester.view.resetDevicePixelRatio);
    final root = Directory.systemTemp.createTempSync('mootool-widget-review-');
    final controller =
        AppController(AppPaths(root), desktopHost: MemoryDesktopHost());
    addTearDown(() {
      controller.dispose();
      if (root.existsSync()) root.deleteSync(recursive: true);
    });
    await tester.runAsync(controller.load);
    final failures = <String>[];
    final originalHandler = FlutterError.onError;
    var currentId = '';
    FlutterError.onError =
        (details) => failures.add('$currentId: ${details.exceptionAsString()}');
    addTearDown(() => FlutterError.onError = originalHandler);
    for (final id in [...toolRegistry.map((t) => t.id), 'settings']) {
      currentId = id;
      controller.activeToolId = id == 'settings' ? 'mootool' : id;
      if (id == 'settings') {
        controller.openSettings();
      } else {
        controller.closeOverlays();
      }
      await tester.pumpWidget(MooToolApp(controller: controller));
      await tester.pump(const Duration(milliseconds: 50));
      Object? error;
      while ((error = tester.takeException()) != null) {
        failures.add('$id: $error');
      }
    }
    await tester.pumpWidget(const SizedBox.shrink());
    FlutterError.onError = originalHandler;
    expect(failures.toSet(), isEmpty, reason: failures.toSet().join('\n'));
  });
}
