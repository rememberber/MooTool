package com.rememberber.mootool.next.compose.domain

import com.rememberber.mootool.next.compose.model.HistoryRecord
import com.rememberber.mootool.next.compose.sessions.DiffSession

object DiffHistoryRestore {
    fun apply(session: DiffSession, item: HistoryRecord) {
        val meta = DiffHistoryMetadata.decode(item.options, session.highlightMode)
        session.ignoreWhitespace = meta.ignoreWhitespace
        session.highlightMode = meta.highlightMode
        session.left = item.input
        session.right = item.output
    }
}
