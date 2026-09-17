package com.rememberber.mootool.next.compose.domain

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class TraySyncPresentationTest {
    @Test
    fun shouldInstallTrayRequiresEnabledAndSupported() {
        assertTrue(TraySyncPresentation.shouldInstallTray(trayEnabled = true, traySupported = true))
        assertFalse(TraySyncPresentation.shouldInstallTray(trayEnabled = false, traySupported = true))
        assertFalse(TraySyncPresentation.shouldInstallTray(trayEnabled = true, traySupported = false))
    }

    @Test
    fun menuRevisionChangesWhenLanguageOrHostRevisionChanges() {
        val base = TraySyncPresentation.menuRevision(true, "zh-CN", 0, 1L)
        val languageChanged = TraySyncPresentation.menuRevision(true, "en-US", 0, 1L)
        val hostChanged = TraySyncPresentation.menuRevision(true, "zh-CN", 2, 1L)
        assertNotEquals(base, languageChanged)
        assertNotEquals(base, hostChanged)
        val autoCheckChanged = TraySyncPresentation.menuRevision(
            true,
            "zh-CN",
            0,
            1L,
            autoCheckUpdates = true,
            autoDownloadUpdates = false,
        )
        assertNotEquals(base, autoCheckChanged)
    }
}
