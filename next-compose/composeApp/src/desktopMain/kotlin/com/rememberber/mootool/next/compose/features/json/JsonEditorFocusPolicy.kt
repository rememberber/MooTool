package com.rememberber.mootool.next.compose.features.json

import com.rememberber.mootool.next.compose.sessions.JsonSession

internal fun jsonEditorAutoFocusEnabled(session: JsonSession, toolActive: Boolean): Boolean {
    if (!toolActive) return false
    if (session.findOpen || session.historyOpen || session.pathPickerOpen || session.gitDialogOpen) return false
    if (session.vaultConflict != null) return false
    if (session.vaultDeleteConfirmPath.isNotBlank()) return false
    if (session.vaultContextMenuPath.isNotBlank()) return false
    if (session.dialogTitle.isNotBlank()) return false
    if (session.conversionMode.isNotBlank()) return false
    if (session.dialogInputMode.isNotBlank()) return false
    return true
}
