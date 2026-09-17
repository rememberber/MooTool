package com.rememberber.mootool.next.compose.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class JsonVaultSearchPresentationTest {
    @Test
    fun normalizeQueryTrimsAndLowercases() {
        assertEquals("readme", JsonVaultSearchPresentation.normalizeQuery("  ReadMe "))
        assertFalse(JsonVaultSearchPresentation.isFiltering("   "))
        assertTrue(JsonVaultSearchPresentation.isFiltering("x"))
    }
}
