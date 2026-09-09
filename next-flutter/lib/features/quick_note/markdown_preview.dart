import 'package:flutter/material.dart';

import '../../design/theme.dart';
import 'note_attachments.dart';

class MarkdownPreview extends StatelessWidget {
  const MarkdownPreview({
    super.key,
    required this.source,
    this.noteId,
    this.attachments,
    this.onOpenLink,
  });

  final String source;
  final String? noteId;
  final NoteAttachmentStore? attachments;
  final ValueChanged<String>? onOpenLink;

  @override
  Widget build(BuildContext context) {
    final tokens = tokensOf(context);
    final sanitized = sanitizeMarkdownSource(source);
    return ListView(
      padding: const EdgeInsets.all(16),
      children: [
        for (final block in _blocks(sanitized))
          Padding(
            padding: const EdgeInsets.only(bottom: 10),
            child: _block(block, tokens),
          ),
      ],
    );
  }

  Widget _block(_MdBlock block, MooTokens tokens) {
    switch (block.type) {
      case 'h1':
        return Text(block.text,
            style: const TextStyle(fontSize: 22, fontWeight: FontWeight.w700));
      case 'h2':
        return Text(block.text,
            style: const TextStyle(fontSize: 18, fontWeight: FontWeight.w600));
      case 'h3':
        return Text(block.text,
            style: const TextStyle(fontSize: 16, fontWeight: FontWeight.w600));
      case 'code':
        return DecoratedBox(
          decoration: BoxDecoration(
              color: tokens.surface, border: Border.all(color: tokens.border)),
          child: Padding(
            padding: const EdgeInsets.all(10),
            child: Text(block.text,
                style: const TextStyle(fontFamily: 'monospace', fontSize: 13)),
          ),
        );
      case 'quote':
        return DecoratedBox(
          decoration: BoxDecoration(
              border: Border(left: BorderSide(color: tokens.accent, width: 3))),
          child: Padding(
            padding: const EdgeInsets.only(left: 10),
            child:
                Text(block.text, style: TextStyle(color: tokens.textSecondary)),
          ),
        );
      case 'hr':
        return Divider(color: tokens.border);
      case 'ul':
      case 'ol':
      case 'task':
        return Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            for (final item in block.items)
              Padding(
                padding: const EdgeInsets.only(bottom: 4),
                child: Text(item, style: const TextStyle(height: 1.45)),
              ),
          ],
        );
      case 'table':
        return Table(
          border: TableBorder.all(color: tokens.border),
          children: [
            for (final row in block.rows)
              TableRow(children: [
                for (final cell in row)
                  Padding(
                      padding: const EdgeInsets.all(6),
                      child: Text(cell, style: const TextStyle(fontSize: 13)))
              ])
          ],
        );
      case 'image':
        final file = noteId != null && attachments != null
            ? attachments!.resolve(noteId!, block.text)
            : null;
        if (file != null) {
          return Image.file(file, height: 180, fit: BoxFit.contain);
        }
        return Text('![${block.alt}](${block.text})',
            style: TextStyle(color: tokens.textSecondary));
      default:
        return _inline(block.text, tokens);
    }
  }

  Widget _inline(String text, MooTokens tokens) {
    final spans = <InlineSpan>[];
    final expression = RegExp(
        r'`([^`]+)`|\*\*([^*]+)\*\*|\*([^*]+)\*|\[([^\]]+)\]\(([^)]+)\)|!\[([^\]]*)\]\(([^)]+)\)');
    var index = 0;
    for (final match in expression.allMatches(text)) {
      if (match.start > index) {
        spans.add(TextSpan(text: text.substring(index, match.start)));
      }
      if (match[1] != null) {
        spans.add(TextSpan(
            text: match[1], style: const TextStyle(fontFamily: 'monospace')));
      } else if (match[2] != null) {
        spans.add(TextSpan(
            text: match[2],
            style: const TextStyle(fontWeight: FontWeight.w700)));
      } else if (match[3] != null) {
        spans.add(TextSpan(
            text: match[3],
            style: const TextStyle(fontStyle: FontStyle.italic)));
      } else if (match[4] != null) {
        spans.add(WidgetSpan(
            child: GestureDetector(
          onTap: () => onOpenLink?.call(match[5]!),
          child: Text(match[4]!,
              style: TextStyle(
                  color: tokens.accent, decoration: TextDecoration.underline)),
        )));
      } else if (match[7] != null) {
        spans.add(TextSpan(text: match[6] ?? 'image'));
      }
      index = match.end;
    }
    if (index < text.length) spans.add(TextSpan(text: text.substring(index)));
    return Text.rich(TextSpan(
        style: const TextStyle(height: 1.5, fontSize: 14), children: spans));
  }
}

