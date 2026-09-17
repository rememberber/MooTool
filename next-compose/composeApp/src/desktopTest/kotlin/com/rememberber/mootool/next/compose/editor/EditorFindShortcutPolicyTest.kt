package com.rememberber.mootool.next.compose.editor

import androidx.compose.ui.input.key.Key
import com.rememberber.mootool.next.compose.model.ToolId
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class EditorFindShortcutPolicyTest {
    @Test
    fun jsonHostQuickNoteAndRuntimeAcceptFindAndReplaceKeys() {
        listOf(ToolId.Json, ToolId.QuickNote, ToolId.Host, ToolId.Java).forEach { toolId ->
            assertTrue(
                EditorFindShortcutPolicy.opensShellFind(toolId, Key.F, meta = true, shift = false, alt = false),
            )
            assertTrue(
                EditorFindShortcutPolicy.opensShellFind(toolId, Key.R, meta = true, shift = false, alt = false),
            )
        }
    }

    @Test
    fun httpAcceptsFindOnlyNotReplaceShortcut() {
        assertTrue(
            EditorFindShortcutPolicy.opensShellFind(ToolId.Http, Key.F, meta = true, shift = false, alt = false),
        )
        assertFalse(
            EditorFindShortcutPolicy.opensShellFind(ToolId.Http, Key.R, meta = true, shift = false, alt = false),
        )
    }

    @Test
    fun toolsOutsideScopeDoNotOpenShellFind() {
        assertFalse(
            EditorFindShortcutPolicy.opensShellFind(ToolId.TextDiff, Key.F, meta = true, shift = false, alt = false),
        )
        assertFalse(
            EditorFindShortcutPolicy.opensShellFind(ToolId.Reformat, Key.R, meta = true, shift = false, alt = false),
        )
    }
}
