package com.rememberber.mootool.next.compose.domain

/** F01 随手记 Vault 底栏路径与操作守卫（对齐 [JsonVaultFooterPresentation]）。 */
object QuickNoteVaultFooterPresentation {
    fun effectivePath(vaultSelectedPath: String, currentFile: String): String =
        JsonVaultFooterPresentation.effectivePath(vaultSelectedPath, currentFile)

    fun showFooter(path: String): Boolean = JsonVaultFooterPresentation.showFooter(path)

    fun footerDirty(path: String, currentFile: String, documentDirty: Boolean): Boolean =
        path == currentFile && documentDirty

    fun canDuplicate(isDirectory: Boolean?): Boolean = JsonVaultFooterPresentation.canDuplicate(isDirectory)
}
