package com.rememberber.mootool.next.compose.ui.components

import com.rememberber.mootool.next.compose.storage.VaultEntry

/** 对照 Electron `vaultTreeExpand.ts` `ancestorDirectoryPaths`。 */
fun vaultAncestorDirectoryPaths(relativePath: String): List<String> {
    if (!relativePath.contains('/')) return emptyList()
    val parts = relativePath.split('/').filter { it.isNotEmpty() }
    return (1 until parts.size).map { index -> parts.take(index).joinToString("/") }
}

/**
 * 对照 Electron `resolveExpandedPaths`：`collapseAll` 时仅保留 [selectedPath] 的祖先目录展开，
 * 避免选中深层文件后「全部折叠」把当前项藏进折叠树。
 */
fun vaultTreeExpandForMode(
    expandMode: String,
    items: List<VaultEntry>,
    selectedPath: String = "",
): Map<String, Boolean> {
    val directories = items.filter { it.directory }.map { it.relativePath }
    return when (expandMode) {
        "collapseAll" -> {
            val keep = vaultAncestorDirectoryPaths(selectedPath).toSet()
            directories.associateWith { it in keep }
        }
        "expandAll" -> directories.associateWith { true }
        else -> directories.associateWith { path -> !path.contains('/') }
    }
}
