package com.rememberber.mootool.next.compose.domain

import com.rememberber.mootool.next.compose.model.HistoryRecord
import com.rememberber.mootool.next.compose.sessions.RegexSession
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RegexHistoryRestoreTest {
    @Test
    fun restoresPatternSourceFlagsAndSwitchesToTestTab() {
        val session = RegexSession()
        session.tab = "common"
        session.options = RegexOptions(global = false)
        val options = RegexHistoryMetadata.encode(
            RegexOptions(global = true, ignoreCase = true, multiline = true, dotAll = true),
        )
        RegexHistoryRestore.apply(
            session,
            HistoryRecord(
                toolId = "regex",
                operation = "s",
                summary = "s",
                input = "(moo)(\\d+)",
                output = "moo1",
                options = options,
                createdAt = "0",
            ),
        )
        assertEquals("(moo)(\\d+)", session.pattern)
        assertEquals("moo1", session.source)
        assertEquals("test", session.tab)
        assertTrue(session.options.global)
        assertTrue(session.options.ignoreCase)
        assertTrue(session.options.multiline)
        assertTrue(session.options.dotAll)
    }

    @Test
    fun keepsSessionFlagsWhenOptionsMissing() {
        val session = RegexSession()
        session.options = RegexOptions(global = false, ignoreCase = true)
        RegexHistoryRestore.apply(
            session,
            HistoryRecord(
                toolId = "regex",
                operation = "s",
                summary = "s",
                input = "a",
                output = "b",
                options = "",
                createdAt = "0",
            ),
        )
        assertFalse(session.options.global)
        assertTrue(session.options.ignoreCase)
    }
}
