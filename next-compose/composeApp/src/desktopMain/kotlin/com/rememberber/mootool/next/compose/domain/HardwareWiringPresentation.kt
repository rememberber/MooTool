package com.rememberber.mootool.next.compose.domain

/** F25 系统信息采集工具栏启用守卫（可单测）。 */
object HardwareWiringPresentation {
    fun refreshEnabled(loading: Boolean): Boolean = !loading

    fun copyReportEnabled(loading: Boolean, hasGroups: Boolean): Boolean = !loading && hasGroups
}
