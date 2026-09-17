package com.rememberber.mootool.next.compose.domain

import com.rememberber.mootool.next.compose.model.HistoryRecord
import com.rememberber.mootool.next.compose.sessions.JsonSession
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class JsonHistoryRestoreTest {
    @Test
    fun restoresFormattedEditorText() {
        val session = JsonSession()
        session.pathResult = "stale"
        val item = HistoryRecord(
            id = 1,
            toolId = "json",
            operation = "fmt",
            summary = "fmt",
            input = """{"a":1}""",
            output = """{
  "a": 1
}""",
            options = JsonHistoryMetadata.encodeEditor(),
            createdAt = "0",
        )
        JsonHistoryRestore.apply(session, item)
        assertEquals("""{
  "a": 1
}""", JsonHistoryRestore.editorText(item))
        assertEquals("", session.pathResult)
    }

    @Test
    fun pathQueryRestoresResultWithoutEditorMutation() {
        val session = JsonSession()
        val item = HistoryRecord(
            id = 2,
            toolId = "json",
            operation = "path",
            summary = "JSONPath",
            input = """{"a":1}""",
            output = "1",
            options = JsonHistoryMetadata.encodePathQuery(),
            createdAt = "0",
        )
        JsonHistoryRestore.apply(session, item)
        assertNull(JsonHistoryRestore.editorText(item))
        assertEquals("1", session.pathResult)
        assertEquals("JSONPath", session.dialogTitle)
    }
}
