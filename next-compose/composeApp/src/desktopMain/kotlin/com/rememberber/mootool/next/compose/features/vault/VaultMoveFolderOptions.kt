package com.rememberber.mootool.next.compose.features.vault

import com.rememberber.mootool.next.compose.storage.VaultEntry

internal fun vaultMoveFolderOptions(
    entries: List<VaultEntry>,
    sourcePath: String,
    rootLabel: String,
): List<Pair<String, String>> {
    val source = sourcePath.trim()
    val directories = entries
        .filter { it.directory }
        .map { it.relativePath }
        .filter { path -> path != source && (source.isBlank() || !path.startsWith("$source/")) }
        .distinct()
        .sorted()
    return listOf("" to rootLabel) + directories.map { path -> path to path }
}
