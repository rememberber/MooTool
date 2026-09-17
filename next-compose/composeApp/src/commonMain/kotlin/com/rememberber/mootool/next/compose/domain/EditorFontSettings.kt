package com.rememberber.mootool.next.compose.domain

import com.rememberber.mootool.next.compose.model.EditorSettings

/** Aligns with Electron `normalizeFontName` in `next/src/shared/contracts/settings.ts`. */
object EditorFontSettings {
    fun normalizeFontName(value: String, fallback: String): String {
        val trimmed = value.trim().take(120)
        return trimmed.ifEmpty { fallback }
    }

    fun normalizeEditorSettings(editor: EditorSettings, defaults: EditorSettings): EditorSettings =
        editor.copy(
            jsonFontName = normalizeFontName(editor.jsonFontName, defaults.jsonFontName),
            quickNoteFontName = normalizeFontName(editor.quickNoteFontName, defaults.quickNoteFontName),
        )
}
