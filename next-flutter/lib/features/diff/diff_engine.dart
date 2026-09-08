class DiffSegment {
  const DiffSegment({
    required this.type,
    required this.leftStart,
    required this.leftEnd,
    required this.rightStart,
    required this.rightEnd,
    required this.wholeLine,
  });
  final String type;
  final int leftStart;
  final int leftEnd;
  final int rightStart;
  final int rightEnd;
  final bool wholeLine;

  Map<String, Object?> toJson() => {
        'type': type,
        'leftStart': leftStart,
        'leftEnd': leftEnd,
        'rightStart': rightStart,
        'rightEnd': rightEnd,
        'wholeLine': wholeLine,
      };

  @override
  bool operator ==(Object other) =>
      other is DiffSegment &&
      other.type == type &&
      other.leftStart == leftStart &&
      other.leftEnd == leftEnd &&
      other.rightStart == rightStart &&
      other.rightEnd == rightEnd &&
      other.wholeLine == wholeLine;

  @override
  int get hashCode =>
      Object.hash(type, leftStart, leftEnd, rightStart, rightEnd, wholeLine);
}

class UnifiedSpan {
  const UnifiedSpan(
      {required this.start, required this.end, required this.type});
  final int start;
  final int end;
  final String type;
}

class UnifiedDiffView {
  const UnifiedDiffView({
    required this.text,
    required this.lineSpans,
    required this.characterSpans,
    required this.characterEventCount,
  });
  final String text;
  final List<UnifiedSpan> lineSpans;
  final List<UnifiedSpan> characterSpans;
  final int characterEventCount;
}

class DiffResult {
  const DiffResult({
    required this.leftText,
    required this.rightText,
    required this.segments,
    required this.unified,
    required this.unifiedView,
    required this.added,
    required this.removed,
    required this.changed,
  });
  final String leftText;
  final String rightText;
  final List<DiffSegment> segments;
  final String unified;
  final UnifiedDiffView unifiedView;
  final int added;
  final int removed;
  final int changed;
}

class _PatchDelta<T> {
  _PatchDelta({
    required this.type,
    required this.sourcePosition,
    required this.targetPosition,
    required this.source,
    required this.target,
  });
  final String type;
  final int sourcePosition;
  final int targetPosition;
  final List<T> source;
  final List<T> target;
}

class _Change<T> {
  _Change({required this.value, this.added = false, this.removed = false});
  final List<T> value;
  final bool added;
  final bool removed;
}

class DiffEngine {
  DiffResult compareText(String left, String right, bool ignoreWhitespace) {
    final leftLines = _splitLines(left);
    final rightLines = _splitLines(right);
    final patch = _buildPatch(leftLines, rightLines);
    final segments = _buildUiSegments(left, right, patch, ignoreWhitespace);
    final unifiedView = _buildUnifiedView(patch, leftLines, ignoreWhitespace);
    var added = 0;
    var removed = 0;
    var changed = 0;
    for (final delta in patch) {
      if (delta.type == 'insert') added += delta.target.length;
      if (delta.type == 'delete') removed += delta.source.length;
      if (delta.type == 'change') {
        final paired = delta.source.length < delta.target.length
            ? delta.source.length
            : delta.target.length;
        changed += paired;
        removed += (delta.source.length - paired).clamp(0, delta.source.length);
        added += (delta.target.length - paired).clamp(0, delta.target.length);
      }
    }
    return DiffResult(
      leftText: left,
      rightText: right,
      segments: segments,
      unified: unifiedView.text,
      unifiedView: unifiedView,
      added: added,
      removed: removed,
      changed: changed,
    );
  }

  List<String> _splitLines(String text) =>
      text.split(RegExp(r'\r\n|[\n\v\f\r\u0085\u2028\u2029]'));

