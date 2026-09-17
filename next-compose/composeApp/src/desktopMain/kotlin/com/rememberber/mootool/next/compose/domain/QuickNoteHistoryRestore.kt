package com.rememberber.mootool.next.compose.domain

import com.rememberber.mootool.next.compose.model.HistoryRecord
import com.rememberber.mootool.next.compose.sessions.QuickNoteSession

/** F01 历史恢复（对齐 Electron：仅写回正文，不写 restored notice）。 */
object QuickNoteHistoryRestore {
    fun apply(session: QuickNoteSession, item: HistoryRecord) {
        session.error = ""
        session.notice = ""
    }

    fun editorText(item: HistoryRecord): String = item.input
}
