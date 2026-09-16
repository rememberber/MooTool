package com.rememberber.mootool.next.compose.editor

import com.rememberber.mootool.next.compose.domain.FindMatch
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class EditorFindHighlightTest {
    @Test
    fun spans_highlights_current_from_editor_selection() {
        val matches = listOf(FindMatch(0, 2), FindMatch(3, 5))
        val spans = EditorFindHighlight.spans(10, matches, selectionStart = 3, selectionEnd = 5)
        assertFalse(spans[0].third)
        assertTrue(spans[1].third)
    }
}