  List<_PatchDelta<T>> _buildPatch<T>(List<T> source, List<T> target) {
    final changes = _diffArrays(source, target);
    final patch = <_PatchDelta<T>>[];
    var sourcePosition = 0;
    var targetPosition = 0;
    var index = 0;
    while (index < changes.length) {
      final change = changes[index];
      if (!change.added && !change.removed) {
        sourcePosition += change.value.length;
        targetPosition += change.value.length;
        index += 1;
        continue;
      }
      final deltaSourcePosition = sourcePosition;
      final deltaTargetPosition = targetPosition;
      final removed = <T>[];
      final added = <T>[];
      while (index < changes.length &&
          (changes[index].added || changes[index].removed)) {
        final edit = changes[index];
        if (edit.removed) {
          removed.addAll(edit.value);
          sourcePosition += edit.value.length;
        } else if (edit.added) {
          added.addAll(edit.value);
          targetPosition += edit.value.length;
        }
        index += 1;
      }
      patch.add(_PatchDelta(
        type: removed.isNotEmpty && added.isNotEmpty
            ? 'change'
            : removed.isNotEmpty
                ? 'delete'
                : 'insert',
        sourcePosition: deltaSourcePosition,
        targetPosition: deltaTargetPosition,
        source: removed,
        target: added,
      ));
    }
    return patch;
  }

  List<_Change<T>> _diffArrays<T>(List<T> source, List<T> target) {
    final n = source.length;
    final m = target.length;
    final dp = List.generate(n + 1, (_) => List<int>.filled(m + 1, 0));
    for (var i = n - 1; i >= 0; i--) {
      for (var j = m - 1; j >= 0; j--) {
        dp[i][j] = source[i] == target[j]
            ? dp[i + 1][j + 1] + 1
            : (dp[i + 1][j] > dp[i][j + 1] ? dp[i + 1][j] : dp[i][j + 1]);
      }
    }
    final changes = <_Change<T>>[];
    var i = 0;
    var j = 0;
    while (i < n && j < m) {
      if (source[i] == target[j]) {
        _push(changes, source[i], added: false, removed: false);
        i += 1;
        j += 1;
      } else if (dp[i + 1][j] >= dp[i][j + 1]) {
        _push(changes, source[i], added: false, removed: true);
        i += 1;
      } else {
        _push(changes, target[j], added: true, removed: false);
        j += 1;
      }
    }
    while (i < n) {
      _push(changes, source[i], added: false, removed: true);
      i += 1;
    }
    while (j < m) {
      _push(changes, target[j], added: true, removed: false);
      j += 1;
    }
    return changes;
  }

  void _push<T>(List<_Change<T>> changes, T value,
      {required bool added, required bool removed}) {
    if (changes.isNotEmpty &&
        changes.last.added == added &&
        changes.last.removed == removed) {
      changes.last.value.add(value);
      return;
    }
    changes.add(_Change(value: [value], added: added, removed: removed));
  }

