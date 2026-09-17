package com.rememberber.mootool.next.compose.features.quicknote

import com.rememberber.mootool.next.compose.app.AppContainer
import com.rememberber.mootool.next.compose.app.AppPaths
import com.rememberber.mootool.next.compose.domain.NoteFrontmatter
import com.rememberber.mootool.next.compose.domain.VaultConflictState
import com.rememberber.mootool.next.compose.features.vault.applyQuickNoteVaultConflictSaveCopy
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
import kotlin.test.assertTrue

/** 对齐 [QuickNoteScreen] `onSaveCopy`：旁路副本 + 编辑器回到磁盘正文。 */
class QuickNoteVaultConflictSaveCopyFlowTest {
    @Test
    fun saveCopyWritesSiblingAndReloadsDiskIntoEditor() {
        val productRoot = createTempDirectory("mootool-qn-conflict-savecopy-")
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
        val path = "note.md"
        val editorBody = "local body\n"
        vault.write(path, "---\ntitle: T\n---\n\ndisk body\n")
        val onDiskBody = vault.readNote(path).content
        val session = QuickNoteSession().apply {
            currentFile = path
            savedText = "saved baseline\n"
            SwingUtilities.invokeAndWait {
                editor.setText(editorBody, recordUndo = false)
            }
        }
        val pending = VaultConflictState(path, editorBody, onDiskBody, deleted = false)
        val fixedTime = 1_700_000_002_000L
        try {
            SwingUtilities.invokeAndWait {
                assertTrue(applyQuickNoteVaultConflictSaveCopy(container, session, vault, pending, null, fixedTime))
            }
            SwingUtilities.invokeAndWait { }
            val copy = "note.local-$fixedTime.md"
            assertEquals(onDiskBody, session.savedText)
            assertEquals(onDiskBody, session.editor.text)
            assertEquals(path, session.currentFile)
            assertEquals(editorBody, NoteFrontmatter.parse(vault.read(copy), "copy").content)
            assertTrue(vault.read(path).contains(onDiskBody))
        } finally {
            productRoot.toFile().deleteRecursively()
        }
    }

    @Test
    fun externalDeleteWhileDirtyYieldsDeletedConflict() {
        val productRoot = createTempDirectory("mootool-qn-conflict-deleted-")
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
        val path = "gone.md"
        vault.write(path, "---\ntitle: G\n---\n\nbody\n")
        val session = QuickNoteSession().apply {
            currentFile = path
            savedText = "body\n"
            SwingUtilities.invokeAndWait {
                editor.setText("edited locally\n", recordUndo = false)
            }
        }
        vault.delete(path)
        var conflict: VaultConflictState? = null
        SwingUtilities.invokeAndWait {
            quickNoteApplyExternalChange(container, session, vault, listOf(path)) { conflict = it }
        }
        val pending = conflict!!
        assertTrue(pending.deleted)
        assertEquals(path, pending.relativePath)
        productRoot.toFile().deleteRecursively()
    }

    @Test
    fun saveCopyWhenExternallyDeletedOpensCopyAsCurrentFile() {
        val productRoot = createTempDirectory("mootool-qn-conflict-savecopy-deleted-")
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
        val path = "gone.md"
        val editorBody = "edited locally\n"
        val fixedTime = 1_700_000_004_000L
        val session = QuickNoteSession().apply {
            currentFile = path
            savedText = "body\n"
            SwingUtilities.invokeAndWait {
                editor.setText(editorBody, recordUndo = false)
            }
        }
        val pending = VaultConflictState(path, editorBody, diskText = null, deleted = true)
        try {
            SwingUtilities.invokeAndWait {
                assertTrue(applyQuickNoteVaultConflictSaveCopy(container, session, vault, pending, null, fixedTime))
            }
            SwingUtilities.invokeAndWait { }
            val copy = "gone.local-$fixedTime.md"
            assertEquals(copy, session.currentFile)
            assertEquals(editorBody, session.savedText)
            assertEquals(editorBody, NoteFrontmatter.parse(vault.read(copy), "copy").content)
            assertNull(session.vaultConflict)
        } finally {
            productRoot.toFile().deleteRecursively()
        }
    }
}
