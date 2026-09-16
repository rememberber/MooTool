package com.rememberber.mootool.next.compose.storage

import com.rememberber.mootool.next.compose.app.AppPaths
import com.rememberber.mootool.next.compose.model.AppSettings
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BackupInfoTest {
    @Test
    fun resolvesEffectiveDataAndVaultRoots() {
        val bootstrap = AppPaths.resolve(createTempDirectory("backup-info-").toString()).also { it.ensureCreated() }
        val customData = createTempDirectory("custom-data-")
        val jsonVault = createTempDirectory("json-vault-")
        val quickVault = createTempDirectory("quick-vault-")
        val settings = AppSettings.Default.copy(
            data = AppSettings.Default.data.copy(directory = customData.toString()),
            vault = AppSettings.Default.vault.copy(
                jsonPath = jsonVault.toString(),
                quickNotePath = quickVault.toString(),
            ),
        )
        val dirs = DataPathConfig.withEffectiveDataRoot(bootstrap, settings.data.directory)
        val info = BackupInfoResolver.resolve(dirs, quickVault, jsonVault)
        assertEquals(customData.toAbsolutePath().normalize().toString(), info.dataDirectory)
        assertTrue(info.databasePath.contains("mootool-compose.sqlite"))
        assertEquals(jsonVault.toAbsolutePath().normalize().toString(), info.jsonVaultPath)
        assertEquals(quickVault.toAbsolutePath().normalize().toString(), info.quickNotePath)
        assertEquals(dirs.databaseFile.toAbsolutePath().normalize(), info.pathForOpen(BackupOpenLocation.DatabaseFile))
        assertEquals(dirs.settingsFile.parent.toAbsolutePath().normalize(), info.pathForOpen(BackupOpenLocation.SettingsConfig))
        assertEquals(jsonVault.toAbsolutePath().normalize(), info.pathForOpen(BackupOpenLocation.JsonVault))
    }
}
