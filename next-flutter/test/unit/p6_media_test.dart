import 'dart:convert';
import 'dart:io';
import 'dart:typed_data';

import 'package:flutter_test/flutter_test.dart';
import 'package:image/image.dart' as img;
import 'package:mootool_next_flutter/app/app_controller.dart';
import 'package:mootool_next_flutter/app/app_paths.dart';
import 'package:mootool_next_flutter/core/samples/simple_pdf.dart';
import 'package:mootool_next_flutter/features/image/image_library.dart';
import 'package:mootool_next_flutter/features/image/image_process.dart';
import 'package:mootool_next_flutter/features/image/image_tools.dart';
import 'package:mootool_next_flutter/features/message_board/message_board.dart';
import 'package:mootool_next_flutter/features/pdf/page_ranges.dart';
import 'package:mootool_next_flutter/features/pdf/pdf_service.dart';
import 'package:mootool_next_flutter/features/pdf/pdf_session.dart';

Uint8List _solidPng(
    {int width = 16, int height = 16, int r = 0, int g = 0, int b = 0}) {
  final image = img.Image(width: width, height: height);
  img.fill(image, color: img.ColorRgb8(r, g, b));
  return Uint8List.fromList(img.encodePng(image));
}

void main() {
  test('message board clamps UTF-16 length like Electron', () {
    final session = MessageBoardSession();
    session.message = session.clampMessage('a' * 90);
    expect(session.message.length, maxMessageLength);
    session.applyPreset('closed', '暂停营业');
    expect(session.theme, 'coral');
    session.restore({
      'message': 'x' * 12,
      'theme': 'midnight',
      'alignment': 'left',
      'size': 125
    });
    expect(session.alignment, 'left');
    expect(session.size, 125);
    expect(session.theme, 'midnight');
  });

  test('PDF page ranges match Electron fixtures', () {
    expect(parsePageSelection('1-3;2;7;9-10', 10), [1, 2, 3, 7, 9, 10]);
    expect(parsePageSelection('1-2, 4，6', 10), [1, 2, 4, 6]);
    expect(() => parsePageSelection('3-1', 5), throwsA(isA<FormatException>()));
    expect(
        () => parsePageSelection('1;;2', 5), throwsA(isA<FormatException>()));
    expect(() => parsePageSelection('1-6', 5), throwsA(isA<FormatException>()));
    expect(selectSplitPages('2-8', 'odd', '', 10), [3, 5, 7]);
    expect(selectSplitPages('2-8', 'even', '', 10), [2, 4, 6, 8]);
    expect(selectSplitPages('2-8', 'custom', '1;3-4;8-10', 10), [3, 4, 8]);
  });

  test('SimplePdf split and merge keep user order and reject foreign PDFs', () {
    final root = Directory.systemTemp.createTempSync('mootool-pdf-');
    addTearDown(() => root.deleteSync(recursive: true));
    final source = File('${root.path}/Sample.PDF')
      ..writeAsBytesSync(SimplePdf(['one', 'two', 'three', 'four']).encode());
    final inspected = inspectPdfFile(source);
    expect(inspected.pageCount, 4);
    inspected.rule = 'odd';
    inspected.pageRange = '1-4';
    final split =
        splitPdfTasks([inspected], outputDirectory: '${root.path}/out');
    expect(split.pageCount, 2);
    expect(SimplePdf.parse(File(split.outputs.single).readAsBytesSync()).pages,
        ['one', 'three']);
    final second = File('${root.path}/b.pdf')
      ..writeAsBytesSync(SimplePdf(['x', 'y']).encode());
    final merged = mergePdfTasks([
      inspectPdfFile(source)..pageRange = '2;4',
      inspectPdfFile(second)..pageRange = '1-2',
    ], '${root.path}/out/merged.pdf');
    expect(SimplePdf.parse(File(merged.outputs.single).readAsBytesSync()).pages,
        ['two', 'four', 'x', 'y']);
    expect(() => SimplePdf.parse(Uint8List.fromList([1, 2, 3, 4])),
        throwsA(isA<FormatException>()));
  });

  test('image helpers match Electron fixtures', () {
    expect(scaledDimensions(1200, 800, 0.5), (width: 600, height: 400));
    expect(scaledDimensions(3, 3, 0), (width: 1, height: 1));
    expect(
        processedImageName('photo.jpg', 'compressed'), 'photo_compressed.jpg');
    expect(processedImageName('logo.png', 'watermarked', 'jpeg'),
        'logo_watermarked.jpg');
    expect(ensureImageDataUrl('YWJj'), 'data:image/png;base64,YWJj');
    expect(ensureImageDataUrl('data:image/jpeg;base64,YWJj'),
        'data:image/jpeg;base64,YWJj');
    expect(watermarkAnchor(1000, 600, 200, 40, 'bottom-right', 20),
        (x: 780.0, y: 580.0));
    expect(watermarkAnchor(1000, 600, 200, 40, 'center', 20),
        (x: 400.0, y: 320.0));
  });

  test('image library compress watermark and SVG stay in the product data dir',
      () async {
    final root = Directory.systemTemp.createTempSync('mootool-img-');
    addTearDown(() => root.deleteSync(recursive: true));
    final library = ImageLibrary(Directory('${root.path}/images'));
    final original = await library.save(
        name: 'block.png', bytes: _solidPng(width: 40, height: 20, r: 200));
    expect(original.width, 40);
    final compressed = compressImageBytes(
        original.bytes, CompressImageOptions(scale: 0.5, format: 'png'));
    final decoded = img.decodeImage(compressed)!;
    expect(decoded.width, 20);
    expect(decoded.height, 10);
    expect(compressed.length, isNot(original.size));
    final marked =
        watermarkImageBytes(original.bytes, WatermarkImageOptions(text: 'Moo'));
    expect(marked, isNot(original.bytes));
    final svg = vectorizeImage(_solidPng(), VectorizeOptions(preset: 'bw'));
    expect(svg.contains('<path'), isTrue);
    expect(svg.contains('<image'), isFalse);
    expect(svg.contains('data:image/'), isFalse);
    await library.save(
        name: 'block_svg.svg', bytes: Uint8List.fromList(utf8.encode(svg)));
    expect((await library.list()).map((item) => item.name),
        contains('block_svg.svg'));
    await expectLater(
        library.save(name: '../escape.png', bytes: original.bytes),
        throwsA(isA<FormatException>()));
  });

  test('message board and pdf session survive persist', () async {
    final root = Directory.systemTemp.createTempSync('mootool-p6-');
    addTearDown(() => root.deleteSync(recursive: true));
    final first = AppController(AppPaths(root));
    addTearDown(first.dispose);
    await first.load();
    first.messageBoard.message = '暂停营业';
    first.messageBoard.theme = 'coral';
    first.messageBoard.alignment = 'left';
    first.messageBoard.size = 110;
    first.pdf.tab = 'merge';
    first.pdf.splitRows.add(
        PdfTaskRow(path: '/tmp/a.pdf', name: 'a.pdf', size: 12, pageCount: 2));
    await first.persist();

    final second = AppController(AppPaths(root));
    addTearDown(second.dispose);
    await second.load();
    expect(second.messageBoard.message, '暂停营业');
    expect(second.messageBoard.theme, 'coral');
    expect(second.messageBoard.size, 110);
    expect(second.pdf.tab, 'merge');
    expect(second.pdf.splitRows.single.name, 'a.pdf');
  });
}
