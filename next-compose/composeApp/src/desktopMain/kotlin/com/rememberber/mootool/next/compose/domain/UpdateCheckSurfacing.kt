package com.rememberber.mootool.next.compose.domain

/**
 * Electron `checkForUpdatesAndBroadcast(automatic)`：后台检查仅在 `status === 'available'` 时广播；
 * 失败时 automatic 不广播。
 */
object UpdateCheckSurfacing {
    fun shouldApplyUiResult(automatic: Boolean, status: UpdateCheckStatus): Boolean =
        !automatic || status == UpdateCheckStatus.Available

    fun shouldApplyUiError(automatic: Boolean): Boolean = !automatic
}