  List<DiffSegment> _buildUiSegments(String left, String right,
      List<_PatchDelta<String>> patch, bool ignoreWhitespace) {
    final leftLineStarts = _computeLineStartOffsets(left);
    final rightLineStarts = _computeLineStartOffsets(right);
    final segments = <DiffSegment>[];
    for (final delta in patch) {
      if (delta.type == 'delete') {
        for (var index = 0; index < delta.source.length; index++) {
          final line = delta.source[index];
          if (ignoreWhitespace && _isAllWhitespace(line)) continue;
          final start =
              _safeLineStart(leftLineStarts, delta.sourcePosition + index);
          segments.add(DiffSegment(
              type: 'delete',
              leftStart: start,
              leftEnd: start + line.length,
              rightStart: -1,
              rightEnd: -1,
              wholeLine: true));
        }
        continue;
      }
      if (delta.type == 'insert') {
        for (var index = 0; index < delta.target.length; index++) {
          final line = delta.target[index];
          if (ignoreWhitespace && _isAllWhitespace(line)) continue;
          final start =
              _safeLineStart(rightLineStarts, delta.targetPosition + index);
          segments.add(DiffSegment(
              type: 'insert',
              leftStart: -1,
              leftEnd: -1,
              rightStart: start,
              rightEnd: start + line.length,
              wholeLine: true));
        }
        continue;
      }
      final paired = delta.source.length < delta.target.length
          ? delta.source.length
          : delta.target.length;
      for (var index = 0; index < paired; index++) {
        final leftLine = delta.source[index];
        final rightLine = delta.target[index];
        final leftLineStart =
            _safeLineStart(leftLineStarts, delta.sourcePosition + index);
        final rightLineStart =
            _safeLineStart(rightLineStarts, delta.targetPosition + index);
        final characterPatch =
            _buildPatch(leftLine.split(''), rightLine.split(''));
        for (final characterDelta in characterPatch) {
          final leftStart = leftLineStart + characterDelta.sourcePosition;
          final leftEnd = leftStart + characterDelta.source.length;
          final rightStart = rightLineStart + characterDelta.targetPosition;
          final rightEnd = rightStart + characterDelta.target.length;
          if (characterDelta.type == 'delete') {
            final deleted = _safeSlice(leftLine, characterDelta.sourcePosition,
                characterDelta.source.length);
            if (!ignoreWhitespace || !_isAllWhitespace(deleted)) {
              segments.add(DiffSegment(
                  type: 'delete',
                  leftStart: leftStart,
                  leftEnd: leftEnd,
                  rightStart: -1,
                  rightEnd: -1,
                  wholeLine: false));
            }
          } else if (characterDelta.type == 'insert') {
            final inserted = _safeSlice(rightLine,
                characterDelta.targetPosition, characterDelta.target.length);
            if (!ignoreWhitespace || !_isAllWhitespace(inserted)) {
              segments.add(DiffSegment(
                  type: 'insert',
                  leftStart: -1,
                  leftEnd: -1,
                  rightStart: rightStart,
                  rightEnd: rightEnd,
                  wholeLine: false));
            }
          } else {
            final before = _safeSlice(leftLine, characterDelta.sourcePosition,
                characterDelta.source.length);
            final after = _safeSlice(rightLine, characterDelta.targetPosition,
                characterDelta.target.length);
            if (!ignoreWhitespace ||
                !_equalsIgnoringWhitespace(before, after)) {
              segments.add(DiffSegment(
                  type: 'change',
                  leftStart: leftStart,
                  leftEnd: leftEnd,
                  rightStart: rightStart,
                  rightEnd: rightEnd,
                  wholeLine: false));
            }
          }
        }
      }
      for (var index = paired; index < delta.source.length; index++) {
        final line = delta.source[index];
        if (ignoreWhitespace && _isAllWhitespace(line)) continue;
        final start =
            _safeLineStart(leftLineStarts, delta.sourcePosition + index);
        segments.add(DiffSegment(
            type: 'delete',
            leftStart: start,
            leftEnd: start + line.length,
            rightStart: -1,
            rightEnd: -1,
            wholeLine: true));
      }
      for (var index = paired; index < delta.target.length; index++) {
        final line = delta.target[index];
        if (ignoreWhitespace && _isAllWhitespace(line)) continue;
        final start =
            _safeLineStart(rightLineStarts, delta.targetPosition + index);
        segments.add(DiffSegment(
            type: 'insert',
            leftStart: -1,
            leftEnd: -1,
            rightStart: start,
            rightEnd: start + line.length,
            wholeLine: true));
      }
    }
    return segments;
  }

  UnifiedDiffView _buildUnifiedView(List<_PatchDelta<String>> patch,
      List<String> originalLines, bool ignoreWhitespace) {
    final lines = _generateUnifiedDiffLines(originalLines, patch, 3);
    final text = lines.join('\n');
    final lineSpans = <UnifiedSpan>[];
    final characterSpans = <UnifiedSpan>[];
    var characterEventCount = 0;
    var offset = 0;
    var deletedStarts = <int>[];
    var deletedTexts = <String>[];
    var addedStarts = <int>[];
    var addedTexts = <String>[];

    void flush() {
      characterEventCount += _addIntralineSpans(deletedStarts, deletedTexts,
          addedStarts, addedTexts, characterSpans, ignoreWhitespace);
      deletedStarts = [];
      deletedTexts = [];
      addedStarts = [];
      addedTexts = [];
    }

    for (var index = 0; index < lines.length; index++) {
      final line = lines[index];
      final lineStart = offset;
      final lineEnd = lineStart + line.length;
      if (line.startsWith('@@')) {
        flush();
        lineSpans.add(
            UnifiedSpan(start: lineStart, end: lineEnd, type: 'hunk-line'));
      } else if (line.startsWith('---') || line.startsWith('+++')) {
        lineSpans.add(
            UnifiedSpan(start: lineStart, end: lineEnd, type: 'header-line'));
      } else if (line.startsWith('+')) {
        final content = line.substring(1);
        if (!ignoreWhitespace || !_isAllWhitespace(content)) {
          lineSpans.add(
              UnifiedSpan(start: lineStart, end: lineEnd, type: 'add-line'));
        }
        addedStarts.add(lineStart);
        addedTexts.add(content);
      } else if (line.startsWith('-')) {
        final content = line.substring(1);
        if (!ignoreWhitespace || !_isAllWhitespace(content)) {
          lineSpans.add(
              UnifiedSpan(start: lineStart, end: lineEnd, type: 'delete-line'));
        }
        deletedStarts.add(lineStart);
        deletedTexts.add(content);
      }
      offset = index < lines.length - 1 ? lineEnd + 1 : lineEnd;
    }
    flush();
    return UnifiedDiffView(
      text: text,
      lineSpans: lineSpans,
      characterSpans: characterSpans,
      characterEventCount: characterEventCount,
    );
  }

