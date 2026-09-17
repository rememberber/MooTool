package com.rememberber.mootool.next.compose.features.diff

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import com.rememberber.mootool.next.compose.domain.DiffSegment
import com.rememberber.mootool.next.compose.domain.DiffSegmentType
import com.rememberber.mootool.next.compose.domain.GitDiffDecoration

internal class DiffHighlightTransformation(
    private val segments: List<DiffSegment>,
    private val side: String,
    private val highlightMode: String,
    private val added: Color,
    private val removed: Color,
    private val changed: Color
) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        return TransformedText(annotateSide(text.text, segments, side, highlightMode, added, removed, changed), OffsetMapping.Identity)
    }
}

internal fun annotateSide(
    text: String,
    segments: List<DiffSegment>,
    side: String,
    highlightMode: String,
    added: Color,
    removed: Color,
    changed: Color
): AnnotatedString {
    val builder = AnnotatedString.Builder(text)
    for (segment in segments) {
        val from = if (side == "left") segment.leftStart else segment.rightStart
        val to = if (side == "left") segment.leftEnd else segment.rightEnd
        if (from < 0 || to < 0 || to <= from) continue
        val color = when (segment.type) {
            DiffSegmentType.Insert -> added
            DiffSegmentType.Delete -> removed
            DiffSegmentType.Change -> changed
        }
        val end = to.coerceAtMost(text.length)
        val start = from.coerceIn(0, text.length)
        if (start >= end) continue
        GitDiffDecoration.rangesForSide(text, listOf(segment), side, highlightMode).forEach { range ->
            val tierColor = when (range.tier) {
                GitDiffDecoration.Tier.Line -> color.copy(alpha = 0.16f)
                GitDiffDecoration.Tier.Character -> color.copy(alpha = 0.42f)
            }
            val rs = range.start.coerceIn(0, text.length)
            val re = range.end.coerceIn(rs, text.length)
            if (re > rs) builder.addStyle(SpanStyle(background = tierColor), rs, re)
        }
    }
    return builder.toAnnotatedString()
}
