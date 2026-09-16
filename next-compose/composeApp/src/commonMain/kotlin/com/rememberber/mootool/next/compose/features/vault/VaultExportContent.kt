package com.rememberber.mootool.next.compose.features.vault

/** Vault 导出条目为当前打开且缓冲区脏时，应导出编辑器内容而非磁盘副本（对齐 Electron `exportNote` / 编辑器 `content`）。 */
fun vaultExportUsesEditorBuffer(
    exportPath: String,
    openPath: String,
    editorText: String,
    savedText: String,
): Boolean =
    exportPath.isNotBlank() &&
        exportPath == openPath &&
        editorText != savedText

fun vaultExportText(
    exportPath: String,
    openPath: String,
    editorText: String,
    savedText: String,
    diskText: String,
): String = if (vaultExportUsesEditorBuffer(exportPath, openPath, editorText, savedText)) editorText else diskText
