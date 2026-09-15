package com.rememberber.mootool.next.compose.domain

import kotlin.math.max
import kotlin.math.min

data class ColumnRange(
    val startLine: Int,
    val endLine: Int,
    val startColumn: Int,
    val endColumn: Int
) {
    val top: Int get() = min(startLine, endLine)
    val bottom: Int get() = max(startLine, endLine)
    val left: Int get() = min(startColumn, endColumn)
    val right: Int get() = max(startColumn, endColumn)
    val empty: Boolean get() = left == right
}

object ColumnEditEngine {
    fun insert(text: String, startLine: Int, endLine: Int, column: Int, insertion: String, tabSize: Int): String {
        val trailing = text.endsWith("\n")
        val lines = splitKeepLast(text).toMutableList()
        val from = startLine.coerceAtLeast(0).coerceAtMost(lines.lastIndex)
        val to = endLine.coerceAtLeast(0).coerceAtMost(lines.lastIndex)
        val top = min(from, to)
        val bottom = max(from, to)
        val insert = insertion
        for (index in top..bottom) {
            lines[index] = insertAtVisualColumn(lines[index], column, insert, tabSize)
        }
        return join(lines, trailing)
    }

    fun delete(text: String, startLine: Int, endLine: Int, startColumn: Int, endColumn: Int, tabSize: Int): String {
        val trailing = text.endsWith("\n")
        val lines = splitKeepLast(text).toMutableList()
        val from = startLine.coerceAtLeast(0).coerceAtMost(lines.lastIndex)
        val to = endLine.coerceAtLeast(0).coerceAtMost(lines.lastIndex)
        val top = min(from, to)
        val bottom = max(from, to)
        val left = min(startColumn, endColumn)
        val right = max(startColumn, endColumn)
        if (left == right) return text
        for (index in top..bottom) {
            lines[index] = deleteOverlapping(lines[index], left, right, tabSize)
        }
        return join(lines, trailing)
    }

    fun replace(text: String, range: ColumnRange, insertion: String, tabSize: Int): String {
        val cleared = if (range.empty) text else delete(text, range.top, range.bottom, range.left, range.right, tabSize)
        return insert(cleared, range.top, range.bottom, range.left, insertion, tabSize)
    }

    fun paste(text: String, startLine: Int, startColumn: Int, clipboard: String, tabSize: Int): String {
        val clipLines = splitClipboard(clipboard)
        val trailing = text.endsWith("\n") || clipboard.endsWith("\n")
        val lines = splitKeepLast(text).toMutableList()
        val origin = startLine.coerceAtLeast(0)
        while (lines.size < origin + clipLines.size) {
            lines.add("")
        }
        clipLines.forEachIndexed { offset, piece ->
            val index = origin + offset
            lines[index] = insertAtVisualColumn(lines[index], startColumn, piece, tabSize)
        }
        return join(lines, trailing)
    }

    fun pasteIntoSelection(text: String, range: ColumnRange, clipboard: String, tabSize: Int): String {
        val cleared = if (range.empty) text else delete(text, range.top, range.bottom, range.left, range.right, tabSize)
        val clipLines = splitClipboard(clipboard)
        val rowCount = range.bottom - range.top + 1
        val pieces = if (clipLines.size == 1 && rowCount > 1) List(rowCount) { clipLines[0] } else clipLines
        return paste(cleared, range.top, range.left, pieces.joinToString("\n"), tabSize)
    }

    fun extract(text: String, range: ColumnRange, tabSize: Int): String {
        val lines = splitKeepLast(text)
        if (lines.isEmpty() || range.empty) return ""
        val top = range.top.coerceIn(0, lines.lastIndex)
        val bottom = range.bottom.coerceIn(0, lines.lastIndex)
        return (top..bottom).joinToString("\n") { extractOverlapping(lines[it], range.left, range.right, tabSize) }
    }

    fun visualColumn(line: String, charIndex: Int, tabSize: Int): Int {
        val limit = charIndex.coerceIn(0, line.length)
        var column = 0
        var index = 0
        while (index < limit) {
            val codePoint = Character.codePointAt(line, index)
            column += glyphWidth(codePoint, column, tabSize)
            index += Character.charCount(codePoint)
        }
        return column
    }

    fun indexOfVisualColumn(line: String, column: Int, tabSize: Int): Int {
        var visual = 0
        var index = 0
        while (index < line.length) {
            if (visual >= column) return index
            val codePoint = Character.codePointAt(line, index)
            val width = glyphWidth(codePoint, visual, tabSize)
            visual += width
            index += Character.charCount(codePoint)
            if (visual > column) return index
        }
        return line.length + max(0, column - visual)
    }

