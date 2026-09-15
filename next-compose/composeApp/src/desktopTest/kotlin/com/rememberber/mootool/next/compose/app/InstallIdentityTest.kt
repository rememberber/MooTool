package com.rememberber.mootool.next.compose.app

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class InstallIdentityTest {
    @Test
    fun upgradeCodeAndUninstallScopeStayOnThisProduct() {
        assertEquals(ProductIdentity.WINDOWS_UPGRADE_UUID, InstallIdentity.WINDOWS_UPGRADE_UUID)
        assertEquals(ProductIdentity.APPLICATION_ID, InstallIdentity.MACOS_BUNDLE_ID)
        assertEquals("mootool-next-compose", InstallIdentity.LINUX_PACKAGE)
        assertTrue(InstallIdentity.ownsWindowsUpgradeCode("d6574bad-ff7c-4038-8d17-b9c7988787ba"))
        assertFalse(InstallIdentity.uninstallMustNotTouch().any { it.contains("next-compose") })
        assertTrue(InstallIdentity.foreignInstallMarkers.any { it.contains("MooTool") })
        assertEquals("public.app-category.developer-tools", InstallIdentity.MACOS_APP_CATEGORY)
        assertEquals("12.0", InstallIdentity.MACOS_MINIMUM_SYSTEM)
        assertTrue(InstallIdentity.MACOS_SCREEN_CAPTURE_USAGE.contains("Screen Recording"))
        val gradle = java.io.File("build.gradle.kts").let { if (it.exists()) it else java.io.File("composeApp/build.gradle.kts") }
        val gradleText = gradle.readText()
        assertTrue(gradleText.contains("NSScreenCaptureUsageDescription"))
        assertTrue(gradleText.contains(InstallIdentity.MACOS_SCREEN_CAPTURE_USAGE))
        assertEquals("MooTool-Next-Compose-0.1.0-mac-arm64.dmg", InstallIdentity.distArtifactName("0.1.0", "mac", "arm64"))
        assertEquals("MooTool-Next-Compose-0.1.0-win-x64-setup.msi", InstallIdentity.distArtifactName("0.1.0", "win", "x64"))
        assertEquals("MooTool-Next-Compose-0.1.0-linux-x64.deb", InstallIdentity.distArtifactName("0.1.0", "linux", "x64"))
    }
}
