package com.rememberber.mootool.next.compose.features.vault

import com.rememberber.mootool.next.compose.app.AppPaths
import com.rememberber.mootool.next.compose.domain.VaultConflictEngine
import com.rememberber.mootool.next.compose.storage.NoteVault
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertEquals

class QuickNoteVaultConflictCopyTest {
    @Test
    fun saveCopyWritesSiblingWithoutOverwritingOnDisk() {
        val root = createTempDirectory("mootool-note-conflict-copy-")
        val vault = NoteVault(AppPaths.resolve(root.toString()).also { it.ensureCreated() })
        val path = "daily/note.md"
        vault.createDirectory("daily")
        val onDisk = "---\ntitle: Disk\n---\n\nbody"
        val editor = "---\ntitle: Local\n---\n\nedited"
        vault.write(path, onDisk)
        val copy = VaultConflictEngine.conflictCopyName(path, 1_700_000_000_456L)
        assertEquals("daily/note.local-1700000000456.md", copy)
        vault.write(copy, editor)
        assertEquals(onDisk, vault.read(path))
        assertEquals(editor, vault.read(copy))
    }
}
