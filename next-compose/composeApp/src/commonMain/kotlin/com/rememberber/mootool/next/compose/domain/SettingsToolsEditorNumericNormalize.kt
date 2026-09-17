package com.rememberber.mootool.next.compose.domain

/** 工具默认值 QR/随机长度与编辑器字号失焦提交（对齐 Electron `NumberSetting` + `clampNumber`）。 */
object SettingsToolsNumericNormalize {
    fun commitQrCodeSize(raw: String): NumericSettingCommitResult =
        commitBoundedInt(raw, minimum = 120, maximum = 2_000)

    fun commitRandomStringLength(raw: String): NumericSettingCommitResult =
        commitBoundedInt(raw, minimum = 1, maximum = 4_096)
}

object SettingsEditorNumericNormalize {
    fun commitJsonFontSize(raw: String): NumericSettingCommitResult =
        commitBoundedInt(raw, minimum = 11, maximum = 24)

    fun commitQuickNoteFontSize(raw: String): NumericSettingCommitResult =
        commitBoundedInt(raw, minimum = 11, maximum = 24)
}

sealed class NumericSettingCommitResult {
    data class Accepted(val value: Int) : NumericSettingCommitResult()

    data object Rejected : NumericSettingCommitResult()
}

private fun commitBoundedInt(raw: String, minimum: Int, maximum: Int): NumericSettingCommitResult {
    val trimmed = raw.trim()
    val parsed = trimmed.toIntOrNull() ?: return NumericSettingCommitResult.Rejected
    return NumericSettingCommitResult.Accepted(SettingsNumericBounds.clampNumber(parsed, minimum, maximum))
}
