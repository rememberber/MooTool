package com.rememberber.mootool.next.compose.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class HttpHistoryMetadataTest {
    @Test
    fun encodeAndDecodeStatusJsonOrPlainNumber() {
        assertEquals(404, HttpHistoryMetadata.decodeStatus(HttpHistoryMetadata.encodeStatus(404)))
        assertEquals(200, HttpHistoryMetadata.decodeStatus("200"))
    }

    @Test
    fun parseMethodPrefersOperationThenSummaryPrefix() {
        assertEquals(HttpMethod.POST, HttpHistoryMetadata.parseMethod("POST", "POST https://example.com"))
        assertEquals(HttpMethod.GET, HttpHistoryMetadata.parseMethod("", "GET /path"))
        assertNull(HttpHistoryMetadata.parseMethod("", ""))
    }
}
