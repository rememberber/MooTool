package com.rememberber.mootool.next.compose.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class LegacyTimeConvertDraftTest {
    @Test
    fun parsesChineseTimestampToLocalLine() {
        val line = "时间戳: 1700000000 --> 时间(Asia/Shanghai): 2023-11-15 06:13:20"
        val entry = LegacyTimeConvertDraft.parseLine(line)
        assertNotNull(entry)
        assertEquals("1700000000", entry.input)
        assertEquals("2023-11-15 06:13:20", entry.output)
        assertEquals("Asia/Shanghai", entry.zone)
        assertEquals("second", entry.unit)
    }

    @Test
    fun parsesEnglishLocalToTimestampLine() {
        val line = "Time (UTC): 2023-11-15 06:13:20 --> Timestamp: 1700000000"
        val entry = LegacyTimeConvertDraft.parseLine(line)
        assertNotNull(entry)
        assertEquals("2023-11-15 06:13:20", entry.input)
        assertEquals("1700000000", entry.output)
        assertEquals("UTC", entry.zone)
    }

    @Test
    fun parseStripsJavaConsoleUtilTimestamps() {
        val raw = """

            2026-01-01 12:00:00.000 

            时间戳: 1700000000 --> 时间(Asia/Shanghai): 2023-11-15 06:13:20
        """.trimIndent()
        val entries = LegacyTimeConvertDraft.parse(raw)
        assertEquals(1, entries.size)
        assertEquals("1700000000", entries.first().input)
    }
}
