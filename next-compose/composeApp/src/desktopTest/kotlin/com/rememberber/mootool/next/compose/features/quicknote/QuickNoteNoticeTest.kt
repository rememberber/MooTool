package com.rememberber.mootool.next.compose.features.quicknote

import kotlin.test.Test
import kotlin.test.assertEquals

class QuickNoteNoticeTest {
    @Test
    fun user_edit_clears_notice_unless_column_latch() {
        assertEquals("", quickNoteNoticeOnUserDocumentChange(columnLatch = false, columnEditHint = "hint"))
        assertEquals("hint", quickNoteNoticeOnUserDocumentChange(columnLatch = true, columnEditHint = "hint"))
    }

    @Test
    fun user_edit_clears_stale_error() {
        assertEquals("", quickNoteErrorOnUserDocumentChange())
    }
}
