package com.rememberber.mootool.next.compose.domain

import com.rememberber.mootool.next.compose.model.HistoryRecord
import com.rememberber.mootool.next.compose.sessions.HttpSession
import kotlin.test.Test
import kotlin.test.assertEquals

class HttpHistoryRestoreTest {
    @Test
    fun restoresUrlAndMethodFromOperation() {
        val session = HttpSession()
        val item = HistoryRecord(
            toolId = "http",
            operation = "POST",
            summary = "POST https://example.com",
            input = "https://example.com",
            output = "ok",
            options = HttpHistoryMetadata.encodeStatus(201),
            createdAt = "",
        )
        HttpHistoryRestore.apply(session, item)
        assertEquals("https://example.com", session.url)
        assertEquals(HttpMethod.POST, session.method)
        assertEquals(201, HttpHistoryMetadata.decodeStatus(item.options))
    }

    @Test
    fun parsesMethodFromSummaryWhenOperationIsLegacyStatus() {
        assertEquals(HttpMethod.GET, HttpHistoryMetadata.parseMethod("200", "GET https://x.test"))
    }
}
