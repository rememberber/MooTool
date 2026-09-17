package com.rememberber.mootool.next.compose.domain

/** F15 正则：测试/取消守卫。 */
object RegexWiringPresentation {
    fun canRunTest(pattern: String, running: Boolean): Boolean = pattern.isNotBlank() && !running

    fun showCancel(running: Boolean): Boolean = running
}
