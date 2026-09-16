package com.rememberber.mootool.next.compose.domain

/**
 * Electron `UpdateManager.setAutoDownload(true)`：已有 `available` 结果且未就绪时立即 `download()`。
 */
object UpdateAutoDownloadTrigger {
    fun shouldStartDownload(
        autoDownloadEnabled: Boolean,
        busy: Boolean,
        checkStatus: UpdateCheckStatus?,
        hasDownloadPack: Boolean,
        installerReady: Boolean,
    ): Boolean =
        autoDownloadEnabled &&
            !busy &&
            checkStatus == UpdateCheckStatus.Available &&
            hasDownloadPack &&
            !installerReady
}
