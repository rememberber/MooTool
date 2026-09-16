package com.rememberber.mootool.next.compose.features.json

import kotlin.test.Test
import kotlin.test.assertEquals

class JsonVaultNewFileContentTest {
    @Test
    fun blankEditorUsesElectronDefaultBody() {
        assertEquals("{\n\n}", newJsonVaultFileContent(""))
        assertEquals("{\n\n}", newJsonVaultFileContent("   "))
    }

    @Test
    fun keepsCurrentEditorText() {
        assertEquals("{\"a\":1}", newJsonVaultFileContent("{\"a\":1}"))
    }
}
