package com.rememberber.mootool.next.compose.domain

/** F08 会话持久化 tab/scope/query（无通用历史；对齐 Electron 模块级 UI 状态）。 */
object EnvSessionMetadata {
    const val TAB_ENVIRONMENT = "environment"
    const val TAB_RUNTIME = "runtime"

    const val SCOPE_USER = "user"
    const val SCOPE_SYSTEM = "system"
    const val SCOPE_PROCESS = "process"

    fun normalizeTab(raw: String): String =
        when (raw.trim().lowercase()) {
            TAB_RUNTIME -> TAB_RUNTIME
            else -> TAB_ENVIRONMENT
        }

    fun normalizeScope(raw: String): String =
        when (raw.trim().lowercase()) {
            SCOPE_USER -> SCOPE_USER
            SCOPE_SYSTEM -> SCOPE_SYSTEM
            else -> SCOPE_PROCESS
        }
}
