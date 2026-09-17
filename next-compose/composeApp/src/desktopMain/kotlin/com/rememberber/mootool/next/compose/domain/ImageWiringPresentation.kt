package com.rememberber.mootool.next.compose.domain

/** F23 图片：busy/选中态工具栏守卫（SVG 数值 clamp 见 [ImageSvgWiringPresentation]）。 */
object ImageWiringPresentation {
    fun canImport(busy: Boolean): Boolean = !busy

    fun canProcessSelection(selectedCount: Int, busy: Boolean): Boolean = selectedCount > 0 && !busy

    fun canActOnCurrent(hasCurrent: Boolean, busy: Boolean = false): Boolean = hasCurrent && !busy

    fun canCopyCurrent(hasCurrent: Boolean): Boolean = hasCurrent

    fun canExportBase64(text: String): Boolean = text.isNotBlank()

    fun canStartWatermark(text: String): Boolean = text.isNotBlank()
}
