package com.rememberber.mootool.next.compose.features.json

import com.rememberber.mootool.next.compose.domain.JsonPathEntry
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class JsonInspectorPathTreeLimitTest {
    @Test
    fun visibleEntries_capped_at_eighty() {
        val paths = (1..100).map { index ->
            JsonPathEntry(path = "$.n$index", label = "n$index", preview = "", depth = 0)
        }
        val visible = jsonInspectorVisiblePathEntries(paths)
        assertEquals(80, visible.size)
        assertEquals("$.n1", visible.first().path)
        assertEquals("$.n80", visible.last().path)
        assertTrue(jsonInspectorPathTreeShowsTruncationHint(paths))
    }

    @Test
    fun no_truncation_hint_when_within_limit() {
        val paths = (1..40).map { index ->
            JsonPathEntry(path = "$.k$index", label = "k$index", preview = "", depth = 0)
        }
        assertEquals(40, jsonInspectorVisiblePathEntries(paths).size)
        assertFalse(jsonInspectorPathTreeShowsTruncationHint(paths))
    }
}
