package com.rememberber.mootool.next.compose.domain

/** F04 JSON Vault 底栏路径与操作守卫（树选中 vs 当前打开文件）。 */
object JsonVaultFooterPresentation {
    fun effectivePath(vaultSelectedPath: String, currentFile: String): String =
        vaultSelectedPath.ifBlank { currentFile }

    fun showFooter(path: String): Boolean = path.isNotBlank()

    fun footerDirty(path: String, currentFile: String, editorText: String, savedText: String): Boolean =
        path == currentFile && editorText != savedText

    fun canDuplicate(isDirectory: Boolean?): Boolean = isDirectory == false

    fun canRename(isDirectory: Boolean?): Boolean = isDirectory == false

    fun canShowVaultActions(path: String, isDirectory: Boolean?): Boolean =
        showFooter(path) && canRename(isDirectory)

    /** Vault Git 操作前 flush：无打开文件或文档已干净时跳过写盘（对齐 Electron `prepareGitAction`）。 */
    fun gitFlushSkipsWhenClean(currentFile: String, editorDirty: Boolean): Boolean =
        currentFile.isBlank() || !editorDirty

    /** 无 Vault 文件但编辑器有非示例内容时阻止 Git 操作（返回 i18n 键 `git.flush.untitled`）。 */
    fun gitUntitledBlockKey(currentFile: String, editorHasUserDraft: Boolean): String? =
        if (currentFile.isBlank() && editorHasUserDraft) "git.flush.untitled" else null
}
