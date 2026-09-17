package com.rememberber.mootool.next.compose.domain

import com.rememberber.mootool.next.compose.model.HistoryRecord
import com.rememberber.mootool.next.compose.sessions.EncodeSession
import kotlin.test.Test
import kotlin.test.assertEquals

class EncodeHistoryRestoreTest {
    @Test
    fun restoresTabCharsetAndReverseDirection() {
        val session = EncodeSession()
        val options = EncodeHistoryMetadata.encode(EncodeTab.Url, forward = false, UrlCharset.Gb2312, AsciiFormat.Hex)
        EncodeHistoryRestore.apply(
            session,
            HistoryRecord(
                id = 1,
                toolId = "encode",
                operation = "s",
                summary = "s",
                input = "in",
                output = "out",
                options = options,
                createdAt = "0",
            ),
        )
        assertEquals(EncodeTab.Url, session.tab)
        assertEquals(UrlCharset.Gb2312, session.charset)
        assertEquals("out", session.left())
        assertEquals("in", session.right())
    }
}
