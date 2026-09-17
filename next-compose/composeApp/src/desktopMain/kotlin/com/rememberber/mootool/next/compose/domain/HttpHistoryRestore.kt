package com.rememberber.mootool.next.compose.domain

import com.rememberber.mootool.next.compose.model.HistoryRecord
import com.rememberber.mootool.next.compose.sessions.HttpSession

/** F09 HTTP 历史恢复 URL + 方法。 */
object HttpHistoryRestore {
    fun apply(session: HttpSession, item: HistoryRecord) {
        session.url = item.input
        HttpHistoryMetadata.parseMethod(item.operation, item.summary)?.let { session.method = it }
    }
}
