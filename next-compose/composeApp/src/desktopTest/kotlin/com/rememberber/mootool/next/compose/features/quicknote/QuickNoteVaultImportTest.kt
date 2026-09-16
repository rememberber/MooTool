package com.rememberber.mootool.next.compose.features.quicknote

import com.rememberber.mootool.next.compose.app.AppPaths
import com.rememberber.mootool.next.compose.storage.NoteVault
import com.rememberber.mootool.next.compose.storage.VaultEntry
import kotlin.io.path.createTempDirectory
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertEquals
import java.io.File

class QuickNoteVaultImportTest {
    @Test
    fun importLandsUnderTargetDirectoryWithPortableRelativePath() {
        val root = createTempDirectory("mootool-quicknote-import-")
        val vault = NoteVault(AppPaths.resolve(root.toString()).also { it.ensureCreated() })
        vault.createDirectory("Work")
        val source = File.createTempFile("picked-", ".txt").apply { writeText("hello") }
        val relative = importQuickNoteVaultFile(vault, source, "Work")
        assertEquals("Work/${source.name}", relative.replace('\\', '/'))
    }

    @Test
    fun targetDirectoryUsesVaultSelectionLikeTreeDrop() {
        val items = listOf(
            VaultEntry("Work/a.md", "a.md", false, 0),
        )
        assertEquals("Work", quickNoteVaultImportTargetDirectory("Work/a.md", "", items))
        assertEquals("", quickNoteVaultImportTargetDirectory("", "", items))
    }
}
