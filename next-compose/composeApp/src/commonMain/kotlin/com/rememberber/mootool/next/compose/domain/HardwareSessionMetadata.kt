package com.rememberber.mootool.next.compose.domain

/** F25 会话持久化 tab/序列号显示（无通用历史）。 */
object HardwareSessionMetadata {
    private val knownTabs = setOf("system", "cpu", "memory", "storage", "network")

    fun normalizeTab(raw: String): String {
        val trimmed = raw.trim().lowercase()
        return trimmed.takeIf { it in knownTabs } ?: "system"
    }
}
