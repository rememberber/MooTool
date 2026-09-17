package com.rememberber.mootool.next.compose.domain

/** F13 编码解码：转换方向启用守卫。 */
object EncodeWiringPresentation {
    fun canConvert(sourceText: String): Boolean = sourceText.isNotBlank()
}
