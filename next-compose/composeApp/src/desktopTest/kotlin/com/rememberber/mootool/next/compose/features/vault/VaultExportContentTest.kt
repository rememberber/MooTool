package com.rememberber.mootool.next.compose.features.vault

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class VaultExportContentTest {
    @Test
    fun usesEditorWhenExportingOpenDirtyFile() {
        assertTrue(vaultExportUsesEditorBuffer("a.json", "a.json", "{ \"x\": 1 }", "{}"))
        assertEquals(
            "{ \"x\": 1 }",
            vaultExportText("a.json", "a.json", "{ \"x\": 1 }", "{}", diskText = "{}"),
        )
    }

    @Test
    fun usesDiskForOtherPathsOrCleanBuffer() {
        assertFalse(vaultExportUsesEditorBuffer("b.json", "a.json", "edited", "saved"))
        assertFalse(vaultExportUsesEditorBuffer("a.json", "a.json", "same", "same"))
        assertEquals("disk", vaultExportText("b.json", "a.json", "edited", "saved", diskText = "disk"))
    }
}
