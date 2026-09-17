package com.rememberber.mootool.next.compose.domain

/**
 * Live wiring between global editor settings and open tool sessions.
 *
 * Electron reference:
 * - JSON: initial wrap from settings; Compose also syncs while the tool is open ([jsonSoftWrapFromSettings]).
 * - HTTP: editors read `settings.editor` on each render (no local wrap state).
 * - Quick Note: per-note `lineWrap` in frontmatter; new notes use global softWrap ([newQuickNoteLineWrap]).
 * - Runtime (`RuntimeTool.tsx`): code editor `wrap={false}` — global softWrap does not apply.
 * - Host: plain textarea without soft-wrap toggle.
 */
object EditorSettingsLiveApply {
    fun jsonSoftWrapFromSettings(sessionWrap: Boolean, settingsSoftWrap: Boolean): Boolean =
        if (sessionWrap != settingsSoftWrap) settingsSoftWrap else sessionWrap

    /** HTTP request/response `EditorHost` wraps follow settings on every recomposition (Electron network tools). */
    fun httpEditorWrap(settingsSoftWrap: Boolean): Boolean = settingsSoftWrap

    /** Aligns with Electron `createQuickNote({ lineWrap: settings.editor.softWrap })`. */
    fun newQuickNoteLineWrap(settingsSoftWrap: Boolean): Boolean = settingsSoftWrap

    /** Electron `RuntimeTool.tsx` hardcodes `wrap={false}`. */
    fun runtimeEditorWrap(): Boolean = false
}
