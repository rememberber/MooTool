package com.rememberber.mootool.next.compose.editor

import com.rememberber.mootool.next.compose.model.ToolId
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class EditorFindToolScopeTest {
    @Test
    fun electronFindCoversJsonQuickNoteHostAndHttpOnly() {
        assertTrue(EditorFindToolScope.supportsElectronFind(ToolId.Json))
        assertTrue(EditorFindToolScope.supportsElectronFind(ToolId.QuickNote))
        assertTrue(EditorFindToolScope.supportsElectronFind(ToolId.Host))
        assertTrue(EditorFindToolScope.supportsElectronFind(ToolId.Http))
        assertFalse(EditorFindToolScope.supportsElectronFind(ToolId.Java))
        assertFalse(EditorFindToolScope.supportsElectronFind(ToolId.TextDiff))
    }

    @Test
    fun composeAddsCodeRunFindOnTopOfElectronSet() {
        assertTrue(EditorFindToolScope.supportsComposeFind(ToolId.Java))
        assertTrue(EditorFindToolScope.supportsComposeFind(ToolId.Json))
        assertFalse(EditorFindToolScope.supportsComposeFind(ToolId.Reformat))
    }
}
