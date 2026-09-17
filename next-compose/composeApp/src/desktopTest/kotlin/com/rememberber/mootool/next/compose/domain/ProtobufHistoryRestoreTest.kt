package com.rememberber.mootool.next.compose.domain

import com.rememberber.mootool.next.compose.model.HistoryRecord
import com.rememberber.mootool.next.compose.sessions.ProtobufSession
import kotlin.test.Test
import kotlin.test.assertEquals

class ProtobufHistoryRestoreTest {
    @Test
    fun restoresJsonToBinaryFromMetadata() {
        val session = ProtobufSession()
        val options = ProtobufHistoryMetadata.encode(
            tab = "json",
            operation = "jsonToBinary",
            messageName = "Demo",
            format = "Hex",
        )
        ProtobufHistoryRestore.apply(
            session,
            HistoryRecord(
                toolId = "protobuf",
                operation = "jsonToBinary",
                summary = "json→binary",
                input = """{"ok":true}""",
                output = "0a0101",
                options = options,
                createdAt = "",
            ),
        )
        assertEquals("json", session.tab)
        assertEquals("Demo", session.messageName)
        assertEquals("""{"ok":true}""", session.json)
        assertEquals("0a0101", session.binary)
        assertEquals(ProtobufSession.formatOf("Hex"), session.format)
        assertEquals("", session.error)
    }
}
