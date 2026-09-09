class PdfTaskRow {
  PdfTaskRow({
    required this.path,
    required this.name,
    required this.size,
    required this.pageCount,
    this.selected = true,
    this.pageRange = '',
    this.rule = 'odd',
    this.customRule = '',
    this.status = 'ready',
  });

  final String path;
  final String name;
  final int size;
  final int pageCount;
  bool selected;
  String pageRange;
  String rule;
  String customRule;
  String status;

  Map<String, Object?> toJson() => {
        'path': path,
        'name': name,
        'size': size,
        'pageCount': pageCount,
        'selected': selected,
        'pageRange': pageRange,
        'rule': rule,
        'customRule': customRule,
        'status': status,
      };

  factory PdfTaskRow.fromJson(Map<String, Object?> json) => PdfTaskRow(
        path: json['path'] as String? ?? '',
        name: json['name'] as String? ?? '',
        size: json['size'] as int? ?? 0,
        pageCount: json['pageCount'] as int? ?? 0,
        selected: json['selected'] as bool? ?? true,
        pageRange: json['pageRange'] as String? ?? '',
        rule: json['rule'] as String? ?? 'odd',
        customRule: json['customRule'] as String? ?? '',
        status: json['status'] as String? ?? 'ready',
      );
}

class PdfSession {
  String tab = 'split';
  final List<PdfTaskRow> splitRows = [];
  final List<PdfTaskRow> mergeRows = [];
  final List<String> lastOutputs = [];
  String notice = '';
  bool busy = false;

  Map<String, Object?> toJson() => {
        'tab': tab,
        'splitRows': [for (final row in splitRows) row.toJson()],
        'mergeRows': [for (final row in mergeRows) row.toJson()],
        'lastOutputs': lastOutputs,
        'notice': notice,
      };

  void restore(Map<String, Object?> json) {
    tab = json['tab'] as String? ?? 'split';
    splitRows
      ..clear()
      ..addAll(_rows(json['splitRows']));
    mergeRows
      ..clear()
      ..addAll(_rows(json['mergeRows']));
    lastOutputs
      ..clear()
      ..addAll([
        for (final item in json['lastOutputs'] as List? ?? const []) '$item'
      ]);
    notice = json['notice'] as String? ?? '';
  }

  List<PdfTaskRow> _rows(Object? raw) => [
        for (final item in raw as List? ?? const [])
          if (item is Map) PdfTaskRow.fromJson(Map<String, Object?>.from(item)),
      ];
}
