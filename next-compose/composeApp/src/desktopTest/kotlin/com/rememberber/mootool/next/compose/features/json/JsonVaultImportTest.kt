package com.rememberber.mootool.next.compose.features.json

import com.rememberber.mootool.next.compose.app.AppPaths
import com.rememberber.mootool.next.compose.storage.JsonVault
import com.rememberber.mootool.next.compose.storage.VaultEntry
import kotlin.io.path.createTempDirectory
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertEquals
import java.io.File

class JsonVaultImportTest {
    @Test
    fun importLandsUnderTargetDirectoryWithPortableRelativePath() {
        val root = createTempDirectory("mootool-json-import-")
        val vault = JsonVault(AppPaths.resolve(root.toString()).also { it.ensureCreated() })
        vault.createDirectory("Work")
        val source = File.createTempFile("picked-", ".json").apply { writeText("{}") }
        val relative = importJsonVaultFile(vault, source, "Work")
        assertEquals("Work/${source.name}", relative.replace('\\', '/'))
    }

    @Test
    fun targetDirectoryUsesVaultSelectionLikeTreeDrop() {
        val items = listOf(
            VaultEntry("Work/a.json", "a.json", false, 0),
        )
        assertEquals("Work", jsonVaultImportTargetDirectory("Work/a.json", "", items))
        assertEquals("", jsonVaultImportTargetDirectory("", "", items))
    }
}
