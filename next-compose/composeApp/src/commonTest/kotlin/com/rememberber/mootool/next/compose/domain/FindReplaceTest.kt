package com.rememberber.mootool.next.compose.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FindReplaceTest {
    @Test
    fun findsCaseInsensitiveAndWholeWordMatches() {
        val text = "JSON json Jsonify"
        val all = FindReplace.findAll(text, "json", FindReplaceOptions())
        assertEquals(3, all.size)
        val whole = FindReplace.findAll(text, "json", FindReplaceOptions(wholeWord = true))
        assertEquals(2, whole.size)
    }

    @Test
    fun replaceAllIsOneTransactionCount() {
        val (next, count) = FindReplace.replaceAll("a-a-a", "a", "b", FindReplaceOptions())
        assertEquals("b-b-b", next)
        assertEquals(3, count)
    }

    @Test
    fun replaceCurrent_prefers_exact_selection_match() {
        val (next, match) = FindReplace.replaceCurrent(
            "aa aa",
            "aa",
            "XX",
            FindReplaceOptions(),
            fromIndex = 2,
            selectionStart = 0,
            selectionEnd = 2,
        )
        assertEquals("XX aa", next)
        assertEquals(0, match?.start)
        assertEquals(2, match?.end)
    }
}