String sanitizeMarkdownSource(String source) => source
    .replaceAll(RegExp(r'<script[\s\S]*?</script>', caseSensitive: false), '')
    .replaceAll(RegExp(r'<iframe[\s\S]*?</iframe>', caseSensitive: false), '');

class _MdBlock {
  _MdBlock(this.type,
      {this.text = '',
      this.items = const [],
      this.rows = const [],
      this.alt = ''});
  final String type;
  final String text;
  final List<String> items;
  final List<List<String>> rows;
  final String alt;
}

List<_MdBlock> _blocks(String source) {
  final blocks = <_MdBlock>[];
  final lines = source.replaceAll('\r\n', '\n').split('\n');
  var index = 0;
  while (index < lines.length) {
    final line = lines[index];
    if (line.startsWith('```')) {
      final code = <String>[];
      index += 1;
      while (index < lines.length && !lines[index].startsWith('```')) {
        code.add(lines[index]);
        index += 1;
      }
      blocks.add(_MdBlock('code', text: code.join('\n')));
      index += 1;
      continue;
    }
    if (RegExp(r'^#{1,3} ').hasMatch(line)) {
      final level = line.startsWith('###')
          ? 'h3'
          : line.startsWith('##')
              ? 'h2'
              : 'h1';
      blocks.add(
          _MdBlock(level, text: line.replaceFirst(RegExp(r'^#{1,3} '), '')));
      index += 1;
      continue;
    }
    if (line.trim() == '---' || line.trim() == '***') {
      blocks.add(_MdBlock('hr'));
      index += 1;
      continue;
    }
    if (line.startsWith('> ')) {
      blocks.add(_MdBlock('quote', text: line.substring(2)));
      index += 1;
      continue;
    }
    if (RegExp(r'^[-*] \[[ xX]\] ').hasMatch(line)) {
      final items = <String>[];
      while (index < lines.length &&
          RegExp(r'^[-*] \[[ xX]\] ').hasMatch(lines[index])) {
        final checked = RegExp(r'^[-*] \[[xX]\] ').hasMatch(lines[index]);
        items.add(
            '${checked ? '☑' : '☐'} ${lines[index].replaceFirst(RegExp(r'^[-*] \[[ xX]\] '), '')}');
        index += 1;
      }
      blocks.add(_MdBlock('task', items: items));
      continue;
    }
    if (RegExp(r'^[-*] ').hasMatch(line)) {
      final items = <String>[];
      while (index < lines.length && RegExp(r'^[-*] ').hasMatch(lines[index])) {
        items.add('• ${lines[index].substring(2)}');
        index += 1;
      }
      blocks.add(_MdBlock('ul', items: items));
      continue;
    }
    if (RegExp(r'^\d+\. ').hasMatch(line)) {
      final items = <String>[];
      var number = 1;
      while (
          index < lines.length && RegExp(r'^\d+\. ').hasMatch(lines[index])) {
        items.add(
            '$number. ${lines[index].replaceFirst(RegExp(r'^\d+\. '), '')}');
        number += 1;
        index += 1;
      }
      blocks.add(_MdBlock('ol', items: items));
      continue;
    }
    if (line.contains('|') &&
        index + 1 < lines.length &&
        RegExp(r'^\s*\|?[\s:|-]+\|').hasMatch(lines[index + 1])) {
      List<String> cells(String row) => [
            for (final cell in row.split('|'))
              if (cell.trim().isNotEmpty) cell.trim()
          ];
      final rows = [cells(line)];
      index += 2;
      while (index < lines.length && lines[index].contains('|')) {
        rows.add(cells(lines[index]));
        index += 1;
      }
      blocks.add(_MdBlock('table', rows: rows));
      continue;
    }
    final image = RegExp(r'^!\[([^\]]*)\]\(([^)]+)\)$').firstMatch(line.trim());
    if (image != null) {
      blocks.add(_MdBlock('image', text: image[2]!, alt: image[1] ?? ''));
      index += 1;
      continue;
    }
    if (line.trim().isEmpty) {
      index += 1;
      continue;
    }
    blocks.add(_MdBlock('p', text: line));
    index += 1;
  }
  return blocks;
}
