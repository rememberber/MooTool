package com.rememberber.mootool.next.compose.domain

import java.time.DateTimeException
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.time.format.ResolverStyle
import java.time.format.DateTimeFormatterBuilder
import java.time.temporal.ChronoField

enum class TimestampUnit { Second, Millisecond }

data class TimestampConversion(
    val localTime: String,
    val unit: TimestampUnit,
    val milliseconds: Long
)

class TimeException(val code: String, message: String) : RuntimeException(message)

object TimeEngine {
    val commonTimezones = listOf(
        "UTC", "Asia/Shanghai", "Asia/Tokyo", "Asia/Seoul", "Asia/Singapore", "Asia/Hong_Kong",
        "Asia/Kolkata", "Asia/Dubai", "Europe/London", "Europe/Paris", "Europe/Berlin", "Europe/Moscow",
        "America/New_York", "America/Chicago", "America/Denver", "America/Los_Angeles",
        "Australia/Sydney", "Pacific/Auckland"
    )

    val quickTimezones = listOf(
        "UTC" to "UTC",
        "Asia/Shanghai" to "+8",
        "Asia/Tokyo" to "+9",
        "America/New_York" to "-5",
        "America/Los_Angeles" to "-8",
        "Europe/Paris" to "+1",
        "Europe/Moscow" to "+3"
    )

    private val formatter: DateTimeFormatter = DateTimeFormatterBuilder()
        .parseDefaulting(ChronoField.ERA, 1)
        .appendPattern("uuuu-MM-dd HH:mm:ss")
        .toFormatter()
        .withResolverStyle(ResolverStyle.STRICT)

    fun timestampToLocal(input: String, unit: TimestampUnit, zone: String): TimestampConversion {
        val normalized = input.trim()
        if (!Regex("^-?\\d+$").matches(normalized)) {
            throw TimeException("invalid-timestamp", "invalid-timestamp")
        }
        val value = normalized.toLongOrNull() ?: throw TimeException("invalid-timestamp", "invalid-timestamp")
        val milliseconds = try {
            when (unit) {
                TimestampUnit.Second -> Math.multiplyExact(value, 1000L)
                TimestampUnit.Millisecond -> value
            }
        } catch (_: ArithmeticException) {
            throw TimeException("invalid-timestamp", "invalid-timestamp")
        }
        val localTime = try {
            formatLocalTime(milliseconds, zone)
        } catch (_: DateTimeException) {
            throw TimeException("invalid-timestamp", "invalid-timestamp")
        }
        return TimestampConversion(localTime, unit, milliseconds)
    }

    fun localToTimestamp(input: String, unit: TimestampUnit, zone: String): String {
        val zoneId = zoneId(zone)
        val local = try {
            LocalDateTime.parse(input.trim(), formatter)
        } catch (_: DateTimeParseException) {
            throw TimeException("invalid-local-time", "invalid-local-time")
        }
        val offsets = zoneId.rules.getValidOffsets(local)
        val zoned = when {
            offsets.isEmpty() -> throw TimeException("dst-gap", "dst-gap")
            offsets.size > 1 -> throw TimeException("dst-overlap", "dst-overlap")
            else -> ZonedDateTime.of(local, zoneId)
        }
        val milliseconds = zoned.toInstant().toEpochMilli()
        return if (unit == TimestampUnit.Second) (milliseconds / 1000).toString() else milliseconds.toString()
    }

    fun formatLocalTime(milliseconds: Long, zone: String): String {
        val instant = Instant.ofEpochMilli(milliseconds)
        return ZonedDateTime.ofInstant(instant, zoneId(zone)).format(formatter)
    }

    fun formatTimezoneLabel(zone: String, nowMillis: Long = System.currentTimeMillis()): String {
        val offset = ZonedDateTime.ofInstant(Instant.ofEpochMilli(nowMillis), zoneId(zone)).offset
        val formatted = offset.formattedGmt()
        return "$zone (GMT$formatted)"
    }

    fun systemZone(): String = ZoneId.systemDefault().id

    private fun zoneId(zone: String): ZoneId = try {
        ZoneId.of(zone)
    } catch (_: DateTimeException) {
        throw TimeException("invalid-timestamp", "invalid-zone")
    }

    private fun ZoneOffset.formattedGmt(): String {
        val total = totalSeconds
        val sign = if (total >= 0) "+" else "-"
        val abs = kotlin.math.abs(total)
        val hours = abs / 3600
        val minutes = (abs % 3600) / 60
        return "$sign${hours.toString().padStart(2, '0')}:${minutes.toString().padStart(2, '0')}"
    }
}
