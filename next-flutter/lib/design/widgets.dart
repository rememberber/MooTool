import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import '../core/editor/editor_document.dart';
import 'theme.dart';

class MooCodeEditor extends StatefulWidget {
  const MooCodeEditor({
    super.key,
    required this.document,
    required this.onChanged,
    this.readOnly = false,
    this.wrap = true,
    this.fontSize = 14,
    this.fontFamily,
    this.autofocus = false,
  });

  final EditorDocument document;
  final ValueChanged<String> onChanged;
  final bool readOnly;
  final bool wrap;
  final double fontSize;
  final String? fontFamily;
  final bool autofocus;

  @override
  State<MooCodeEditor> createState() => _MooCodeEditorState();
}

class _MooCodeEditorState extends State<MooCodeEditor> {
  late final TextEditingController _controller;
  late final FocusNode _focus;
  TextRange _composing = TextRange.empty;

  @override
  void initState() {
    super.initState();
    _controller = TextEditingController(text: widget.document.text)
      ..selection = TextSelection(
        baseOffset: widget.document.selectionStart,
        extentOffset: widget.document.selectionEnd,
      );
    _focus = FocusNode();
    _controller.addListener(_onController);
  }

  @override
  void didUpdateWidget(covariant MooCodeEditor oldWidget) {
    super.didUpdateWidget(oldWidget);
    if (oldWidget.document != widget.document ||
        widget.document.text != _controller.text) {
      final composing = _controller.value.composing;
      if (!composing.isValid) {
        _controller.value = TextEditingValue(
          text: widget.document.text,
          selection: TextSelection(
            baseOffset: widget.document.selectionStart
                .clamp(0, widget.document.text.length)
                .toInt(),
            extentOffset: widget.document.selectionEnd
                .clamp(0, widget.document.text.length)
                .toInt(),
          ),
        );
      }
    }
  }

  void _onController() {
    _composing = _controller.value.composing;
    final selection = _controller.selection;
    if (_controller.text != widget.document.text) {
      widget.document.apply(
        _controller.text,
        selectionStart: selection.start,
        selectionEnd: selection.end,
        recordUndo: true,
      );
      widget.onChanged(_controller.text);
    } else {
      widget.document.restoreView(start: selection.start, end: selection.end);
    }
  }

  @override
  void dispose() {
    _controller.removeListener(_onController);
    _controller.dispose();
    _focus.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final tokens = tokensOf(context);
    final lineCount = '\n'.allMatches(widget.document.text).length + 1;
    return Shortcuts(
      shortcuts: {
        LogicalKeySet(LogicalKeyboardKey.meta, LogicalKeyboardKey.keyZ):
            const UndoIntent(),
        LogicalKeySet(LogicalKeyboardKey.control, LogicalKeyboardKey.keyZ):
            const UndoIntent(),
        LogicalKeySet(LogicalKeyboardKey.meta, LogicalKeyboardKey.shift,
            LogicalKeyboardKey.keyZ): const RedoIntent(),
        LogicalKeySet(LogicalKeyboardKey.control, LogicalKeyboardKey.keyY):
            const RedoIntent(),
      },
      child: Actions(
        actions: {
          UndoIntent: CallbackAction<UndoIntent>(onInvoke: (_) {
            if (_composing.isValid) return null;
            widget.document.undo();
            _syncFromDocument();
            widget.onChanged(widget.document.text);
            return null;
          }),
          RedoIntent: CallbackAction<RedoIntent>(onInvoke: (_) {
            if (_composing.isValid) return null;
            widget.document.redo();
            _syncFromDocument();
            widget.onChanged(widget.document.text);
            return null;
          }),
        },
        child: DecoratedBox(
          decoration: BoxDecoration(
              color: tokens.surface, border: Border.all(color: tokens.border)),
          child: Row(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              SizedBox(
                width: 44,
                child: ColoredBox(
                  color: tokens.toolbar,
                  child: ListView.builder(
                    itemCount: lineCount,
                    itemBuilder: (context, index) => SizedBox(
                      height: widget.fontSize * 1.5,
                      child: Align(
                        alignment: Alignment.centerRight,
                        child: Padding(
                          padding: const EdgeInsets.only(right: 8),
                          child: Text('${index + 1}',
                              style: TextStyle(
                                  fontSize: 11,
                                  color: tokens.textSecondary,
                                  fontFamily: 'monospace')),
                        ),
                      ),
                    ),
                  ),
                ),
              ),
              Expanded(
                child: TextField(
                  controller: _controller,
                  focusNode: _focus,
                  autofocus: widget.autofocus,
                  readOnly: widget.readOnly,
                  maxLines: null,
                  expands: true,
                  style: TextStyle(
                      fontFamily: widget.fontFamily ?? 'monospace',
                      fontSize: widget.fontSize,
                      height: 1.5,
                      color: tokens.textPrimary),
                  decoration: const InputDecoration(
                      border: InputBorder.none,
                      contentPadding: EdgeInsets.fromLTRB(12, 8, 12, 8),
                      isCollapsed: false),
                  keyboardType: TextInputType.multiline,
                  textAlignVertical: TextAlignVertical.top,
                  smartDashesType: SmartDashesType.disabled,
                  smartQuotesType: SmartQuotesType.disabled,
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }

  void _syncFromDocument() {
    _controller.value = TextEditingValue(
      text: widget.document.text,
      selection: TextSelection(
        baseOffset: widget.document.selectionStart
            .clamp(0, widget.document.text.length)
            .toInt(),
        extentOffset: widget.document.selectionEnd
            .clamp(0, widget.document.text.length)
            .toInt(),
      ),
    );
  }
}

class UndoIntent extends Intent {
  const UndoIntent();
}

class RedoIntent extends Intent {
  const RedoIntent();
}

class CompactButton extends StatelessWidget {
  const CompactButton(
      {super.key,
      required this.label,
      this.icon,
      this.primary = false,
      this.onPressed,
      this.tooltip});

  final String label;
  final IconData? icon;
  final bool primary;
  final VoidCallback? onPressed;
  final String? tooltip;

  @override
  Widget build(BuildContext context) {
    final tokens = tokensOf(context);
    final child = ConstrainedBox(
      constraints: const BoxConstraints(minHeight: 32, minWidth: 32),
      child: TextButton(
        onPressed: onPressed,
        style: TextButton.styleFrom(
          backgroundColor: primary ? tokens.accent : Colors.transparent,
          foregroundColor: primary ? tokens.onAccent : tokens.textPrimary,
          padding: const EdgeInsets.symmetric(horizontal: 10),
          minimumSize: const Size(32, 32),
          shape: RoundedRectangleBorder(
              borderRadius: BorderRadius.circular(6),
              side: BorderSide(color: primary ? tokens.accent : tokens.border)),
        ),
        child: Row(mainAxisSize: MainAxisSize.min, children: [
          if (icon != null) ...[Icon(icon, size: 14), const SizedBox(width: 6)],
          Text(label, style: const TextStyle(fontSize: 12)),
        ]),
      ),
    );
    return tooltip == null ? child : Tooltip(message: tooltip!, child: child);
  }
}
