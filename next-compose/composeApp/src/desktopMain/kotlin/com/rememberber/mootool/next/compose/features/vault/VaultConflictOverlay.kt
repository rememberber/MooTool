package com.rememberber.mootool.next.compose.features.vault

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
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
    reloadButtonModifier: Modifier = Modifier,
    saveCopyButtonModifier: Modifier = Modifier,
    keepButtonModifier: Modifier = Modifier,
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
            reloadButtonModifier = reloadButtonModifier,
            saveCopyButtonModifier = saveCopyButtonModifier,
            keepButtonModifier = keepButtonModifier,
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
