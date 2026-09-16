package com.rememberber.mootool.next.compose.editor

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import com.rememberber.mootool.next.compose.domain.FindMatch
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FindHighlightTransformationTest {
    @Test
    fun findMatchAtSelection_prefers_exact_range() {
        val matches = listOf(FindMatch(0, 2), FindMatch(3, 5))
        assertEquals(matches[0], findMatchAtSelection(matches, 0, 2))
    }

    @Test
    fun findMatchAtSelection_uses_caret_inside_match() {
        val matches = listOf(FindMatch(0, 2), FindMatch(3, 5))
        assertEquals(matches[1], findMatchAtSelection(matches, 4, 4))
    }

    @Test
    fun filter_preserves_text_and_marks_spans() {
        val matches = listOf(FindMatch(0, 2), FindMatch(3, 5))
        val transformed = FindHighlightTransformation(
            matches,
            matches[1],
            Color.Red,
            Color.Blue,
        ).filter(AnnotatedString("aa aa"))
        assertEquals("aa aa", transformed.text.text)
        assertEquals(2, transformed.text.spanStyles.size)
        assertTrue(transformed.text.spanStyles.any { it.start == 3 && it.end == 5 })
    }
}
