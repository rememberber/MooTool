package com.rememberber.mootool.next.compose.domain

/** F25 系统信息采集工具栏启用守卫（可单测）。 */
object HardwareWiringPresentation {
    fun refreshEnabled(loading: Boolean): Boolean = !loading

    fun copyReportEnabled(loading: Boolean, hasGroups: Boolean): Boolean = !loading && hasGroups

    fun interfacesCommandEnabled(loading: Boolean, running: Boolean): Boolean = !loading && !running

    sealed interface CollectOutcome {
        data class Success(val snapshot: HardwareSnapshot) : CollectOutcome
        data class Failure(val error: Throwable) : CollectOutcome
    }

    fun runCollect(loadSampleMs: Long = 200): CollectOutcome =
        runCatching { HardwareEngine.collect(loadSampleMs) }.fold(
            onSuccess = { CollectOutcome.Success(it) },
            onFailure = { CollectOutcome.Failure(it) },
        )
}
