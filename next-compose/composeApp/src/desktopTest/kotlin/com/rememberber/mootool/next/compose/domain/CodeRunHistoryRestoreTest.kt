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

    @Test
    fun infersRuntimeFromSummaryUsingWiringDisplayNames() {
        val session = CodeRunSession()
        CodeRunHistoryRestore.apply(
            session,
            HistoryRecord(
                id = 2,
                toolId = "java",
                operation = "Groovy",
                summary = "Groovy · 1",
                input = "println 42",
                output = "",
                options = "",
                createdAt = "0",
            ),
        )
        assertEquals("java", session.tab)
        assertEquals("groovy", session.javaMode)
        assertEquals("println 42", session.groovyEditor.text)
    }

    @Test
    fun infersJavaRuntimeFromSummaryWhenOptionsMissing() {
        val session = CodeRunSession()
        CodeRunHistoryRestore.apply(
            session,
            HistoryRecord(
                id = 3,
                toolId = "java",
                operation = "Java",
                summary = "Java · 2",
                input = "System.out.println(1);",
                output = "",
                options = "",
                createdAt = "0",
            ),
        )
        assertEquals("java", session.tab)
        assertEquals("java", session.javaMode)
        assertEquals("System.out.println(1);", session.javaEditor.text)
    }

    @Test
    fun infersPythonRuntimeFromSummaryWhenOptionsMissing() {
        val session = CodeRunSession()
        CodeRunHistoryRestore.apply(
            session,
            HistoryRecord(
                id = 4,
                toolId = "java",
                operation = "Python",
                summary = "Python · 3",
                input = "print(1)",
                output = "",
                options = "",
                createdAt = "0",
            ),
        )
        assertEquals("python", session.tab)
        assertEquals("print(1)", session.pythonEditor.text)
    }
}
