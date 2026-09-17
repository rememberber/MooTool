package com.rememberber.mootool.next.compose.domain

import kotlin.test.Test
import kotlin.test.assertEquals

class SettingsToolsTranslationNormalizeTest {
    @Test
    fun commitLanguagePair_normalizesLegacyNamesAndSameLangToAuto() {
        assertEquals(
            "auto" to "en",
            SettingsToolsTranslationNormalize.commitLanguagePair("English", "en"),
        )
        assertEquals(
            "auto" to "zh-CN",
            SettingsToolsTranslationNormalize.commitLanguagePair("zh-CN", "zh-CN"),
        )
    }

    @Test
    fun commitLanguagePair_trimsAndFallsBackEmptyDrafts() {
        assertEquals(
            "auto" to "zh-CN",
            SettingsToolsTranslationNormalize.commitLanguagePair("  ", "  "),
        )
        assertEquals(
            "en" to "zh-CN",
            SettingsToolsTranslationNormalize.commitLanguagePair(" en ", " zh-CN "),
        )
    }
}
