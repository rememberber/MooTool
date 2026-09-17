package com.rememberber.mootool.next.compose.domain

/** Vault Git diff / F02 并排 diff 高亮区间（对齐 Electron 行+字符双层，默认 `both`）。 */
object GitDiffDecoration {
    const val HIGHLIGHT_BOTH = "both"
    const val HIGHLIGHT_LINES = "lines"
    const val HIGHLIGHT_CHARACTERS = "characters"

    enum class Tier { Line, Character }

    data class Range(val start: Int, val end: Int, val type: DiffSegmentType, val tier: Tier)

    fun visibleSegments(segments: List<DiffSegment>, highlightMode: String): List<DiffSegment> =
        if (highlightMode == HIGHLIGHT_CHARACTERS) segments.filter { !it.wholeLine } else segments

    fun rangesForSide(
        text: String,
        segments: List<DiffSegment>,
        side: String,
        highlightMode: String = HIGHLIGHT_BOTH,
    ): List<Range> {
        if (text.isEmpty()) return emptyList()
        val visible = visibleSegments(segments, highlightMode)
        val out = ArrayList<Range>()
        for (segment in visible) {
            if (!segmentAppliesToSide(segment.type, side)) continue
            val from = if (side == "left") segment.leftStart else segment.rightStart
            val to = if (side == "left") segment.leftEnd else segment.rightEnd
            if (from < 0 || to < 0 || to <= from) continue
            val start = from.coerceIn(0, text.length)
            val end = to.coerceIn(start, text.length)
            if (start >= end) continue
            if (highlightMode != HIGHLIGHT_CHARACTERS) {
                val lineFrom = lineStartAt(text, start)
                val lineTo = lineEndAt(text, start)
                if (lineTo > lineFrom) {
                    out += Range(lineFrom, lineTo, segment.type, Tier.Line)
                }
            }
            if (highlightMode != HIGHLIGHT_LINES) {
                out += Range(start, end, segment.type, Tier.Character)
            }
        }
        return out
    }

    internal fun segmentAppliesToSide(type: DiffSegmentType, side: String): Boolean = when (type) {
        DiffSegmentType.Insert -> side == "right"
        DiffSegmentType.Delete -> side == "left"
        DiffSegmentType.Change -> true
    }

    internal fun lineStartAt(text: String, offset: Int): Int {
        val position = offset.coerceIn(0, text.length)
        if (position == 0) return 0
        val index = text.lastIndexOf('\n', position - 1)
        return if (index < 0) 0 else index + 1
    }

    internal fun lineEndAt(text: String, offset: Int): Int {
        val index = text.indexOf('\n', offset.coerceIn(0, text.length))
        return if (index < 0) text.length else index
    }
}
