package com.rememberber.mootool.next.compose.domain

/** F16 Cron：时区菜单与 session 区规范化（对齐 ZonePicker / 历史恢复）。 */
object CronWiringPresentation {
    fun timezoneMenu(systemZone: String, commonZones: List<String>): List<String> =
        (listOf(systemZone) + commonZones).distinct()

    fun coerceSessionZone(sessionZone: String, fallback: String): String {
        val trimmed = sessionZone.trim()
        return trimmed.ifBlank { fallback }
    }

    fun canParse(expression: String): Boolean = expression.isNotBlank()

    fun canCopyRuns(runs: List<String>): Boolean = runs.isNotEmpty()
}
