package com.rememberber.mootool.next.compose.domain

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/** Electron `configureUpdateChecks`: 2.5s 后首次检查，之后每小时一次。 */
object UpdateAutoCheckTiming {
    const val STARTUP_DELAY_MS = 2_500L
    const val INTERVAL_MS = 60 * 60 * 1_000L
}

class UpdateAutoCheckScheduler(
    private val scope: CoroutineScope,
    private val enabled: () -> Boolean,
    private val autoDownload: () -> Boolean,
    private val check: (autoDownload: Boolean) -> Unit,
    private val startupDelayMs: Long = UpdateAutoCheckTiming.STARTUP_DELAY_MS,
    private val intervalMs: Long = UpdateAutoCheckTiming.INTERVAL_MS,
) {
    private var job: Job? = null

    fun reconfigure() {
        job?.cancel()
        job = null
        if (!enabled()) return
        job = scope.launch {
            delay(startupDelayMs)
            while (isActive) {
                check(autoDownload())
                delay(intervalMs)
            }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
    }
}
