package com.rememberber.mootool.next.compose.domain

import com.rememberber.mootool.next.compose.model.HistoryRecord
import com.rememberber.mootool.next.compose.sessions.CronSession
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CronHistoryRestoreTest {
    @Test
    fun restoresExpressionRunsAndTimeZone() {
        val session = CronSession()
        val expression = "0 0 12 * * ?"
        val options = CronHistoryMetadata.encodeTimeZone("Asia/Shanghai")
        CronHistoryRestore.apply(
            session,
            HistoryRecord(
                id = 1,
                toolId = "cron",
                operation = "describe",
                summary = "s",
                input = expression,
                output = "2026-01-01 12:00:00\n2026-01-02 12:00:00",
                options = options,
                createdAt = "0",
            ),
            language = "zh-CN",
        )
        assertEquals(expression, session.expression)
        assertEquals("Asia/Shanghai", session.zone)
        assertEquals(2, session.runs.size)
        assertTrue(session.description.isNotBlank())
        assertEquals("", session.error)
    }
}
