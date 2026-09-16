package com.rememberber.mootool.next.compose.features.quicknote

import com.rememberber.mootool.next.compose.sessions.QuickNoteSession

internal fun quickNoteEditorAutoFocusEnabled(session: QuickNoteSession, toolActive: Boolean): Boolean {
    if (!toolActive) return false
    if (session.currentFile.isBlank()) return false
    if (session.viewMode != "edit") return false
    if (session.findOpen || session.historyOpen || session.gitDialogOpen) return false
    if (session.dialogMode.isNotBlank()) return false
    if (session.documentInfoPath.isNotBlank()) return false
    if (session.vaultConflict != null) return false
    if (session.vaultContextMenuPath.isNotBlank()) return false
    return true
}
