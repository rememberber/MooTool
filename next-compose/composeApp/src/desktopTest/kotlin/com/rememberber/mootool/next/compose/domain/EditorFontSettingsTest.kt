package com.rememberber.mootool.next.compose.domain

import com.rememberber.mootool.next.compose.model.AppSettings
import kotlin.test.Test
import kotlin.test.assertEquals

class EditorFontSettingsTest {
    @Test
    fun normalizeFontName_matchesElectronSettingsContract() {
        assertEquals("PingFang SC", EditorFontSettings.normalizeFontName("  PingFang SC  ", "ui-monospace"))
        assertEquals("ui-monospace", EditorFontSettings.normalizeFontName("", "ui-monospace"))
        assertEquals("ui-monospace", EditorFontSettings.normalizeFontName("   ", "ui-monospace"))
    }

    @Test
    fun normalizeEditorSettings_trimsBothFontFields() {
        val normalized = EditorFontSettings.normalizeEditorSettings(
            AppSettings.Default.editor.copy(
                jsonFontName = "  PingFang SC  ",
                quickNoteFontName = "",
            ),
            AppSettings.Default.editor,
        )
        assertEquals("PingFang SC", normalized.jsonFontName)
        assertEquals("ui-monospace", normalized.quickNoteFontName)
    }

    @Test
    fun normalizeSqlDialect_matchesElectronPresetsAndLegacyMysql() {
        assertEquals("MySQL", EditorFontSettings.normalizeSqlDialect("mysql", "Standard SQL"))
        assertEquals("PostgreSQL", EditorFontSettings.normalizeSqlDialect(" postgresql ", "Standard SQL"))
        assertEquals("Standard SQL", EditorFontSettings.normalizeSqlDialect("unknown-dialect", "Standard SQL"))
        assertEquals("Standard SQL", EditorFontSettings.normalizeSqlDialect("", "Standard SQL"))
    }

    @Test
    fun normalizeEditorSettings_coercesUnknownSqlDialect() {
        val normalized = EditorFontSettings.normalizeEditorSettings(
            AppSettings.Default.editor.copy(sqlDialect = "legacy"),
            AppSettings.Default.editor,
        )
        assertEquals("Standard SQL", normalized.sqlDialect)
    }
}
