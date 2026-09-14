package com.rememberber.mootool.next.compose.editor

import com.rememberber.mootool.next.compose.domain.ColumnEditEngine
import com.rememberber.mootool.next.compose.domain.ColumnRange
import java.awt.Color
import java.awt.Cursor
import java.awt.Graphics
import java.awt.Rectangle
import java.awt.Toolkit
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.StringSelection
import java.awt.event.InputMethodEvent
import java.awt.event.InputMethodListener
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.geom.Point2D
import javax.swing.text.BadLocationException
import javax.swing.text.Highlighter
import javax.swing.text.JTextComponent
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea

class ColumnEditBinder(private val buffer: EditorBuffer) {
    var enabled: Boolean = false
        set(value) {
            field = value
            if (!value) clearSelection()
            updateCursor()
        }
    var dragWithoutAlt: Boolean = false
        set(value) {
            field = value
            updateCursor()
        }

    var selection: ColumnRange? = null
        private set

    private var composing = false
    private var highlightTag: Any? = null
    private val area: RSyntaxTextArea get() = buffer.area
    private val painter = ColumnSelectionPainter()

    fun clearSelection() {
        selection = null
        removeHighlight()
        updateCursor()
    }

    private val mouse = object : MouseAdapter() {
        override fun mousePressed(event: MouseEvent) {
            if (!enabled) return
            if (columnGesture(event)) {
                event.consume()
                val point = pointOf(event)
                selection = ColumnRange(point.line, point.line, point.column, point.column)
                refreshHighlight()
                javax.swing.SwingUtilities.invokeLater {
                    runCatching {
                        val offset = area.viewToModel2D(Point2D.Double(event.x.toDouble(), event.y.toDouble())).coerceAtLeast(0)
                        area.caretPosition = offset
                    }
                }
            } else {
                clearSelection()
            }
        }

        override fun mouseMoved(event: MouseEvent) {
            if (!enabled) return
            area.cursor = if (event.isAltDown || dragWithoutAlt || selection != null) {
                Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR)
            } else {
                Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR)
            }
        }

        override fun mouseDragged(event: MouseEvent) {
            if (!enabled || selection == null) return
            if (!event.isAltDown && !dragWithoutAlt) return
            event.consume()
            val point = pointOf(event)
            val origin = selection ?: return
            selection = origin.copy(endLine = point.line, endColumn = point.column)
            refreshHighlight()
        }

        override fun mouseReleased(event: MouseEvent) {
            if (selection != null && (event.isAltDown || dragWithoutAlt)) event.consume()
        }
    }

    private val keys = object : KeyAdapter() {
        override fun keyPressed(event: KeyEvent) {
            updateCursor()
            val range = selection ?: return
            if (!enabled || composing) return
            when (event.keyCode) {
                KeyEvent.VK_ESCAPE -> {
                    clearSelection()
                    event.consume()
                }
                KeyEvent.VK_BACK_SPACE -> {
                    applyRange(range) { text, tab ->
                        if (range.empty) {
                            if (range.left <= 0) text
                            else ColumnEditEngine.delete(text, range.top, range.bottom, range.left - 1, range.left, tab)
                        } else {
                            ColumnEditEngine.delete(text, range.top, range.bottom, range.left, range.right, tab)
                        }
                    }
                    selection = range.copy(startColumn = range.left, endColumn = range.left, endLine = range.bottom, startLine = range.top)
                    event.consume()
                }
                KeyEvent.VK_DELETE -> {
                    applyRange(range) { text, tab ->
                        if (range.empty) ColumnEditEngine.delete(text, range.top, range.bottom, range.left, range.left + 1, tab)
                        else ColumnEditEngine.delete(text, range.top, range.bottom, range.left, range.right, tab)
                    }
                    selection = range.copy(startColumn = range.left, endColumn = range.left, endLine = range.bottom, startLine = range.top)
                    event.consume()
                }
                KeyEvent.VK_ENTER -> {
                    typeInto(range, "\n")
                    event.consume()
                }
                KeyEvent.VK_C -> if (event.isMetaDown || event.isControlDown) {
                    copyRange(range)
                    event.consume()
                }
                KeyEvent.VK_X -> if (event.isMetaDown || event.isControlDown) {
                    copyRange(range)
                    applyRange(range) { text, tab -> ColumnEditEngine.delete(text, range.top, range.bottom, range.left, range.right, tab) }
                    selection = range.copy(startColumn = range.left, endColumn = range.left, endLine = range.bottom, startLine = range.top)
                    event.consume()
                }
                KeyEvent.VK_V -> if (event.isMetaDown || event.isControlDown) {
                    val clip = clipboardText() ?: return
                    val tab = tabSize()
                    applyDocument { ColumnEditEngine.paste(it, range.top, range.left, clip, tab) }
                    event.consume()
                }
            }
        }

        override fun keyTyped(event: KeyEvent) {
            val range = selection ?: return
            if (!enabled || composing) return
            if (event.isControlDown || event.isMetaDown || event.isAltDown) return
            val char = event.keyChar
            if (char == '\t') {
                typeInto(range, "\t")
                event.consume()
                return
            }
            if (char == KeyEvent.CHAR_UNDEFINED || char.code < 32 || char == KeyEvent.VK_DELETE.toChar()) return
            typeInto(range, char.toString())
            event.consume()
        }

        override fun keyReleased(event: KeyEvent) {
            updateCursor()
        }
    }

    private val ime = object : InputMethodListener {
        override fun inputMethodTextChanged(event: InputMethodEvent) {
            val text = event.text
            composing = text != null && (text.endIndex - text.beginIndex) > event.committedCharacterCount
        }

        override fun caretPositionChanged(event: InputMethodEvent) = Unit
    }

    private fun typeInto(range: ColumnRange, insertion: String) {
        val tab = tabSize()
        applyDocument { ColumnEditEngine.replace(it, range, insertion, tab) }
        var width = 0
        var column = range.left
        var index = 0
        while (index < insertion.length) {
            val codePoint = Character.codePointAt(insertion, index)
            val step = ColumnEditEngine.glyphWidth(codePoint, column, tab)
            width += step
            column += step
            index += Character.charCount(codePoint)
        }
        selection = ColumnRange(range.top, range.bottom, range.left + width, range.left + width)
        refreshHighlight()
    }

    private fun applyRange(range: ColumnRange, transform: (String, Int) -> String) {
        val tab = tabSize()
        applyDocument { transform(it, tab) }
        refreshHighlight()
    }

    private fun applyDocument(transform: (String) -> String) {
        buffer.replaceAllText(transform(buffer.text))
        val caretLine = (selection ?: return).bottom
        try {
            val offset = area.getLineStartOffset(caretLine.coerceAtMost(area.lineCount - 1).coerceAtLeast(0))
            area.caretPosition = offset.coerceAtMost(buffer.text.length)
        } catch (_: BadLocationException) {
        }
    }

    private fun copyRange(range: ColumnRange) {
        val snippet = ColumnEditEngine.extract(buffer.text, range, tabSize())
        Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(snippet), null)
    }

    private fun clipboardText(): String? = runCatching {
        Toolkit.getDefaultToolkit().systemClipboard.getData(DataFlavor.stringFlavor) as? String
    }.getOrNull()

    private fun columnGesture(event: MouseEvent): Boolean = event.isAltDown || dragWithoutAlt

    private fun tabSize(): Int = area.tabSize.coerceAtLeast(1)

    private fun pointOf(event: MouseEvent): LineColumn {
        val offset = area.viewToModel2D(Point2D.Double(event.x.toDouble(), event.y.toDouble())).coerceAtLeast(0)
        val line = try {
            area.getLineOfOffset(offset.coerceAtMost(area.document.length))
        } catch (_: BadLocationException) {
            0
        }
        val start = try {
            area.getLineStartOffset(line)
        } catch (_: BadLocationException) {
            0
        }
        val lineText = try {
            val end = area.getLineEndOffset(line)
            area.document.getText(start, (end - start).coerceAtLeast(0)).trimEnd('\n', '\r')
        } catch (_: BadLocationException) {
            ""
        }
        val column = ColumnEditEngine.visualColumn(lineText, (offset - start).coerceIn(0, lineText.length), tabSize())
        return LineColumn(line, column)
    }

    private fun refreshHighlight() {
        removeHighlight()
        val range = selection ?: return
        try {
            highlightTag = area.highlighter.addHighlight(0, 0, painter)
            painter.range = range
            painter.tabSize = tabSize()
            area.repaint()
        } catch (_: BadLocationException) {
        }
    }

    private fun removeHighlight() {
        highlightTag?.let { tag ->
            runCatching { area.highlighter.removeHighlight(tag) }
        }
        highlightTag = null
        painter.range = null
        area.repaint()
    }

    private fun updateCursor() {
        area.cursor = if (enabled && (dragWithoutAlt || selection != null)) {
            Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR)
        } else {
            Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR)
        }
    }

    private data class LineColumn(val line: Int, val column: Int)

    private inner class ColumnSelectionPainter : Highlighter.HighlightPainter {
        var range: ColumnRange? = null
        var tabSize: Int = 4

        override fun paint(g: Graphics, p0: Int, p1: Int, bounds: java.awt.Shape, c: JTextComponent) {
            val current = range ?: return
            g.color = Color(0x4F, 0x83, 0xCC, 90)
            val last = (c as? RSyntaxTextArea)?.lineCount?.minus(1) ?: return
            for (line in current.top..current.bottom.coerceAtMost(last)) {
                val rect = lineRectangle(c, line, current.left, current.right, tabSize) ?: continue
                g.fillRect(rect.x, rect.y, rect.width.coerceAtLeast(2), rect.height)
            }
        }
    }

    private fun lineRectangle(area: RSyntaxTextArea, line: Int, left: Int, right: Int, tabSize: Int): Rectangle? {
        return try {
            val start = area.getLineStartOffset(line)
            val end = area.getLineEndOffset(line)
            val raw = area.document.getText(start, end - start)
            val text = raw.trimEnd('\n', '\r')
            val leftIndex = ColumnEditEngine.indexOfVisualColumn(text, left, tabSize).coerceAtMost(text.length)
            val rightIndex = ColumnEditEngine.indexOfVisualColumn(text, right, tabSize).coerceAtMost(text.length)
            val leftView = area.modelToView2D(start + leftIndex) ?: return null
            val rightView = area.modelToView2D(start + rightIndex.coerceAtLeast(leftIndex)) ?: leftView
            val extra = (maxOf(left, right) - ColumnEditEngine.visualColumn(text, text.length, tabSize)).coerceAtLeast(0)
            val space = area.getFontMetrics(area.font).charWidth(' ')
            val x1 = leftView.x.toInt()
            val x2 = rightView.x.toInt() + extra * space
            val width = (x2 - x1).coerceAtLeast(if (left == right) 2 else 1)
            Rectangle(x1, leftView.y.toInt(), width, leftView.height.toInt().coerceAtLeast(1))
        } catch (_: BadLocationException) {
            null
        }
    }

    init {
        area.addMouseListener(mouse)
        area.addMouseMotionListener(mouse)
        area.addKeyListener(keys)
        area.addInputMethodListener(ime)
    }
}

