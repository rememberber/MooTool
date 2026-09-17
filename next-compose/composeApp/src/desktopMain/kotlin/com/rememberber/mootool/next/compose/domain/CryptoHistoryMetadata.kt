package com.rememberber.mootool.next.compose.domain

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class CryptoHistoryMeta(
    val tab: String = "symmetric",
    val operation: String = "",
    val algorithm: String = "",
)

object CryptoHistoryMetadata {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    fun encode(tab: String, operation: String, algorithm: String): String =
        json.encodeToString(CryptoHistoryMeta.serializer(), CryptoHistoryMeta(tab, operation, algorithm))

    fun decode(options: String): CryptoHistoryMeta? {
        val trimmed = options.trim()
        if (trimmed.isEmpty()) return null
        return runCatching { json.decodeFromString(CryptoHistoryMeta.serializer(), trimmed) }.getOrNull()
    }
}
