package com.rememberber.mootool.next.compose.domain

import java.nio.file.Path

/**
 * Resolves Java-edition data paths the same way as Electron `legacyMigrationService.resolveLegacySource`.
 */
data class ResolvedLegacyJavaPaths(
    val databasePath: Path,
    val configPath: Path,
    val quickNoteVaultPath: Path,
    val jsonVaultPath: Path
)

object LegacyJavaDataPaths {
    fun resolve(sourceRoot: Path, config: LegacyJavaConfig): ResolvedLegacyJavaPaths {
        val root = sourceRoot.toAbsolutePath().normalize()
        val configPath = root.resolve("config").resolve("config.setting")
        val configuredDatabaseDirectory = settingValue(config, "func.advanced", "dbFilePath")
        val databasePath = if (configuredDatabaseDirectory.isNotEmpty()) {
            resolveConfiguredPath(root, configuredDatabaseDirectory).resolve("MooTool.db")
        } else {
            root.resolve("MooTool.db")
        }
        val quickNoteVaultPath = resolveConfiguredPath(
            root,
            settingValue(config, "func.quickNote", "quickNoteVaultPath").ifEmpty { "quick-notes" }
        )
        val jsonVaultPath = resolveConfiguredPath(
            root,
            settingValue(config, "func.jsonBeauty", "jsonBeautyVaultPath").ifEmpty { "json-beauty" }
        )
        return ResolvedLegacyJavaPaths(
            databasePath = databasePath,
            configPath = configPath,
            quickNoteVaultPath = quickNoteVaultPath,
            jsonVaultPath = jsonVaultPath
        )
    }

    private fun settingValue(config: LegacyJavaConfig, group: String, key: String): String =
        config[group]?.get(key)?.trim().orEmpty()

    private fun resolveConfiguredPath(root: Path, configured: String): Path {
        val expanded = expandHome(configured.trim())
        return if (expanded.isAbsolute()) expanded.normalize() else root.resolve(expanded).normalize()
    }

    private fun expandHome(value: String): Path {
        if (value == "~") return Path.of(System.getProperty("user.home"))
        if (value.startsWith("~/") || value.startsWith("~\\")) {
            return Path.of(System.getProperty("user.home"), value.substring(2))
        }
        return Path.of(value)
    }
}
