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
import javax.swing.SwingUtilities
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class QuickNoteVaultExternalChangeTest {
    private fun fixture(): Pair<AppContainer, NoteVault> {
        val productRoot = createTempDirectory("mootool-qn-ext-change-")
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
        return container to NoteVault(directories)
    }

    @Test
    fun cleanOpenNoteReloadsWhenChangeNotInPathsList() {
        val (container, vault) = fixture()
        val a = vault.createNote("A", parentPath = "")
        vault.saveNote(a.relativePath, "body-v1", a.metadata)
        vault.createNote("B", parentPath = "")
        val session = QuickNoteSession().apply {
            currentFile = a.relativePath
            vaultSelectedPath = a.relativePath
            savedText = "body-v1"
            editor.setText("body-v1", recordUndo = false)
            metadata = a.metadata
            savedMetadata = a.metadata
        }
        vault.saveNote(a.relativePath, "body-v2", a.metadata)
        SwingUtilities.invokeAndWait {
            assertTrue(
                quickNoteApplyExternalChange(container, session, vault, listOf("other.md")) { },
            )
            assertEquals("body-v2", session.editor.text)
            assertEquals("body-v2", session.savedText)
            assertTrue(session.notice.isNotBlank())
        }
    }

    @Test
    fun cleanOpenNoteClearsWhenDeletedRegardlessOfPathsList() {
        val (container, vault) = fixture()
        val doc = vault.createNote("Gone", parentPath = "")
        vault.saveNote(doc.relativePath, "body", doc.metadata)
        val session = QuickNoteSession().apply {
            currentFile = doc.relativePath
            vaultSelectedPath = doc.relativePath
            savedText = "body"
            editor.setText("body", recordUndo = false)
            metadata = doc.metadata
            savedMetadata = doc.metadata
        }
        vault.delete(doc.relativePath)
        SwingUtilities.invokeAndWait {
            assertTrue(
                quickNoteApplyExternalChange(container, session, vault, listOf("unrelated.md")) { },
            )
            assertTrue(session.currentFile.isBlank())
            assertTrue(session.vaultSelectedPath.isBlank())
            assertTrue(session.editor.text.isBlank())
            assertTrue(session.notice.isNotBlank())
        }
    }
}
