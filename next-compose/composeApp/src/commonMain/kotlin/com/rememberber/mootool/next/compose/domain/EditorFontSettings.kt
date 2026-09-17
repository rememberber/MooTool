package com.rememberber.mootool.next.compose.domain

import com.rememberber.mootool.next.compose.model.EditorSettings

/** Aligns with Electron `normalizeFontName` in `next/src/shared/contracts/settings.ts`. */
object EditorFontSettings {
    /** Same order as Electron `SettingsWindow` editor SQL dialect select and `SqlFormatEngine.dialects`. */
    val sqlDialectPresets: List<String> = listOf(
        "Standard SQL",
        "MySQL",
        "MariaDB",
        "PostgreSQL",
        "Oracle PL/SQL",
        "SQL Server Transact-SQL",
        "IBM DB2",
        "Couchbase N1QL",
        "Amazon Redshift",
        "Spark",
    )

    fun normalizeFontName(value: String, fallback: String): String {
        val trimmed = value.trim().take(120)
        return trimmed.ifEmpty { fallback }
    }

    fun normalizeSqlDialect(value: String, fallback: String): String {
        val trimmed = value.trim()
        if (trimmed.isEmpty()) return fallback
        if (trimmed.equals("mysql", ignoreCase = true)) return "MySQL"
        sqlDialectPresets.firstOrNull { it.equals(trimmed, ignoreCase = true) }?.let { return it }
        return fallback
    }

    fun normalizeEditorSettings(editor: EditorSettings, defaults: EditorSettings): EditorSettings =
        editor.copy(
            sqlDialect = normalizeSqlDialect(editor.sqlDialect, defaults.sqlDialect),
            jsonFontName = normalizeFontName(editor.jsonFontName, defaults.jsonFontName),
            quickNoteFontName = normalizeFontName(editor.quickNoteFontName, defaults.quickNoteFontName),
        )
}
