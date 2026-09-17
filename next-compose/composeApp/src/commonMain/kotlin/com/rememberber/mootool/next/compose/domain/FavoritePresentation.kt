package com.rememberber.mootool.next.compose.domain

/** Cron/Regex/Color 收藏夹搜索语义（对齐 Electron favorite 列表 filter）。 */
object FavoritePresentation {
    fun matchesQuery(
        query: String,
        groupFilter: String,
        name: String,
        snippet: String,
        group: String,
    ): Boolean {
        val needle = query.trim()
        val folder = groupFilter.trim()
        if (folder.isNotEmpty() && !group.equals(folder, ignoreCase = true)) return false
        if (needle.isEmpty()) return true
        return name.contains(needle, ignoreCase = true) ||
            snippet.contains(needle, ignoreCase = true) ||
            group.contains(needle, ignoreCase = true)
    }

    fun defaultName(rawName: String, fallbackSnippet: String, maxSnippetChars: Int = 32): String =
        rawName.trim().ifBlank { fallbackSnippet.take(maxSnippetChars) }
}
