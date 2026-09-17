package com.rememberber.mootool.next.compose.features.settings

import kotlin.test.Test
import kotlin.test.assertEquals

class SettingsNavPresentationTest {
    @Test
    fun stepAndHeaderKeysAlignWithElectronCategories() {
        assertEquals(SettingsNavCategory.Appearance, SettingsNavPresentation.step(SettingsNavCategory.General, 1))
        assertEquals("settings.category.vault", SettingsNavPresentation.contentHeaderLabelKey(SettingsNavCategory.Vault))
        assertEquals("⌁", SettingsNavPresentation.contentHeaderIcon(SettingsNavCategory.Vault))
    }
}
