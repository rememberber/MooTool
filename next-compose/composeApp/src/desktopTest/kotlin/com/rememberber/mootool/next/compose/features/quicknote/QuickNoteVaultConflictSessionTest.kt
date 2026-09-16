package com.rememberber.mootool.next.compose.features.quicknote

import com.rememberber.mootool.next.compose.app.AppContainer
import com.rememberber.mootool.next.compose.app.AppPaths
import com.rememberber.mootool.next.compose.domain.VaultConflictState
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
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class QuickNoteVaultConflictSessionTest {
    @Test
    fun dirtyNoteReportsConflictWhenBodyOnDiskChanges() {
        val fixture = openNote("daily.md", "body on disk\n")
        try {
            SwingUtilities.invokeAndWait {
                fixture.session.editor.setText("body edited locally\n", recordUndo = false)
            }
            fixture.vault.write("daily.md", "---\ntitle: x\n---\n\nremote body\n")
            var conflict: VaultConflictState? = null
            SwingUtilities.invokeAndWait {
                val handled = quickNoteApplyExternalChange(
                    fixture.container,
                    fixture.session,
                    fixture.vault,
                    listOf("daily.md"),
                ) { conflict = it }
                assertFalse(handled)
            }
            val pending = assertNotNull(conflict)
            assertEquals("daily.md", pending.relativePath)
            assertEquals("body edited locally\n", pending.editorText)
            assertTrue(pending.diskText!!.contains("remote"))
        } finally {
            fixture.root.toFile().deleteRecursively()
        }
    }

    private data class Fixture(
        val container: AppContainer,
        val session: QuickNoteSession,
        val vault: NoteVault,
        val root: java.nio.file.Path,
    )

    private fun openNote(path: String, body: String): Fixture {
        val productRoot = createTempDirectory("mootool-note-conflict-session-")
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
        val vault = container.noteVault()
        vault.write(path, "---\ntitle: Test\n---\n\n$body")
        val session = QuickNoteSession().apply {
            currentFile = path
            savedText = body
            editor.setText(body, recordUndo = false)
        }
        return Fixture(container, session, vault, productRoot)
    }
}
