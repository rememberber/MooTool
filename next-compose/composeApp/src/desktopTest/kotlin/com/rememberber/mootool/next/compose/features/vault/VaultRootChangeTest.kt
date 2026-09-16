package com.rememberber.mootool.next.compose.features.vault

import com.rememberber.mootool.next.compose.domain.VaultConflictState
import com.rememberber.mootool.next.compose.sessions.JsonSession
import com.rememberber.mootool.next.compose.sessions.QuickNoteSession
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class VaultRootChangeTest {
    @Test
    fun jsonDismissVaultScopedOverlays() {
        val session = JsonSession().apply {
            gitDialogOpen = true
            vaultConflict = VaultConflictState("a.json", "{}", "{}", false)
            vaultDeleteConfirmPath = "a.json"
            vaultContextMenuPath = "a.json"
            pathPickerOpen = true
            dialogTitle = "t"
            dialogInputMode = "rename"
        }
        session.dismissVaultScopedOverlays()
        assertFalse(session.gitDialogOpen)
        assertNull(session.vaultConflict)
        assertTrue(session.vaultDeleteConfirmPath.isBlank())
        assertTrue(session.vaultContextMenuPath.isBlank())
        assertFalse(session.pathPickerOpen)
        assertTrue(session.dialogTitle.isBlank())
        assertTrue(session.dialogInputMode.isBlank())
    }

    @Test
    fun quickNoteDismissVaultScopedOverlays() {
        val session = QuickNoteSession().apply {
            gitDialogOpen = true
            vaultConflict = VaultConflictState("n.md", "a", "b", false)
            documentInfoPath = "n.md"
            vaultContextMenuPath = "n.md"
            dialogMode = "rename"
            dialogValue = "x"
        }
        session.dismissVaultScopedOverlays()
        assertFalse(session.gitDialogOpen)
        assertNull(session.vaultConflict)
        assertTrue(session.documentInfoPath.isBlank())
        assertTrue(session.vaultContextMenuPath.isBlank())
        assertTrue(session.dialogMode.isBlank())
        assertTrue(session.dialogValue.isBlank())
    }
}
