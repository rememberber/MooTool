package com.rememberber.mootool.next.compose.features.quicknote

import com.rememberber.mootool.next.compose.app.AppContainer
import com.rememberber.mootool.next.compose.app.AppPaths
import com.rememberber.mootool.next.compose.domain.VaultConflictState
import com.rememberber.mootool.next.compose.features.vault.applyQuickNoteVaultConflictKeep
import com.rememberber.mootool.next.compose.features.vault.applyQuickNoteVaultConflictReload
import com.rememberber.mootool.next.compose.sessions.QuickNoteSession
import com.rememberber.mootool.next.compose.storage.AppDatabase
import com.rememberber.mootool.next.compose.storage.HistoryRepository
import com.rememberber.mootool.next.compose.storage.LegacyMigrationRowRepository
import com.rememberber.mootool.next.compose.storage.SessionStore
import com.rememberber.mootool.next.compose.storage.SettingsRepository
import javax.swing.SwingUtilities
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** 对齐 [QuickNoteScreen] `VaultConflictDialog` `onReload` / `onKeep`。 */
class QuickNoteVaultConflictReloadKeepFlowTest {
    @Test
    fun reloadReplacesEditorWithDiskAndClearsConflict() {
        val fixture = openFixture()
        val vault = fixture.vault
        val session = fixture.session
        try {
            val local = "local body\n"
            val diskFile = "---\ntitle: T\n---\n\ndisk body\n"
            vault.write("note.md", diskFile)
            val diskBody = vault.readNote("note.md").content
            SwingUtilities.invokeAndWait { session.editor.setText(local, recordUndo = false) }
            val pending = VaultConflictState("note.md", local, diskBody, deleted = false)
            session.vaultConflict = pending

            SwingUtilities.invokeAndWait { applyQuickNoteVaultConflictReload(fixture.container, session, vault, pending) }
            SwingUtilities.invokeAndWait { }

            assertNull(session.vaultConflict)
            SwingUtilities.invokeAndWait { }
            assertEquals(diskBody, session.editor.text)
            assertEquals(diskBody, session.savedText)
            assertEquals("note.md", session.currentFile)
        } finally {
            fixture.root.toFile().deleteRecursively()
        }
    }

    @Test
    fun reloadWhenDeletedClearsCurrentFileAndKeepsEditorText() {
        val fixture = openFixture()
        val session = fixture.session
        try {
            val local = "edited locally\n"
            SwingUtilities.invokeAndWait { session.editor.setText(local, recordUndo = false) }
            val pending = VaultConflictState("note.md", local, diskText = null, deleted = true)
            session.vaultConflict = pending

            SwingUtilities.invokeAndWait {
                applyQuickNoteVaultConflictReload(fixture.container, session, fixture.vault, pending)
            }

            assertNull(session.vaultConflict)
            assertEquals("", session.currentFile)
            assertEquals(local, session.editor.text)
            assertEquals(local, session.savedText)
        } finally {
            fixture.root.toFile().deleteRecursively()
        }
    }

    @Test
    fun keepDismissesConflictWithoutChangingEditor() {
        val fixture = openFixture()
        val session = fixture.session
        try {
            val local = "keep this\n"
            SwingUtilities.invokeAndWait { session.editor.setText(local, recordUndo = false) }
            session.vaultConflict = VaultConflictState("note.md", local, "disk\n", deleted = false)

            applyQuickNoteVaultConflictKeep(session)

            assertNull(session.vaultConflict)
            assertEquals(local, session.editor.text)
            assertEquals("note.md", session.currentFile)
        } finally {
            fixture.root.toFile().deleteRecursively()
        }
    }

    private data class Fixture(
        val container: AppContainer,
        val session: QuickNoteSession,
        val vault: com.rememberber.mootool.next.compose.storage.NoteVault,
        val root: java.nio.file.Path,
    )

    private fun openFixture(): Fixture {
        val productRoot = createTempDirectory("mootool-qn-conflict-reload-")
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
        val body = "initial\n"
        vault.write("note.md", "---\ntitle: T\n---\n\n$body")
        val session = QuickNoteSession().apply {
            currentFile = "note.md"
            savedText = body
            SwingUtilities.invokeAndWait { editor.setText(body, recordUndo = false) }
        }
        return Fixture(container, session, vault, productRoot)
    }
}
