package com.rememberber.mootool.next.compose.storage

import com.rememberber.mootool.next.compose.app.AppPaths
import com.rememberber.mootool.next.compose.model.AppSettings
import kotlin.io.path.createTempDirectory
import kotlin.io.path.readText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SettingsDataDirectorySanitizeTest {
    @Test
    fun loadClearsRelativeDataDirectory() {
        val root = createTempDirectory("mootool-settings-data-dir-")
        val directories = AppPaths.resolve(root.toString()).also { it.ensureCreated() }
        SettingsRepository(directories).save(
            AppSettings.Default.copy(data = AppSettings.Default.data.copy(directory = "relative/data")),
        )
        val loaded = SettingsRepository(directories).load()
        assertEquals("", loaded.data.directory)
        assertTrue(directories.settingsFile.readText().contains("\"directory\": \"\""))
        root.toFile().deleteRecursively()
    }
}
