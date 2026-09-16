package com.rememberber.mootool.next.compose.features.vault

import com.rememberber.mootool.next.compose.domain.VaultMove

/** 删除 Vault 条目后更新打开文件与树选中；若打开文件被删则 `clearedOpenFile=true`（对齐 Electron 清空 `savedContent`）。 */
internal fun vaultPathsAfterDelete(
    deletedPath: String,
    currentFile: String,
    vaultSelectedPath: String,
): VaultPathsAfterDelete {
    val (file, selected) = VaultMove.clearVaultPathsAfterDelete(deletedPath, currentFile, vaultSelectedPath)
    val clearedOpenFile = file.isBlank() && currentFile.isNotBlank()
    return VaultPathsAfterDelete(file, selected, clearedOpenFile)
}

internal data class VaultPathsAfterDelete(
    val currentFile: String,
    val vaultSelectedPath: String,
    val clearedOpenFile: Boolean,
)
