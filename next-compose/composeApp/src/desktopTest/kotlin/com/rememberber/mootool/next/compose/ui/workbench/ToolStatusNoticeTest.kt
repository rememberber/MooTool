package com.rememberber.mootool.next.compose.ui.workbench

import com.rememberber.mootool.next.compose.sessions.CodeRunSession
import com.rememberber.mootool.next.compose.sessions.PdfSession
import kotlin.test.Test
import kotlin.test.assertEquals

class ToolStatusNoticeTest {
    @Test
    fun applyUserEditClearingStatusNotice_clears_non_blank_notice() {
        var notice = "saved"
        applyUserEditClearingStatusNotice({ notice }, { notice = it }) { }
        assertEquals("", notice)
    }

    @Test
    fun codeRun_clearErrorOnUserEdit_clears_error() {
        val session = CodeRunSession().apply { error = "failed" }
        session.clearErrorOnUserEdit { }
        assertEquals("", session.error)
    }

    @Test
    fun pdf_onUserInput_clears_notice() {
        val session = PdfSession().apply { notice = "merge complete" }
        session.onUserInput { }
        assertEquals("", session.notice)
    }

    @Test
    fun applyUserEditClearingStatusNotice_still_runs_edit_when_notice_empty() {
        var notice = ""
        var edited = false
        applyUserEditClearingStatusNotice({ notice }, { notice = it }) { edited = true }
        assertEquals("", notice)
        assertEquals(true, edited)
    }
}
