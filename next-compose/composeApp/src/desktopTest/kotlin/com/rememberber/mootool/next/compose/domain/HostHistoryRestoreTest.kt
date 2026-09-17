package com.rememberber.mootool.next.compose.domain

import com.rememberber.mootool.next.compose.model.HistoryRecord
import com.rememberber.mootool.next.compose.sessions.HostSession
import kotlin.test.Test
import kotlin.test.assertEquals

class HostHistoryRestoreTest {
    @Test
    fun restoresEditorContent() {
        val session = HostSession()
        session.content = "old"
        HostHistoryRestore.apply(
            session,
            HistoryRecord(
                id = 1,
                toolId = "host",
                operation = HostHistoryMetadata.OPERATION_APPLY,
                summary = "hosts",
                input = "127.0.0.1 example.test",
                output = "/etc/hosts",
                options = "/tmp/hosts.bak",
                createdAt = "0",
            ),
        )
        assertEquals("127.0.0.1 example.test", session.content)
        assertEquals("", session.error)
    }
}
