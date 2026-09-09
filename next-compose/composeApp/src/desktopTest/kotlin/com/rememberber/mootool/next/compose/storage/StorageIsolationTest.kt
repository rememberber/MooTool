package com.rememberber.mootool.next.compose.storage

import com.rememberber.mootool.next.compose.app.AppPaths
import com.rememberber.mootool.next.compose.app.ProductIdentity
import com.rememberber.mootool.next.compose.model.AppSettings
import kotlin.io.path.createTempDirectory
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class StorageIsolationTest {
    @Test
    fun overrideRootKeepsProductNamespaceAndHistoryLimit() {
        val root = createTempDirectory("mootool-compose-test")
        val directories = AppPaths.resolve(root.toString())
        directories.ensureCreated()
        val settings = SettingsRepository(directories)
        settings.load()
        settings.save(AppSettings.Default.copy(general = AppSettings.Default.general.copy(language = "en-US")))
        assertTrue(directories.productMarker.exists())
        assertTrue(directories.productMarker.readText().contains(ProductIdentity.PRODUCT_ID))
        assertTrue(directories.settingsFile.readText().contains("en-US"))
        AppDatabase(directories).use { database ->
            val history = HistoryRepository(database, limit = 3)
            repeat(5) { index ->
                history.save("json", "format", "item-$index", "{\"n\":$index}", "{\"n\":$index}")
            }
            assertEquals(3, history.list("json").size)
        }
        assertTrue(directories.dataRoot.toString().contains("data"))
        assertTrue(!directories.dataRoot.toString().contains("MooTool Next Electron"))
    }

    @Test
    fun vaultRejectsPathEscape() {
        val root = createTempDirectory("mootool-compose-vault")
        val vault = JsonVault(AppPaths.resolve(root.toString()).also { it.ensureCreated() })
        val escaped = runCatching { vault.resolve("../secret.json") }
        assertTrue(escaped.isFailure)
    }
}
