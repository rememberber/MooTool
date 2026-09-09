import 'dart:io';
import 'dart:typed_data';

import 'package:flutter_test/flutter_test.dart';
import 'package:mootool_next_flutter/app/app_controller.dart';
import 'package:mootool_next_flutter/app/app_paths.dart';
import 'package:mootool_next_flutter/app/settings.dart';
import 'package:mootool_next_flutter/core/desktop/desktop_host.dart';
import 'package:mootool_next_flutter/core/desktop/window_policy.dart';

final _png1x1 = Uint8List.fromList(const [
  0x89,
  0x50,
  0x4E,
  0x47,
  0x0D,
  0x0A,
  0x1A,
  0x0A,
  0x00,
  0x00,
  0x00,
  0x0D,
  0x49,
  0x48,
  0x44,
  0x52,
  0x00,
  0x00,
  0x00,
  0x01,
  0x00,
  0x00,
  0x00,
  0x01,
  0x08,
  0x06,
  0x00,
  0x00,
  0x00,
  0x1F,
  0x15,
  0xC4,
  0x89,
  0x00,
  0x00,
  0x00,
  0x0A,
  0x49,
  0x44,
  0x41,
  0x54,
  0x78,
  0x9C,
  0x63,
  0x00,
  0x01,
  0x00,
  0x00,
  0x05,
  0x00,
  0x01,
  0x0D,
  0x0A,
  0x2D,
  0xB4,
  0x00,
  0x00,
  0x00,
  0x00,
  0x49,
  0x45,
  0x4E,
  0x44,
  0xAE,
  0x42,
  0x60,
  0x82,
]);

void main() {
  test('hide without a working tray becomes ask so the window cannot vanish',
      () {
    expect(
        WindowPolicy.effectiveClose(
            requested: CloseBehavior.hide,
            trayEnabled: true,
            trayAvailable: false),
        CloseBehavior.ask);
    expect(
        WindowPolicy.effectiveClose(
            requested: CloseBehavior.hide,
            trayEnabled: false,
            trayAvailable: true),
        CloseBehavior.ask);
    expect(
        WindowPolicy.effectiveClose(
            requested: CloseBehavior.hide,
            trayEnabled: true,
            trayAvailable: true),
        CloseBehavior.hide);
    expect(clipboardImageFileName(_png1x1), 'clipboard.png');
    expect(clipboardImageFileName([0xFF, 0xD8, 0xFF, 0x00]), 'clipboard.jpg');
  });

  test('clipboard image lands in the note vault and image library', () async {
    final root = Directory.systemTemp.createTempSync('mootool-clip-');
    addTearDown(() => root.deleteSync(recursive: true));
    final host = MemoryDesktopHost(clipboardBytes: _png1x1);
    final controller = AppController(AppPaths(root), desktopHost: host);
    addTearDown(controller.dispose);
    await controller.load();
    await controller.pasteNoteImageFromClipboard();
    expect(controller.note.document.text, contains('clipboard.png'));
    await controller.pasteNoteImageFromClipboard();
    expect(controller.note.document.text, contains('clipboard-2.png'));
    await controller.importClipboardImageToLibrary();
    expect(controller.imageAssets, isNotEmpty);

    host.clipboardBytes = null;
    controller.note.notice = '';
    await controller.pasteNoteImageFromClipboard();
    expect(controller.note.notice, isNotEmpty);
  });

  test('close hide without tray asks; hide with tray hides', () async {
    final root = Directory.systemTemp.createTempSync('mootool-close-');
    addTearDown(() => root.deleteSync(recursive: true));
    final host = MemoryDesktopHost(trayWorks: false);
    final controller = AppController(AppPaths(root), desktopHost: host);
    addTearDown(controller.dispose);
    await controller.load();
    controller.settings.closeBehavior = CloseBehavior.hide;
    controller.settings.trayEnabled = true;
    await controller.applyDesktopPolicy();
    expect(controller.effectiveCloseBehavior, CloseBehavior.ask);
    host.simulateCloseRequested();
    expect(controller.closePrompt, isTrue);
    await controller.confirmClose(CloseDecision.hide);
    expect(controller.closePrompt, isTrue);
    expect(host.lastCloseAction, isNull);

    final working = MemoryDesktopHost();
    final second = AppController(AppPaths(root), desktopHost: working);
    addTearDown(second.dispose);
    await second.load();
    second.settings.closeBehavior = CloseBehavior.hide;
    second.settings.trayEnabled = true;
    await second.applyDesktopPolicy();
    expect(second.effectiveCloseBehavior, CloseBehavior.hide);
    await second.handleNativeCloseRequested();
    expect(second.closePrompt, isFalse);
    expect(working.lastCloseAction, 'hide');
    expect(working.windowHidden, isTrue);
  });

  test('startMaximized is applied only on launch', () async {
    final root = Directory.systemTemp.createTempSync('mootool-max-');
    addTearDown(() => root.deleteSync(recursive: true));
    final host = MemoryDesktopHost();
    final controller = AppController(AppPaths(root), desktopHost: host);
    addTearDown(controller.dispose);
    controller.settings.startMaximized = true;
    await controller.load();
    expect(host.windowMaximized, isTrue);
    host.windowMaximized = false;
    await controller.applyDesktopPolicy();
    expect(host.windowMaximized, isFalse);
  });

  test('detached tools persist and do not cancel in-flight HTTP', () async {
    final root = Directory.systemTemp.createTempSync('mootool-detach-');
    addTearDown(() => root.deleteSync(recursive: true));
    final host = MemoryDesktopHost();
    final first = AppController(AppPaths(root), desktopHost: host);
    addTearDown(first.dispose);
    await first.load();
    first.http.sending = true;
    first.detachTool('http');
    first.detachTool('json');
    expect(first.http.sending, isTrue);
    expect(first.coordinator.ownerOf('json'), 'detached-json');
    await first.persist();

    final second =
        AppController(AppPaths(root), desktopHost: MemoryDesktopHost());
    addTearDown(second.dispose);
    await second.load();
    expect(second.detachedToolIds, containsAll(['http', 'json']));
    expect(second.coordinator.ownerOf('json'), 'detached-json');
    second.dockTool('json');
    expect(second.detachedToolIds.contains('json'), isFalse);
    expect(second.coordinator.ownerOf('json'), 'main');
  });
}
