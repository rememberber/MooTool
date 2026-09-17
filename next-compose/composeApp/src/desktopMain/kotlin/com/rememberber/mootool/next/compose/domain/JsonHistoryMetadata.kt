package com.rememberber.mootool.next.compose.domain

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** F04 JSON 历史 `options`（对齐 Electron `HistoryDialog` 仅恢复正文；path 查询另存 marker）。 */
object JsonHistoryMetadata {
    private val codec = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    const val KIND_EDITOR = "editor"
    const val KIND_PATH_QUERY = "pathQuery"

    @Serializable
    data class Wire(val kind: String = KIND_EDITOR)

    fun encodeEditor(): String = codec.encodeToString(Wire(KIND_EDITOR))

    fun encodePathQuery(): String = codec.encodeToString(Wire(KIND_PATH_QUERY))

    fun decode(options: String): Wire {
        val trimmed = options.trim()
        if (trimmed.isEmpty()) return Wire(KIND_EDITOR)
        return runCatching { codec.decodeFromString<Wire>(trimmed) }.getOrDefault(Wire(KIND_EDITOR))
    }

    fun isPathQuery(options: String): Boolean = decode(options).kind == KIND_PATH_QUERY
}
