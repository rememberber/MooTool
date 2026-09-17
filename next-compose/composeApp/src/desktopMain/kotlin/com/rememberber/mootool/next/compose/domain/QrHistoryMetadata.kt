package com.rememberber.mootool.next.compose.domain

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** F19 QR 历史 `options`（对齐 Electron `QrCodeTool` `extraData` JSON）。 */
object QrHistoryMetadata {
    private val codec = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    @Serializable
    data class Wire(
        val operation: String,
        val size: Int = QrEngine.DEFAULT_SIZE,
        val correction: String = QrErrorCorrection.M.name,
    )

    fun encodeGenerate(size: Int, correction: QrErrorCorrection): String =
        codec.encodeToString(Wire("generate", size, correction.name))

    fun encodeRecognize(): String = codec.encodeToString(Wire("recognize"))

    fun decode(options: String): Wire? {
        val trimmed = options.trim()
        if (trimmed.isEmpty()) return null
        return runCatching { codec.decodeFromString<Wire>(trimmed) }.getOrNull()
    }
}
