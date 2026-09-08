import 'dart:convert';
import 'dart:io';

import 'package:file_selector/file_selector.dart';
import 'package:flutter/material.dart';

import '../../app/app_controller.dart';
import '../../design/theme.dart';
import '../../design/widgets.dart';
import 'image_tools.dart';

class ImageToolPage extends StatelessWidget {
  const ImageToolPage({super.key, required this.controller});
  final AppController controller;

  @override
  Widget build(BuildContext context) {
    final tokens = tokensOf(context);
    final current = controller.imageCurrent;
    final panel = controller.imagePanel;
    return Column(
      children: [
        ColoredBox(
          color: tokens.toolbar,
          child: Padding(
            padding: const EdgeInsets.all(8),
            child: Wrap(
              spacing: 6,
              runSpacing: 6,
              children: [
                CompactButton(
                    label: controller.t('image.toggleList'),
                    onPressed: () {
                      controller.imageListVisible = !controller.imageListVisible;
                      controller.refresh();
                    }),
                CompactButton(
                    label: controller.t('image.import'),
                    onPressed: () async {
                      final files = await openFiles(acceptedTypeGroups: [
                        const XTypeGroup(label: 'Images', extensions: [
                          'png',
                          'jpg',
                          'jpeg',
                          'gif',
                          'webp'
                        ])
                      ]);
                      await controller.importImageFiles(
                          [for (final file in files) File(file.path)]);
                    }),
                CompactButton(
                    label: controller.t('image.fromBase64'),
                    onPressed: () {
                      controller.imagePanel =
                          panel == 'base64' ? '' : 'base64';
                      controller.refresh();
                    }),
                CompactButton(
                    label: controller.t('image.compress'),
                    onPressed: controller.imageTargets.isEmpty
                        ? null
                        : () {
                            controller.imagePanel =
                                panel == 'compress' ? '' : 'compress';
                            controller.refresh();
                          }),
                CompactButton(
                    label: controller.t('image.watermark'),
                    onPressed: controller.imageTargets.isEmpty
                        ? null
                        : () {
                            controller.imagePanel =
                                panel == 'watermark' ? '' : 'watermark';
                            controller.refresh();
                          }),
                CompactButton(
                    label: controller.t('image.toSvg'),
                    onPressed: controller.imageTargets.isEmpty
                        ? null
                        : () {
                            controller.imagePanel =
                                panel == 'svg' ? '' : 'svg';
                            controller.refresh();
                          }),
                CompactButton(
                    label: controller.t('image.toBase64'),
                    onPressed: current == null
                        ? null
                        : () => controller.exportCurrentImageBase64()),
                CompactButton(
                    label: controller.t('image.screenshot'),
                    onPressed: () => controller.captureScreenshotToLibrary()),
                CompactButton(
                    label: controller.t('image.fromClipboard'),
                    onPressed: () => controller.importClipboardImageToLibrary()),
              ],
            ),
          ),
        ),
        if (controller.imageNotice.isNotEmpty)
          Padding(
            padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
            child: Align(
              alignment: Alignment.centerLeft,
              child: Text(controller.imageNotice,
                  style: TextStyle(fontSize: 12, color: tokens.textSecondary)),
            ),
          ),
        if (panel.isNotEmpty) _panel(tokens, panel),
        Expanded(
          child: Row(
            children: [
              if (controller.imageListVisible)
                SizedBox(
                  width: 240,
                  child: ColoredBox(
                    color: tokens.surface,
                    child: Column(
                      children: [
                        Padding(
                          padding: const EdgeInsets.all(8),
                          child: Align(
                            alignment: Alignment.centerLeft,
                            child: Text(controller.t('image.library'),
                                style: const TextStyle(
                                    fontWeight: FontWeight.w600)),
                          ),
                        ),
                        Expanded(
                          child: controller.imageAssets.isEmpty
                              ? Center(child: Text(controller.t('image.empty')))
                              : ListView(
                                  children: [
                                    for (final asset in controller.imageAssets)
                                      ListTile(
                                        dense: true,
                                        selected: controller.imageCurrentName ==
                                            asset.name,
                                        leading: Checkbox(
                                          value: controller.imageSelected
                                              .contains(asset.name),
                                          onChanged: (value) => controller
                                              .toggleImageSelection(
                                                  asset.name, value ?? false),
                                        ),
                                        title: Text(asset.name,
                                            overflow: TextOverflow.ellipsis),
                                        subtitle: Text(
                                            '${asset.width} × ${asset.height} · ${_bytes(asset.size)}'),
                                        onTap: () =>
                                            controller.selectImage(asset.name),
                                      ),
                                  ],
                                ),
                        ),
                        Padding(
                          padding: const EdgeInsets.all(8),
                          child: Wrap(
                            spacing: 6,
                            children: [
                              SizedBox(
                                width: 110,
                                child: TextField(
                                  decoration: InputDecoration(
                                      isDense: true,
                                      hintText: controller.t('image.rename')),
                                  onChanged: (value) => controller
                                      .localFor('image')
                                      .options['rename'] = value,
                                ),
                              ),
                              CompactButton(
                                  label: controller.t('image.rename'),
                                  onPressed: current == null
                                      ? null
                                      : () => controller.renameCurrentImage()),
                              CompactButton(
                                  label: controller.t('image.export'),
                                  onPressed: controller.imageTargets.isEmpty
                                      ? null
                                      : () async {
                                          final directory =
                                              await getDirectoryPath();
                                          if (directory == null) return;
                                          await controller.exportImages(
                                              Directory(directory));
                                        }),
                              CompactButton(
                                  label: controller.t('host.delete'),
                                  onPressed: controller.imageTargets.isEmpty
                                      ? null
                                      : () => controller.deleteSelectedImages()),
                            ],
                          ),
                        ),
                      ],
                    ),
                  ),
                ),
              Expanded(
                child: current == null
                    ? Center(child: Text(controller.t('image.emptyPreview')))
                    : Column(
                        children: [
                          Expanded(
                            child: current.name.toLowerCase().endsWith('.svg')
                                ? SingleChildScrollView(
                                    padding: const EdgeInsets.all(12),
                                    child: SelectableText(
                                      utf8.decode(current.bytes),
                                      style: const TextStyle(
                                          fontFamily: 'monospace', fontSize: 12),
                                    ),
                                  )
                                : InteractiveViewer(
                                    minScale: 0.1,
                                    maxScale: 5,
                                    child: Image.memory(current.bytes,
                                        fit: controller.imageFit
                                            ? BoxFit.contain
                                            : BoxFit.none,
                                        filterQuality: FilterQuality.medium),
                                  ),
                          ),
                          Padding(
                            padding: const EdgeInsets.all(8),
                            child: Text(
                                '${current.width} × ${current.height} · ${_bytes(current.size)}',
                                style: const TextStyle(fontSize: 11)),
                          ),
                        ],
                      ),
              ),
            ],
          ),
        ),
      ],
    );
  }

