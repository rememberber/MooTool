package com.rememberber.mootool.next.compose.features.json

import com.rememberber.mootool.next.compose.app.AppContainer
import com.rememberber.mootool.next.compose.app.AppPaths
import com.rememberber.mootool.next.compose.sessions.JsonSession
import com.rememberber.mootool.next.compose.storage.AppDatabase
import com.rememberber.mootool.next.compose.storage.HistoryRepository
import com.rememberber.mootool.next.compose.storage.LegacyMigrationRowRepository
import com.rememberber.mootool.next.compose.storage.SessionStore
import com.rememberber.mootool.next.compose.storage.SettingsRepository
import javax.swing.SwingUtilities
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class JsonVaultReloadOpenFileTest {
    private fun container(): AppContainer {
        val productRoot = createTempDirectory("mootool-json-reload-")
        val directories = AppPaths.resolve(productRoot.toString()).also { it.ensureCreated() }
        val settings = SettingsRepository(directories).also { it.load() }
        val database = AppDatabase(directories)
        return AppContainer(
            directories = directories,
            settingsRepository = settings,
            database = database,
            history = HistoryRepository(database),
            migrationRows = LegacyMigrationRowRepository(database),
            sessions = SessionStore(database),
        )
    }

    @Test
    fun reloadOpenFileWhenCleanAndDiskChanged() {
        val container = container()
        container.jsonVault.createFile("open.json", """{"v":1}""")
        val session = JsonSession().apply {
            currentFile = "open.json"
            vaultSelectedPath = "open.json"
            savedText = """{"v":1}"""
            editor.setText("""{"v":1}""", recordUndo = false)
        }
        container.jsonVault.write("open.json", """{"v":2}""")
        SwingUtilities.invokeAndWait {
            assertEquals(JsonVaultReloadOpenResult.Reloaded, jsonVaultReloadOpenFileIfClean(container, session))
            assertEquals("""{"v":2}""", session.editor.text)
            assertEquals("""{"v":2}""", session.savedText)
        }
    }

    @Test
    fun skipReloadWhenDirty() {
        val container = container()
        container.jsonVault.createFile("open.json", "{}")
        val session = JsonSession().apply {
            currentFile = "open.json"
            savedText = "{}"
            editor.setText("""{"dirty":true}""", recordUndo = false)
        }
        container.jsonVault.write("open.json", """{"disk":true}""")
        assertEquals(JsonVaultReloadOpenResult.Unchanged, jsonVaultReloadOpenFileIfClean(container, session))
        assertEquals("""{"dirty":true}""", session.editor.text)
    }

    @Test
    fun clearSessionWhenOpenFileMissingOnDisk() {
        val container = container()
        container.jsonVault.createFile("open.json", "{}")
        val session = JsonSession().apply {
            currentFile = "open.json"
            vaultSelectedPath = "open.json"
            savedText = "{}"
            editor.setText("{}", recordUndo = false)
        }
        container.jsonVault.delete("open.json")
        SwingUtilities.invokeAndWait {
            assertEquals(JsonVaultReloadOpenResult.ClearedMissing, jsonVaultReloadOpenFileIfClean(container, session))
            assertTrue(session.currentFile.isBlank())
            assertTrue(session.editor.text.isBlank())
        }
    }
}
