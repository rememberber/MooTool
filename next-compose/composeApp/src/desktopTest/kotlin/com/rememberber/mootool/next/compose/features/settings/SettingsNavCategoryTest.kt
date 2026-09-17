package com.rememberber.mootool.next.compose.features.settings

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SettingsNavCategoryTest {
    @Test
    fun storageId_roundTrips_known_categories() {
        assertEquals(SettingsNavCategory.Vault, settingsNavCategoryFromStorageId("vault"))
        assertEquals(SettingsNavCategory.Data, settingsNavCategoryFromStorageId("data"))
        assertEquals(SettingsNavCategory.Runtime, settingsNavCategoryFromStorageId("runtime"))
        assertEquals("ai", SettingsNavCategory.Ai.storageId())
    }

    @Test
    fun unknown_id_falls_back_to_general() {
        assertEquals(SettingsNavCategory.General, settingsNavCategoryFromStorageId("not-a-category"))
    }

    @Test
    fun arrowStepMovesWithinCategories() {
        assertEquals(SettingsNavCategory.Appearance, settingsNavCategoryStep(SettingsNavCategory.General, 1))
        assertEquals(SettingsNavCategory.General, settingsNavCategoryStep(SettingsNavCategory.Appearance, -1))
        assertEquals(SettingsNavCategory.About, settingsNavCategoryStep(SettingsNavCategory.About, 1))
    }

    @Test
    fun categoryLabelKeysMatchElectronSettingsCategoryIds() {
        assertEquals("settings.category.general", SettingsNavCategory.General.categoryLabelKey())
        assertEquals("settings.category.ai", SettingsNavCategory.Ai.categoryLabelKey())
        assertEquals("settings.category.about", SettingsNavCategory.About.categoryLabelKey())
        assertTrue(SettingsNavCategory.entries.all { it.navIcon().isNotBlank() })
    }
}
