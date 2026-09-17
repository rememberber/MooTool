package com.rememberber.mootool.next.compose.domain

import com.rememberber.mootool.next.compose.model.HistoryRecord
import com.rememberber.mootool.next.compose.sessions.CodeRunSession
import kotlin.test.Test
import kotlin.test.assertEquals

class CodeRunHistoryRestoreTest {
    @Test
    fun restoresCodeAndRuntimeTab() {
        val session = CodeRunSession()
        val options = CodeRunHistoryMetadata.encode(CodeRuntime.Node, "", "")
        CodeRunHistoryRestore.apply(
            session,
            HistoryRecord(
                id = 1,
                toolId = "java",
                operation = "Node.js",
                summary = "Node.js · 0",
                input = "console.log(1)",
                output = "1",
                options = options,
                createdAt = "0",
            ),
        )
        assertEquals("node", session.tab)
        assertEquals("console.log(1)", session.nodeEditor.text)
    }
}
