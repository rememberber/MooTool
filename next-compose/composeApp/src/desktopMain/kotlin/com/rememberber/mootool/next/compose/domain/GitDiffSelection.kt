package com.rememberber.mootool.next.compose.domain

object GitDiffSelection {
    fun fileLabel(diff: GitFileDiff): String {
        val path = if (diff.originalPath.isNullOrBlank()) diff.path else "${diff.originalPath} → ${diff.path}"
        val status = diff.status.trim().ifEmpty { "M" }
        return "$status  $path"
    }

    fun selected(files: List<GitFileDiff>, selectedPath: String): GitFileDiff? =
        files.firstOrNull { it.path == selectedPath } ?: files.firstOrNull()
}
