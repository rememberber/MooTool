package com.rememberber.mootool.next.compose.domain

/**
 * HTTP 工具内超时与 `settings.network.requestTimeoutMs` 同步（对照 Electron `commitTimeout`）。
 */
object HttpTimeoutSettings {
    fun clamp(value: Int): Int = HttpEngine.clampTimeout(value)

    fun commit(editedMs: Int, globalMs: Int): CommitResult {
        val next = clamp(editedMs)
        return CommitResult(sessionMs = next, updateGlobal = next != globalMs)
    }
}

data class CommitResult(val sessionMs: Int, val updateGlobal: Boolean)
