package com.rememberber.mootool.next.compose.storage

import com.rememberber.mootool.next.compose.app.AppDirectories
import java.nio.file.Path

/** Folder or file revealed from settings backup rows (Compose splits config vs data roots). */
enum class BackupOpenLocation {
    /** SQLite under effective `dataRoot`. */
    DatabaseFile,
    /** `config/` next to bootstrap settings. */
    SettingsConfig,
    Images,
    QuickNote,
    JsonVault,
}

/** Resolved backup sources (Electron `BackupInfo`; zip export uses the same roots). */
data class BackupInfo(
    val dataDirectory: String,
    val databasePath: String,
    val settingsPath: String,
    val imagesPath: String,
    val quickNotePath: String,
    val jsonVaultPath: String,
) {
    fun pathForOpen(location: BackupOpenLocation): Path =
        when (location) {
            BackupOpenLocation.DatabaseFile -> Path.of(databasePath)
            BackupOpenLocation.SettingsConfig -> Path.of(settingsPath).parent
            BackupOpenLocation.Images -> Path.of(imagesPath)
            BackupOpenLocation.QuickNote -> Path.of(quickNotePath)
            BackupOpenLocation.JsonVault -> Path.of(jsonVaultPath)
        }
}

object BackupInfoResolver {
    fun resolve(
        directories: AppDirectories,
        quickNoteVaultRoot: Path,
        jsonVaultRoot: Path,
    ): BackupInfo =
        BackupInfo(
            dataDirectory = directories.dataRoot.toAbsolutePath().normalize().toString(),
            databasePath = directories.databaseFile.toAbsolutePath().normalize().toString(),
            settingsPath = directories.settingsFile.toAbsolutePath().normalize().toString(),
            imagesPath = directories.dataRoot.resolve("images").toAbsolutePath().normalize().toString(),
            quickNotePath = quickNoteVaultRoot.toAbsolutePath().normalize().toString(),
            jsonVaultPath = jsonVaultRoot.toAbsolutePath().normalize().toString(),
        )
}
