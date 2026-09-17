package com.rememberber.mootool.next.compose.domain

import com.rememberber.mootool.next.compose.model.HistoryRecord
import com.rememberber.mootool.next.compose.sessions.HostSession

object HostHistoryRestore {
    fun apply(session: HostSession, item: HistoryRecord) {
        session.content = item.input
        session.error = ""
    }
}
