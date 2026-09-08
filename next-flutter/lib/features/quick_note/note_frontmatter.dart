class NoteMetadata {
  NoteMetadata({
    this.title = '',
    this.syntax = 'text/markdown',
    this.fontName = '',
    this.fontSize = 14,
    this.lineSpacing = 1.0,
    this.lineWrap = true,
    this.color = '',
  });

  String title;
  String syntax;
  String fontName;
  double fontSize;
  double lineSpacing;
  bool lineWrap;
  String color;

  Map<String, Object?> toJson() => {
        'title': title,
        'syntax': syntax,
        'fontName': fontName,
        'fontSize': fontSize,
        'lineSpacing': lineSpacing,
        'lineWrap': lineWrap,
        'color': color,
      };

  factory NoteMetadata.fromJson(Map<String, Object?> json) => NoteMetadata(
        title: json['title'] as String? ?? '',
        syntax: json['syntax'] as String? ?? 'text/markdown',
        fontName: json['fontName'] as String? ?? '',
        fontSize: (json['fontSize'] as num?)?.toDouble() ?? 14,
        lineSpacing: (json['lineSpacing'] as num?)?.toDouble() ?? 1,
        lineWrap: json['lineWrap'] as bool? ?? true,
        color: json['color'] as String? ?? '',
      );
}

class NoteDocument {
  const NoteDocument({required this.metadata, required this.body});
  final NoteMetadata metadata;
  final String body;
}

NoteDocument parseNoteDocument(String source) {
  final normalized = source.replaceAll('\r\n', '\n');
  if (!normalized.startsWith('---\n')) {
    return NoteDocument(metadata: NoteMetadata(), body: source);
  }
  final end = normalized.indexOf('\n---\n', 4);
  if (end < 0) {
    return NoteDocument(metadata: NoteMetadata(), body: source);
  }
  final raw = normalized.substring(4, end);
  final body = normalized.substring(end + 5);
  final metadata = NoteMetadata();
  for (final line in raw.split('\n')) {
    final separator = line.indexOf(':');
    if (separator < 0) continue;
    final key = line.substring(0, separator).trim();
    var value = line.substring(separator + 1).trim();
    if (value.length >= 2 && value.startsWith('"') && value.endsWith('"')) {
      value = value.substring(1, value.length - 1);
    }
    switch (key) {
      case 'title':
        metadata.title = value;
      case 'syntax':
        metadata.syntax = value;
      case 'font_name':
        metadata.fontName = value;
      case 'font_size':
        metadata.fontSize = double.tryParse(value) ?? metadata.fontSize;
      case 'line_spacing':
        metadata.lineSpacing = double.tryParse(value) ?? metadata.lineSpacing;
      case 'line_wrap':
        metadata.lineWrap = value != '0' && value.toLowerCase() != 'false';
      case 'color':
        metadata.color = value;
    }
  }
  return NoteDocument(metadata: metadata, body: body);
}

String serializeNoteDocument(NoteDocument document) {
  final meta = document.metadata;
  final wrap = meta.lineWrap ? '1' : '0';
  final spacing = meta.lineSpacing == meta.lineSpacing.roundToDouble()
      ? meta.lineSpacing.toStringAsFixed(1)
      : meta.lineSpacing.toString();
  final size = meta.fontSize == meta.fontSize.roundToDouble()
      ? meta.fontSize.round().toString()
      : meta.fontSize.toString();
  return '---\n'
      'title: ${meta.title}\n'
      'syntax: ${meta.syntax}\n'
      'font_name: ${meta.fontName}\n'
      'font_size: "$size"\n'
      'line_spacing: "$spacing"\n'
      'line_wrap: "$wrap"\n'
      '---\n'
      '${document.body}';
}
