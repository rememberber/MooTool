package com.rememberber.mootool.next.compose.features.quicknote

import com.rememberber.mootool.next.compose.app.AppPaths
import com.rememberber.mootool.next.compose.sessions.QuickNoteSession
import com.rememberber.mootool.next.compose.storage.NoteVault
import com.rememberber.mootool.next.compose.storage.SettingsRepository
import javax.swing.SwingUtilities
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class QuickNoteReloadOpenFileTest {
    @Test
    fun reloadOpenNoteWhenCleanAndDiskChanged() {
        val productRoot = createTempDirectory("mootool-qn-reload-")
        val directories = AppPaths.resolve(productRoot.toString()).also { it.ensureCreated() }
        SettingsRepository(directories).load()
        val vault = NoteVault(directories)
        val doc = vault.createNote("Note", parentPath = "")
        vault.saveNote(doc.relativePath, "body-v1", doc.metadata)
        val session = QuickNoteSession().apply {
            currentFile = doc.relativePath
            vaultSelectedPath = doc.relativePath
            savedText = "body-v1"
            editor.setText("body-v1", recordUndo = false)
            metadata = doc.metadata
            savedMetadata = doc.metadata
        }
        vault.saveNote(doc.relativePath, "body-v2", doc.metadata)
        SwingUtilities.invokeAndWait {
            assertEquals(QuickNoteReloadOpenResult.Reloaded, quickNoteReloadOpenFileIfClean(session, vault))
            assertEquals("body-v2", session.editor.text)
            assertEquals("body-v2", session.savedText)
        }
    }

    @Test
    fun skipReloadWhenDirty() {
        val productRoot = createTempDirectory("mootool-qn-reload-dirty-")
        val directories = AppPaths.resolve(productRoot.toString()).also { it.ensureCreated() }
        SettingsRepository(directories).load()
        val vault = NoteVault(directories)
        val doc = vault.createNote("Note", parentPath = "")
        vault.saveNote(doc.relativePath, "saved", doc.metadata)
        val session = QuickNoteSession().apply {
            currentFile = doc.relativePath
            savedText = "saved"
            editor.setText("dirty", recordUndo = false)
            metadata = doc.metadata
            savedMetadata = doc.metadata
        }
        vault.saveNote(doc.relativePath, "disk", doc.metadata)
        assertEquals(QuickNoteReloadOpenResult.Unchanged, quickNoteReloadOpenFileIfClean(session, vault))
        assertEquals("dirty", session.editor.text)
    }

    @Test
    fun clearSessionWhenOpenNoteMissing() {
        val productRoot = createTempDirectory("mootool-qn-reload-missing-")
        val directories = AppPaths.resolve(productRoot.toString()).also { it.ensureCreated() }
        SettingsRepository(directories).load()
        val vault = NoteVault(directories)
        val doc = vault.createNote("Note", parentPath = "")
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
            assertEquals(QuickNoteReloadOpenResult.ClearedMissing, quickNoteReloadOpenFileIfClean(session, vault))
            assertTrue(session.currentFile.isBlank())
            assertTrue(session.vaultSelectedPath.isBlank())
            assertTrue(session.editor.text.isBlank())
        }
    }
}
