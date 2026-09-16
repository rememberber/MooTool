package com.rememberber.mootool.next.compose.features.vault

import com.rememberber.mootool.next.compose.app.AppContainer
import com.rememberber.mootool.next.compose.domain.VaultConflictEngine
import com.rememberber.mootool.next.compose.domain.VaultConflictState
import com.rememberber.mootool.next.compose.domain.VaultRevisionMonitor
import com.rememberber.mootool.next.compose.domain.rebaselineAfterLocalCrud
import com.rememberber.mootool.next.compose.features.json.clearJsonPathQueryResult
import com.rememberber.mootool.next.compose.features.quicknote.quickNoteOpenVaultFile
import com.rememberber.mootool.next.compose.sessions.JsonSession
import com.rememberber.mootool.next.compose.sessions.QuickNoteSession
import com.rememberber.mootool.next.compose.storage.NoteVault
import javax.swing.SwingUtilities

internal fun applyJsonVaultConflictReload(
    container: AppContainer,
    session: JsonSession,
    pending: VaultConflictState,
) {
    if (pending.deleted) {
        session.currentFile = ""
        session.savedText = session.editor.text
    } else {
        vaultOnEdt {
            session.editor.setText(container.jsonVault.read(pending.relativePath), recordUndo = false)
        }
        session.currentFile = pending.relativePath
        session.savedText = session.editor.text
        session.clearJsonPathQueryResult()
    }
    session.vaultConflict = null
    session.notice = container.t("vault.conflict.reloaded")
}

internal fun applyJsonVaultConflictSaveCopy(
    container: AppContainer,
    session: JsonSession,
    pending: VaultConflictState,
    monitor: VaultRevisionMonitor?,
    nowMillis: Long = System.currentTimeMillis(),
): Boolean {
    val copy = VaultConflictEngine.conflictCopyName(pending.relativePath, nowMillis)
    return runCatching { container.jsonVault.write(copy, pending.editorText) }
        .onSuccess {
            monitor?.rebaselineAfterLocalCrud()
            if (pending.deleted) {
                session.currentFile = copy
                session.savedText = pending.editorText
            } else {
                vaultOnEdt {
                    session.editor.setText(container.jsonVault.read(pending.relativePath), recordUndo = false)
                }
                session.currentFile = pending.relativePath
                session.savedText = session.editor.text
                session.clearJsonPathQueryResult()
            }
            session.vaultConflict = null
            session.notice = container.t("vault.conflict.savedCopy", mapOf("path" to copy))
        }
        .onFailure { session.notice = it.message ?: container.t("json.notice.failed") }
        .isSuccess
}

internal fun applyJsonVaultConflictKeep(session: JsonSession) {
    session.vaultConflict = null
}

internal fun applyQuickNoteVaultConflictReload(
    container: AppContainer,
    session: QuickNoteSession,
    vault: NoteVault,
    pending: VaultConflictState,
) {
    if (pending.deleted) {
        session.currentFile = ""
        session.savedText = session.editor.text
    } else {
        quickNoteOpenVaultFile(session, vault, pending.relativePath)
    }
    session.vaultConflict = null
    session.error = ""
    session.notice = container.t("vault.conflict.reloaded")
}

internal fun applyQuickNoteVaultConflictSaveCopy(
    container: AppContainer,
    session: QuickNoteSession,
    vault: NoteVault,
    pending: VaultConflictState,
    monitor: VaultRevisionMonitor?,
    nowMillis: Long = System.currentTimeMillis(),
): Boolean {
    val copy = VaultConflictEngine.conflictCopyName(pending.relativePath, nowMillis)
    return runCatching { vault.write(copy, pending.editorText) }
        .onSuccess {
            monitor?.rebaselineAfterLocalCrud()
            if (pending.deleted) {
                session.currentFile = copy
                session.savedText = pending.editorText
            } else {
                quickNoteOpenVaultFile(session, vault, pending.relativePath)
            }
            session.vaultConflict = null
            session.error = ""
            session.notice = container.t("vault.conflict.savedCopy", mapOf("path" to copy))
        }
        .onFailure { session.error = it.message ?: container.t("quickNote.saveFailed") }
        .isSuccess
}

internal fun applyQuickNoteVaultConflictKeep(session: QuickNoteSession) {
    session.vaultConflict = null
}

private fun vaultOnEdt(block: () -> Unit) {
    if (SwingUtilities.isEventDispatchThread()) block() else SwingUtilities.invokeLater(block)
}
