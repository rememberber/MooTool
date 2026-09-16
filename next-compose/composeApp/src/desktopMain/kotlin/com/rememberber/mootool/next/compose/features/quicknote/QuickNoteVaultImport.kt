package com.rememberber.mootool.next.compose.features.quicknote

import com.rememberber.mootool.next.compose.domain.VaultSelectionPath
import com.rememberber.mootool.next.compose.sessions.QuickNoteSession
import com.rememberber.mootool.next.compose.storage.NoteVault
import com.rememberber.mootool.next.compose.storage.VaultEntry
import com.rememberber.mootool.next.compose.storage.jsonVaultEntryRelativePath
import com.rememberber.mootool.next.compose.storage.jsonVaultImportRelativePath
import java.io.File

internal fun quickNoteVaultImportTargetDirectory(
    vaultSelectedPath: String,
    currentFile: String,
    vaultItems: List<VaultEntry>,
): String {
    val selected = vaultSelectedPath.ifBlank { currentFile }
    return VaultSelectionPath.parentDirectory(selected, vaultItems)
}

/** 对齐 Electron：无打开笔记时保存应进入新建流程，而非静默生成文件名。 */
internal fun openQuickNoteNewNoteDialog(session: QuickNoteSession) {
    session.dialogMode = "note"
    session.dialogValue = ""
    session.dialogTarget = ""
}

/** @return portable vault-relative path of imported entry */
internal fun importQuickNoteVaultFile(
    vault: NoteVault,
    source: File,
    targetDirectory: String,
): String {
    val relativePath = jsonVaultImportRelativePath(targetDirectory, source.name)
    val imported = vault.importFile(source.toPath(), relativePath)
    return jsonVaultEntryRelativePath(vault.root(), imported)
}
