package com.rememberber.mootool.next.compose.domain

/** F04 JSON Vault 搜索 query 规范化（与 `VaultSearchIndex` 共用，可单测）。 */
object JsonVaultSearchPresentation {
    fun normalizeQuery(raw: String): String = raw.trim().lowercase()

    fun isFiltering(raw: String): Boolean = normalizeQuery(raw).isNotEmpty()

    fun pathSegmentMatches(relativePath: String, name: String, needle: String): Boolean {
        if (needle.isEmpty()) return true
        val lowerPath = relativePath.lowercase()
        val lowerName = name.lowercase()
        return lowerPath.contains(needle) || lowerName.contains(needle)
    }

    fun titleMatches(title: String, needle: String): Boolean =
        needle.isNotEmpty() && title.lowercase().contains(needle)

    fun contentMatches(content: String, needle: String, includeContent: Boolean): Boolean =
        includeContent && needle.isNotEmpty() && content.lowercase().contains(needle)
}
