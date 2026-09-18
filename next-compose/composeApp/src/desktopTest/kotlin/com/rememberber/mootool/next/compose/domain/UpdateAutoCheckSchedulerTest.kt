package com.rememberber.mootool.next.compose.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking

private suspend fun waitUntil(timeoutMs: Long, pollMs: Long = 5, predicate: () -> Boolean) {
    val deadline = System.nanoTime() + timeoutMs * 1_000_000
    while (System.nanoTime() < deadline) {
        if (predicate()) return
        delay(pollMs)
    }
    assertTrue(predicate(), "condition not met within ${timeoutMs}ms")
}

class UpdateAutoCheckSchedulerTest {
    @Test
    fun timingMatchesElectron() {
        assertEquals(2_500L, UpdateAutoCheckTiming.STARTUP_DELAY_MS)
        assertEquals(3_600_000L, UpdateAutoCheckTiming.INTERVAL_MS)
    }

    @Test
    fun runsCheckAfterStartupDelayWhenEnabled() = runBlocking {
        var checks = 0
        val startupDelayMs = 15L
        val scheduler = UpdateAutoCheckScheduler(
            scope = CoroutineScope(Dispatchers.Default),
            enabled = { true },
            autoDownload = { true },
            check = { checks++ },
            startupDelayMs = startupDelayMs,
            intervalMs = 60_000,
        )
        scheduler.reconfigure()
        delay(5)
        assertEquals(0, checks)
        waitUntil(startupDelayMs + 150) { checks >= 1 }
        assertEquals(1, checks)
        scheduler.stop()
    }

    @Test
    fun passesLatestAutoDownloadFlagOnEachCheck() = runBlocking {
        var autoDownload = false
        val flags = mutableListOf<Boolean>()
        val startupDelayMs = 50L
        val intervalMs = 100L
        val scheduler = UpdateAutoCheckScheduler(
            scope = CoroutineScope(Dispatchers.Default),
            enabled = { true },
            autoDownload = { autoDownload },
            check = { flags += it },
            startupDelayMs = startupDelayMs,
            intervalMs = intervalMs,
        )
        scheduler.reconfigure()
        waitUntil(startupDelayMs + 200) { flags.size >= 1 }
        assertEquals(1, flags.size)
        assertEquals(false, flags.first())
        autoDownload = true
        waitUntil(intervalMs + 200) { flags.size >= 2 }
        scheduler.stop()
        assertTrue(flags.size >= 2, "expected interval tick after autoDownload flip, got ${flags.size}")
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
