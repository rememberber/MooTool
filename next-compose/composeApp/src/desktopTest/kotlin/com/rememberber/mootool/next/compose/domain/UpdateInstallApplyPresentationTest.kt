package com.rememberber.mootool.next.compose.domain

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class UpdateInstallApplyPresentationTest {
    @Test
    fun openInstallerRequiresReadyAndIdle() {
        assertTrue(UpdateInstallApplyPresentation.canOpenInstaller(busy = false, installerReady = true))
        assertFalse(UpdateInstallApplyPresentation.canOpenInstaller(busy = true, installerReady = true))
        assertFalse(UpdateInstallApplyPresentation.canOpenInstaller(busy = false, installerReady = false))
    }

    @Test
    fun recordsOpenFailureWhenNoPriorError() {
        assertTrue(UpdateInstallApplyPresentation.shouldRecordOpenFailure("", "boom"))
        assertFalse(UpdateInstallApplyPresentation.shouldRecordOpenFailure("existing", "boom"))
        assertFalse(UpdateInstallApplyPresentation.shouldRecordOpenFailure("", null))
    }
}
