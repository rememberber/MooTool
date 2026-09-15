package com.rememberber.mootool.next.compose.ui.workbench

import kotlin.test.Test
import kotlin.test.assertEquals

class CopyFeedbackPolicyTest {
    @Test
    fun copiedAndFailedResetAfterElectronDelay() {
        assertEquals(1400L, CopyFeedbackPolicy.RESET_MS)
        assertEquals(CopyFeedbackPolicy.COPIED, CopyFeedbackPolicy.afterCopy(true))
        assertEquals(CopyFeedbackPolicy.FAILED, CopyFeedbackPolicy.afterCopy(false))
        assertEquals("json.action.copied", CopyFeedbackPolicy.buttonKey(CopyFeedbackPolicy.COPIED))
        assertEquals("json.notice.copyFailed", CopyFeedbackPolicy.buttonKey(CopyFeedbackPolicy.FAILED))
        assertEquals("json.action.copy", CopyFeedbackPolicy.buttonKey(CopyFeedbackPolicy.IDLE))
        assertEquals("json.action.copy", CopyFeedbackPolicy.buttonKey(""))
        assertEquals("reformat.copy", CopyFeedbackPolicy.buttonKey(CopyFeedbackPolicy.IDLE, "reformat.copy"))
        assertEquals("json.action.copied", CopyFeedbackPolicy.buttonKey(CopyFeedbackPolicy.COPIED, "reformat.copy"))
    }
}
