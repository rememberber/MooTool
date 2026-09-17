package com.rememberber.mootool.next.compose.domain

/** F02 历史 `options`（ignoreWhitespace + highlightMode，兼容旧 `\t` 分隔）。 */
object DiffHistoryMetadata {
    data class Parsed(val ignoreWhitespace: Boolean, val highlightMode: String)

    fun encode(ignoreWhitespace: Boolean, highlightMode: String): String =
        "${ignoreWhitespace}\t${highlightMode.trim()}"

    fun decode(options: String, fallbackHighlight: String = "character"): Parsed {
        val trimmed = options.trim()
        if (trimmed.isEmpty()) return Parsed(ignoreWhitespace = false, highlightMode = fallbackHighlight)
        val parts = trimmed.split('\t', limit = 2)
        val ignore = parts[0].trim().equals("true", ignoreCase = true)
        val mode = parts.getOrNull(1)?.trim().orEmpty().ifBlank { fallbackHighlight }
        return Parsed(ignoreWhitespace = ignore, highlightMode = mode)
    }
}
