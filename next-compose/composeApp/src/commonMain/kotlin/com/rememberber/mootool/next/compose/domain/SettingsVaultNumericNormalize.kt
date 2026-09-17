package com.rememberber.mootool.next.compose.domain

/** Vault Git 自动提交/拉取数值失焦提交（对齐 Electron `NumberSetting` + `clampNumber`）。 */
object SettingsVaultNumericNormalize {
    fun commitAutoCommitIdleSeconds(raw: String): VaultNumericCommitResult =
        commitBoundedInt(raw, minimum = 5, maximum = 3_600)

    fun commitAutoCommitInactiveSeconds(raw: String): VaultNumericCommitResult =
        commitBoundedInt(raw, minimum = 5, maximum = 3_600)

    fun commitAutoPullMinutes(raw: String): VaultNumericCommitResult =
        commitBoundedInt(raw, minimum = 0, maximum = 1_440)

    private fun commitBoundedInt(raw: String, minimum: Int, maximum: Int): VaultNumericCommitResult {
        val trimmed = raw.trim()
        val parsed = trimmed.toIntOrNull() ?: return VaultNumericCommitResult.Rejected
        return VaultNumericCommitResult.Accepted(SettingsNumericBounds.clampNumber(parsed, minimum, maximum))
    }
}

sealed class VaultNumericCommitResult {
    data class Accepted(val value: Int) : VaultNumericCommitResult()

    data object Rejected : VaultNumericCommitResult()
}
