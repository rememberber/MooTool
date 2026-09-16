package com.rememberber.mootool.next.compose.features.json

import com.rememberber.mootool.next.compose.domain.VaultConflictState
import com.rememberber.mootool.next.compose.sessions.JsonSession
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class JsonEditorFocusPolicyTest {
    @Test
    fun autoFocus_when_tool_active_and_no_modals() {
        val session = JsonSession()
        assertTrue(jsonEditorAutoFocusEnabled(session, toolActive = true))
    }

    @Test
    fun autoFocus_blocked_by_find_and_path_picker() {
        val session = JsonSession().apply { findOpen = true }
        assertFalse(jsonEditorAutoFocusEnabled(session, toolActive = true))
        session.findOpen = false
        session.pathPickerOpen = true
        assertFalse(jsonEditorAutoFocusEnabled(session, toolActive = true))
    }

    @Test
    fun autoFocus_blocked_by_vault_context_menu() {
        val session = JsonSession().apply { vaultContextMenuPath = "a.json" }
        assertFalse(jsonEditorAutoFocusEnabled(session, toolActive = true))
    }

    @Test
    fun autoFocus_blocked_by_inspector_conversion_overlay() {
        val session = JsonSession().apply { conversionMode = "bean" }
        assertFalse(jsonEditorAutoFocusEnabled(session, toolActive = true))
    }

    @Test
    fun autoFocus_blocked_by_vault_external_conflict() {
        val session = JsonSession().apply {
            vaultConflict = VaultConflictState("a.json", "{}", "{}", deleted = false)
        }
        assertFalse(jsonEditorAutoFocusEnabled(session, toolActive = true))
    }

    @Test
    fun autoFocus_blocked_by_git_panel_and_vault_delete_confirm() {
        val session = JsonSession().apply { gitDialogOpen = true }
        assertFalse(jsonEditorAutoFocusEnabled(session, toolActive = true))
        session.gitDialogOpen = false
        session.vaultDeleteConfirmPath = "remove.json"
        assertFalse(jsonEditorAutoFocusEnabled(session, toolActive = true))
    }
}