  List<String> _generateUnifiedDiffLines(List<String> originalLines,
      List<_PatchDelta<String>> patch, int contextSize) {
    if (patch.isEmpty) return [];
    final lines = ['--- old', '+++ new'];
    var currentGroup = [patch[0]];
    var previous = patch[0];
    for (var index = 1; index < patch.length; index++) {
      final next = patch[index];
      final closeEnough =
          previous.sourcePosition + previous.source.length + contextSize >=
              next.sourcePosition - contextSize;
      if (closeEnough) {
        currentGroup.add(next);
      } else {
        lines.addAll(
            _processUnifiedGroup(originalLines, currentGroup, contextSize));
        currentGroup = [next];
      }
      previous = next;
    }
    lines
        .addAll(_processUnifiedGroup(originalLines, currentGroup, contextSize));
    return lines;
  }

  List<String> _processUnifiedGroup(List<String> originalLines,
      List<_PatchDelta<String>> deltas, int contextSize) {
    final output = <String>[];
    var originalTotal = 0;
    var revisedTotal = 0;
    var current = deltas[0];
    final originalStart =
        (current.sourcePosition + 1 - contextSize).clamp(1, 1 << 30);
    final revisedStart =
        (current.targetPosition + 1 - contextSize).clamp(1, 1 << 30);
    final contextStart =
        (current.sourcePosition - contextSize).clamp(0, originalLines.length);
    for (var line = contextStart; line < current.sourcePosition; line++) {
      output.add(' ${originalLines[line]}');
      originalTotal += 1;
      revisedTotal += 1;
    }
    output.addAll(_deltaText(current));
    originalTotal += current.source.length;
    revisedTotal += current.target.length;
    for (var index = 1; index < deltas.length; index++) {
      final next = deltas[index];
      final intermediateStart = current.sourcePosition + current.source.length;
      for (var line = intermediateStart; line < next.sourcePosition; line++) {
        output.add(' ${originalLines[line]}');
        originalTotal += 1;
        revisedTotal += 1;
      }
      output.addAll(_deltaText(next));
      originalTotal += next.source.length;
      revisedTotal += next.target.length;
      current = next;
    }
    final trailingStart = current.sourcePosition + current.source.length;
    final trailingEnd = (trailingStart + contextSize) < originalLines.length
        ? trailingStart + contextSize
        : originalLines.length;
    for (var line = trailingStart; line < trailingEnd; line++) {
      output.add(' ${originalLines[line]}');
      originalTotal += 1;
      revisedTotal += 1;
    }
    output.insert(
        0, '@@ -$originalStart,$originalTotal +$revisedStart,$revisedTotal @@');
    return output;
  }

  List<String> _deltaText(_PatchDelta<String> delta) => [
        ...delta.source.map((line) => '-$line'),
        ...delta.target.map((line) => '+$line'),
      ];

