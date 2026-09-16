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

class JsonVaultExternalChangeTest {
    @Test
    fun cleanOpenFileReloadsWhenChangeNotInPathsList() {
        val productRoot = createTempDirectory("mootool-json-ext-change-")
        val directories = AppPaths.resolve(productRoot.toString()).also { it.ensureCreated() }
        val settings = SettingsRepository(directories).also { it.load() }
        val database = AppDatabase(directories)
        val container = AppContainer(
            directories = directories,
            settingsRepository = settings,
            database = database,
            history = HistoryRepository(database),
            migrationRows = LegacyMigrationRowRepository(database),
            sessions = SessionStore(database),
        )
        container.jsonVault.createFile("open.json", """{"v":1}""")
        container.jsonVault.createFile("other.json", "{}")
        val session = JsonSession().apply {
            currentFile = "open.json"
            savedText = """{"v":1}"""
            editor.setText("""{"v":1}""", recordUndo = false)
        }
        container.jsonVault.write("open.json", """{"v":2}""")
        SwingUtilities.invokeAndWait {
            assertTrue(
                jsonVaultApplyExternalChange(container, session, listOf("other.json")) { },
            )
            assertEquals("""{"v":2}""", session.editor.text)
            assertTrue(session.notice.isNotBlank())
        }
    }

    @Test
    fun cleanOpenFileClearsWhenDeletedRegardlessOfPathsList() {
        val productRoot = createTempDirectory("mootool-json-ext-delete-")
        val directories = AppPaths.resolve(productRoot.toString()).also { it.ensureCreated() }
        val settings = SettingsRepository(directories).also { it.load() }
        val database = AppDatabase(directories)
        val container = AppContainer(
            directories = directories,
            settingsRepository = settings,
            database = database,
            history = HistoryRepository(database),
            migrationRows = LegacyMigrationRowRepository(database),
            sessions = SessionStore(database),
        )
        container.jsonVault.createFile("gone.json", """{"x":1}""")
        val session = JsonSession().apply {
            currentFile = "gone.json"
            vaultSelectedPath = "gone.json"
            savedText = """{"x":1}"""
            editor.setText("""{"x":1}""", recordUndo = false)
        }
        container.jsonVault.delete("gone.json")
        SwingUtilities.invokeAndWait {
            assertTrue(
                jsonVaultApplyExternalChange(container, session, listOf("other.json")) { },
            )
            assertTrue(session.currentFile.isBlank())
            assertTrue(session.vaultSelectedPath.isBlank())
            assertTrue(session.editor.text.isBlank())
            assertTrue(session.notice.isNotBlank())
        }
    }
}
