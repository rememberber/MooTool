package com.rememberber.mootool.next.compose.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking

class UpdateAutoCheckSchedulerTest {
    @Test
    fun timingMatchesElectron() {
        assertEquals(2_500L, UpdateAutoCheckTiming.STARTUP_DELAY_MS)
        assertEquals(3_600_000L, UpdateAutoCheckTiming.INTERVAL_MS)
    }

    @Test
    fun runsCheckAfterStartupDelayWhenEnabled() = runBlocking {
        var checks = 0
        val scheduler = UpdateAutoCheckScheduler(
            scope = CoroutineScope(Dispatchers.Default),
            enabled = { true },
            autoDownload = { true },
            check = { checks++ },
            startupDelayMs = 15,
            intervalMs = 60_000,
        )
        scheduler.reconfigure()
        delay(5)
        assertEquals(0, checks)
        delay(20)
        assertEquals(1, checks)
        scheduler.stop()
    }

    @Test
    fun passesLatestAutoDownloadFlagOnEachCheck() = runBlocking {
        var autoDownload = false
        val flags = mutableListOf<Boolean>()
        val scheduler = UpdateAutoCheckScheduler(
            scope = CoroutineScope(Dispatchers.Default),
            enabled = { true },
            autoDownload = { autoDownload },
            check = { flags += it },
            startupDelayMs = 10,
            intervalMs = 20,
        )
        scheduler.reconfigure()
        delay(15)
        autoDownload = true
        delay(25)
        scheduler.stop()
        assertTrue(flags.size >= 2)
        assertEquals(false, flags.first())
        assertEquals(true, flags.last())
    }

    @Test
    fun doesNotScheduleWhenDisabled() = runBlocking {
        var checks = 0
        val scheduler = UpdateAutoCheckScheduler(
            scope = CoroutineScope(Dispatchers.Default),
            enabled = { false },
            autoDownload = { false },
            check = { checks++ },
            startupDelayMs = 5,
            intervalMs = 10,
        )
        scheduler.reconfigure()
        delay(30)
        assertEquals(0, checks)
        scheduler.stop()
    }
}
