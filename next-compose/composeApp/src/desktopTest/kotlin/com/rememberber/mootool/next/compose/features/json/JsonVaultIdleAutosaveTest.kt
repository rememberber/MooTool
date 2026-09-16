package com.rememberber.mootool.next.compose.features.json

import com.rememberber.mootool.next.compose.app.AppContainer
import com.rememberber.mootool.next.compose.app.AppPaths
import com.rememberber.mootool.next.compose.sessions.JsonSession
import com.rememberber.mootool.next.compose.storage.AppDatabase
import com.rememberber.mootool.next.compose.storage.HistoryRepository
import com.rememberber.mootool.next.compose.storage.LegacyMigrationRowRepository
import com.rememberber.mootool.next.compose.storage.SessionStore
import com.rememberber.mootool.next.compose.storage.SettingsRepository
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class JsonVaultIdleAutosaveTest {
    @Test
    fun idleAutosaveConflictWritesNotice() {
        val productRoot = createTempDirectory("mootool-json-idle-autosave-")
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
        container.jsonVault.createFile("a.json", """{"saved":true}""")
        val session = JsonSession().apply {
            currentFile = "a.json"
            savedText = """{"saved":true}"""
            editor.setText("""{"dirty":true}""", recordUndo = false)
        }
        container.jsonVault.write("a.json", """{"disk":true}""")
        assertFalse(jsonVaultIdleAutosaveAttempt(container, session, monitor = null) { })
        assertTrue(session.notice.isNotBlank())
    }

    @Test
    fun idleAutosaveSucceedsWhenCleanConflictCheckPasses() {
        val productRoot = createTempDirectory("mootool-json-idle-autosave-ok-")
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
        container.jsonVault.createFile("a.json", "{}")
        val session = JsonSession().apply {
            currentFile = "a.json"
            savedText = "{}"
            editor.setText("""{"a":1}""", recordUndo = false)
        }
        assertTrue(jsonVaultIdleAutosaveAttempt(container, session, monitor = null) { })
        assertEquals("""{"a":1}""", session.savedText)
    }
}
