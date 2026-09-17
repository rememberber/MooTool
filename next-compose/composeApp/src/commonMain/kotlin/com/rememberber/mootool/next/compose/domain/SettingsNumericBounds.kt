package com.rememberber.mootool.next.compose.domain

import com.rememberber.mootool.next.compose.model.AppSettings

/** Aligns with Electron `clampNumber` / `normalizeSettings` in `next/src/shared/contracts/settings.ts`. */
object SettingsNumericBounds {
    fun clampNumber(value: Int, minimum: Int, maximum: Int): Int = value.coerceIn(minimum, maximum)

    fun normalizeTranslationProvider(value: String, fallback: String): String =
        when (value.lowercase()) {
            "google", "bing" -> value.lowercase()
            else -> fallback
        }

    fun normalizeQrErrorCorrection(value: String, fallback: String): String =
        when (value.uppercase()) {
            "L", "M", "Q", "H" -> value.uppercase()
            else -> fallback
        }

    fun normalize(settings: AppSettings, defaults: AppSettings = AppSettings.Default): AppSettings =
        settings.copy(
            appearance = settings.appearance.copy(
                fontSize = clampNumber(settings.appearance.fontSize, 12, 18),
            ),
            editor = settings.editor.copy(
                jsonFontSize = clampNumber(settings.editor.jsonFontSize, 11, 24),
                quickNoteFontSize = clampNumber(settings.editor.quickNoteFontSize, 11, 24),
            ),
            network = settings.network.copy(
                requestTimeoutMs = clampNumber(settings.network.requestTimeoutMs, 1_000, 120_000),
                translationTimeoutMs = clampNumber(settings.network.translationTimeoutMs, 1_000, 120_000),
            ),
            vault = settings.vault.copy(
                autoCommitIdleSeconds = clampNumber(settings.vault.autoCommitIdleSeconds, 5, 3_600),
                autoCommitInactiveSeconds = clampNumber(settings.vault.autoCommitInactiveSeconds, 5, 3_600),
                autoPullMinutes = clampNumber(settings.vault.autoPullMinutes, 0, 1_440),
            ),
            tools = settings.tools.copy(
                qrCodeSize = clampNumber(settings.tools.qrCodeSize, 120, 2_000),
                randomStringLength = clampNumber(settings.tools.randomStringLength, 1, 4_096),
                qrErrorCorrection = normalizeQrErrorCorrection(
                    settings.tools.qrErrorCorrection,
                    defaults.tools.qrErrorCorrection,
                ),
                translationProvider = normalizeTranslationProvider(
                    settings.tools.translationProvider,
                    defaults.tools.translationProvider,
                ),
            ),
        )
}
