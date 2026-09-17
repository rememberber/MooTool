package com.rememberber.mootool.next.compose.domain

/** F24 PDF 历史 `operation`（split / merge）。 */
object PdfHistoryMetadata {
    const val OP_SPLIT = "split"
    const val OP_MERGE = "merge"

    fun normalizeOperation(operation: String): String = when (operation.trim()) {
        OP_MERGE -> OP_MERGE
        else -> OP_SPLIT
    }
}
