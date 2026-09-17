package com.rememberber.mootool.next.compose.domain

/** F12 UA 解析：解析按钮守卫。 */
object UaWiringPresentation {
    fun canParse(source: String): Boolean = source.isNotBlank()
}
