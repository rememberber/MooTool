package com.rememberber.mootool.next.compose.domain

/** F18 时间：字段转换与复制守卫。 */
object TimeWiringPresentation {
    fun canConvertTimestamp(timestamp: String): Boolean = timestamp.isNotBlank()

    fun canConvertLocal(localTime: String): Boolean = localTime.isNotBlank()

    fun canCopyField(value: String): Boolean = value.isNotBlank()

    sealed interface ConvertOutcome {
        data class ToLocal(val result: TimestampConversion) : ConvertOutcome
        data class ToTimestamp(val timestamp: String) : ConvertOutcome
        data class Failure(val error: Throwable) : ConvertOutcome
    }

    fun runTimestampToLocal(timestamp: String, unit: TimestampUnit, zone: String): ConvertOutcome =
        runCatching { TimeEngine.timestampToLocal(timestamp, unit, zone) }.fold(
            onSuccess = { ConvertOutcome.ToLocal(it) },
            onFailure = { ConvertOutcome.Failure(it) },
        )

    fun runLocalToTimestamp(localTime: String, unit: TimestampUnit, zone: String): ConvertOutcome =
        runCatching { TimeEngine.localToTimestamp(localTime, unit, zone) }.fold(
            onSuccess = { ConvertOutcome.ToTimestamp(it) },
            onFailure = { ConvertOutcome.Failure(it) },
        )
}
