package com.rememberber.mootool.next.compose.domain

import kotlin.test.Test
import kotlin.test.assertEquals

class LegacyConsoleDraftTest {
    @Test
    fun extractMessagesSkipsConsoleTimestamps() {
        val raw = """

            2026-01-01 12:00:00.000 

            2 * 3 = 6


            2026-01-02 13:00:00.000 

            DEC(ff) = 255
        """.trimIndent()
        assertEquals(listOf("2 * 3 = 6", "DEC(ff) = 255"), LegacyConsoleDraft.extractMessages(raw))
    }

    @Test
    fun extractQrGenerateContentFromConsoleLog() {
        val raw = """

            2026-01-01 12:00:00.000 

            生成:
            https://example.com/path
        """.trimIndent()
        assertEquals("https://example.com/path", LegacyConsoleDraft.extractQrGenerateContent(raw))
    }
}
