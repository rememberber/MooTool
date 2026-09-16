package com.rememberber.mootool.next.compose.domain

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class UpdateAutoDownloadTriggerTest {
    @Test
    fun startsWhenAvailableAndNotReady() {
        assertTrue(
            UpdateAutoDownloadTrigger.shouldStartDownload(
                autoDownloadEnabled = true,
                busy = false,
                checkStatus = UpdateCheckStatus.Available,
                hasDownloadPack = true,
                installerReady = false,
            ),
        )
    }

    @Test
    fun skipsWhenDisabledBusyOrNotAvailable() {
        assertFalse(
            UpdateAutoDownloadTrigger.shouldStartDownload(
                autoDownloadEnabled = false,
                busy = false,
                checkStatus = UpdateCheckStatus.Available,
                hasDownloadPack = true,
                installerReady = false,
            ),
        )
        assertFalse(
            UpdateAutoDownloadTrigger.shouldStartDownload(
                autoDownloadEnabled = true,
                busy = true,
                checkStatus = UpdateCheckStatus.Available,
                hasDownloadPack = true,
                installerReady = false,
            ),
        )
        assertFalse(
            UpdateAutoDownloadTrigger.shouldStartDownload(
                autoDownloadEnabled = true,
                busy = false,
                checkStatus = UpdateCheckStatus.Latest,
                hasDownloadPack = true,
                installerReady = false,
            ),
        )
        assertFalse(
            UpdateAutoDownloadTrigger.shouldStartDownload(
                autoDownloadEnabled = true,
                busy = false,
                checkStatus = UpdateCheckStatus.Available,
                hasDownloadPack = true,
                installerReady = true,
            ),
        )
    }
}
