package com.rememberber.mootool.next.compose.domain

/**
 * Vault 外部冲突对话框显隐与文案（对齐 Electron conflict overlay；JSON/随手记共用）。
 */
object VaultConflictPresentation {
    fun showReloadAction(deleted: Boolean): Boolean = !deleted

    /** 说明行：修改 vs 外部删除。 */
    fun hintMessageKey(deleted: Boolean): String =
        if (deleted) "vault.conflict.hintDeleted" else "vault.conflict.hint"

    fun buildUnifiedPreview(
        editorText: String,
        diskText: String,
        maxChars: Int = 4_000,
    ): String =
        DiffEngine.compare(editorText, diskText, ignoreWhitespace = false).unified.take(maxChars)

    fun previewText(
        deleted: Boolean,
        deletedMessage: String,
        noDiffMessage: String,
        unifiedDiff: String,
    ): String = when {
        deleted -> deletedMessage
        unifiedDiff.isNotBlank() -> unifiedDiff
        else -> noDiffMessage
    }

    /** 外部删除时主操作是「另存副本」（对齐 Electron 无 reload 时的按钮权重）。 */
    fun saveCopyProminent(deleted: Boolean): Boolean = deleted

    fun reloadProminent(deleted: Boolean): Boolean = !deleted

    fun showDiffPreview(deleted: Boolean): Boolean = !deleted
}
