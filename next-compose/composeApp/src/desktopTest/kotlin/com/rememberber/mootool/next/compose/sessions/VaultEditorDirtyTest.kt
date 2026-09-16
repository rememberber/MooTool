package com.rememberber.mootool.next.compose.sessions

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class VaultEditorDirtyTest {
    @Test
    fun jsonDirtyWhenBufferDiffersFromSaved() {
        val session = JsonSession()
        session.currentFile = "a.json"
        session.savedText = "{}"
        session.editor.setText("{\"a\":1}", recordUndo = false)
        assertTrue(session.isVaultEditorDirty())
    }

    @Test
    fun quickNoteDirtyWhenMetadataChanges() {
        val session = QuickNoteSession()
        session.currentFile = "note.md"
        session.savedText = session.editor.text
        session.savedMetadata = session.metadata
        session.metadata = session.metadata.copy(title = "Renamed")
        assertTrue(session.isVaultEditorDirty())
        assertFalse(session.editor.text != session.savedText)
    }
}
