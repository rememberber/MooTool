package com.rememberber.mootool.next.compose.domain

import com.rememberber.mootool.next.compose.model.HistoryRecord
import com.rememberber.mootool.next.compose.sessions.PdfSession

/** F24 PDF 历史恢复：最近输出路径列表。 */
object PdfHistoryRestore {
    fun apply(session: PdfSession, item: HistoryRecord) {
        session.lastOutputs = item.output.lines().map { it.trim() }.filter { it.isNotEmpty() }
    }
}
