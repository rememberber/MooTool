package com.rememberber.mootool.next.compose.domain

/**
 * Live wiring between global **tools** defaults and open tool sessions.
 *
 * Electron reference: QR / crypto random length initialize from settings; changing defaults in
 * `SettingsWindow` should be reflected while the tool stays open (Compose mirrors JSON softWrap sync).
 */
object ToolsSettingsLiveApply {
    fun qrCodeSize(settingsQrCodeSize: Int): Int =
        SettingsNumericBounds.clampNumber(settingsQrCodeSize, 120, 2_000)

    fun qrErrorCorrection(settingsValue: String, fallback: String = "M"): String =
        SettingsNumericBounds.normalizeQrErrorCorrection(settingsValue, fallback)

    fun randomStringLength(settingsRandomStringLength: Int): Int =
        SettingsNumericBounds.clampNumber(settingsRandomStringLength, 1, 4_096)
}
