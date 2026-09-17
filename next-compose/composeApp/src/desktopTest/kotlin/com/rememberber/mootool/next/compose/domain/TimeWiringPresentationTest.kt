package com.rememberber.mootool.next.compose.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TimeWiringPresentationTest {
    @Test
    fun convertGuards() {
        assertFalse(TimeWiringPresentation.canConvertTimestamp(""))
        assertTrue(TimeWiringPresentation.canConvertLocal("2026-01-01"))
    }

    @Test
    fun runTimestampToLocalUsesEngine() {
        val outcome = TimeWiringPresentation.runTimestampToLocal("0", TimestampUnit.Second, "Asia/Shanghai")
        assertTrue(outcome is TimeWiringPresentation.ConvertOutcome.ToLocal)
        assertEquals(
            "1970-01-01 08:00:00",
            (outcome as TimeWiringPresentation.ConvertOutcome.ToLocal).result.localTime,
        )
    }

    @Test
    fun runLocalToTimestampUsesEngine() {
        val outcome = TimeWiringPresentation.runLocalToTimestamp(
            "1970-01-01 08:00:00",
            TimestampUnit.Second,
            "Asia/Shanghai",
        )
        assertTrue(outcome is TimeWiringPresentation.ConvertOutcome.ToTimestamp)
        assertEquals("0", (outcome as TimeWiringPresentation.ConvertOutcome.ToTimestamp).timestamp)
    }

    @Test
    fun runTimestampToLocalSurfacesInvalidInput() {
        val outcome = TimeWiringPresentation.runTimestampToLocal("nope", TimestampUnit.Second, "UTC")
        assertTrue(outcome is TimeWiringPresentation.ConvertOutcome.Failure)
    }
}
