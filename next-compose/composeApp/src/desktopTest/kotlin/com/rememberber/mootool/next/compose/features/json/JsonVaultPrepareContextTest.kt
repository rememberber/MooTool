package com.rememberber.mootool.next.compose.features.json

import com.rememberber.mootool.next.compose.app.AppContainer
import com.rememberber.mootool.next.compose.app.AppPaths
import com.rememberber.mootool.next.compose.domain.VaultConflictState
import com.rememberber.mootool.next.compose.sessions.JsonSession
import com.rememberber.mootool.next.compose.storage.AppDatabase
import com.rememberber.mootool.next.compose.storage.HistoryRepository
import com.rememberber.mootool.next.compose.storage.LegacyMigrationRowRepository
import com.rememberber.mootool.next.compose.storage.SessionStore
import com.rememberber.mootool.next.compose.storage.SettingsRepository
import com.rememberber.mootool.next.compose.storage.VaultEntry
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class JsonVaultPrepareContextTest {
    @Test
    fun prepareContextSaveConflictWritesNoticeAndReturnsFalse() {
        val productRoot = createTempDirectory("mootool-json-prepare-")
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
        container.jsonVault.createFile("open.json", """{"saved":true}""")
        container.jsonVault.createFile("other.json", "{}")
        val session = JsonSession().apply {
            currentFile = "open.json"
            savedText = """{"saved":true}"""
            editor.setText("""{"dirty":true}""", recordUndo = false)
        }
        container.jsonVault.write("open.json", """{"disk":true}""")
        var conflict: VaultConflictState? = null
        val entry = VaultEntry("other.json", "other.json", false, 0)
        val ok = prepareJsonVaultContext(container, session, monitor = null, entry) { conflict = it }
        assertFalse(ok)
        assertTrue(session.notice.isNotBlank())
        assertTrue(conflict != null)
        assertEquals("open.json", session.vaultSelectedPath)
    }

    @Test
    fun prepareContextSaveConflictRevertsTreeToOpenFileNotPendingHighlight() {
        val productRoot = createTempDirectory("mootool-json-prepare-highlight-")
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
        container.jsonVault.createFile("open.json", """{"saved":true}""")
        container.jsonVault.createFile("other.json", "{}")
        val session = JsonSession().apply {
            currentFile = "open.json"
            vaultSelectedPath = "other.json"
            savedText = """{"saved":true}"""
            editor.setText("""{"dirty":true}""", recordUndo = false)
        }
        container.jsonVault.write("open.json", """{"disk":true}""")
        val entry = VaultEntry("other.json", "other.json", false, 0)
        assertFalse(prepareJsonVaultContext(container, session, monitor = null, entry) { })
        assertEquals("open.json", session.vaultSelectedPath)
        assertEquals("open.json", session.currentFile)
    }

    @Test
    fun prepareContextDirectoryEntryFlushesDirtyOpenFile() {
        val productRoot = createTempDirectory("mootool-json-prepare-dir-")
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
        container.jsonVault.createFile("open.json", "{}")
        container.jsonVault.createDirectory("folder")
        val session = JsonSession().apply {
            currentFile = "open.json"
            savedText = "{}"
            editor.setText("""{"dirty":true}""", recordUndo = false)
        }
        val entry = VaultEntry("folder", "folder", true, 0)
        assertTrue(prepareJsonVaultContext(container, session, monitor = null, entry) { })
        assertEquals("""{"dirty":true}""", session.savedText)
        assertEquals("folder", session.vaultSelectedPath)
    }

    @Test
    fun prepareContextDirectoryFlushConflictKeepsTreeSelection() {
        val productRoot = createTempDirectory("mootool-json-prepare-dir-conflict-")
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
        container.jsonVault.createFile("open.json", """{"saved":true}""")
        container.jsonVault.createDirectory("folder")
        val session = JsonSession().apply {
            currentFile = "open.json"
            vaultSelectedPath = "open.json"
            savedText = """{"saved":true}"""
            editor.setText("""{"dirty":true}""", recordUndo = false)
        }
        container.jsonVault.write("open.json", """{"disk":true}""")
        val entry = VaultEntry("folder", "folder", true, 0)
        assertFalse(prepareJsonVaultContext(container, session, monitor = null, entry) { })
        assertEquals("open.json", session.vaultSelectedPath)
        assertTrue(session.notice.isNotBlank())
    }

    @Test
    fun prepareContextDirectoryFlushConflictRevertsHighlightToOpenFile() {
        val productRoot = createTempDirectory("mootool-json-prepare-dir-highlight-")
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
        container.jsonVault.createFile("open.json", """{"saved":true}""")
        container.jsonVault.createFile("other.json", "{}")
        container.jsonVault.createDirectory("folder")
        val session = JsonSession().apply {
            currentFile = "open.json"
            vaultSelectedPath = "other.json"
            savedText = """{"saved":true}"""
            editor.setText("""{"dirty":true}""", recordUndo = false)
        }
        container.jsonVault.write("open.json", """{"disk":true}""")
        val entry = VaultEntry("folder", "folder", true, 0)
        assertFalse(prepareJsonVaultContext(container, session, monitor = null, entry) { })
        assertEquals("open.json", session.vaultSelectedPath)
        assertEquals("open.json", session.currentFile)
    }
}
