package com.rememberber.mootool.next.compose.domain

/** F04 JSON Vault 搜索 query 规范化（与 `VaultSearchIndex` 共用，可单测）。 */
object JsonVaultSearchPresentation {
    fun normalizeQuery(raw: String): String = raw.trim().lowercase()

    fun isFiltering(raw: String): Boolean = normalizeQuery(raw).isNotEmpty()
}
