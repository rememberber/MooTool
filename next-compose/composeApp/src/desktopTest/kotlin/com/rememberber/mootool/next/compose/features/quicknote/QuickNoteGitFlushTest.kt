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
import kotlin.test.assertNull

class QuickNoteGitFlushTest {
    @Test
    fun gitFlushSkipsWhenOnlyMetadataMatchesAndBodyClean() {
        val productRoot = createTempDirectory("mootool-qn-git-flush-")
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
        val note = vault.createNote("Clean", parentPath = "")
        vault.saveNote(note.relativePath, "body", note.metadata)
        val session = QuickNoteSession().apply {
            currentFile = note.relativePath
            editor.setText("body", recordUndo = false)
            savedText = "body"
            metadata = note.metadata
            savedMetadata = note.metadata
        }
        assertNull(
            quickNoteGitFlushBeforeAction(container, session, vault, monitor = null) { },
        )
    }

    @Test
    fun gitFlushSkipsWhenBodyAndMetadataUnchanged() {
        val session = QuickNoteSession().apply {
            currentFile = "note.md"
            savedText = editor.text
            savedMetadata = metadata
        }
        val productRoot = createTempDirectory("mootool-qn-git-flush2-")
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
        assertNull(
            quickNoteGitFlushBeforeAction(container, session, NoteVault(directories), monitor = null) { },
        )
    }
}
