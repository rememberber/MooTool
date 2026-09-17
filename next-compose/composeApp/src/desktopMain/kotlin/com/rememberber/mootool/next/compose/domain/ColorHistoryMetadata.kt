package com.rememberber.mootool.next.compose.domain

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** F22 调色板历史 `options`（格式 + 操作 wire）。 */
@Serializable
data class ColorHistoryMeta(
    val format: String = ColorFormat.HEX_UPPER.name,
    val operation: String = "pick",
)

object ColorHistoryMetadata {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    fun encode(format: ColorFormat, operation: String): String =
        json.encodeToString(ColorHistoryMeta.serializer(), ColorHistoryMeta(format = format.name, operation = operation))

    fun decode(options: String): ColorHistoryMeta? {
        val trimmed = options.trim()
        if (trimmed.isEmpty()) return null
        runCatching { json.decodeFromString<ColorHistoryMeta>(trimmed) }.getOrNull()?.let { return it }
        return legacyMeta(trimmed)
    }

    fun parseFormat(meta: ColorHistoryMeta?): ColorFormat? {
        meta ?: return null
        return ColorFormat.entries.find { it.name == meta.format }
    }

    private fun legacyMeta(raw: String): ColorHistoryMeta? {
        ColorFormat.entries.find { it.name == raw }?.let { return ColorHistoryMeta(format = it.name, operation = "pick") }
        ColorOperation.entries.find { it.name == raw }?.let {
            return ColorHistoryMeta(format = ColorFormat.HEX_UPPER.name, operation = it.name)
        }
        if (raw == "swap") return ColorHistoryMeta(operation = "swap")
        return null
    }
}
