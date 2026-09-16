package com.rememberber.mootool.next.compose.domain

import com.rememberber.mootool.next.compose.sessions.CalculatorSession
import kotlin.test.Test
import kotlin.test.assertEquals

class LegacyCalculatorDraftTest {
    @Test
    fun appliesConsoleLogNewestFirstAndParsesHexConversion() {
        val session = CalculatorSession()
        val raw = """

            2026-01-01 12:00:00.000 

            1 + 1 = 2


            2026-01-02 13:00:00.000 

            DEC(ff) = 255
        """.trimIndent()
        LegacyCalculatorDraft.apply(session, raw)
        assertEquals(listOf("DEC(ff) = 255", "1 + 1 = 2"), session.log)
        assertEquals("ff", session.hex)
        assertEquals("255", session.decimal)
    }
}
