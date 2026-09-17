package com.rememberber.mootool.next.compose.domain

import com.rememberber.mootool.next.compose.model.HistoryRecord
import com.rememberber.mootool.next.compose.sessions.CronSession

/** F16 历史恢复：表达式、运行行与 options 时区（对齐 Electron `extraData.timeZone`）。 */
object CronHistoryRestore {
    fun apply(session: CronSession, item: HistoryRecord, language: String) {
        session.expression = item.input.trim()
        session.error = ""
        runCatching { session.fields = CronEngine.split(session.expression) }
        session.runs = item.output.split('\n').filter { it.isNotBlank() }
        CronHistoryMetadata.parseTimeZone(item.options)?.let { session.zone = it }
        session.description = runCatching {
            CronEngine.describe(session.expression, language)
        }.getOrDefault("")
        session.error = ""
    }
}
