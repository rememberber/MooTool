import 'dart:io';
import 'dart:typed_data';

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:image/image.dart' as img;
import 'package:mootool_next_flutter/app/app_controller.dart';
import 'package:mootool_next_flutter/app/app_paths.dart';
import 'package:mootool_next_flutter/app/settings.dart';
import 'package:mootool_next_flutter/core/desktop/desktop_host.dart';
import 'package:mootool_next_flutter/design/theme.dart';
import 'package:mootool_next_flutter/features/image/image_process.dart';

Uint8List _rgbPng({required int width, required int height}) {
  final image = img.Image(width: width, height: height);
  for (var y = 0; y < height; y++) {
    for (var x = 0; x < width; x++) {
      image.setPixelRgb(x, y, x < width / 2 ? 255 : 0, y < height / 2 ? 255 : 0, 80);
    }
  }
  return Uint8List.fromList(img.encodePng(image));
}

void main() {
  test('crop keeps the requested region and rejects empty crops', () {
    final source = _rgbPng(width: 8, height: 6);
    final cropped = cropImageBytes(source, left: 4, top: 0, width: 4, height: 3);
    final decoded = img.decodeImage(cropped)!;
    expect(decoded.width, 4);
    expect(decoded.height, 3);
    expect(
        () => cropImageBytes(source, left: 0, top: 0, width: 0, height: 2),
        throwsA(isA<FormatException>()));
  });

  test('screenshot draft can crop, keep full, or cancel without saving',
      () async {
    final root = Directory.systemTemp.createTempSync('mootool-crop-');
    addTearDown(() => root.deleteSync(recursive: true));
    final png = _rgbPng(width: 8, height: 6);
    final host = MemoryDesktopHost(screenshotBytes: png);
    final controller = AppController(AppPaths(root), desktopHost: host);
    addTearDown(controller.dispose);
    await controller.load();
    await controller.captureScreenshotToLibrary();
    expect(controller.screenshotDraft, isNotNull);
    expect(controller.imageAssets, isEmpty);
    controller.screenshotCropLeft = 4;
    controller.screenshotCropTop = 0;
    controller.screenshotCropWidth = 4;
    controller.screenshotCropHeight = 6;
    await controller.confirmScreenshotDraft(crop: true);
    expect(controller.screenshotDraft, isNull);
    expect(controller.imageAssets, hasLength(1));
    final saved = await controller.imageLibrary.read(controller.imageAssets.first.name);
    final decoded = img.decodeImage(saved.bytes)!;
    expect(decoded.width, 4);
    expect(decoded.height, 6);

    await controller.captureScreenshotToLibrary();
    controller.cancelScreenshotDraft();
    expect(controller.screenshotDraft, isNull);
    expect(controller.imageAssets, hasLength(1));
  });

  test('missing screenshot bytes is an explicit miss, not a library write',
      () async {
    final root = Directory.systemTemp.createTempSync('mootool-miss-');
    addTearDown(() => root.deleteSync(recursive: true));
    final host = MemoryDesktopHost(
        caps: DesktopCapabilities(screenshot: true, screenColor: true));
    final controller = AppController(AppPaths(root), desktopHost: host);
    addTearDown(controller.dispose);
    await controller.load();
    await controller.captureScreenshotToLibrary();
    expect(controller.imageAssets, isEmpty);
    expect(controller.imageNotice, isNotEmpty);
    await controller.pickScreenColorInto('colorBoard');
    expect(controller.localFor('colorBoard').left, isEmpty);
  });

  test('interface styles use distinct tokens instead of reusing modern', () {
    final modern = tokensFor(InterfaceStyle.modern, Brightness.light);
    final quiet = tokensFor(InterfaceStyle.quiet, Brightness.light);
    final hero = tokensFor(InterfaceStyle.hero, Brightness.light);
    final smartisan = tokensFor(InterfaceStyle.smartisan, Brightness.light);
    final miui = tokensFor(InterfaceStyle.miuiV5, Brightness.light);
    final claude = tokensFor(InterfaceStyle.claude, Brightness.light);
    expect(quiet.workspace, isNot(modern.workspace));
    expect(hero.sidebar, isNot(modern.sidebar));
    expect(smartisan.accent, isNot(modern.accent));
    expect(miui.accent, isNot(modern.accent));
    expect(claude.workspace, isNot(modern.workspace));
    expect(
        tokensFor(InterfaceStyle.quiet, Brightness.dark).workspace,
        isNot(tokensFor(InterfaceStyle.modern, Brightness.dark).workspace));
  });
}
