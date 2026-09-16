package com.rememberber.mootool.next.compose.features.json

import com.rememberber.mootool.next.compose.domain.VaultSelectionPath
import com.rememberber.mootool.next.compose.sessions.JsonSession
import com.rememberber.mootool.next.compose.storage.JsonVault
import com.rememberber.mootool.next.compose.storage.VaultEntry
import com.rememberber.mootool.next.compose.storage.jsonVaultEntryRelativePath
import com.rememberber.mootool.next.compose.storage.jsonVaultImportRelativePath
import java.io.File

internal fun jsonVaultImportTargetDirectory(
    vaultSelectedPath: String,
    currentFile: String,
    vaultItems: List<VaultEntry>,
): String {
    val selected = vaultSelectedPath.ifBlank { currentFile }
    return VaultSelectionPath.parentDirectory(selected, vaultItems)
}

/** 对齐 Electron `saveSelected` 无选中时 `beginCreateFile`。 */
internal fun openJsonVaultNewFileDialog(
    session: JsonSession,
    vaultItems: List<VaultEntry>,
) {
    session.dialogInputMode = "json-file"
    val parent = jsonVaultImportTargetDirectory(
        session.vaultSelectedPath,
        session.currentFile,
        vaultItems,
    )
    session.dialogInput = VaultSelectionPath.join(parent, "snippet.json")
    session.dialogTarget = ""
}

/** @return portable vault-relative path of imported entry */
internal fun importJsonVaultFile(
    vault: JsonVault,
    source: File,
    targetDirectory: String,
): String {
    val relativePath = jsonVaultImportRelativePath(targetDirectory, source.name)
    val imported = vault.importFile(source.toPath(), relativePath)
    return jsonVaultEntryRelativePath(vault.root(), imported)
}
