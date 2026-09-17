package com.rememberber.mootool.next.compose.domain

import com.rememberber.mootool.next.compose.model.CustomToolGroup
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class SettingsLayoutNormalizeTest {
    @Test
    fun normalizeCustomGroups_matchesElectronSettingsContract() {
        val groups = SettingsLayoutNormalize.normalizeCustomGroups(
            listOf(
                CustomToolGroup("daily", "  My tools  ", listOf("json", "json", "mootool", "unknown")),
                CustomToolGroup("daily", "Second", listOf("cron")),
                CustomToolGroup("empty-name", "   ", listOf("regex")),
            )
        )
        assertEquals(
            listOf(
                CustomToolGroup("daily", "My tools", listOf("json")),
                CustomToolGroup("daily-2", "Second", listOf("cron")),
            ),
            groups,
        )
    }

    @Test
    fun normalizeInterfaceStyle_unknownFallsBackToModern() {
        assertEquals("modern", SettingsLayoutNormalize.normalizeInterfaceStyle("unknown"))
        assertEquals("hero", SettingsLayoutNormalize.normalizeInterfaceStyle("hero"))
        assertEquals("miui-v5", SettingsLayoutNormalize.normalizeInterfaceStyle("miuiv5"))
    }

    @Test
    fun normalizeAccentColor_matchesElectronAccentPresets() {
        assertEquals("blue", SettingsLayoutNormalize.normalizeAccentColor("unknown"))
        assertEquals("purple", SettingsLayoutNormalize.normalizeAccentColor("purple"))
        assertEquals("yellow", SettingsLayoutNormalize.normalizeAccentColor("orange"))
        assertEquals("green", SettingsLayoutNormalize.normalizeAccentColor("teal"))
        assertEquals("red", SettingsLayoutNormalize.normalizeAccentColor("  red  "))
    }

    @Test
    fun normalizeUiFontFamily_trimsAndMapsSystemUiAlias() {
        assertEquals("system", SettingsLayoutNormalize.normalizeUiFontFamily("system-ui"))
        assertEquals("system", SettingsLayoutNormalize.normalizeUiFontFamily("  system-ui  "))
        assertEquals("serif", SettingsLayoutNormalize.normalizeUiFontFamily("  Serif  "))
        assertEquals("system", SettingsLayoutNormalize.normalizeUiFontFamily("   "))
        assertEquals("PingFang SC", SettingsLayoutNormalize.normalizeUiFontFamily("  PingFang SC  "))
    }

    @Test
    fun sanitizePaneSizes_dropsUnsafeKeysAndKeepsDpWidths() {
        val sanitized = SettingsLayoutNormalize.sanitizePaneSizes(
            mapOf(
                "json" to listOf(240f, 280f),
                "invalid" to listOf(0f, 1f),
                "../unsafe" to listOf(1f, 1f),
                "host" to listOf(220f, 180f),
            )
        )
        assertEquals(listOf(240f, 280f), sanitized["json"])
        assertEquals(listOf(220f, 180f), sanitized["host"])
        assertFalse(sanitized.containsKey("invalid"))
        assertFalse(sanitized.containsKey("../unsafe"))
    }
}
