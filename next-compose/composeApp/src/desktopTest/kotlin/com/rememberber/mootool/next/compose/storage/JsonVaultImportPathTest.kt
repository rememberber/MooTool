package com.rememberber.mootool.next.compose.storage

import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals

class JsonVaultImportPathTest {
    @Test
    fun importRelativePathJoinsDirectory() {
        assertEquals("a/b.json", jsonVaultImportRelativePath("a", "b.json"))
        assertEquals("b.json", jsonVaultImportRelativePath("", "b.json"))
        assertEquals("b.json", jsonVaultImportRelativePath("  ", "b.json"))
    }

    @Test
    fun entryRelativePathFromRoot() {
        val root = Path.of("/vault")
        val entry = Path.of("/vault/folder/x.json")
        assertEquals("folder/x.json", jsonVaultEntryRelativePath(root, entry))
    }
}
