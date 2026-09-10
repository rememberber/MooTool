package com.rememberber.mootool.next.compose.domain

import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class CronEngineTest {
    @Test
    fun buildsAndSplitsQuartzStyleExpressions() {
        assertEquals("0 * * * * ?", CronEngine.build(CronEngine.defaultFields))
        assertEquals("2027", CronEngine.split("0 15 10 ? * MON-FRI 2027").year)
        assertFailsWith<CronException> { CronEngine.split("* * * * *") }
    }

    @Test
    fun calculatesWeekdayRunsInTimezone() {
        val runs = CronEngine.nextRuns(
            "0 0 9 ? * MON-FRI",
            "Asia/Shanghai",
            count = 2,
            from = Instant.parse("2026-07-17T02:00:00Z")
        )
        assertEquals(2, runs.size)
        assertTrue(runs[0].contains("2026-07-20 09:00:00"))
    }

    @Test
    fun filtersOptionalYearAndLeapDay() {
        val newYear = CronEngine.nextRuns(
            "0 0 0 1 1 ? 2028",
            "UTC",
            count = 1,
            from = Instant.parse("2026-01-01T00:00:00Z")
        ).single()
        assertTrue(newYear.contains("2028-01-01 00:00:00"))
        val leap = CronEngine.nextRuns(
            "0 0 0 29 2 ?",
            "UTC",
            count = 1,
            from = Instant.parse("2027-03-01T00:00:00Z")
        ).single()
        assertTrue(leap.contains("2028-02-29 00:00:00"))
    }

    @Test
    fun describesLanguageAndRejectsImpossibleSchedules() {
        val english = CronEngine.describe("0 0 9 ? * MON-FRI", "en-US")
        assertTrue(english.contains("9:00") || english.contains("09:00"))
        assertFailsWith<CronException> {
            CronEngine.nextRuns("0 0 0 1 1 ? 1999", "UTC", count = 1, from = Instant.parse("2026-01-01T00:00:00Z"))
        }
        assertFailsWith<CronException> {
            CronEngine.nextRuns("not-a-cron", "UTC", count = 1, from = Instant.parse("2026-01-01T00:00:00Z"))
        }
    }
}
