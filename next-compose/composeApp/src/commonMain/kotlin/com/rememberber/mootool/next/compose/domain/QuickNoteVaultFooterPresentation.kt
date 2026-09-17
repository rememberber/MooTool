package com.rememberber.mootool.next.compose.domain

/** F01 随手记 Vault 底栏路径与操作守卫（对齐 [JsonVaultFooterPresentation]）。 */
object QuickNoteVaultFooterPresentation {
    fun effectivePath(vaultSelectedPath: String, currentFile: String): String =
        JsonVaultFooterPresentation.effectivePath(vaultSelectedPath, currentFile)

    fun showFooter(path: String): Boolean = JsonVaultFooterPresentation.showFooter(path)

    fun footerDirty(path: String, currentFile: String, documentDirty: Boolean): Boolean =
        path == currentFile && documentDirty

    fun canDuplicate(isDirectory: Boolean?): Boolean = JsonVaultFooterPresentation.canDuplicate(isDirectory)

    /** Vault Git 操作前 flush：无打开笔记或正文/metadata 均已保存时跳过写盘。 */
    fun gitFlushSkipsWhenClean(currentFile: String, documentDirty: Boolean): Boolean =
        JsonVaultFooterPresentation.gitFlushSkipsWhenClean(currentFile, documentDirty)

    /** 无 Vault 笔记但编辑器有非示例内容时阻止 Git 操作（i18n 键 `git.flush.untitled`）。 */
    fun gitUntitledBlockKey(currentFile: String, editorText: String, sampleText: String): String? =
        if (currentFile.isBlank() && editorText.isNotBlank() && editorText != sampleText) {
            "git.flush.untitled"
        } else {
            null
        }
}
