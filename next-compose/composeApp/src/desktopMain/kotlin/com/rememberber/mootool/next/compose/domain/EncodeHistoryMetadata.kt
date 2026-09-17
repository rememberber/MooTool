package com.rememberber.mootool.next.compose.domain

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class EncodeHistoryMeta(
    val tab: String = "unicode",
    val direction: String = "forward",
    val charset: String = "utf-8",
    val asciiFormat: String = "decimal",
)

object EncodeHistoryMetadata {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    fun encode(tab: EncodeTab, forward: Boolean, charset: UrlCharset, asciiFormat: AsciiFormat): String =
        json.encodeToString(
            EncodeHistoryMeta.serializer(),
            EncodeHistoryMeta(
                tab = tab.name.lowercase(),
                direction = if (forward) "forward" else "reverse",
                charset = if (charset == UrlCharset.Gb2312) "gb2312" else "utf-8",
                asciiFormat = if (asciiFormat == AsciiFormat.Hex) "hex" else "decimal",
            ),
        )

    fun decode(options: String): EncodeHistoryMeta? {
        val trimmed = options.trim()
        if (trimmed.isEmpty()) return null
        return runCatching { json.decodeFromString(EncodeHistoryMeta.serializer(), trimmed) }.getOrNull()
    }
}