  int _addIntralineSpans(
      List<int> deletedStarts,
      List<String> deletedTexts,
      List<int> addedStarts,
      List<String> addedTexts,
      List<UnifiedSpan> output,
      bool ignoreWhitespace) {
    var events = 0;
    final pairs = deletedTexts.length < addedTexts.length
        ? deletedTexts.length
        : addedTexts.length;
    for (var index = 0; index < pairs; index++) {
      final before = deletedTexts[index];
      final after = addedTexts[index];
      final beforeBase = deletedStarts[index] + 1;
      final afterBase = addedStarts[index] + 1;
      for (final delta in _buildPatch(before.split(''), after.split(''))) {
        final beforeText =
            _safeSlice(before, delta.sourcePosition, delta.source.length);
        final afterText =
            _safeSlice(after, delta.targetPosition, delta.target.length);
        if (delta.type == 'delete') {
          if (ignoreWhitespace && _isAllWhitespace(beforeText)) continue;
          output.add(UnifiedSpan(
              start: beforeBase + delta.sourcePosition,
              end: beforeBase + delta.sourcePosition + delta.source.length,
              type: 'delete-character'));
        } else if (delta.type == 'insert') {
          if (ignoreWhitespace && _isAllWhitespace(afterText)) continue;
          output.add(UnifiedSpan(
              start: afterBase + delta.targetPosition,
              end: afterBase + delta.targetPosition + delta.target.length,
              type: 'add-character'));
        } else {
          if (ignoreWhitespace &&
              _equalsIgnoringWhitespace(beforeText, afterText)) continue;
          if (beforeText.isNotEmpty) {
            output.add(UnifiedSpan(
                start: beforeBase + delta.sourcePosition,
                end: beforeBase + delta.sourcePosition + delta.source.length,
                type: 'change-character'));
          }
          if (afterText.isNotEmpty) {
            output.add(UnifiedSpan(
                start: afterBase + delta.targetPosition,
                end: afterBase + delta.targetPosition + delta.target.length,
                type: 'change-character'));
          }
        }
        events += 1;
      }
    }
    return events;
  }

  List<int> _computeLineStartOffsets(String text) {
    final starts = [0];
    for (var index = 0; index < text.length; index++) {
      if (text.codeUnitAt(index) == 10) starts.add(index + 1);
    }
    return starts;
  }

  int _safeLineStart(List<int> starts, int lineIndex) {
    if (lineIndex < 0) return 0;
    if (lineIndex >= starts.length) return starts.isEmpty ? 0 : starts.last;
    return starts[lineIndex];
  }

  String _safeSlice(String value, int position, int length) {
    final start = position.clamp(0, value.length);
    final end = (position + length).clamp(start, value.length);
    return value.substring(start, end);
  }

  bool _isAllWhitespace(String value) {
    for (var index = 0; index < value.length; index++) {
      if (!_isJavaWhitespace(value.codeUnitAt(index))) return false;
    }
    return true;
  }

  bool _equalsIgnoringWhitespace(String left, String right) {
    var leftIndex = 0;
    var rightIndex = 0;
    while (leftIndex < left.length && rightIndex < right.length) {
      final leftCode = left.codeUnitAt(leftIndex);
      final rightCode = right.codeUnitAt(rightIndex);
      if (_isJavaWhitespace(leftCode)) {
        leftIndex += 1;
        continue;
      }
      if (_isJavaWhitespace(rightCode)) {
        rightIndex += 1;
        continue;
      }
      if (leftCode != rightCode) return false;
      leftIndex += 1;
      rightIndex += 1;
    }
    while (leftIndex < left.length) {
      if (!_isJavaWhitespace(left.codeUnitAt(leftIndex))) return false;
      leftIndex += 1;
    }
    while (rightIndex < right.length) {
      if (!_isJavaWhitespace(right.codeUnitAt(rightIndex))) return false;
      rightIndex += 1;
    }
    return true;
  }

  bool _isJavaWhitespace(int code) {
    return (code >= 0x0009 && code <= 0x000d) ||
        (code >= 0x001c && code <= 0x001f) ||
        code == 0x0020 ||
        code == 0x1680 ||
        (code >= 0x2000 && code <= 0x2006) ||
        (code >= 0x2008 && code <= 0x200a) ||
        code == 0x2028 ||
        code == 0x2029 ||
        code == 0x205f ||
        code == 0x3000;
  }
}
