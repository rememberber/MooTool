package com.rememberber.mootool.next.compose.domain

import com.rememberber.mootool.next.compose.sessions.JsonSession
import kotlin.test.Test
import kotlin.test.assertEquals

class JsonFormatOptionsTest {
    @Test
    fun normalizeInspectorIndent_keepsTwoAndFour() {
        assertEquals(2, JsonFormatOptions(spaces = 2).normalizeInspectorIndent().spaces)
        assertEquals(4, JsonFormatOptions(spaces = 4).normalizeInspectorIndent().spaces)
    }

    @Test
    fun normalizeInspectorIndent_mapsInvalidToTwo() {
        assertEquals(2, JsonFormatOptions(spaces = 0).normalizeInspectorIndent().spaces)
        assertEquals(2, JsonFormatOptions(spaces = 3).normalizeInspectorIndent().spaces)
        assertEquals(2, JsonFormatOptions(spaces = 8).normalizeInspectorIndent().spaces)
    }

    @Test
    fun jsonSessionRestore_normalizesInspectorIndent() {
        val snapshot = JsonSession().snapshot().copy(spaces = 0, sortKeys = true)
        val restored = JsonSession().also { it.restore(snapshot) }
        assertEquals(2, restored.formatOptions.spaces)
        assertEquals(true, restored.formatOptions.sortKeys)
    }
}
