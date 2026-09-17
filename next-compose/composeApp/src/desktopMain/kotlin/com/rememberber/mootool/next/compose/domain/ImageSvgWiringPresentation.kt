package com.rememberber.mootool.next.compose.domain

/** F23 SVG 批量选项 clamp（可单测，对齐 ImageScreen 输入边界）。 */
object ImageSvgWiringPresentation {
    fun coerceColors(value: Int): Int = value.coerceIn(2, 64)

    fun coerceSpeckle(value: Int): Int = value.coerceIn(0, 128)

    fun parseColorsField(raw: String, fallback: Int): Int {
        val parsed = raw.trim().toIntOrNull() ?: return coerceColors(fallback)
        return coerceColors(parsed)
    }

    fun parseSpeckleField(raw: String, fallback: Int): Int {
        val parsed = raw.trim().toIntOrNull() ?: return coerceSpeckle(fallback)
        return coerceSpeckle(parsed)
    }

    fun canStartSvgBatch(selectedCount: Int, busy: Boolean): Boolean = selectedCount > 0 && !busy
}
