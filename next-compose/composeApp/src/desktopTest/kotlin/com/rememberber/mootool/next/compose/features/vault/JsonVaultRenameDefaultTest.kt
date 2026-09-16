package com.rememberber.mootool.next.compose.features.vault

import com.rememberber.mootool.next.compose.storage.VaultEntry
import kotlin.test.Test
import kotlin.test.assertEquals

class JsonVaultRenameDefaultTest {
    @Test
    fun stripsJsonExtensionFromFile() {
        val entry = VaultEntry("drafts/a.json", "a.json", false, 1)
        assertEquals("a", jsonVaultRenameDefault(entry))
    }

    @Test
    fun directoryUsesLeafName() {
        val entry = VaultEntry("Work/Nested", "Nested", true, 0)
        assertEquals("Nested", jsonVaultRenameDefault(entry))
    }

    @Test
    fun fileNameHelperIgnoresCase() {
        assertEquals("snippet", jsonVaultRenameDefaultFromFileName("snippet.JSON"))
    }
}
