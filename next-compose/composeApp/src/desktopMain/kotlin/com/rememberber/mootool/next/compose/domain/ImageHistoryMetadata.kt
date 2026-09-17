package com.rememberber.mootool.next.compose.domain

/** F23 图片历史 `operation`（process / svg）。 */
object ImageHistoryMetadata {
    const val OP_PROCESS = "process"
    const val OP_SVG = "svg"

    fun normalizeOperation(operation: String): String = when (operation.trim()) {
        OP_SVG -> OP_SVG
        else -> OP_PROCESS
    }
}
