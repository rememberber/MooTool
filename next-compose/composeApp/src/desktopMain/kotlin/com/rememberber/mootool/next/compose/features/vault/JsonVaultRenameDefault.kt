package com.rememberber.mootool.next.compose.features.vault

import com.rememberber.mootool.next.compose.storage.VaultEntry

internal fun jsonVaultRenameDefault(entry: VaultEntry): String {
    if (entry.directory) {
        val leaf = entry.relativePath.substringAfterLast('/')
        return if (leaf.isBlank() || leaf == entry.relativePath) entry.relativePath else leaf
    }
    return jsonVaultRenameDefaultFromFileName(entry.name)
}

internal fun jsonVaultRenameDefaultFromFileName(fileName: String): String =
    fileName.replace(Regex("""\.json$""", RegexOption.IGNORE_CASE), "")
