package com.rememberber.mootool.next.compose.domain

import com.rememberber.mootool.next.compose.model.CustomToolGroup
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class CustomGroupDraftTest {
    @Test
    fun blankNameOrEmptyToolsAreInvalid() {
        val blank = CustomToolGroup("1", "  ", listOf("json"))
        val emptyTools = CustomToolGroup("2", "Dev", emptyList())
        val ok = CustomToolGroup("3", "Dev", listOf("json"))
        assertEquals("1", CustomGroupDraft.invalid(listOf(blank, ok))?.id)
        assertEquals("2", CustomGroupDraft.invalid(listOf(emptyTools))?.id)
        assertNull(CustomGroupDraft.invalid(listOf(ok)))
    }

    @Test
    fun persistableTrimsNamesAndCopiesIds() {
        val draft = listOf(CustomToolGroup("1", "  JSON  ", listOf("json", "java")))
        val saved = CustomGroupDraft.persistable(draft)
        assertEquals("JSON", saved.single().name)
        assertEquals(listOf("json", "java"), saved.single().toolIds)
        assertEquals(listOf("json", "java"), CustomGroupDraft.copyOf(draft).single().toolIds)
    }
}
