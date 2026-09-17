package com.rememberber.mootool.next.compose.domain

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** F18 历史 `options` / Electron `extraData`（zone + unit）。 */
@Serializable
data class TimeHistoryMeta(
    val zone: String = "",
    val unit: String = "second",
)

object TimeHistoryMetadata {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    fun encode(zone: String, unit: TimestampUnit): String {
        val unitName = if (unit == TimestampUnit.Millisecond) "millisecond" else "second"
        return json.encodeToString(TimeHistoryMeta.serializer(), TimeHistoryMeta(zone.trim(), unitName))
    }

    fun decode(options: String): TimeHistoryMeta? {
        val trimmed = options.trim()
        if (trimmed.isEmpty()) return null
        return runCatching { json.decodeFromString(TimeHistoryMeta.serializer(), trimmed) }.getOrNull()
    }

    fun unitFromMeta(meta: TimeHistoryMeta): TimestampUnit =
        if (meta.unit.trim().equals("millisecond", ignoreCase = true)) TimestampUnit.Millisecond else TimestampUnit.Second
}
