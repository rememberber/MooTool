package com.rememberber.mootool.next.compose.features.vault

import androidx.compose.runtime.Composable
import com.rememberber.mootool.next.compose.app.AppContainer
import com.rememberber.mootool.next.compose.domain.VaultConflictState
import com.rememberber.mootool.next.compose.domain.VaultRevisionMonitor
import com.rememberber.mootool.next.compose.sessions.JsonSession
import com.rememberber.mootool.next.compose.sessions.QuickNoteSession
import com.rememberber.mootool.next.compose.storage.NoteVault

@Composable
fun JsonVaultConflictOverlay(
    container: AppContainer,
    session: JsonSession,
    monitor: VaultRevisionMonitor?,
    onRefresh: () -> Unit,
) {
    session.vaultConflict?.let { pending ->
        VaultConflictDialog(
            container = container,
            conflict = pending,
            onReload = {
                applyJsonVaultConflictReload(container, session, pending)
                onRefresh()
            },
            onSaveCopy = {
                applyJsonVaultConflictSaveCopy(container, session, pending, monitor)
                onRefresh()
            },
            onKeep = {
                applyJsonVaultConflictKeep(session)
                onRefresh()
            },
        )
    }
}

@Composable
fun QuickNoteVaultConflictOverlay(
    container: AppContainer,
    session: QuickNoteSession,
    vault: NoteVault,
    monitor: VaultRevisionMonitor?,
    onRefresh: () -> Unit,
) {
    session.vaultConflict?.let { pending ->
        VaultConflictDialog(
            container = container,
            conflict = pending,
            onReload = {
                applyQuickNoteVaultConflictReload(container, session, vault, pending)
                onRefresh()
            },
            onSaveCopy = {
                applyQuickNoteVaultConflictSaveCopy(container, session, vault, pending, monitor)
                onRefresh()
            },
            onKeep = {
                applyQuickNoteVaultConflictKeep(session)
                onRefresh()
            },
        )
    }
}
