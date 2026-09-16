package com.rememberber.mootool.next.compose.sessions

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class ToolModalOverlaysTest {
    @Test
    fun json_dismissModalOverlays_keeps_session_modals_for_electron_parity() {
        val session = JsonSession()
        session.pathPickerOpen = true
        session.pathPickerSelection = "$.items[0]"
        session.historyOpen = true
        session.gitDialogOpen = true
        session.vaultDeleteConfirmPath = "a.json"
        session.vaultContextMenuPath = "b.json"
        session.dialogTitle = "title"
        session.conversionMode = "xml"
        session.conversionInput = "<a/>"
        session.dismissModalOverlays()
        assert(session.pathPickerOpen)
        assertEquals("$.items[0]", session.pathPickerSelection)
        assert(session.historyOpen)
        assert(session.gitDialogOpen)
        assertEquals("a.json", session.vaultDeleteConfirmPath)
        assertEquals("b.json", session.vaultContextMenuPath)
        assertEquals("title", session.dialogTitle)
        assertEquals("xml", session.conversionMode)
        assertEquals("<a/>", session.conversionInput)
    }

    @Test
    fun quickNote_dismissModalOverlays_keeps_session_modals_for_electron_parity() {
        val session = QuickNoteSession()
        session.historyOpen = true
        session.gitDialogOpen = true
        session.documentInfoPath = "notes/a.md"
        session.vaultContextMenuPath = "notes/b.md"
        session.dialogMode = "rename"
        session.dialogTarget = "a.md"
        session.dismissModalOverlays()
        assert(session.historyOpen)
        assert(session.gitDialogOpen)
        assertEquals("notes/a.md", session.documentInfoPath)
        assertEquals("notes/b.md", session.vaultContextMenuPath)
        assertEquals("rename", session.dialogMode)
        assertEquals("a.md", session.dialogTarget)
    }

    @Test
    fun http_dismissModalOverlays_clears_transient_dialogs() {
        val session = HttpSession()
        session.historyOpen = true
        session.curlOpen = true
        session.saveOpen = true
        session.deleteConfirm = true
        session.dismissModalOverlays()
        assertFalse(session.historyOpen)
        assertFalse(session.curlOpen)
        assertFalse(session.saveOpen)
        assertFalse(session.deleteConfirm)
    }

    @Test
    fun color_dismissModalOverlays_clears_picking_and_dialogs() {
        val session = ColorSession().apply {
            historyOpen = true
            favoritesOpen = true
            saveFavoriteOpen = true
            picking = true
            dismissModalOverlays()
        }
        assertFalse(session.historyOpen)
        assertFalse(session.favoritesOpen)
        assertFalse(session.saveFavoriteOpen)
        assertFalse(session.picking)
    }

    @Test
    fun host_and_image_dismissModalOverlays_clears_flags() {
        val host = HostSession().apply {
            historyOpen = true
            applyConfirm = true
            profileContextMenuId = "profile-1"
            dismissModalOverlays()
        }
        assertFalse(host.historyOpen)
        assertFalse(host.applyConfirm)
        assertEquals("", host.profileContextMenuId)

        val image = ImageSession().apply {
            compressOpen = true
            deleteOpen = true
            busy = true
            dismissModalOverlays()
        }
        assertFalse(image.compressOpen)
        assertFalse(image.deleteOpen)
        assertFalse(image.busy)
    }
}
