package com.rememberber.mootool.next.compose.domain

/** A03 Vault Git 面板 load/操作/flush/remote 校验失败 toast 呈现（对齐 Electron `VaultGitDialog`）。 */
object VaultGitToastPresentation {
    fun shouldToastPanelFailure(): Boolean = true

    fun shouldToastActionFailure(): Boolean = true

    fun shouldToastFlushBlocked(): Boolean = true

    fun shouldToastInvalidRemote(): Boolean = true

    fun panelFailureMessage(error: Throwable?, fallback: String): String =
        error?.message?.takeIf { it.isNotBlank() } ?: fallback

    fun actionFailureMessage(message: String, fallback: String): String =
        message.ifBlank { fallback }
}
