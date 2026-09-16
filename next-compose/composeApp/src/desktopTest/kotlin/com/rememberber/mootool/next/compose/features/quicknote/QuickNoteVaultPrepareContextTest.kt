package com.rememberber.mootool.next.compose.features.quicknote

import com.rememberber.mootool.next.compose.app.AppContainer
import com.rememberber.mootool.next.compose.app.AppPaths
import com.rememberber.mootool.next.compose.domain.NoteFrontmatter
import com.rememberber.mootool.next.compose.domain.VaultConflictState
import com.rememberber.mootool.next.compose.sessions.QuickNoteSession
import com.rememberber.mootool.next.compose.storage.AppDatabase
import com.rememberber.mootool.next.compose.storage.HistoryRepository
import com.rememberber.mootool.next.compose.storage.LegacyMigrationRowRepository
import com.rememberber.mootool.next.compose.storage.NoteVault
import com.rememberber.mootool.next.compose.storage.SessionStore
import com.rememberber.mootool.next.compose.storage.SettingsRepository
import com.rememberber.mootool.next.compose.storage.VaultEntry
import javax.swing.SwingUtilities
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class QuickNoteVaultPrepareContextTest {
    @Test
    fun prepareContextSaveConflictWritesErrorAndReturnsFalse() {
        val productRoot = createTempDirectory("mootool-qn-prepare-")
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
        val open = vault.createNote("Open", parentPath = "")
        vault.saveNote(open.relativePath, "saved-body", open.metadata)
        val other = vault.createNote("Other", parentPath = "")
        val session = QuickNoteSession().apply {
            currentFile = open.relativePath
            savedText = "saved-body"
            editor.setText("dirty-body", recordUndo = false)
            metadata = open.metadata
            savedMetadata = open.metadata
        }
        val onDisk = vault.readNote(open.relativePath)
        java.nio.file.Files.writeString(
            vault.resolve(open.relativePath),
            NoteFrontmatter.serialize(onDisk.metadata, "disk-body"),
            java.nio.charset.StandardCharsets.UTF_8,
        )
        var conflict: VaultConflictState? = null
        val entry = VaultEntry(other.relativePath, other.relativePath.substringAfterLast('/'), false, 0)
        val ok = prepareQuickNoteVaultContext(container, session, vault, monitor = null, entry) { conflict = it }
        assertFalse(ok)
        assertTrue(session.error.isNotBlank())
        assertTrue(conflict != null)
        assertEquals(open.relativePath, session.vaultSelectedPath)
    }

    @Test
    fun prepareContextSaveConflictRevertsTreeToOpenFileNotPendingHighlight() {
        val productRoot = createTempDirectory("mootool-qn-prepare-highlight-")
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
        val open = vault.createNote("Open", parentPath = "")
        vault.saveNote(open.relativePath, "saved-body", open.metadata)
        val other = vault.createNote("Other", parentPath = "")
        val session = QuickNoteSession().apply {
            currentFile = open.relativePath
            vaultSelectedPath = other.relativePath
            savedText = "saved-body"
            editor.setText("dirty-body", recordUndo = false)
            metadata = open.metadata
            savedMetadata = open.metadata
        }
        val onDisk = vault.readNote(open.relativePath)
        java.nio.file.Files.writeString(
            vault.resolve(open.relativePath),
            NoteFrontmatter.serialize(onDisk.metadata, "disk-body"),
            java.nio.charset.StandardCharsets.UTF_8,
        )
        val entry = VaultEntry(other.relativePath, other.relativePath.substringAfterLast('/'), false, 0)
        assertFalse(prepareQuickNoteVaultContext(container, session, vault, monitor = null, entry) { })
        assertEquals(open.relativePath, session.vaultSelectedPath)
        assertEquals(open.relativePath, session.currentFile)
    }

    @Test
    fun prepareContextDirectorySaveConflictKeepsTreeSelection() {
        val productRoot = createTempDirectory("mootool-qn-prepare-dir-")
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
        val open = vault.createNote("Open", parentPath = "")
        vault.saveNote(open.relativePath, "saved-body", open.metadata)
        vault.createDirectory("folder")
        val session = QuickNoteSession().apply {
            currentFile = open.relativePath
            vaultSelectedPath = open.relativePath
            savedText = "saved-body"
            editor.setText("dirty-body", recordUndo = false)
            metadata = open.metadata
            savedMetadata = open.metadata
        }
        val onDisk = vault.readNote(open.relativePath)
        java.nio.file.Files.writeString(
            vault.resolve(open.relativePath),
            NoteFrontmatter.serialize(onDisk.metadata, "disk-body"),
            java.nio.charset.StandardCharsets.UTF_8,
        )
        val entry = VaultEntry("folder", "folder", true, 0)
        assertFalse(prepareQuickNoteVaultContext(container, session, vault, monitor = null, entry) { })
        assertEquals(open.relativePath, session.vaultSelectedPath)
        assertEquals(open.relativePath, session.currentFile)
        assertTrue(session.error.isNotBlank())
    }

    @Test
    fun prepareContextDirectorySelectClearsEditorWithUntitledMetadata() {
        val productRoot = createTempDirectory("mootool-qn-prepare-dir-clear-")
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
        val open = vault.createNote("Open", parentPath = "")
        vault.saveNote(open.relativePath, "body", open.metadata)
        vault.createDirectory("folder")
        val session = QuickNoteSession().apply {
            currentFile = open.relativePath
            savedText = "body"
            editor.setText("body", recordUndo = false)
            metadata = open.metadata
            savedMetadata = open.metadata
        }
        val entry = VaultEntry("folder", "folder", true, 0)
        SwingUtilities.invokeAndWait {
            assertTrue(prepareQuickNoteVaultContext(container, session, vault, monitor = null, entry) { })
            assertEquals("folder", session.vaultSelectedPath)
            assertTrue(session.currentFile.isBlank())
            assertTrue(session.editor.text.isBlank())
            assertEquals("Untitled", session.metadata.title)
        }
    }
}
