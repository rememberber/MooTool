package com.rememberber.mootool.next.compose.domain

import com.rememberber.mootool.next.compose.storage.JsonVault
import com.rememberber.mootool.next.compose.storage.NoteVault

/** 本机 Vault 落盘后登记监视器哈希，避免 [VaultRevisionMonitor] 误报外部变更（DIFF-422 链）。 */
fun VaultRevisionMonitor?.noteOwnWriteJsonVaultFile(vault: JsonVault, relativePath: String) {
    if (this == null || relativePath.isBlank()) return
    runCatching {
        val text = vault.read(relativePath)
        noteOwnWrite(relativePath, VaultConflictEngine.sha256Text(text))
    }
}

fun VaultRevisionMonitor?.noteOwnWriteQuickNoteFile(vault: NoteVault, relativePath: String) {
    if (this == null || relativePath.isBlank()) return
    runCatching {
        val raw = vault.read(relativePath)
        noteOwnWrite(relativePath, VaultConflictEngine.sha256Text(raw))
    }
}

/** 删除、移动、重命名等多路径/删路径变更后同步监视器基线（DIFF-425）。 */
fun VaultRevisionMonitor?.rebaselineAfterLocalCrud() {
    this?.rebaseline()
}
