package com.rememberber.mootool.next.compose.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CronWiringPresentationTest {
    @Test
    fun timezoneMenuDedupesSystemZone() {
        val menu = CronWiringPresentation.timezoneMenu("UTC", listOf("UTC", "Asia/Shanghai"))
        assertEquals(listOf("UTC", "Asia/Shanghai"), menu)
    }

    @Test
    fun coerceSessionZoneUsesFallback() {
        assertEquals("Asia/Shanghai", CronWiringPresentation.coerceSessionZone("  ", "Asia/Shanghai"))
        assertEquals("Europe/London", CronWiringPresentation.coerceSessionZone("Europe/London", "UTC"))
    }

    @Test
    fun parseAndCopyRunsGuards() {
        assertFalse(CronWiringPresentation.canParse(""))
        assertTrue(CronWiringPresentation.canParse("0 0 * * *"))
        assertFalse(CronWiringPresentation.canCopyRuns(emptyList()))
        assertTrue(CronWiringPresentation.canCopyRuns(listOf("2026-01-01")))
    }
}
