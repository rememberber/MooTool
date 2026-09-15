package com.rememberber.mootool.next.compose.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SystemFontsTest {
    @Test
    fun listKeepsMonoFirstAndIncludesCurrent() {
        val fonts = SystemFonts.list("Comic Sans MS")
        assertEquals("ui-monospace", fonts.first())
        assertTrue(fonts.contains("Menlo"))
        assertTrue(fonts.contains("Comic Sans MS"))
        val rest = fonts.drop(1)
        assertEquals(rest.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it }), rest)
    }

    @Test
    fun displayNameUsesLabelOrEmpty() {
        assertEquals("等宽字体", SystemFonts.displayName("ui-monospace", mapOf("ui-monospace" to "等宽字体"), "系统字体"))
        assertEquals("系统字体", SystemFonts.displayName("", mapOf("ui-monospace" to "等宽字体"), "系统字体"))
        assertEquals("Menlo", SystemFonts.displayName("Menlo", emptyMap(), "系统字体"))
    }
}
