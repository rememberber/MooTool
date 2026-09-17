package com.rememberber.mootool.next.compose.domain

import com.rememberber.mootool.next.compose.model.HistoryRecord
import com.rememberber.mootool.next.compose.sessions.TimeSession

/** F18 历史恢复（对齐 Electron `HistoryDialog` + `extraData` zone/unit）。 */
object TimeHistoryRestore {
    private val timestampPattern = Regex("^[+-]?\\d+$")

    fun apply(session: TimeSession, item: HistoryRecord) {
        TimeHistoryMetadata.decode(item.options)?.let { meta ->
            if (meta.zone.isNotBlank()) session.zone = meta.zone
            session.unit = TimeHistoryMetadata.unitFromMeta(meta)
        }
        val input = item.input.trim()
        val output = item.output.trim()
        when {
            timestampPattern.matches(input) -> {
                session.timestamp = input
                if (output.isNotEmpty()) session.localTime = output
            }
            timestampPattern.matches(output) -> {
                session.localTime = input.ifBlank { session.localTime }
                session.timestamp = output
            }
            else -> {
                val value = output.ifBlank { input }
                if (timestampPattern.matches(value)) session.timestamp = value else session.localTime = value
            }
        }
        session.error = ""
    }
}
