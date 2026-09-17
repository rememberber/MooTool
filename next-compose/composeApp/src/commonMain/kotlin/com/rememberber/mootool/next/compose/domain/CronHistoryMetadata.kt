package com.rememberber.mootool.next.compose.domain

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** Cron history `options` / Electron `extraData` timezone payload. */
@Serializable
data class CronHistoryMeta(
    @SerialName("timeZone") val timeZone: String = "",
)

object CronHistoryMetadata {
    private val json = Json { ignoreUnknownKeys = true }

    fun encodeTimeZone(zone: String): String {
        val trimmed = zone.trim()
        if (trimmed.isEmpty()) return ""
        return json.encodeToString(CronHistoryMeta.serializer(), CronHistoryMeta(trimmed))
    }

    /** Accepts Electron JSON (`{"timeZone":"…"}`) or legacy plain IANA id in `options`. */
    fun parseTimeZone(options: String): String? {
        val trimmed = options.trim()
        if (trimmed.isEmpty()) return null
        if (trimmed.startsWith("{")) {
            return runCatching {
                json.decodeFromString(CronHistoryMeta.serializer(), trimmed).timeZone.trim().takeIf { it.isNotEmpty() }
            }.getOrNull()
        }
        return trimmed
    }
}
