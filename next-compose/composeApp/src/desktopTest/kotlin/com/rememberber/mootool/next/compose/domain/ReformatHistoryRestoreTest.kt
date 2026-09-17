package com.rememberber.mootool.next.compose.domain

import com.rememberber.mootool.next.compose.model.HistoryRecord
import com.rememberber.mootool.next.compose.sessions.ReformatSession
import kotlin.test.Test
import kotlin.test.assertEquals

class ReformatHistoryRestoreTest {
    @Test
    fun restoresTextTabOutput() {
        val session = ReformatSession()
        val options = ReformatHistoryMetadata.encode(ReformatType.Java, "text", 4)
        ReformatHistoryRestore.apply(
            session,
            HistoryRecord(
                id = 1,
                toolId = "reformat",
                operation = "s",
                summary = "s",
                input = "in",
                output = "out",
                options = options,
                createdAt = "0",
            ),
        )
        assertEquals(ReformatType.Java, session.type)
        assertEquals(4, session.indent)
        assertEquals("text", session.tab)
        assertEquals("out", session.text)
    }
}
