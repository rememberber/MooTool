package com.rememberber.mootool.next.compose.domain

import com.rememberber.mootool.next.compose.model.HistoryRecord
import com.rememberber.mootool.next.compose.sessions.RegexSession

/** F15 历史恢复（对齐 Electron `onApplyRecord`：pattern/source + flags 并重跑匹配）。 */
object RegexHistoryRestore {
    fun apply(session: RegexSession, item: HistoryRecord) {
        session.pattern = item.input
        session.source = item.output
        session.options = RegexHistoryMetadata.decode(item.options, session.options)
        session.tab = "test"
    }
}
