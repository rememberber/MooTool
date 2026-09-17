package com.rememberber.mootool.next.compose.domain

/** F01/F04 列编辑与 IME 产品窗证据路径（对齐 `prepare-editor-ime-evidence.sh`）。 */
object EditorColumnEditPresentation {
    const val JSON_IME_SAMPLE = "ime-sample.json"
    const val QUICK_NOTE_IME_SAMPLE = "ime-sample.md"

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
    )
}
