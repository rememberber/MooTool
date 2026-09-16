package com.rememberber.mootool.next.compose.storage

import com.rememberber.mootool.next.compose.app.AppPaths
import com.rememberber.mootool.next.compose.model.AppSettings
import com.rememberber.mootool.next.compose.model.ToolId
import kotlin.io.path.readText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SettingsVaultPathSanitizeTest {
    @Test
    fun loadClearsRelativeVaultPaths() {
        val root = kotlin.io.path.createTempDirectory("mootool-settings-vault-")
        val directories = AppPaths.resolve(root.toString()).also { it.ensureCreated() }
        val repository = SettingsRepository(directories)
        repository.save(
            AppSettings.Default.copy(
                vault = AppSettings.Default.vault.copy(
                    quickNotePath = "relative/quick",
                    jsonPath = "./json"
                ),
                tools = AppSettings.Default.tools.copy(exportDirectory = "exports/out")
            )
        )
        val loaded = SettingsRepository(directories).load()
        assertEquals("", loaded.vault.quickNotePath)
        assertEquals("", loaded.vault.jsonPath)
        assertEquals("", loaded.tools.exportDirectory)
        assertTrue(SettingsRepository(directories).load().vault.quickNotePath.isEmpty())
        assertTrue(directories.settingsFile.readText().contains("\"quickNotePath\": \"\""))
        root.toFile().deleteRecursively()
    }

    @Test
    fun loadNormalizesHiddenNavigationToolIds() {
        val root = kotlin.io.path.createTempDirectory("mootool-settings-nav-")
        val directories = AppPaths.resolve(root.toString()).also { it.ensureCreated() }
        val repository = SettingsRepository(directories)
        repository.save(
            AppSettings.Default.copy(
                layout = AppSettings.Default.layout.copy(
                    hiddenNavigationToolIds = listOf("json", "json", ToolId.Mootool.id, "bogus", "qrCode")
                )
            )
        )
        val loaded = SettingsRepository(directories).load()
        assertEquals(listOf("json", "qrCode"), loaded.layout.hiddenNavigationToolIds)
        assertTrue(directories.settingsFile.readText().contains("\"hiddenNavigationToolIds\""))
        root.toFile().deleteRecursively()
    }

    @Test
    fun saveNormalizesHiddenNavigationToolIds() {
        val root = kotlin.io.path.createTempDirectory("mootool-settings-nav-save-")
        val directories = AppPaths.resolve(root.toString()).also { it.ensureCreated() }
        val repository = SettingsRepository(directories)
        repository.save(
            AppSettings.Default.copy(
                layout = AppSettings.Default.layout.copy(
                    hiddenNavigationToolIds = listOf("json", "json", ToolId.Mootool.id, "bogus")
                )
            )
        )
        assertEquals(listOf("json"), repository.current.layout.hiddenNavigationToolIds)
        root.toFile().deleteRecursively()
    }
}
