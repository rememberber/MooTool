package com.rememberber.mootool.next.compose.domain

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class ProtobufHistoryMeta(
    val tab: String = "",
    val operation: String = "",
    val messageName: String = "",
    val format: String = "",
)

/** F07 历史 options：新 JSON + 旧 `tab|operation|…` 管道格式。 */
object ProtobufHistoryMetadata {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    fun encode(tab: String, operation: String, messageName: String = "", format: String = ""): String =
        json.encodeToString(
            ProtobufHistoryMeta.serializer(),
            ProtobufHistoryMeta(tab = tab, operation = operation, messageName = messageName, format = format),
        )

    fun encodeLegacyPipe(options: String): String {
        val legacy = decodeLegacyPipe(options)
        return encode(legacy.tab, legacy.operation, legacy.messageName, legacy.format)
    }

    fun decode(options: String): ProtobufHistoryMeta? {
        val trimmed = options.trim()
        if (trimmed.isEmpty()) return null
        if (trimmed.startsWith("{")) {
            return runCatching { json.decodeFromString(ProtobufHistoryMeta.serializer(), trimmed) }.getOrNull()
        }
        return decodeLegacyPipe(trimmed)
    }

    private fun decodeLegacyPipe(options: String): ProtobufHistoryMeta {
        val parts = options.split('|')
        val historyTab = parts.getOrNull(0).orEmpty()
        val operation = parts.getOrNull(1).orEmpty()
        val historyMessage = parts.getOrNull(2).orEmpty()
        val historyFormat = parts.getOrNull(3).orEmpty().ifEmpty { parts.getOrNull(2).orEmpty() }
        val messageName = if (historyMessage.isNotEmpty() && historyTab == "json") historyMessage else ""
        val format = if (historyFormat == "Hex" || historyFormat == "Base64") historyFormat else ""
        return ProtobufHistoryMeta(tab = historyTab, operation = operation, messageName = messageName, format = format)
    }
}
