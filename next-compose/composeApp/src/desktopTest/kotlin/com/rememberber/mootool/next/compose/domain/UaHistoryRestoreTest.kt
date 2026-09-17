package com.rememberber.mootool.next.compose.domain

import com.rememberber.mootool.next.compose.model.HistoryRecord
import com.rememberber.mootool.next.compose.sessions.UaSession
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class UaHistoryRestoreTest {
    private val codec = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    @Test
    fun restoresSourceAndParsedResult() {
        val parsed = UaEngine.parse(UaEngine.presets.first().second)
        val session = UaSession()
        UaHistoryRestore.apply(
            session,
            HistoryRecord(
                id = 1,
                toolId = "uaParse",
                operation = "ua",
                summary = "ua",
                input = UaEngine.presets.first().second,
                output = codec.encodeToString(parsed),
                options = UaHistoryMetadata.MARKER,
                createdAt = "0",
            ),
        )
        assertEquals(UaEngine.presets.first().second, session.source)
        assertNotNull(session.result)
        assertEquals(parsed.browser, session.result?.browser)
    }
}
