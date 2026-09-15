package com.rememberber.mootool.next.compose.domain

object NoteListEngine {
    enum class Prefix { Bullet, Numbered }

    data class Result(
        val text: String,
        val selectionStart: Int,
        val selectionEnd: Int
    )

    fun prefixSelectedLines(text: String, start: Int, end: Int, prefix: Prefix): Result {
        if (text.isEmpty()) {
            val inserted = when (prefix) {
                Prefix.Bullet -> "- "
                Prefix.Numbered -> "1. "
            }
            return Result(inserted, 0, inserted.length)
        }
        val caretStart = start.coerceIn(0, text.length)
        val caretEnd = end.coerceIn(caretStart, text.length)
        val from = (caretStart - 1).coerceAtLeast(0)
        val lineStart = text.lastIndexOf('\n', from) + 1
        val nextBreak = text.indexOf('\n', caretEnd)
        val lineEnd = if (nextBreak < 0) text.length else nextBreak
        val selected = text.substring(lineStart, lineEnd)
        val transformed = selected.split('\n').mapIndexed { index, line ->
            when (prefix) {
                Prefix.Bullet -> "- $line"
                Prefix.Numbered -> "${index + 1}. $line"
            }
        }.joinToString("\n")
        val next = text.substring(0, lineStart) + transformed + text.substring(lineEnd)
        return Result(next, lineStart, lineStart + transformed.length)
    }
}
