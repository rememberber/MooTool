package com.rememberber.mootool.next.compose.ui.workbench

object CopyFeedbackPolicy {
    const val IDLE = "idle"
    const val COPIED = "copied"
    const val FAILED = "failed"
    const val RESET_MS = 1400L

    fun afterCopy(success: Boolean): String = if (success) COPIED else FAILED

    fun buttonKey(state: String, idleKey: String = "json.action.copy"): String = when (state) {
        COPIED -> "json.action.copied"
        FAILED -> "json.notice.copyFailed"
        else -> idleKey
    }
}
