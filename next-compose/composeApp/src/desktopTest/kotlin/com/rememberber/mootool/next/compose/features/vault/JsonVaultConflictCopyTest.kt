package com.rememberber.mootool.next.compose.features.vault

import com.rememberber.mootool.next.compose.app.AppPaths
import com.rememberber.mootool.next.compose.domain.VaultConflictEngine
import com.rememberber.mootool.next.compose.storage.JsonVault
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertEquals

class JsonVaultConflictCopyTest {
    @Test
    fun saveCopyWritesSiblingWithoutOverwritingOnDisk() {
        val root = createTempDirectory("mootool-json-conflict-copy-")
        val vault = JsonVault(AppPaths.resolve(root.toString()).also { it.ensureCreated() })
        val path = "work/sample.json"
        vault.createDirectory("work")
        val onDisk = """{"v":1}"""
        val editor = """{"v":2,"unsaved":true}"""
        vault.write(path, onDisk)
        val copy = VaultConflictEngine.conflictCopyName(path, 1_700_000_000_123L)
        assertEquals("work/sample.local-1700000000123.json", copy)
        vault.write(copy, editor)
        assertEquals(onDisk, vault.read(path))
        assertEquals(editor, vault.read(copy))
    }
}