  Widget _panel(tokens, String panel) {
    final session = controller.localFor('image');
    if (panel == 'screenshot') {
      return Padding(
        padding: const EdgeInsets.all(8),
        child: Wrap(
          spacing: 8,
          runSpacing: 6,
          crossAxisAlignment: WrapCrossAlignment.center,
          children: [
            _cropField('X', controller.screenshotCropLeft,
                (value) => controller.screenshotCropLeft = value),
            _cropField('Y', controller.screenshotCropTop,
                (value) => controller.screenshotCropTop = value),
            _cropField('W', controller.screenshotCropWidth,
                (value) => controller.screenshotCropWidth = value),
            _cropField('H', controller.screenshotCropHeight,
                (value) => controller.screenshotCropHeight = value),
            CompactButton(
                label: controller.t('image.keepFull'),
                onPressed: () =>
                    controller.confirmScreenshotDraft(crop: false)),
            CompactButton(
                label: controller.t('image.cropConfirm'),
                onPressed: () =>
                    controller.confirmScreenshotDraft(crop: true)),
            CompactButton(
                label: controller.t('common.cancel'),
                onPressed: controller.cancelScreenshotDraft),
          ],
        ),
      );
    }
    if (panel == 'base64') {
      return Padding(
        padding: const EdgeInsets.all(8),
        child: Row(
          children: [
            Expanded(
              child: TextField(
                maxLines: 3,
                decoration: InputDecoration(
                    isDense: true,
                    hintText: controller.t('image.base64Hint')),
                onChanged: (value) => controller.imageBase64Draft = value,
              ),
            ),
            const SizedBox(width: 8),
            CompactButton(
                label: controller.t('image.import'),
                onPressed: () => controller.importImageBase64(
                    controller.imageBase64Draft)),
          ],
        ),
      );
    }
    if (panel == 'compress') {
      return Padding(
        padding: const EdgeInsets.all(8),
        child: Wrap(
          spacing: 8,
          runSpacing: 6,
          crossAxisAlignment: WrapCrossAlignment.center,
          children: [
            Text(controller.t('image.quality')),
            SizedBox(
              width: 140,
              child: Slider(
                min: 10,
                max: 100,
                value: double.tryParse(session.options['quality'] ?? '80') ?? 80,
                onChanged: (value) {
                  session.options['quality'] = '${value.round()}';
                  controller.refresh();
                },
              ),
            ),
            Text(controller.t('image.scale')),
            SizedBox(
              width: 140,
              child: Slider(
                min: 10,
                max: 100,
                value: double.tryParse(session.options['scale'] ?? '100') ?? 100,
                onChanged: (value) {
                  session.options['scale'] = '${value.round()}';
                  controller.refresh();
                },
              ),
            ),
            DropdownButton<String>(
              value: session.options['format'] ?? 'auto',
              items: [
                for (final format in ['auto', 'png', 'jpeg'])
                  DropdownMenuItem(value: format, child: Text(format)),
              ],
              onChanged: (value) {
                session.options['format'] = value ?? 'auto';
                controller.refresh();
              },
            ),
            CompactButton(
                label: controller.t('image.startProcess'),
                primary: true,
                onPressed: () => controller.compressSelectedImages(
                      CompressImageOptions(
                        quality:
                            (double.tryParse(session.options['quality'] ?? '80') ??
                                    80) /
                                100,
                        scale:
                            (double.tryParse(session.options['scale'] ?? '100') ??
                                    100) /
                                100,
                        format: session.options['format'] ?? 'auto',
                      ),
                      session.options['mode'] == 'overwrite' ? 'overwrite' : 'keep',
                    )),
          ],
        ),
      );
    }
    if (panel == 'watermark') {
      return Padding(
        padding: const EdgeInsets.all(8),
        child: Wrap(
          spacing: 8,
          runSpacing: 6,
          crossAxisAlignment: WrapCrossAlignment.center,
          children: [
            SizedBox(
              width: 180,
              child: TextField(
                decoration: InputDecoration(
                    isDense: true, hintText: controller.t('image.watermarkText')),
                controller: TextEditingController(
                    text: session.options['watermark'] ?? 'MooTool'),
                onChanged: (value) => session.options['watermark'] = value,
              ),
            ),
            DropdownButton<String>(
              value: session.options['position'] ?? 'bottom-right',
              items: [
                for (final position in [
                  'bottom-right',
                  'bottom-left',
                  'top-right',
                  'top-left',
                  'center',
                  'tile'
                ])
                  DropdownMenuItem(value: position, child: Text(position)),
              ],
              onChanged: (value) {
                session.options['position'] = value ?? 'bottom-right';
                controller.refresh();
              },
            ),
            CompactButton(
                label: controller.t('image.startProcess'),
                primary: true,
                onPressed: () => controller.watermarkSelectedImages(
                      WatermarkImageOptions(
                        text: session.options['watermark'] ?? 'MooTool',
                        position: session.options['position'] ?? 'bottom-right',
                      ),
                    )),
          ],
        ),
      );
    }
    return Padding(
      padding: const EdgeInsets.all(8),
      child: Wrap(
        spacing: 8,
        runSpacing: 6,
        crossAxisAlignment: WrapCrossAlignment.center,
        children: [
          DropdownButton<String>(
            value: session.options['preset'] ?? 'bw',
            items: [
              for (final preset in ['poster', 'photo', 'bw'])
                DropdownMenuItem(value: preset, child: Text(preset)),
            ],
            onChanged: (value) {
              session.options['preset'] = value ?? 'bw';
              controller.refresh();
            },
          ),
          CompactButton(
              label: controller.t('image.svgStart'),
              primary: true,
              onPressed: () => controller.vectorizeSelectedImages(
                    VectorizeOptions(
                      preset: session.options['preset'] ?? 'bw',
                      colorCount: int.tryParse(
                              session.options['colorCount'] ?? '16') ??
                          16,
                    ),
                  )),
          Text(controller.t('image.svgHint'),
              style: TextStyle(fontSize: 11, color: tokens.textSecondary)),
        ],
      ),
    );
  }

  Widget _cropField(String label, int value, ValueChanged<int> onChanged) {
    return SizedBox(
      width: 72,
      child: TextField(
        controller: TextEditingController(text: '$value')
          ..selection = TextSelection.collapsed(offset: '$value'.length),
        decoration: InputDecoration(isDense: true, labelText: label),
        onChanged: (next) => onChanged(int.tryParse(next) ?? value),
      ),
    );
  }

  String _bytes(int bytes) {
    if (bytes < 1024) return '$bytes B';
    if (bytes < 1024 * 1024) return '${(bytes / 1024).toStringAsFixed(1)} KB';
    return '${(bytes / (1024 * 1024)).toStringAsFixed(1)} MB';
  }
}
