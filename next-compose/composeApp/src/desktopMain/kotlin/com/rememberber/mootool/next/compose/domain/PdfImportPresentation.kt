package com.rememberber.mootool.next.compose.domain

/** F24 导入 inspect 提示与加密 PDF 错误呈现（对齐 Electron 导入 toast）。 */
object PdfImportPresentation {
    fun shouldShowStructureNotice(info: PdfFileInfo): Boolean = info.hasSpecialObjects

    fun structureNoticeArgs(info: PdfFileInfo): Map<String, String> = mapOf(
        "forms" to info.formFieldCount.toString(),
        "bookmarks" to info.bookmarkCount.toString(),
        "signatures" to info.signatureFieldCount.toString(),
    )

    fun showEncryptedImportToast(error: Throwable): Boolean =
        (error as? PdfException)?.code == "encrypted"
}
