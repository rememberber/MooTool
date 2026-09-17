package com.rememberber.mootool.next.compose.domain

/** Vault Git diff 区 UI 语义（对齐 Electron `VaultGitDiffView`；高亮仍走 Compose `DiffEngine`）。 */
object GitDiffPresentation {
    /** 与 F02 默认 `both` 一致：`GitDiffDecoration` 行 16% + 字符 42% 叠层。 */
    val diffHighlightMode: String = GitDiffDecoration.HIGHLIGHT_BOTH

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

    /** RSTA 语法键（Vault Git diff 只读 [EditorHost]）。 */
    fun rstaSyntaxForFile(diff: GitFileDiff): String {
        val language = languageForFile(diff)
        val mime = when (language) {
            TextCodeEditorLanguage.Json -> "application/json"
            TextCodeEditorLanguage.Markdown -> "text/markdown"
            TextCodeEditorLanguage.Java -> "text/java"
            TextCodeEditorLanguage.Javascript -> "text/javascript"
            TextCodeEditorLanguage.Typescript -> "text/typescript"
            TextCodeEditorLanguage.Python -> "text/python"
            TextCodeEditorLanguage.Xml -> "text/xml"
            TextCodeEditorLanguage.Html -> "text/html"
            TextCodeEditorLanguage.Yaml -> "text/yaml"
            TextCodeEditorLanguage.Sql -> "text/sql"
            TextCodeEditorLanguage.Text -> "text/plain"
        }
        return DocumentFormatEngine.rstaSyntax(mime)
    }
}
