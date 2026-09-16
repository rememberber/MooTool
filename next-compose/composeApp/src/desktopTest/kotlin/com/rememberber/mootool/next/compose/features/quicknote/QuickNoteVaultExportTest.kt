package com.rememberber.mootool.next.compose.features.quicknote

import com.rememberber.mootool.next.compose.app.AppPaths
import com.rememberber.mootool.next.compose.sessions.QuickNoteSession
import com.rememberber.mootool.next.compose.storage.NoteVault
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertEquals

class QuickNoteVaultExportTest {
    @Test
    fun exportBodyUsesEditorWhenCurrentFile() {
        val root = createTempDirectory("mootool-export-body-")
        val vault = NoteVault(AppPaths.resolve(root.toString()).also { it.ensureCreated() })
        val note = vault.createNote("T", parentPath = "")
        vault.saveNote(note.relativePath, "on-disk", note.metadata)
        val session = QuickNoteSession().apply {
            currentFile = note.relativePath
            editor.setText("in-editor", recordUndo = false)
            savedText = "on-disk"
        }
        assertEquals("in-editor", quickNoteExportBody(session, vault, note.relativePath))
        val other = vault.createNote("Other", parentPath = "")
        vault.saveNote(other.relativePath, "other-body", other.metadata)
        assertEquals("other-body", quickNoteExportBody(session, vault, other.relativePath))
    }

    @Test
    fun defaultFileNameUsesTitleTxtLikeElectron() {
        assertEquals("My Note.txt", quickNoteExportDefaultFileName("folder/a.md", "My Note"))
        assertEquals("keep.me.txt", quickNoteExportDefaultFileName("a.md", "keep.me")) // Electron `${title}.txt`
    }

    @Test
    fun defaultFileNameFallsBackToLeaf() {
        assertEquals("note.md", quickNoteExportDefaultFileName("dir/note.md", null))
    }
}
