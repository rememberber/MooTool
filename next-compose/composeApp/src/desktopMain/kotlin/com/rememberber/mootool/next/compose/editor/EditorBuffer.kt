package com.rememberber.mootool.next.compose.editor

import java.util.ArrayDeque
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener
import javax.swing.text.Document
import javax.swing.undo.CompoundEdit
import javax.swing.undo.UndoManager
import com.rememberber.mootool.next.compose.domain.DiffSegment
import com.rememberber.mootool.next.compose.domain.DiffSegmentType
import com.rememberber.mootool.next.compose.ui.theme.EditorPalette
import com.rememberber.mootool.next.compose.ui.theme.toAwtColor
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea
import org.fife.ui.rsyntaxtextarea.SyntaxConstants
import org.fife.ui.rsyntaxtextarea.SyntaxScheme
import org.fife.ui.rsyntaxtextarea.TokenTypes
import org.fife.ui.rtextarea.RTextScrollPane

data class EditorSnapshot(
    val text: String,
    val caret: Int,
    val selectionStart: Int,
    val selectionEnd: Int,
    val revision: Long
)

class EditorBuffer(
    initial: String = "",
    var syntax: String = SyntaxConstants.SYNTAX_STYLE_JSON
) {
    val area: RSyntaxTextArea = RSyntaxTextArea(initial).apply {
        syntaxEditingStyle = syntax
        isCodeFoldingEnabled = true
        tabSize = 2
        antiAliasingEnabled = true
        markOccurrences = true
        isAutoIndentEnabled = true
        enableInputMethods(true)
    }
    val scrollPane: RTextScrollPane = RTextScrollPane(area, true)
    val undoManager: UndoManager = UndoManager().apply { limit = 400 }
    val columnEdits: ColumnEditBinder
    var revision: Long = 0
        private set
    var dirty: Boolean = false
        private set
    private var grouping: CompoundEdit? = null
    private val findHighlights = mutableListOf<Any>()
    private val diffHighlights = mutableListOf<Any>()
    private var suppressUserDocumentChange = 0

    /** 用户直接改文档时触发；[setText] 等批量加载不触发。 */
    var onUserDocumentChange: (() -> Unit)? = null

    init {
        area.document.addUndoableEditListener { event ->
            val group = grouping
            if (group != null) group.addEdit(event.edit) else undoManager.addEdit(event.edit)
        }
        area.document.addDocumentListener(object : DocumentListener {
            override fun insertUpdate(e: DocumentEvent) = notifyDocumentChanged()
            override fun removeUpdate(e: DocumentEvent) = notifyDocumentChanged()
            override fun changedUpdate(e: DocumentEvent) = Unit
        })
        columnEdits = ColumnEditBinder(this)
    }

    val text: String get() = area.text.orEmpty()
    val document: Document get() = area.document

    fun setText(value: String, recordUndo: Boolean) {
        suppressUserDocumentChange++
        try {
            if (!recordUndo) {
                undoManager.discardAllEdits()
                area.text = value
                undoManager.discardAllEdits()
            } else {
                atomicDocument {
                    val length = area.document.length
                    if (length > 0) area.document.remove(0, length)
                    area.document.insertString(0, value, null)
                }
            }
        } finally {
            suppressUserDocumentChange--
        }
        markChanged()
    }

    fun replaceRange(start: Int, end: Int, value: String) {
        atomicDocument {
            area.document.remove(start, end - start)
            area.document.insertString(start, value, null)
        }
        area.caretPosition = start + value.length
        markChanged()
    }

    fun replaceAllText(value: String) {
        atomicDocument {
            val length = area.document.length
            if (length > 0) area.document.remove(0, length)
            area.document.insertString(0, value, null)
        }
        markChanged()
    }

    fun select(start: Int, end: Int) {
        val boundedStart = start.coerceIn(0, text.length)
        val boundedEnd = end.coerceIn(0, text.length)
        area.select(boundedStart, boundedEnd)
        area.caretPosition = boundedEnd
    }

    fun canUndo(): Boolean = undoManager.canUndo()
    fun canRedo(): Boolean = undoManager.canRedo()

    fun undo() {
        if (undoManager.canUndo()) {
            undoManager.undo()
            markChanged()
        }
    }

    fun redo() {
        if (undoManager.canRedo()) {
            undoManager.redo()
            markChanged()
        }
    }

    fun snapshot(): EditorSnapshot = EditorSnapshot(
        text = text,
        caret = area.caretPosition,
        selectionStart = area.selectionStart,
        selectionEnd = area.selectionEnd,
        revision = revision
    )

    fun restoreSelection(start: Int, end: Int, caret: Int) {
        area.selectionStart = start.coerceIn(0, text.length)
        area.selectionEnd = end.coerceIn(0, text.length)
        area.caretPosition = caret.coerceIn(0, text.length)
    }

    fun applyTheme(
        dark: Boolean,
        fontName: String,
        fontSize: Int,
        wrap: Boolean,
        palette: EditorPalette? = null,
        lineSpacing: Double = 1.0
    ) {
        area.syntaxEditingStyle = syntax
        area.font = java.awt.Font(fontName, java.awt.Font.PLAIN, fontSize)
        area.lineWrap = wrap
        area.wrapStyleWord = wrap
        val fg = palette?.foreground?.toAwtColor() ?: if (dark) java.awt.Color(0xED, 0xED, 0xEE) else java.awt.Color(0x20, 0x21, 0x24)
        val bg = palette?.background?.toAwtColor() ?: if (dark) java.awt.Color(0x1C, 0x1C, 0x1E) else java.awt.Color.WHITE
        val line = palette?.currentLine?.toAwtColor() ?: if (dark) java.awt.Color(0x2A, 0x2A, 0x2E) else java.awt.Color(0xF4, 0xF6, 0xFA)
        val caret = palette?.caret?.toAwtColor() ?: fg
        area.foreground = fg
        area.background = bg
        area.currentLineHighlightColor = line
        area.caretColor = caret
        if (palette != null) {
            applySyntaxScheme(palette)
            area.selectionColor = palette.selection.toAwtColor()
        }
        scrollPane.lineNumbersEnabled = true
        scrollPane.background = bg
        scrollPane.gutter.background = bg
        scrollPane.gutter.lineNumberColor = fg
        applyLineSpacing(lineSpacing)
    }

    private fun applyLineSpacing(spacing: Double) {
        val multiplier = com.rememberber.mootool.next.compose.domain.NoteFrontmatter.clampLineSpacing(spacing)
        if (multiplier <= 1.0) return
        runCatching {
            val field = org.fife.ui.rsyntaxtextarea.RSyntaxTextArea::class.java.getDeclaredField("lineHeight")
            field.isAccessible = true
            val base = area.lineHeight
            val next = kotlin.math.round(base * multiplier).toInt().coerceAtLeast(base)
            field.setInt(area, next)
            area.revalidate()
            scrollPane.revalidate()
            area.repaint()
        }
    }

    private fun applySyntaxScheme(palette: EditorPalette) {
        val scheme = SyntaxScheme(true)
        fun paint(type: Int, color: java.awt.Color) {
            scheme.getStyle(type)?.foreground = color
        }
        val keyword = palette.keyword.toAwtColor()
        val string = palette.string.toAwtColor()
        val number = palette.number.toAwtColor()
        val comment = palette.comment.toAwtColor()
        val property = palette.property.toAwtColor()
        val operator = palette.operator.toAwtColor()
        val function = palette.function.toAwtColor()
        val literal = palette.literal.toAwtColor()
        paint(TokenTypes.RESERVED_WORD, keyword)
        paint(TokenTypes.RESERVED_WORD_2, keyword)
        paint(TokenTypes.DATA_TYPE, function)
        paint(TokenTypes.FUNCTION, function)
        paint(TokenTypes.LITERAL_STRING_DOUBLE_QUOTE, string)
        paint(TokenTypes.LITERAL_CHAR, string)
        paint(TokenTypes.LITERAL_BACKQUOTE, string)
        paint(TokenTypes.LITERAL_NUMBER_DECIMAL_INT, number)
        paint(TokenTypes.LITERAL_NUMBER_FLOAT, number)
        paint(TokenTypes.LITERAL_NUMBER_HEXADECIMAL, number)
        paint(TokenTypes.LITERAL_BOOLEAN, literal)
        paint(TokenTypes.COMMENT_EOL, comment)
        paint(TokenTypes.COMMENT_MULTILINE, comment)
        paint(TokenTypes.COMMENT_DOCUMENTATION, comment)
        paint(TokenTypes.VARIABLE, property)
        paint(TokenTypes.REGEX, property)
        paint(TokenTypes.OPERATOR, operator)
        paint(TokenTypes.SEPARATOR, operator)
        area.syntaxScheme = scheme
    }

    fun applySyntax(value: String) {
        if (syntax == value && area.syntaxEditingStyle == value) return
        syntax = value
        area.syntaxEditingStyle = value
    }

    fun setEditable(value: Boolean) {
        if (area.isEditable == value) return
        area.isEditable = value
    }

    fun markMatches(spans: List<Triple<Int, Int, Boolean>>, matchColor: java.awt.Color, currentColor: java.awt.Color) {
        clearMatches()
        val highlighter = area.highlighter ?: return
        spans.forEach { (start, end, current) ->
            val from = start.coerceIn(0, text.length)
            val to = end.coerceIn(from, text.length)
            if (to > from) {
                val painter = javax.swing.text.DefaultHighlighter.DefaultHighlightPainter(if (current) currentColor else matchColor)
                runCatching { findHighlights += highlighter.addHighlight(from, to, painter) }
            }
        }
    }

    fun clearMatches() {
        val highlighter = area.highlighter ?: return
        findHighlights.forEach { tag -> runCatching { highlighter.removeHighlight(tag) } }
        findHighlights.clear()
    }

    fun markDiffSide(
        segments: List<DiffSegment>,
        side: String,
        added: java.awt.Color,
        removed: java.awt.Color,
        changed: java.awt.Color,
    ) {
        clearDiffHighlights()
        val highlighter = area.highlighter ?: return
        segments.forEach { segment ->
            when (segment.type) {
                DiffSegmentType.Insert -> if (side != "right") return@forEach
                DiffSegmentType.Delete -> if (side != "left") return@forEach
                DiffSegmentType.Change -> Unit
            }
            val from = if (side == "left") segment.leftStart else segment.rightStart
            val to = if (side == "left") segment.leftEnd else segment.rightEnd
            if (from < 0 || to < 0 || to <= from) return@forEach
            val base = when (segment.type) {
                DiffSegmentType.Insert -> added
                DiffSegmentType.Delete -> removed
                DiffSegmentType.Change -> changed
            }
            val color = java.awt.Color(base.red, base.green, base.blue, 96)
            val start = from.coerceIn(0, text.length)
            val end = to.coerceIn(start, text.length)
            if (end <= start) return@forEach
            val painter = javax.swing.text.DefaultHighlighter.DefaultHighlightPainter(color)
            runCatching { diffHighlights += highlighter.addHighlight(start, end, painter) }
        }
    }

    fun clearDiffHighlights() {
        val highlighter = area.highlighter ?: return
        diffHighlights.forEach { tag -> runCatching { highlighter.removeHighlight(tag) } }
        diffHighlights.clear()
    }

    fun requestFocus() {
        area.requestFocusInWindow()
    }

    fun setColumnEditing(enabled: Boolean, dragWithoutAlt: Boolean = false) {
        columnEdits.enabled = enabled
        columnEdits.dragWithoutAlt = dragWithoutAlt
    }

    fun bindAppShortcuts(shortcuts: EditorAppShortcuts) {
        val menu = java.awt.Toolkit.getDefaultToolkit().menuShortcutKeyMaskEx
        bindStroke(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F, menu), ACTION_FIND, shortcuts.onFind)
        bindStroke(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_R, menu), ACTION_FIND, shortcuts.onFind)
        bindStroke(
            javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F, menu or java.awt.event.InputEvent.SHIFT_DOWN_MASK),
            ACTION_FORMAT,
            shortcuts.onFormat
        )
        bindStroke(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_S, menu), ACTION_SAVE, shortcuts.onSave)
        bindStroke(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_ENTER, menu), ACTION_SEND, shortcuts.onSend)
    }

    private fun bindStroke(stroke: javax.swing.KeyStroke, name: String, action: (() -> Unit)?) {
        val input = area.getInputMap(javax.swing.JComponent.WHEN_FOCUSED)
        if (action == null) {
            if (input.get(stroke) == name) input.remove(stroke)
            area.actionMap.remove(name)
            return
        }
        input.put(stroke, name)
        area.actionMap.put(name, object : javax.swing.AbstractAction() {
            override fun actionPerformed(e: java.awt.event.ActionEvent) {
                action.invoke()
            }
        })
    }

    fun installFileDrop(handler: (List<java.io.File>) -> Boolean) {
        val original = area.transferHandler
        area.transferHandler = object : javax.swing.TransferHandler() {
            override fun canImport(support: TransferSupport): Boolean {
                if (support.isDataFlavorSupported(java.awt.datatransfer.DataFlavor.javaFileListFlavor)) return true
                return original?.canImport(support) == true
            }

            override fun importData(support: TransferSupport): Boolean {
                if (support.isDataFlavorSupported(java.awt.datatransfer.DataFlavor.javaFileListFlavor)) {
                    val data = support.transferable.getTransferData(java.awt.datatransfer.DataFlavor.javaFileListFlavor)
                    val files = (data as? List<*>)?.filterIsInstance<java.io.File>().orEmpty()
                    return handler(files)
                }
                return original?.importData(support) == true
            }
        }
    }

    private fun notifyDocumentChanged() {
        markChanged()
        if (suppressUserDocumentChange == 0) onUserDocumentChange?.invoke()
    }

    private fun markChanged() {
        revision += 1
        dirty = true
    }

    private fun atomicDocument(block: () -> Unit) {
        val group = CompoundEdit()
        grouping = group
        try {
            block()
        } finally {
            grouping = null
            group.end()
            undoManager.addEdit(group)
        }
    }

    companion object {
        const val ACTION_FIND = "mootool-find"
        const val ACTION_FORMAT = "mootool-format"
        const val ACTION_SAVE = "mootool-save"
        const val ACTION_SEND = "mootool-send"
    }
}

class EditorOwnership {
    private val transferring = ArrayDeque<String>()

    fun beginTransfer(toolId: String) {
        transferring.add(toolId)
    }

    fun endTransfer(toolId: String) {
        transferring.remove(toolId)
    }

    fun isTransferring(toolId: String): Boolean = transferring.contains(toolId)
}

data class EditorAppShortcuts(
    val onFind: (() -> Unit)? = null,
    val onFormat: (() -> Unit)? = null,
    val onSave: (() -> Unit)? = null,
    val onSend: (() -> Unit)? = null
)

object EditorLimits {
    const val LARGE_DOCUMENT_BYTES = 5 * 1024 * 1024

    fun utf8Size(text: String): Int = text.toByteArray(Charsets.UTF_8).size

    fun exceedsLargeDocument(text: String): Boolean = utf8Size(text) >= LARGE_DOCUMENT_BYTES
}
