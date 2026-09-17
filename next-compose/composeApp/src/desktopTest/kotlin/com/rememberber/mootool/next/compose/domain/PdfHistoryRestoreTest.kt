package com.rememberber.mootool.next.compose.domain

import com.rememberber.mootool.next.compose.model.HistoryRecord
import com.rememberber.mootool.next.compose.sessions.PdfSession
import kotlin.test.Test
import kotlin.test.assertEquals

class PdfHistoryRestoreTest {
    @Test
    fun restoresLastOutputPaths() {
        val session = PdfSession()
        val item = HistoryRecord(
            toolId = "pdf",
            operation = PdfHistoryMetadata.OP_SPLIT,
            summary = "split",
            input = "a.pdf",
            output = "/out/1.pdf\n/out/2.pdf",
            createdAt = "",
        )
        PdfHistoryRestore.apply(session, item)
        assertEquals(listOf("/out/1.pdf", "/out/2.pdf"), session.lastOutputs)
    }
}
