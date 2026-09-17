package com.rememberber.mootool.next.compose.domain

import com.rememberber.mootool.next.compose.model.AppLanguage
import com.rememberber.mootool.next.compose.model.AppSettings
import com.rememberber.mootool.next.compose.model.CloseBehavior
import kotlin.test.Test
import kotlin.test.assertEquals

class SettingsGeneralNormalizeTest {
    @Test
    fun normalizeLanguage_matchesElectronLanguageList() {
        assertEquals(AppLanguage.EnUS.code, SettingsGeneralNormalize.normalizeLanguage("en-US"))
        assertEquals(AppLanguage.EnUS.code, SettingsGeneralNormalize.normalizeLanguage("  en-us  "))
        assertEquals(AppLanguage.ZhCN.code, SettingsGeneralNormalize.normalizeLanguage("bogus"))
    }

    @Test
    fun normalizeCloseBehavior_matchesElectronCloseBehaviors() {
        assertEquals(CloseBehavior.Hide.name.lowercase(), SettingsGeneralNormalize.normalizeCloseBehavior("hide"))
        assertEquals(CloseBehavior.Quit.name.lowercase(), SettingsGeneralNormalize.normalizeCloseBehavior("  QUIT  "))
        assertEquals(CloseBehavior.Ask.name.lowercase(), SettingsGeneralNormalize.normalizeCloseBehavior("invalid"))
    }

    @Test
    fun apply_normalizesGeneralSlice() {
        val normalized = SettingsGeneralNormalize.apply(
            AppSettings.Default.copy(
                general = AppSettings.Default.general.copy(
                    language = "unknown",
                    closeBehavior = "bogus",
                ),
            ),
        )
        assertEquals(AppLanguage.ZhCN.code, normalized.general.language)
        assertEquals(CloseBehavior.Ask.name.lowercase(), normalized.general.closeBehavior)
    }
}