    fun glyphWidth(codePoint: Int, column: Int, tabSize: Int): Int {
        if (codePoint == '\t'.code) {
            val size = tabSize.coerceAtLeast(1)
            return size - column.mod(size)
        }
        if (codePoint == '\r'.code) return 0
        val type = Character.getType(codePoint)
        if (type == Character.NON_SPACING_MARK.toInt() ||
            type == Character.COMBINING_SPACING_MARK.toInt() ||
            type == Character.ENCLOSING_MARK.toInt()
        ) {
            return 0
        }
        return if (isWide(codePoint)) 2 else 1
    }

    internal fun insertAtVisualColumn(line: String, column: Int, insertion: String, tabSize: Int): String {
        val index = indexOfVisualColumn(line, column, tabSize)
        if (index > line.length) {
            return line + " ".repeat(index - line.length) + insertion
        }
        if (index == line.length && visualColumn(line, line.length, tabSize) < column) {
            return line + " ".repeat(column - visualColumn(line, line.length, tabSize)) + insertion
        }
        return line.substring(0, index) + insertion + line.substring(index)
    }

    internal fun splitKeepLast(text: String): List<String> {
        if (text.isEmpty()) return listOf("")
        val lines = mutableListOf<String>()
        var start = 0
        for (index in text.indices) {
            if (text[index] == '\n') {
                lines += text.substring(start, index)
                start = index + 1
            }
        }
        if (start < text.length || !text.endsWith("\n")) {
            lines += text.substring(start)
        } else if (lines.isEmpty()) {
            lines += ""
        }
        return lines.ifEmpty { listOf("") }
    }

    private fun splitClipboard(clipboard: String): List<String> {
        val lines = mutableListOf<String>()
        var start = 0
        for (index in clipboard.indices) {
            if (clipboard[index] == '\n') {
                lines += clipboard.substring(start, index)
                start = index + 1
            }
        }
        lines += clipboard.substring(start)
        return lines.ifEmpty { listOf("") }
    }

    private fun join(lines: List<String>, trailingNewline: Boolean): String {
        val joined = lines.joinToString("\n")
        return if (trailingNewline && !joined.endsWith("\n")) "$joined\n" else joined
    }

    private fun deleteOverlapping(line: String, left: Int, right: Int, tabSize: Int): String {
        if (left >= right) return line
        val builder = StringBuilder()
        var visual = 0
        var index = 0
        var keepCluster = true
        while (index < line.length) {
            val codePoint = Character.codePointAt(line, index)
            val width = glyphWidth(codePoint, visual, tabSize)
            if (width == 0) {
                if (keepCluster) builder.appendCodePoint(codePoint)
            } else {
                val start = visual
                val end = visual + width
                keepCluster = end <= left || start >= right
                if (keepCluster) builder.appendCodePoint(codePoint)
                visual = end
            }
            index += Character.charCount(codePoint)
        }
        return builder.toString()
    }

    private fun extractOverlapping(line: String, left: Int, right: Int, tabSize: Int): String {
        if (left >= right) return ""
        val builder = StringBuilder()
        var visual = 0
        var index = 0
        var takeCluster = false
        while (index < line.length) {
            val codePoint = Character.codePointAt(line, index)
            val width = glyphWidth(codePoint, visual, tabSize)
            if (width == 0) {
                if (takeCluster) builder.appendCodePoint(codePoint)
            } else {
                val start = visual
                val end = visual + width
                takeCluster = start < right && end > left
                if (takeCluster) builder.appendCodePoint(codePoint)
                visual = end
            }
            index += Character.charCount(codePoint)
        }
        return builder.toString()
    }

    private fun isWide(codePoint: Int): Boolean {
        if (Character.isIdeographic(codePoint)) return true
        return codePoint in 0x1100..0x115F ||
            codePoint in 0x2329..0x232A ||
            codePoint in 0x2E80..0xA4CF ||
            codePoint in 0xAC00..0xD7A3 ||
            codePoint in 0xF900..0xFAFF ||
            codePoint in 0xFE10..0xFE19 ||
            codePoint in 0xFE30..0xFE6F ||
            codePoint in 0xFF00..0xFF60 ||
            codePoint in 0xFFE0..0xFFE6 ||
            codePoint in 0x1F300..0x1FAFF
    }
}
