package com.rememberber.mootool.next.compose.sessions

import com.rememberber.mootool.next.compose.domain.BoardAlignment
import com.rememberber.mootool.next.compose.domain.BoardTheme
import com.rememberber.mootool.next.compose.domain.MessageBoardSessionRestore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MessageBoardSessionRestoreTest {
    @Test
    fun restoresThemeAlignmentAndClearsPresenting() {
        val session = MessageBoardSession().apply {
            message = "old"
            presenting = true
            displayAwake = true
            notice = "n"
        }
        MessageBoardSessionRestore.apply(
            session,
            MessageBoardSessionSnapshot(
                message = "x".repeat(80),
                theme = "midnight",
                alignment = "left",
                size = 200,
            ),
        )
        assertEquals("x".repeat(80), session.message)
        assertEquals(BoardTheme.Midnight, session.theme)
        assertEquals(BoardAlignment.Left, session.alignment)
        assertEquals(130, session.size)
        assertTrue(session.restored)
        assertFalse(session.presenting)
        assertFalse(session.displayAwake)
        assertEquals("", session.notice)
    }
}
