package com.rememberber.mootool.next.compose.domain

import kotlinx.serialization.json.Json

/** F15 历史 `options` / Electron `extraData.options`。 */
object RegexHistoryMetadata {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    fun encode(options: RegexOptions): String =
        json.encodeToString(RegexOptions.serializer(), options)

    fun decode(options: String, fallback: RegexOptions = RegexOptions()): RegexOptions {
        val trimmed = options.trim()
        if (trimmed.isEmpty()) return fallback
        return runCatching { json.decodeFromString(RegexOptions.serializer(), trimmed) }.getOrDefault(fallback)
    }
}
