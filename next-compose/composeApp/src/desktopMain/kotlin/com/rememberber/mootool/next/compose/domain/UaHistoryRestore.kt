package com.rememberber.mootool.next.compose.domain

import com.rememberber.mootool.next.compose.model.HistoryRecord
import com.rememberber.mootool.next.compose.sessions.UaSession
import kotlinx.serialization.json.Json

object UaHistoryRestore {
    private val resultCodec = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    fun apply(session: UaSession, item: HistoryRecord) {
        session.source = item.input
        session.result = runCatching { resultCodec.decodeFromString<UaResult>(item.output) }.getOrNull()
        session.error = ""
    }
}
