class TextRangeValue {
  const TextRangeValue(this.start, this.end);
  final int start;
  final int end;
  int get length => end - start;
  bool get isCollapsed => start == end;
}

class ColumnSelection {
  const ColumnSelection(
      {required this.startLine,
      required this.startColumn,
      required this.endLine,
      required this.endColumn});
  final int startLine;
  final int startColumn;
  final int endLine;
  final int endColumn;
  int get top => startLine < endLine ? startLine : endLine;
  int get bottom => startLine < endLine ? endLine : startLine;
  int get left => startColumn < endColumn ? startColumn : endColumn;
  int get right => startColumn < endColumn ? endColumn : startColumn;
}

class EditorSnapshot {
  const EditorSnapshot(
      {required this.text,
      required this.selectionStart,
      required this.selectionEnd,
      this.column});
  final String text;
  final int selectionStart;
  final int selectionEnd;
  final ColumnSelection? column;
}

class EditorDocument {
  EditorDocument(
      {String text = '',
      int selectionStart = 0,
      int selectionEnd = 0,
      this.id = 'untitled'})
      : _text = text,
        _selectionStart = selectionStart,
        _selectionEnd = selectionEnd;

  final String id;
  String _text;
  int _selectionStart;
  int _selectionEnd;
  ColumnSelection? column;
  int revision = 0;
  int savedRevision = 0;
  double scrollX = 0;
  double scrollY = 0;
  final List<EditorSnapshot> _undo = [];
  final List<EditorSnapshot> _redo = [];

  String get text => _text;
  int get selectionStart => _selectionStart;
  int get selectionEnd => _selectionEnd;
  bool get dirty => revision != savedRevision;
  bool get canUndo => _undo.isNotEmpty;
  bool get canRedo => _redo.isNotEmpty;
  bool get composingBlocked => false;

  void markSaved() => savedRevision = revision;

  void resetHistory() {
    _undo.clear();
    _redo.clear();
  }

  TextRangeValue get selection => TextRangeValue(
        _selectionStart <= _selectionEnd ? _selectionStart : _selectionEnd,
        _selectionStart <= _selectionEnd ? _selectionEnd : _selectionStart,
      );

  void restoreView(
      {required int start,
      required int end,
      double? scrollX,
      double? scrollY}) {
    _selectionStart = start.clamp(0, _text.length);
    _selectionEnd = end.clamp(0, _text.length);
    if (scrollX != null) this.scrollX = scrollX;
    if (scrollY != null) this.scrollY = scrollY;
  }

  void apply(String next,
      {int? selectionStart, int? selectionEnd, bool recordUndo = true}) {
    if (recordUndo) {
      _undo.add(_snapshot());
      _redo.clear();
    }
    _text = next;
    _selectionStart = (selectionStart ?? next.length).clamp(0, next.length);
    _selectionEnd = (selectionEnd ?? _selectionStart).clamp(0, next.length);
    column = null;
    revision += 1;
  }

  void transformSelectionOrAll(String Function(String value) transform) {
    final range = selection;
    if (range.isCollapsed) {
      final next = transform(_text);
      apply(next, selectionStart: 0, selectionEnd: 0);
      return;
    }
    final next = transform(_text.substring(range.start, range.end));
    apply(
      '${_text.substring(0, range.start)}$next${_text.substring(range.end)}',
      selectionStart: range.start,
      selectionEnd: range.start + next.length,
    );
  }

  void deleteInColumn() {
    final box = column;
    if (box == null) return;
    final lines = _text.split('\n');
    for (var line = box.top; line <= box.bottom && line < lines.length; line++) {
      final start = box.left.clamp(0, lines[line].length);
      final end = box.right.clamp(start, lines[line].length);
      lines[line] = '${lines[line].substring(0, start)}${lines[line].substring(end)}';
    }
    apply(lines.join('\n'),
        selectionStart: _offset(lines, box.top, box.left),
        selectionEnd: _offset(lines, box.bottom, box.left));
    column = ColumnSelection(
      startLine: box.startLine,
      startColumn: box.left,
      endLine: box.endLine,
      endColumn: box.left,
    );
  }

  void setColumnFromSelection() {
    final start = _lineColumn(selection.start);
    final end = _lineColumn(selection.end);
    column = ColumnSelection(
      startLine: start.$1,
      startColumn: start.$2,
      endLine: end.$1,
      endColumn: end.$2,
    );
  }

  (int, int) _lineColumn(int offset) {
    final lines = _text.split('\n');
    var remaining = offset.clamp(0, _text.length);
    for (var i = 0; i < lines.length; i++) {
      if (remaining <= lines[i].length) return (i, remaining);
      remaining -= lines[i].length + 1;
    }
    return (lines.length - 1, lines.isEmpty ? 0 : lines.last.length);
  }

  void replaceSelection(String insertion) {
    final range = selection;
    apply(
      '${_text.substring(0, range.start)}$insertion${_text.substring(range.end)}',
      selectionStart: range.start + insertion.length,
      selectionEnd: range.start + insertion.length,
    );
  }

  /// Insert the same text at the left edge of each line in a rectangular selection.
  void insertInColumn(String insertion) {
    final box = column;
    if (box == null) {
      replaceSelection(insertion);
      return;
    }
    final lines = _text.split('\n');
    for (var line = box.top;
        line <= box.bottom && line < lines.length;
        line++) {
      final columnIndex = box.left.clamp(0, lines[line].length);
      lines[line] =
          '${lines[line].substring(0, columnIndex)}$insertion${lines[line].substring(columnIndex)}';
    }
    final next = lines.join('\n');
    apply(next,
        selectionStart: _offset(lines, box.top, box.left + insertion.length),
        selectionEnd: _offset(lines, box.bottom, box.left + insertion.length));
    column = ColumnSelection(
      startLine: box.startLine,
      startColumn: box.startColumn + insertion.length,
      endLine: box.endLine,
      endColumn: box.endColumn + insertion.length,
    );
  }

  bool undo() {
    if (_undo.isEmpty) return false;
    _redo.add(_snapshot());
    _restore(_undo.removeLast());
    return true;
  }

  bool redo() {
    if (_redo.isEmpty) return false;
    _undo.add(_snapshot());
    _restore(_redo.removeLast());
    return true;
  }

  void isolateFrom(EditorDocument other) {
    if (identical(this, other) || id == other.id) return;
    // Separate undo stacks are already per document; this documents the contract.
  }

  Map<String, Object?> toViewJson() => {
        'id': id,
        'text': _text,
        'selectionStart': _selectionStart,
        'selectionEnd': _selectionEnd,
        'revision': revision,
        'savedRevision': savedRevision,
        'scrollX': scrollX,
        'scrollY': scrollY,
      };

  EditorSnapshot _snapshot() => EditorSnapshot(
      text: _text,
      selectionStart: _selectionStart,
      selectionEnd: _selectionEnd,
      column: column);

  void _restore(EditorSnapshot snapshot) {
    _text = snapshot.text;
    _selectionStart = snapshot.selectionStart.clamp(0, _text.length);
    _selectionEnd = snapshot.selectionEnd.clamp(0, _text.length);
    column = snapshot.column;
    revision += 1;
  }

  static int _offset(List<String> lines, int line, int column) {
    var offset = 0;
    for (var i = 0; i < line && i < lines.length; i++) {
      offset += lines[i].length + 1;
    }
    if (line >= lines.length) return offset;
    return offset + column.clamp(0, lines[line].length);
  }
}
