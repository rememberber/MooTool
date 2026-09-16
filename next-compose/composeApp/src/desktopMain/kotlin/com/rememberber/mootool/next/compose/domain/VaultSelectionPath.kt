package com.rememberber.mootool.next.compose.domain

import com.rememberber.mootool.next.compose.storage.VaultEntry

object VaultSelectionPath {
    /** Electron `selectedDirectory(selectedEntry)`. */
    fun parentDirectory(selectedPath: String, entries: List<VaultEntry>): String {
        if (selectedPath.isBlank()) return ""
        val entry = entries.find { it.relativePath == selectedPath }
        if (entry?.directory == true) return selectedPath
        return selectedPath.substringBeforeLast('/', "")
    }

    fun join(parent: String, name: String): String {
        val trimmed = name.trim().removePrefix("/").removeSuffix("/")
        if (trimmed.isEmpty()) return parent
        return if (parent.isBlank()) trimmed else "$parent/$trimmed"
    }

    /** 对话框只输入单段名称（无 `/`）时落在当前选中目录下。 */
    fun resolveEntryPath(
        input: String,
        vaultSelectedPath: String,
        currentFile: String,
        entries: List<VaultEntry>,
    ): String {
        val trimmed = input.trim().removeSuffix("/")
        if (trimmed.isEmpty()) return ""
        if (trimmed.contains('/')) return trimmed
        val selected = vaultSelectedPath.ifBlank { currentFile }
        return join(parentDirectory(selected, entries), trimmed)
    }
}
