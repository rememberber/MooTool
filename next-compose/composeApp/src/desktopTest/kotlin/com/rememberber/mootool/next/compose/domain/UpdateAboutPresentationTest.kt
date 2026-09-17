package com.rememberber.mootool.next.compose.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class UpdateAboutPresentationTest {
    @Test
    fun statusMessageKeysMatchAboutPageStates() {
        assertEquals("settings.update.checking", UpdateAboutPresentation.statusMessageKey("checking", false))
        assertEquals("settings.update.noPackage", UpdateAboutPresentation.statusMessageKey("available", false))
        assertEquals("settings.update.available", UpdateAboutPresentation.statusMessageKey("available", true))
        assertEquals("settings.update.upToDate", UpdateAboutPresentation.statusMessageKey("latest", false))
    }

    @Test
    fun actionVisibilityMatchesElectronAboutButtons() {
        assertTrue(UpdateAboutPresentation.showDownloadAction(hasDownloadPack = true, installerReady = false))
        assertFalse(UpdateAboutPresentation.showDownloadAction(hasDownloadPack = true, installerReady = true))
        assertTrue(UpdateAboutPresentation.showOpenInstallerAction(installerReady = true))
        assertTrue(UpdateAboutPresentation.showCancelDownloadAction("downloading"))
        assertFalse(UpdateAboutPresentation.showCancelDownloadAction("idle"))
    }

    @Test
    fun releaseNotesHiddenWhenUnpublished() {
        assertFalse(
            UpdateAboutPresentation.showReleaseNotes(
                releaseNotes = "notes",
                latestVersion = "2.0",
                checkStatus = "unpublished",
            ),
        )
        assertTrue(
            UpdateAboutPresentation.showReleaseNotes(
                releaseNotes = "notes",
                latestVersion = "2.0",
                checkStatus = "available",
            ),
        )
    }

    @Test
    fun checkAndProgressGuards() {
        assertFalse(UpdateAboutPresentation.canCheckForUpdates(busy = true))
        assertTrue(UpdateAboutPresentation.showDownloadProgress("downloading", hasProgress = true))
        assertFalse(UpdateAboutPresentation.showDownloadProgress("idle", hasProgress = true))
    }

    @Test
    fun updateResultCardVisibilityMatchesElectron() {
        assertEquals("settings.update.available", UpdateAboutPresentation.resultHeadlineKey("available"))
        assertEquals("settings.update.resultUpToDate", UpdateAboutPresentation.resultHeadlineKey("latest"))
        assertTrue(UpdateAboutPresentation.showMissingDownloadLine("available", hasDownloadPack = false))
        assertFalse(UpdateAboutPresentation.showMissingDownloadLine("available", hasDownloadPack = true))
        assertTrue(UpdateAboutPresentation.showDownloadFileLine(hasDownloadPack = true, fileName = "pkg.dmg"))
        assertEquals("network down", UpdateAboutPresentation.errorDisplayText("network down", "ignored"))
    }
}
