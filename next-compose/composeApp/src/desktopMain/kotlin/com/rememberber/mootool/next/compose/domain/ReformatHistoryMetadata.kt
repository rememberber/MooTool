package com.rememberber.mootool.next.compose.domain

/** F03 历史 `options`（`type|tab|indent|fileName?`，对齐 Electron `ReformatTool`）。 */
object ReformatHistoryMetadata {
    data class Parsed(
        val type: ReformatType?,
        val tab: String,
        val indent: Int,
        val fileName: String,
    )

    fun encode(type: ReformatType, tab: String, indent: Int, fileName: String = ""): String =
        if (tab == "file") {
            "${type.name.lowercase()}|$tab|$indent|$fileName"
        } else {
            "${type.name.lowercase()}|$tab|$indent"
        }

    fun decode(options: String): Parsed {
        val parts = options.split('|')
        val historyType = parts.getOrNull(0).orEmpty()
        val historyTab = parts.getOrNull(1).orEmpty().ifBlank { "text" }
        val historyIndent = parts.getOrNull(2)?.toIntOrNull()?.coerceIn(2, 6) ?: 4
        val type = ReformatType.entries.firstOrNull { it.name.equals(historyType, ignoreCase = true) }
        val fileName = parts.getOrNull(3).orEmpty()
        return Parsed(type = type, tab = historyTab, indent = historyIndent, fileName = fileName)
    }
}
