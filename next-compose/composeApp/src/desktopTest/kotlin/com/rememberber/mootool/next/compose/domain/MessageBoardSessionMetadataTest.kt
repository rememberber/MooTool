package com.rememberber.mootool.next.compose.domain

import kotlin.test.Test
import kotlin.test.assertEquals

class MessageBoardSessionMetadataTest {
    @Test
    fun normalizeThemeAndAlignment() {
        assertEquals("cobalt", MessageBoardSessionMetadata.normalizeTheme("Cobalt"))
        assertEquals("center", MessageBoardSessionMetadata.normalizeAlignment("CENTER"))
    }

    @Test
    fun normalizeSize_clampsToEngineRange() {
        assertEquals(70, MessageBoardSessionMetadata.normalizeSize(10))
        assertEquals(130, MessageBoardSessionMetadata.normalizeSize(200))
    }
}
