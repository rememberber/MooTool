import 'dart:io';
import 'dart:typed_data';

import 'package:path/path.dart' as p;

import '../../core/samples/simple_pdf.dart';
import 'page_ranges.dart';
import 'pdf_session.dart';

const maxPdfTasks = 20;

class PdfOperationResult {
  PdfOperationResult({required this.outputs, required this.pageCount});
  final List<String> outputs;
  final int pageCount;
}

PdfTaskRow inspectPdfFile(File file) {
  if (p.extension(file.path).toLowerCase() != '.pdf') {
    throw const FormatException('Only PDF files are supported');
  }
  final bytes = file.readAsBytesSync();
  final pdf = SimplePdf.parse(bytes);
  return PdfTaskRow(
    path: file.path,
    name: p.basename(file.path),
    size: bytes.length,
    pageCount: pdf.pages.length,
    pageRange: '1-${pdf.pages.length}',
  );
}

List<PdfTaskRow> appendPdfTasks(
    List<PdfTaskRow> current, List<PdfTaskRow> additions) {
  final existing = {for (final row in current) row.path};
  return [
    ...current,
    for (final row in additions)
      if (!existing.contains(row.path)) row,
  ].take(maxPdfTasks).toList();
}

PdfOperationResult splitPdfTasks(List<PdfTaskRow> tasks,
    {String? outputDirectory}) {
  if (tasks.isEmpty) throw const FormatException('Select at least one PDF task');
  final outputs = <String>[];
  var pageCount = 0;
  for (final task in tasks) {
    final source = File(task.path).readAsBytesSync();
    final pdf = SimplePdf.parse(source);
    final pages = selectSplitPages(
        task.pageRange, task.rule, task.customRule, pdf.pages.length);
    if (pages.isEmpty) {
      throw FormatException('No pages selected for ${task.name}');
    }
    final indexes = [for (final page in pages) page - 1];
    final bytes = SimplePdf.split(source, indexes);
    final directory = outputDirectory ?? p.dirname(task.path);
    Directory(directory).createSync(recursive: true);
    final name =
        '${p.basenameWithoutExtension(task.path).toLowerCase()}_split.pdf';
    final output = File(p.join(directory, name));
    output.writeAsBytesSync(bytes);
    outputs.add(output.path);
    pageCount += indexes.length;
  }
  return PdfOperationResult(outputs: outputs, pageCount: pageCount);
}

PdfOperationResult mergePdfTasks(List<PdfTaskRow> tasks, String outputPath) {
  if (tasks.length < 2) {
    throw const FormatException('Select at least two PDF files');
  }
  final documents = <Uint8List>[];
  var pageCount = 0;
  for (final task in tasks) {
    final source = File(task.path).readAsBytesSync();
    final pdf = SimplePdf.parse(source);
    final range = task.pageRange.isEmpty ? '1-${pdf.pages.length}' : task.pageRange;
    final pages = parsePageSelection(range, pdf.pages.length);
    if (pages.isEmpty) continue;
    documents.add(SimplePdf.split(source, [for (final page in pages) page - 1]));
    pageCount += pages.length;
  }
  if (pageCount == 0) throw const FormatException('No pages selected');
  final output = File(outputPath);
  output.parent.createSync(recursive: true);
  output.writeAsBytesSync(SimplePdf.merge(documents));
  return PdfOperationResult(outputs: [output.path], pageCount: pageCount);
}
