package com.rememberber.mootool.next.compose.domain

import com.rememberber.mootool.next.compose.model.HistoryRecord
import com.rememberber.mootool.next.compose.sessions.DiffSession
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DiffHistoryRestoreTest {
    @Test
    fun restoresSidesOptionsAndHighlightMode() {
        val session = DiffSession()
        session.highlightMode = "character"
        val options = DiffHistoryMetadata.encode(ignoreWhitespace = true, highlightMode = "line")
        DiffHistoryRestore.apply(
            session,
            HistoryRecord(
                id = 1,
                toolId = "diff",
                operation = "compare",
                summary = "s",
                input = "left\n",
                output = "right\n",
                options = options,
                createdAt = "0",
            ),
        )
        assertEquals("left\n", session.left)
        assertEquals("right\n", session.right)
        assertTrue(session.ignoreWhitespace)
        assertEquals("line", session.highlightMode)
    }
}
