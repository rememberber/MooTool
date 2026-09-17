package com.rememberber.mootool.next.compose.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GitDiffDecorationTest {
    @Test
    fun bothModeEmitsLineAndCharacterRangesOnRightInsert() {
        val text = "alpha\nbeta\n"
        val segments = listOf(
            DiffSegment(DiffSegmentType.Insert, -1, -1, 10, 15, wholeLine = false),
        )
        val ranges = GitDiffDecoration.rangesForSide(text, segments, side = "right", highlightMode = GitDiffDecoration.HIGHLIGHT_BOTH)
        assertTrue(ranges.any { it.tier == GitDiffDecoration.Tier.Line && it.start == 6 && it.end == 10 })
        assertTrue(ranges.any { it.tier == GitDiffDecoration.Tier.Character && it.start == 10 && it.end == 11 })
    }

    @Test
    fun deleteSegmentsOnlyPaintLeftSide() {
        val text = "remove me"
        val segments = listOf(
            DiffSegment(DiffSegmentType.Delete, 0, 6, -1, -1, wholeLine = false),
        )
        assertTrue(GitDiffDecoration.rangesForSide(text, segments, "left").isNotEmpty())
        assertTrue(GitDiffDecoration.rangesForSide(text, segments, "right").isEmpty())
    }

    @Test
    fun characterModeSkipsWholeLineSegments() {
        val segments = listOf(
            DiffSegment(DiffSegmentType.Change, 0, 5, 0, 5, wholeLine = true),
            DiffSegment(DiffSegmentType.Change, 7, 9, 7, 9, wholeLine = false),
        )
        val ranges = GitDiffDecoration.rangesForSide("0123456789", segments, "left", GitDiffDecoration.HIGHLIGHT_CHARACTERS)
        assertEquals(1, ranges.count { it.tier == GitDiffDecoration.Tier.Character })
        assertEquals(7, ranges.first { it.tier == GitDiffDecoration.Tier.Character }.start)
    }
}
