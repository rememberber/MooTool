package com.rememberber.mootool.next.compose.features.quicknote

import com.rememberber.mootool.next.compose.domain.VaultConflictState
import com.rememberber.mootool.next.compose.sessions.QuickNoteSession
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class QuickNoteEditorFocusPolicyTest {
    @Test
    fun autoFocus_requires_edit_mode_and_open_note() {
        val session = QuickNoteSession().apply {
            currentFile = "a.md"
            viewMode = "edit"
        }
        assertTrue(quickNoteEditorAutoFocusEnabled(session, toolActive = true))
        session.viewMode = "preview"
        assertFalse(quickNoteEditorAutoFocusEnabled(session, toolActive = true))
    }

    @Test
    fun autoFocus_blocked_by_vault_context_menu() {
        val session = QuickNoteSession().apply {
            currentFile = "a.md"
            viewMode = "edit"
            vaultContextMenuPath = "a.md"
        }
        assertFalse(quickNoteEditorAutoFocusEnabled(session, toolActive = true))
    }

    @Test
    fun autoFocus_blocked_by_vault_external_conflict() {
        val session = QuickNoteSession().apply {
            currentFile = "a.md"
            viewMode = "edit"
            vaultConflict = VaultConflictState("a.md", "local", "disk", deleted = false)
        }
        assertFalse(quickNoteEditorAutoFocusEnabled(session, toolActive = true))
    }

    @Test
    fun autoFocus_blocked_by_git_panel() {
        val session = QuickNoteSession().apply {
            currentFile = "a.md"
            viewMode = "edit"
            gitDialogOpen = true
        }
        assertFalse(quickNoteEditorAutoFocusEnabled(session, toolActive = true))
    }
}
