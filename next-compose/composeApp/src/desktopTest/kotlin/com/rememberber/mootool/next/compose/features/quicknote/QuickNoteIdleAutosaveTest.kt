package com.rememberber.mootool.next.compose.features.quicknote

import com.rememberber.mootool.next.compose.app.AppContainer
import com.rememberber.mootool.next.compose.app.AppPaths
import com.rememberber.mootool.next.compose.sessions.QuickNoteSession
import com.rememberber.mootool.next.compose.storage.AppDatabase
import com.rememberber.mootool.next.compose.storage.HistoryRepository
import com.rememberber.mootool.next.compose.storage.LegacyMigrationRowRepository
import com.rememberber.mootool.next.compose.storage.NoteVault
import com.rememberber.mootool.next.compose.storage.SessionStore
import com.rememberber.mootool.next.compose.storage.SettingsRepository
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class QuickNoteIdleAutosaveTest {
    @Test
    fun idleAutosaveConflictWritesError() {
        val productRoot = createTempDirectory("mootool-qn-idle-autosave-")
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
        val vault = NoteVault(directories)
        val note = vault.createNote(title = "A", parentPath = "")
        val session = QuickNoteSession().apply {
            currentFile = note.relativePath
            savedText = note.content
            editor.setText("dirty", recordUndo = false)
        }
        vault.write(note.relativePath, "disk")
        assertFalse(quickNoteIdleAutosaveAttempt(container, session, vault, monitor = null) { })
        assertTrue(session.error.isNotBlank())
    }
}
