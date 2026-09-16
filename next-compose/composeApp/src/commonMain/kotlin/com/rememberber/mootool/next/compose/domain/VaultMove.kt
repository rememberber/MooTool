package com.rememberber.mootool.next.compose.domain

object VaultMove {
    fun parentDirectory(relativePath: String): String {
        val index = relativePath.lastIndexOf('/')
        return if (index <= 0) "" else relativePath.substring(0, index)
    }

    /** 移动 `movedPath`（文件或目录）是否会改变 `openPath` 在磁盘上的位置（对齐 Electron `moveTreeEntry` 的 `affectsSelection`）。 */
    fun moveAffectsOpenPath(openPath: String, movedPath: String): Boolean {
        val open = openPath.trim()
        val moved = movedPath.trim().trim('/')
        if (open.isEmpty() || moved.isEmpty()) return false
        return open == moved || open.startsWith("$moved/")
    }

    fun canMoveToDirectory(relativePath: String, targetDirectory: String): Boolean {
        val source = relativePath.trim('/').trim()
        val target = targetDirectory.trim('/').trim()
        if (source.isEmpty()) return false
        val currentDirectory = parentDirectory(source)
        return currentDirectory != target &&
            source != target &&
            (target.isEmpty() || !target.startsWith("$source/"))
    }

    fun retargetAfterMove(current: String, from: String, next: String): String = when {
        current == from -> next
        current.startsWith("$from/") -> next + current.removePrefix(from)
        else -> current
    }

    /** 重命名/移动 Vault 条目后同步 `currentFile` 与树选中路径（对齐 Electron `updateSelectionAfterPathChange`）。 */
    fun retargetVaultPaths(
        currentFile: String,
        vaultSelectedPath: String,
        from: String,
        next: String,
    ): Pair<String, String> = Pair(
        retargetAfterMove(currentFile, from, next),
        retargetAfterMove(vaultSelectedPath, from, next),
    )

    /** 删除 `deleted` 路径（或其前缀祖先）后，若 `path` 落在其下则清空。 */
    fun clearPathIfDeleted(deleted: String, path: String): String {
        val root = deleted.trim().trim('/')
        val current = path.trim()
        if (root.isEmpty() || current.isEmpty()) return current
        return if (current == root || current.startsWith("$root/")) "" else current
    }

    fun clearVaultPathsAfterDelete(
        deleted: String,
        currentFile: String,
        vaultSelectedPath: String,
    ): Pair<String, String> = Pair(
        clearPathIfDeleted(deleted, currentFile),
        clearPathIfDeleted(deleted, vaultSelectedPath),
    )
}
