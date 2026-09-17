package com.rememberber.mootool.next.compose.domain

import kotlin.test.Test
import kotlin.test.assertEquals

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
}
