package com.rememberber.mootool.next.compose.features.settings

import kotlin.test.Test
import kotlin.test.assertEquals

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
}
