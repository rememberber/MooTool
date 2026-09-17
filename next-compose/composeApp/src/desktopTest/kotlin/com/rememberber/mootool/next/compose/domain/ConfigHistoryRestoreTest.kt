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
            options = ConfigHistoryMetadata.VALIDATE,
            createdAt = "",
        )
        ConfigHistoryRestore.apply(session, item)
        assertEquals("validate", session.tab)
        assertEquals("a: 1", session.validateSource)
        assertEquals(true, session.valid)
    }

    @Test
    fun restoresToYamlConvertTab() {
        val session = ConfigSession()
        ConfigHistoryRestore.apply(
            session,
            HistoryRecord(
                toolId = "config",
                operation = "convert",
                summary = "to yaml",
                input = "a=1",
                output = "a: 1",
                options = ConfigHistoryMetadata.TO_YAML,
                createdAt = "",
            ),
        )
        assertEquals("convert", session.tab)
        assertEquals("a=1", session.properties)
        assertEquals("a: 1", session.yaml)
    }
}
