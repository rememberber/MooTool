package com.rememberber.mootool.next.compose.domain

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** F09 HTTP 历史：`operation`=方法名，`options`=状态码 JSON/纯数字。 */
@Serializable
private data class HttpHistoryMeta(val status: Int? = null)

object HttpHistoryMetadata {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    fun encodeStatus(status: Int): String = json.encodeToString(HttpHistoryMeta.serializer(), HttpHistoryMeta(status = status))

    fun decodeStatus(options: String): Int? {
        val trimmed = options.trim()
        if (trimmed.isEmpty()) return null
        trimmed.toIntOrNull()?.let { return it }
        return runCatching { json.decodeFromString<HttpHistoryMeta>(trimmed).status }.getOrNull()
    }

    fun parseMethod(operation: String, summary: String = ""): HttpMethod? {
        val op = operation.trim()
        HttpMethod.entries.find { it.name == op }?.let { return it }
        val prefix = summary.trim().substringBefore(' ').trim()
        return HttpMethod.entries.find { it.name.equals(prefix, ignoreCase = true) }
    }
}
