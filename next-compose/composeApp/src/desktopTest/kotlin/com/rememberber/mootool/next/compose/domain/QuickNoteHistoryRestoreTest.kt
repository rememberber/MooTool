package com.rememberber.mootool.next.compose.domain

import com.rememberber.mootool.next.compose.model.HistoryRecord
import com.rememberber.mootool.next.compose.sessions.QuickNoteSession
import kotlin.test.Test
import kotlin.test.assertEquals

class QuickNoteHistoryRestoreTest {
    @Test
    fun restoresInputAndClearsNotices() {
        val session = QuickNoteSession()
        session.notice = "saved"
        session.error = "err"
        val item = HistoryRecord(
            id = 1,
            toolId = "quickNote",
            operation = "note.md",
            summary = "note.md",
            input = "hello",
            output = "",
            options = QuickNoteHistoryMetadata.encode("note.md"),
            createdAt = "0",
        )
        QuickNoteHistoryRestore.apply(session, item)
        assertEquals("hello", QuickNoteHistoryRestore.editorText(item))
        assertEquals("", session.notice)
        assertEquals("", session.error)
        assertEquals("note.md", QuickNoteHistoryMetadata.decode(item.options, item.summary))
    }
}
