package com.rememberber.mootool.next.compose.domain

import com.rememberber.mootool.next.compose.model.AppSettings
import kotlin.test.Test
import kotlin.test.assertEquals

class SettingsNumericBoundsTest {
    @Test
    fun normalize_matchesElectronSettingsContract() {
        val raw = AppSettings.Default.copy(
            appearance = AppSettings.Default.appearance.copy(fontSize = 200),
            editor = AppSettings.Default.editor.copy(jsonFontSize = 1),
            network = AppSettings.Default.network.copy(
                requestTimeoutMs = 50,
                translationTimeoutMs = 999_999,
            ),
            vault = AppSettings.Default.vault.copy(
                autoCommitIdleSeconds = 1,
                autoCommitInactiveSeconds = 999_999,
            ),
            tools = AppSettings.Default.tools.copy(
                qrCodeSize = 12,
                randomStringLength = 99_999,
                translationProvider = "bing",
            ),
        )
        val normalized = SettingsNumericBounds.normalize(raw)
        assertEquals(18, normalized.appearance.fontSize)
        assertEquals(11, normalized.editor.jsonFontSize)
        assertEquals(120, normalized.tools.qrCodeSize)
        assertEquals(4_096, normalized.tools.randomStringLength)
        assertEquals(1_000, normalized.network.requestTimeoutMs)
        assertEquals(120_000, normalized.network.translationTimeoutMs)
        assertEquals(5, normalized.vault.autoCommitIdleSeconds)
        assertEquals(3_600, normalized.vault.autoCommitInactiveSeconds)
        assertEquals("bing", normalized.tools.translationProvider)
    }

    @Test
    fun appearanceFontSize_keepsMidRangeValues() {
        val raw = AppSettings.Default.copy(
            appearance = AppSettings.Default.appearance.copy(fontSize = 15),
        )
        assertEquals(15, SettingsNumericBounds.normalize(raw).appearance.fontSize)
    }
}
