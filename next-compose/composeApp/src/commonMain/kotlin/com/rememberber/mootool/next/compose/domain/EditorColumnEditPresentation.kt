package com.rememberber.mootool.next.compose.domain

/** F01/F04 列编辑与 IME 产品窗证据路径（对齐 `prepare-editor-ime-evidence.sh`）。 */
object EditorColumnEditPresentation {
    const val JSON_IME_SAMPLE = "ime-sample.json"
    const val QUICK_NOTE_IME_SAMPLE = "ime-sample.md"

    /** JSON/随手记列编辑闩锁时的状态栏 notice 键（wrap 时提示逻辑行语义）。 */
    fun columnNoticeKey(columnLatch: Boolean, wrap: Boolean): String? = when {
        !columnLatch -> null
        wrap -> "quickNote.columnEdit.wrap"
        else -> "quickNote.columnEdit.hint"
    }

    /** 对齐 Electron `columnEditingExtensions`：闩锁后等同始终 Alt 列选手势。 */
    fun columnDragWithoutAlt(columnLatch: Boolean): Boolean = columnLatch

    fun columnGestureActive(altDown: Boolean, columnLatch: Boolean): Boolean =
        altDown || columnLatch

    /** 换行切换时列选基于逻辑行，清掉旧选区（对齐 CodeMirror 重配 wrap/列选）。 */
    fun clearSelectionOnWrapChange(hasColumnSelection: Boolean): Boolean = hasColumnSelection

    val commandPaletteKeywords: List<String> = listOf(
        "column",
        "columnedit",
        "alt-drag",
        "latch",
        "ime",
        "inputmethod",
        "列编辑",
        "闩锁",
        "输入法",
        "logical",
        "wrap",
        "alt-drag",
    )
}
