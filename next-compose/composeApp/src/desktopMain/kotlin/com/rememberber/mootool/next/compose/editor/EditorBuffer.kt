package com.rememberber.mootool.next.compose.editor

import java.util.ArrayDeque
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener
import javax.swing.text.Document
import javax.swing.undo.CompoundEdit
import javax.swing.undo.UndoManager
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea
import org.fife.ui.rsyntaxtextarea.SyntaxConstants
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
    }
    val scrollPane: RTextScrollPane = RTextScrollPane(area, true)
    val undoManager: UndoManager = UndoManager().apply { limit = 400 }
    val columnEdits: ColumnEditBinder
    var revision: Long = 0
        private set
    var dirty: Boolean = false
        private set
    private var grouping: CompoundEdit? = null

    init {
        area.document.addUndoableEditListener { event ->
            val group = grouping
            if (group != null) group.addEdit(event.edit) else undoManager.addEdit(event.edit)
        }
        area.document.addDocumentListener(object : DocumentListener {
            override fun insertUpdate(e: DocumentEvent) = markChanged()
            override fun removeUpdate(e: DocumentEvent) = markChanged()
            override fun changedUpdate(e: DocumentEvent) = Unit
        })
        columnEdits = ColumnEditBinder(this)
    }

    val text: String get() = area.text.orEmpty()
    val document: Document get() = area.document

    fun setText(value: String, recordUndo: Boolean) {
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

    fun applyTheme(dark: Boolean, fontName: String, fontSize: Int, wrap: Boolean) {
        area.syntaxEditingStyle = syntax
        area.font = java.awt.Font(fontName, java.awt.Font.PLAIN, fontSize)
        area.lineWrap = wrap
        area.wrapStyleWord = wrap
        if (dark) {
            area.foreground = java.awt.Color(0xED, 0xED, 0xEE)
            area.background = java.awt.Color(0x1C, 0x1C, 0x1E)
            area.currentLineHighlightColor = java.awt.Color(0x2A, 0x2A, 0x2E)
            area.caretColor = java.awt.Color(0xED, 0xED, 0xEE)
        } else {
            area.foreground = java.awt.Color(0x20, 0x21, 0x24)
            area.background = java.awt.Color.WHITE
            area.currentLineHighlightColor = java.awt.Color(0xF4, 0xF6, 0xFA)
            area.caretColor = java.awt.Color(0x20, 0x21, 0x24)
        }
        scrollPane.lineNumbersEnabled = true
    }

    fun requestFocus() {
        area.requestFocusInWindow()
    }

    fun setColumnEditing(enabled: Boolean, dragWithoutAlt: Boolean = false) {
        columnEdits.enabled = enabled
        columnEdits.dragWithoutAlt = dragWithoutAlt
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
