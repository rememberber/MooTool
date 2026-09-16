package com.rememberber.mootool.next.compose.editor

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import com.rememberber.mootool.next.compose.domain.FindMatch

/** Compose 文本框内查找命中背景（对齐 Electron `codeEditorSearchHighlight`）。 */
internal class FindHighlightTransformation(
    private val matches: List<FindMatch>,
    private val current: FindMatch?,
    private val matchColor: Color,
    private val currentColor: Color,
) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        if (matches.isEmpty()) {
            return TransformedText(text, OffsetMapping.Identity)
        }
        val builder = AnnotatedString.Builder(text)
        for (match in matches) {
            val start = match.start.coerceIn(0, text.length)
            val end = match.end.coerceIn(start, text.length)
            if (end <= start) continue
            val isCurrent = current != null && match.start == current.start && match.end == current.end
            val background = if (isCurrent) currentColor else matchColor
            builder.addStyle(SpanStyle(background = background), start, end)
        }
        return TransformedText(builder.toAnnotatedString(), OffsetMapping.Identity)
    }
}

internal fun findMatchAtSelection(matches: List<FindMatch>, selectionStart: Int, selectionEnd: Int): FindMatch? {
    val start = minOf(selectionStart, selectionEnd)
    val end = maxOf(selectionStart, selectionEnd)
    if (end > start) {
        matches.find { it.start == start && it.end == end }?.let { return it }
    }
    return matches.find { selectionStart in it.start until it.end }
}
