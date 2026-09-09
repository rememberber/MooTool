package com.rememberber.mootool.next.compose.domain

import com.cronutils.descriptor.CronDescriptor
import com.cronutils.model.CronType
import com.cronutils.model.definition.CronDefinitionBuilder
import com.cronutils.model.time.ExecutionTime
import com.cronutils.parser.CronParser
import kotlinx.serialization.Serializable
import java.time.DateTimeException
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@Serializable
data class CronFields(
    val second: String = "0",
    val minute: String = "*",
    val hour: String = "*",
    val day: String = "*",
    val month: String = "*",
    val week: String = "?",
    val year: String = ""
)

data class CronPreset(val id: String, val labelKey: String, val expression: String)

class CronException(val code: String, message: String) : RuntimeException(message)

object CronEngine {
    const val DEFAULT_COUNT = 10
    const val SEARCH_LIMIT = 100_000

    val defaultFields = CronFields()

    val presets: List<CronPreset> = listOf(
        CronPreset("minute", "cron.everyMinute", "0 * * * * ?"),
        CronPreset("hour", "cron.everyHour", "0 0 * * * ?"),
        CronPreset("day", "cron.everyDay", "0 0 0 * * ?"),
        CronPreset("weekdays", "cron.weekdays", "0 0 9 ? * MON-FRI")
    )

    private val parser = CronParser(CronDefinitionBuilder.instanceDefinitionFor(CronType.QUARTZ))
    private val stamp = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

    fun build(fields: CronFields): String {
        val required = listOf(fields.second, fields.minute, fields.hour, fields.day, fields.month, fields.week)
        if (required.any { it.isBlank() }) throw CronException("incomplete", "All Cron fields are required")
        return (required + fields.year.trim().let { if (it.isEmpty()) emptyList() else listOf(it) }).joinToString(" ")
    }

    fun split(expression: String): CronFields {
        val parts = expression.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
        if (parts.size != 6 && parts.size != 7) {
            throw CronException("invalid", "Cron requires 6 or 7 fields")
        }
        return CronFields(
            second = parts[0],
            minute = parts[1],
            hour = parts[2],
            day = parts[3],
            month = parts[4],
            week = parts[5],
            year = parts.getOrElse(6) { "" }
        )
    }

    fun nextRuns(
        expression: String,
        timeZone: String,
        count: Int = DEFAULT_COUNT,
        from: Instant = Instant.now()
    ): List<String> {
        val fields = split(expression)
        val quartz = listOf(fields.second, fields.minute, fields.hour, fields.day, fields.month, fields.week).joinToString(" ")
        val cron = try {
            parser.parse(quartz)
        } catch (error: RuntimeException) {
            throw CronException("invalid", error.message ?: "invalid")
        }
        val zone = try {
            ZoneId.of(timeZone)
        } catch (_: DateTimeException) {
            throw CronException("invalid", "invalid-timezone")
        }
        val execution = ExecutionTime.forCron(cron)
        val runs = ArrayList<String>(count)
        var cursor = ZonedDateTime.ofInstant(from, zone)
        var guard = 0
        while (runs.size < count && guard < SEARCH_LIMIT) {
            guard += 1
            val next = execution.nextExecution(cursor).orElse(null) ?: break
            cursor = next
            if (matchesYear(next.year, fields.year)) {
                runs += "${next.format(stamp)} ${zoneDisplay(next)}"
            }
        }
        if (runs.size < count) throw CronException("no-runs", "No matching run time in the supported year range")
        return runs
    }

    fun describe(expression: String, language: String): String {
        val fields = split(expression)
        val quartz = listOf(fields.second, fields.minute, fields.hour, fields.day, fields.month, fields.week).joinToString(" ")
        val cron = try {
            parser.parse(quartz)
        } catch (error: RuntimeException) {
            throw CronException("invalid", error.message ?: "invalid")
        }
        val locale = when (language) {
            "zh-CN" -> Locale.SIMPLIFIED_CHINESE
            "ja-JP" -> Locale.JAPANESE
            else -> Locale.US
        }
        return try {
            CronDescriptor.instance(locale).describe(cron)
        } catch (error: RuntimeException) {
            throw CronException("invalid", error.message ?: "invalid")
        }
    }

    internal fun matchesYear(year: Int, expression: String): Boolean {
        if (expression.isBlank() || expression == "*") return true
        return expression.split(',').any { part ->
            val step = Regex("""^(\*|\d{4}-\d{4})/(\d+)$""").matchEntire(part)
            if (step != null) {
                val (start, end) = if (step.groupValues[1] == "*") 1970 to 2199 else {
                    val bounds = step.groupValues[1].split('-').map { it.toInt() }
                    bounds[0] to bounds[1]
                }
                year in start..end && (year - start) % step.groupValues[2].toInt() == 0
            } else {
                val range = Regex("""^(\d{4})-(\d{4})$""").matchEntire(part)
                if (range != null) year in range.groupValues[1].toInt()..range.groupValues[2].toInt()
                else part.toIntOrNull() == year
            }
        }
    }

    private fun zoneDisplay(time: ZonedDateTime): String {
        val hours = time.offset.totalSeconds / 3600.0
        val whole = hours.toInt()
        return if (hours == whole.toDouble()) {
            "GMT" + (if (whole >= 0) "+" else "") + whole
        } else {
            time.offset.id
        }
    }
}
