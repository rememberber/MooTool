package com.rememberber.mootool.next.compose.domain

import com.rememberber.mootool.next.compose.model.HistoryRecord
import com.rememberber.mootool.next.compose.sessions.ReformatSession

object ReformatHistoryRestore {
    fun apply(session: ReformatSession, item: HistoryRecord) {
        val meta = ReformatHistoryMetadata.decode(item.options)
        meta.type?.let { session.type = it }
        session.indent = meta.indent
        if (meta.tab == "file") {
            session.tab = "file"
            session.fileSource = item.input
            session.fileResult = item.output
            session.fileName = meta.fileName
        } else {
            session.tab = "text"
            session.text = item.output.ifEmpty { item.input }
        }
        session.error = ""
    }
}
