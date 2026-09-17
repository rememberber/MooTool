package com.rememberber.mootool.next.compose.domain

/** F01 随手记历史 `options`：保存时的 Vault 相对路径（summary 同源，便于跨实现导入）。 */
object QuickNoteHistoryMetadata {
    fun encode(relativePath: String): String = relativePath.trim()

    fun decode(options: String, fallbackSummary: String = ""): String {
        val trimmed = options.trim()
        return trimmed.ifEmpty { fallbackSummary.trim() }
    }
}
