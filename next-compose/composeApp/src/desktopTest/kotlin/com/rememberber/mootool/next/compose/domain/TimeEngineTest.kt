package com.rememberber.mootool.next.compose.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class TimeEngineTest {
    @Test
    fun convertsSecondsAndMillisecondsIntoIanaZone() {
        assertEquals("1970-01-01 08:00:00", TimeEngine.timestampToLocal("0", TimestampUnit.Second, "Asia/Shanghai").localTime)
        assertEquals(
            "2024-01-01 00:00:00",
            TimeEngine.timestampToLocal("1704067200000", TimestampUnit.Millisecond, "UTC").localTime
        )
    }

    @Test
    fun keepsExplicitSecondUnitForThirteenDigitInput() {
        val result = TimeEngine.timestampToLocal("1704067200000", TimestampUnit.Second, "UTC")
        assertEquals(TimestampUnit.Second, result.unit)
        assertTrue(result.localTime != "2024-01-01 00:00:00")
    }

    @Test
    fun convertsLocalTimeBackToSelectedUnit() {
        assertEquals("0", TimeEngine.localToTimestamp("1970-01-01 08:00:00", TimestampUnit.Second, "Asia/Shanghai"))
        assertEquals("1704067200000", TimeEngine.localToTimestamp("2024-01-01 00:00:00", TimestampUnit.Millisecond, "UTC"))
    }

    @Test
    fun rejectsInvalidDatesAndFormatsTimezoneLabels() {
        assertFailsWith<TimeException> { TimeEngine.localToTimestamp("2024-02-31 00:00:00", TimestampUnit.Second, "UTC") }
        assertFailsWith<TimeException> { TimeEngine.timestampToLocal("hello", TimestampUnit.Second, "UTC") }
        assertEquals("UTC (GMT+00:00)", TimeEngine.formatTimezoneLabel("UTC", 0))
    }

    @Test
    fun supportsNegativeEpochAndLeapDay() {
        assertEquals("1969-12-31 23:59:59", TimeEngine.timestampToLocal("-1", TimestampUnit.Second, "UTC").localTime)
        assertEquals("2024-02-29 12:00:00", TimeEngine.timestampToLocal(
            TimeEngine.localToTimestamp("2024-02-29 12:00:00", TimestampUnit.Second, "UTC"),
            TimestampUnit.Second,
            "UTC"
        ).localTime)
        assertFailsWith<TimeException> { TimeEngine.localToTimestamp("2023-02-29 00:00:00", TimestampUnit.Second, "UTC") }
    }

    @Test
    fun explainsDstGapAndOverlap() {
        val gap = assertFailsWith<TimeException> {
            TimeEngine.localToTimestamp("2024-03-10 02:30:00", TimestampUnit.Second, "America/New_York")
        }
        assertEquals("dst-gap", gap.code)
        val overlap = assertFailsWith<TimeException> {
            TimeEngine.localToTimestamp("2024-11-03 01:30:00", TimestampUnit.Second, "America/New_York")
        }
        assertEquals("dst-overlap", overlap.code)
    }

    @Test
    fun sameInstantAcrossZones() {
        val shanghai = TimeEngine.localToTimestamp("2024-01-01 08:00:00", TimestampUnit.Second, "Asia/Shanghai")
        val utc = TimeEngine.localToTimestamp("2024-01-01 00:00:00", TimestampUnit.Second, "UTC")
        assertEquals(shanghai, utc)
    }
}
