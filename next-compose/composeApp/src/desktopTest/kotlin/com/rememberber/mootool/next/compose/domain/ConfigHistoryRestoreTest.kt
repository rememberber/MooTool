package com.rememberber.mootool.next.compose.domain

import com.rememberber.mootool.next.compose.model.HistoryRecord
import com.rememberber.mootool.next.compose.sessions.ConfigSession
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ConfigHistoryRestoreTest {
    @Test
    fun restoresValidateTab() {
        val session = ConfigSession()
        val item = HistoryRecord(
            toolId = "config",
            operation = "validate",
            summary = "validate",
            input = "a: 1",
            output = "valid",
            options = "validate",
            createdAt = "",
        )
        ConfigHistoryRestore.apply(session, item)
        assertEquals("validate", session.tab)
        assertEquals("a: 1", session.validateSource)
        assertEquals(true, session.valid)
    }
}
