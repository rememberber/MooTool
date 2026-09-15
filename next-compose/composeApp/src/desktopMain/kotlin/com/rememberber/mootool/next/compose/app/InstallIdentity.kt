package com.rememberber.mootool.next.compose.app

/**
 * Installer / uninstaller identity for next-compose only.
 * Other MooTool products keep their own bundle IDs, UpgradeCode and data roots.
 */
object InstallIdentity {
    const val WINDOWS_UPGRADE_UUID = ProductIdentity.WINDOWS_UPGRADE_UUID
    const val WINDOWS_APP_USER_MODEL_ID = ProductIdentity.APPLICATION_ID
    const val MACOS_BUNDLE_ID = ProductIdentity.APPLICATION_ID
    const val LINUX_PACKAGE = ProductIdentity.LINUX_PACKAGE
    const val LINUX_DESKTOP_ENTRY = ProductIdentity.LINUX_DESKTOP_ENTRY
    const val LINUX_RPM_LICENSE = "MIT"
    const val MACOS_APP_CATEGORY = "public.app-category.developer-tools"
    const val MACOS_MINIMUM_SYSTEM = "12.0"
    const val MACOS_SCREEN_CAPTURE_USAGE =
        "MooTool Next Compose needs Screen Recording permission to pick colors from the screen and capture a region."

    val foreignInstallMarkers: List<String> = listOf(
        "MooTool.app",
        "MooTool Next.app",
        "com.luoboduner.mootool",
        "com.rememberber.mootool.next",
        "mootool-next",
        "MooTool Next"
    )

    fun uninstallMustNotTouch(): List<String> = listOf(
        "~/Library/Application Support/com.luoboduner.mootool",
        "~/Library/Application Support/com.rememberber.mootool.next",
        "%APPDATA%/MooTool",
        "%APPDATA%/MooToolNext",
        "~/.config/mootool",
        "~/.config/mootool-next"
    )

    fun ownsWindowsUpgradeCode(uuid: String): Boolean =
        uuid.equals(WINDOWS_UPGRADE_UUID, ignoreCase = true)

    fun distArtifactName(version: String, os: String, arch: String): String {
        val safe = version.trim().ifBlank { "0.0.0" }
        return when (os) {
            "mac" -> "MooTool-Next-Compose-$safe-mac-$arch.dmg"
            "win" -> "MooTool-Next-Compose-$safe-win-$arch-setup.msi"
            "linux" -> "MooTool-Next-Compose-$safe-linux-$arch.deb"
            else -> "MooTool-Next-Compose-$safe-$os-$arch"
        }
    }
}
