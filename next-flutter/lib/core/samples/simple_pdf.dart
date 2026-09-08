import 'dart:convert';
import 'dart:typed_data';

/// Deterministic, uncompressed PDF 1.4 writer/reader for page extract/merge.
/// Only documents produced by [SimplePdf] are supported. Arbitrary encrypted
/// or object-stream PDFs are rejected rather than rasterized.
class SimplePdf {
  SimplePdf(this.pages);
  final List<String> pages;

  Uint8List encode() {
    final objects = <String>[];
    objects.add('<< /Type /Catalog /Pages 2 0 R >>');
    final kids = [for (var i = 0; i < pages.length; i++) '${3 + i} 0 R'];
    objects.add(
        '<< /Type /Pages /Kids [${kids.join(' ')}] /Count ${pages.length} >>');
    final contentStart = 3 + pages.length;
    for (var i = 0; i < pages.length; i++) {
      objects.add(
          '<< /Type /Page /Parent 2 0 R /MediaBox [0 0 612 792] /Contents ${contentStart + i} 0 R /Resources << /Font << /F1 ${contentStart + pages.length} 0 R >> >> >>');
    }
    for (final text in pages) {
      final stream = 'BT /F1 18 Tf 72 720 Td (${_escape(text)}) Tj ET';
      objects.add('<< /Length ${stream.length} >>\nstream\n$stream\nendstream');
    }
    objects.add('<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>');
    return _assemble(objects);
  }

  static SimplePdf parse(Uint8List bytes) {
    final source = utf8.decode(bytes, allowMalformed: false);
    if (!source.startsWith('%PDF-1.4') ||
        source.contains('/Encrypt') ||
        source.contains('/ObjStm')) {
      throw const FormatException(
          'Unsupported PDF: only uncompressed SimplePdf documents can be split or merged');
    }
    final texts = RegExp(r'\(([^\\()]*)\) Tj')
        .allMatches(source)
        .map((match) => match.group(1)!)
        .toList();
    if (texts.isEmpty) throw const FormatException('No extractable text pages');
    return SimplePdf(texts);
  }

  static Uint8List split(Uint8List source, List<int> pageIndexes) {
    final pdf = parse(source);
    return SimplePdf([for (final index in pageIndexes) pdf.pages[index]])
        .encode();
  }

  static Uint8List merge(List<Uint8List> documents) {
    final pages = <String>[];
    for (final document in documents) {
      pages.addAll(parse(document).pages);
    }
    return SimplePdf(pages).encode();
  }

  static String _escape(String text) => text
      .replaceAll('\\', r'\\')
      .replaceAll('(', r'\(')
      .replaceAll(')', r'\)');

  static Uint8List _assemble(List<String> objects) {
    final buffer = BytesBuilder();
    buffer.add(utf8.encode('%PDF-1.4\n'));
    final offsets = <int>[0];
    for (var i = 0; i < objects.length; i++) {
      offsets.add(buffer.length);
      buffer.add(utf8.encode('${i + 1} 0 obj\n${objects[i]}\nendobj\n'));
    }
    final xref = buffer.length;
    final lines =
        StringBuffer('xref\n0 ${objects.length + 1}\n0000000000 65535 f \n');
    for (var i = 1; i <= objects.length; i++) {
      lines.write('${offsets[i].toString().padLeft(10, '0')} 00000 n \n');
    }
    buffer.add(utf8.encode(
        '$lines trailer << /Size ${objects.length + 1} /Root 1 0 R >>\nstartxref\n$xref\n%%EOF\n'));
    return buffer.toBytes();
  }
}
