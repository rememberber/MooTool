package com.rememberber.mootool.next.compose.features.json

import com.rememberber.mootool.next.compose.app.AppContainer
import com.rememberber.mootool.next.compose.app.AppPaths
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

class JsonVaultSaveSelectionTest {
    @Test
    fun saveFromUserActionOpensNewFileDialogWhenNoVaultFile() {
        val productRoot = createTempDirectory("mootool-json-save-selection-")
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
        val session = JsonSession().apply {
            vaultSelectedPath = "folder"
            editor.setText("{}", recordUndo = false)
        }
        val items = listOf(VaultEntry("folder", "folder", true, 0))
        assertTrue(jsonVaultSaveFromUserAction(container, session, items, monitor = null) { })
        assertEquals("json-file", session.dialogInputMode)
        assertTrue(session.dialogInput.endsWith("snippet.json"))
    }

    @Test
    fun saveJsonVaultRejectsBlankCurrentFile() {
        val productRoot = createTempDirectory("mootool-json-save-blank-")
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
        val session = JsonSession().apply { editor.setText("{}", recordUndo = false) }
        assertFalse(saveJsonVault(container, session, monitor = null) { }.isSuccess)
    }
}
