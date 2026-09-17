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
        val japanese = CronEngine.describe("0 0 9 ? * MON-FRI", "ja-JP")
        assertTrue(japanese.contains("9:00") || japanese.contains("09:00"))
        assertFailsWith<CronException> {
            CronEngine.nextRuns("0 0 0 1 1 ? 1999", "UTC", count = 1, from = Instant.parse("2026-01-01T00:00:00Z"))
        }
        assertFailsWith<CronException> {
            CronEngine.nextRuns("not-a-cron", "UTC", count = 1, from = Instant.parse("2026-01-01T00:00:00Z"))
        }
    }

    @Test
    fun nthWeekdayAndDstSpringForward() {
        val secondWed = CronEngine.nextRuns(
            "0 0 12 ? * WED#2",
            "UTC",
            count = 1,
            from = Instant.parse("2026-03-01T00:00:00Z"),
        ).single()
        assertEquals("2026-03-11 12:00:00", secondWed.substring(0, 19), secondWed)

        val dstHour = CronEngine.nextRuns(
            "0 0 2 * * ?",
            "America/New_York",
            count = 3,
            from = Instant.parse("2024-03-09T05:00:00Z"),
        )
        // cron-utils skips the non-existent 02:00 on 2024-03-10 (US spring forward); Electron cron-parser may emit 03:00 that day — see DIFF-004 / DIFF-519.
        assertEquals(
            listOf("2024-03-09 02:00:00", "2024-03-11 02:00:00", "2024-03-12 02:00:00"),
            dstHour.map { it.substring(0, 19) },
            dstHour.joinToString("\n"),
        )
    }

    @Test
    fun runLinesIncludeNumericOffset() {
        val line = CronEngine.formatRunLine(
            java.time.ZonedDateTime.parse("2026-07-20T09:00:00+08:00[Asia/Shanghai]"),
        )
        assertTrue(line.contains("2026-07-20 09:00:00"))
        assertTrue(line.contains("+08:00"))
    }
}
