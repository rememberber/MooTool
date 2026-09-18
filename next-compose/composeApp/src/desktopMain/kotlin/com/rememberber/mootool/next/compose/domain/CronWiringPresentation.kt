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

    fun parseActionEnabled(expression: String): Boolean = canParse(expression)

    fun canCopyRuns(runs: List<String>): Boolean = runs.isNotEmpty()

    fun copyRunsActionEnabled(runs: List<String>): Boolean = canCopyRuns(runs)

    sealed interface ScheduleOutcome {
        data class Success(val runs: List<String>, val description: String) : ScheduleOutcome
        data class Failure(val error: Throwable) : ScheduleOutcome
    }

    fun runPreview(expression: String, zone: String, language: String): ScheduleOutcome =
        runCatching {
            val runs = CronEngine.nextRuns(expression, zone)
            val description = CronEngine.describe(expression, language)
            runs to description
        }.fold(
            onSuccess = { (runs, description) -> ScheduleOutcome.Success(runs, description) },
            onFailure = { ScheduleOutcome.Failure(it) },
        )

    fun shouldToastPreviewFailure(error: Throwable): Boolean = true
}
