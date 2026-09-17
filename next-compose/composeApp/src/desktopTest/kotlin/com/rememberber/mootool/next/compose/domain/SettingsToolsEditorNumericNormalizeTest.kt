package com.rememberber.mootool.next.compose.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class SettingsToolsEditorNumericNormalizeTest {
    @Test
    fun commitQrCodeSize_clampsAndRejects() {
        assertEquals(
            300,
            (SettingsToolsNumericNormalize.commitQrCodeSize("300") as NumericSettingCommitResult.Accepted).value,
        )
        assertEquals(
            120,
            (SettingsToolsNumericNormalize.commitQrCodeSize("12") as NumericSettingCommitResult.Accepted).value,
        )
        assertEquals(
            2_000,
            (SettingsToolsNumericNormalize.commitQrCodeSize("9999") as NumericSettingCommitResult.Accepted).value,
        )
        assertIs<NumericSettingCommitResult.Rejected>(SettingsToolsNumericNormalize.commitQrCodeSize("abc"))
    }

    @Test
    fun commitRandomStringLength_clampsAndRejects() {
        assertEquals(
            16,
            (SettingsToolsNumericNormalize.commitRandomStringLength("16") as NumericSettingCommitResult.Accepted).value,
        )
        assertEquals(
            1,
            (SettingsToolsNumericNormalize.commitRandomStringLength("0") as NumericSettingCommitResult.Accepted).value,
        )
        assertEquals(
            4_096,
            (SettingsToolsNumericNormalize.commitRandomStringLength("99999") as NumericSettingCommitResult.Accepted).value,
        )
    }

    @Test
    fun commitEditorFontSizes_clampsToElevenThroughTwentyFour() {
        assertEquals(
            14,
            (SettingsEditorNumericNormalize.commitJsonFontSize("14") as NumericSettingCommitResult.Accepted).value,
        )
        assertEquals(
            11,
            (SettingsEditorNumericNormalize.commitQuickNoteFontSize("1") as NumericSettingCommitResult.Accepted).value,
        )
        assertEquals(
            24,
            (SettingsEditorNumericNormalize.commitQuickNoteFontSize("99") as NumericSettingCommitResult.Accepted).value,
        )
        assertIs<NumericSettingCommitResult.Rejected>(SettingsEditorNumericNormalize.commitJsonFontSize(""))
    }
}
