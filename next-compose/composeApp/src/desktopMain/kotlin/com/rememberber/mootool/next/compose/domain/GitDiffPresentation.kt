package com.rememberber.mootool.next.compose.domain

/** Vault Git diff 区 UI 语义（对齐 Electron `VaultGitDiffView`；高亮仍走 Compose `DiffEngine`）。 */
object GitDiffPresentation {
    fun languageForFile(diff: GitFileDiff): TextCodeEditorLanguage {
        val pathHint = diff.originalPath?.takeIf { it.isNotBlank() } ?: diff.path
        return TextCodeEditorLanguages.resolveFromPath(pathHint)
    }

    fun showFilePicker(fileCount: Int): Boolean = fileCount > 1

    fun showSideBySide(preview: GitDiffPreview): Boolean = preview == GitDiffPreview.Text

    fun previewMessageKey(preview: GitDiffPreview): String? = when (preview) {
        GitDiffPreview.Binary -> "git.diffBinary"
        GitDiffPreview.TooLarge -> "git.diffTooLarge"
        GitDiffPreview.Text -> null
    }
}
