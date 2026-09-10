package com.rememberber.mootool.next.compose.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DiffEngineTest {
    @Test
    fun buildsJavaCompatibleCharacterRangesAndWholeLineChanges() {
        val result = DiffEngine.compare("one\ntwo\n", "one\nthree\nplus\n", false)
        assertEquals(1, result.changed)
        assertEquals(1, result.added)
        assertEquals(0, result.removed)
        assertEquals(
            listOf(
                DiffSegment(DiffSegmentType.Change, 5, 7, 5, 9, false),
                DiffSegment(DiffSegmentType.Insert, -1, -1, 10, 14, true)
            ),
            result.segments
        )
    }

    @Test
    fun createsThreeLineContextUnifiedFormat() {
        val result = DiffEngine.compare("one\ntwo\n", "one\nthree\nplus\n", false)
        assertEquals(
            listOf("--- old", "+++ new", "@@ -1,3 +1,4 @@", " one", "-two", "+three", "+plus", " ").joinToString("\n"),
            result.unified
        )
        assertEquals(
            listOf(
                UnifiedSpanType.HeaderLine,
                UnifiedSpanType.HeaderLine,
                UnifiedSpanType.HunkLine,
                UnifiedSpanType.DeleteLine,
                UnifiedSpanType.AddLine,
                UnifiedSpanType.AddLine
            ),
            result.unifiedView.lineSpans.map { it.type }
        )
        assertEquals(1, result.unifiedView.characterEventCount)
    }

    @Test
    fun ignoresWhitespaceOnlyCharacterDifferencesWithoutHidingUnifiedPatch() {
        val result = DiffEngine.compare("one  two\n", "one two\n", true)
        assertEquals(emptyList(), result.segments)
        assertTrue(result.unified.contains("-one  two"))
        assertTrue(result.unified.contains("+one two"))
        assertEquals(emptyList(), result.unifiedView.characterSpans)
    }

    @Test
    fun retainsTrailingEmptyLinesAndReportsCompleteInsertedAndDeletedLines() {
        val inserted = DiffEngine.compare("same\n", "same\nnew\n", false)
        val deleted = DiffEngine.compare("same\nold\n", "same\n", false)
        assertTrue(
            inserted.segments.contains(DiffSegment(DiffSegmentType.Insert, -1, -1, 5, 8, true))
        )
        assertTrue(
            deleted.segments.contains(DiffSegment(DiffSegmentType.Delete, 5, 8, -1, -1, true))
        )
    }

    @Test
    fun returnsNoPatchForIdenticalEmptyText() {
        val result = DiffEngine.compare("", "", false)
        assertEquals(emptyList(), result.segments)
        assertEquals("", result.unified)
        assertEquals(0, result.added)
        assertEquals(0, result.removed)
        assertEquals(0, result.changed)
    }

    @Test
    fun splitsCrLfAsOneTerminatorAndKeepsUnicodeOffsets() {
        val result = DiffEngine.compare("hi\r\nold\r\n", "hi\r\n😀\r\n", false)
        assertTrue(result.segments.any { it.type == DiffSegmentType.Change || it.type == DiffSegmentType.Delete })
        assertTrue(result.unified.contains("-old"))
        assertTrue(result.unified.contains("+😀"))
    }
}
