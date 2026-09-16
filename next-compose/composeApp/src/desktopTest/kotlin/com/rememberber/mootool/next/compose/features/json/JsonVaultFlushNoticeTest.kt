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
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class JsonVaultFlushNoticeTest {
    @Test
    fun flushOrNoticeSucceedsWhenVaultClean() {
        val productRoot = createTempDirectory("mootool-json-flush-notice-")
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
            editor.setText("{}", recordUndo = false)
        }
        assertTrue(jsonVaultFlushDirtyOrNotice(container, session, monitor = null) { })
        assertTrue(session.notice.isBlank())
    }

    @Test
    fun flushOrNoticeFailsWithNoticeOnConflict() {
        val productRoot = createTempDirectory("mootool-json-flush-conflict-")
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
        assertFalse(jsonVaultFlushDirtyOrNotice(container, session, monitor = null) { })
        assertTrue(session.notice.isNotBlank())
    }

    @Test
    fun requireFlushThrowsWhenConflict() {
        val productRoot = createTempDirectory("mootool-json-require-flush-")
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
        val thrown = runCatching {
            jsonVaultRequireFlushDirty(container, session, monitor = null) { }
        }.exceptionOrNull()
        assertTrue(thrown is IllegalStateException)
        assertTrue(session.notice.isNotBlank())
    }

    @Test
    fun duplicateSnippetFailsWithNoticeOnConflict() {
        val productRoot = createTempDirectory("mootool-json-dup-flush-")
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
        val result = duplicateJsonVaultSnippet(container, session, monitor = null, "a.json") { }
        assertTrue(result.isFailure)
        assertTrue(session.notice.isNotBlank())
    }
}
