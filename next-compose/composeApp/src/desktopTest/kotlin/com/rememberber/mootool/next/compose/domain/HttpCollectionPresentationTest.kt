package com.rememberber.mootool.next.compose.domain

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HttpCollectionPresentationTest {
    @Test
    fun deleteSavedActionEnabledRequiresSelection() {
        assertFalse(HttpCollectionPresentation.deleteSavedActionEnabled(""))
        assertFalse(HttpCollectionPresentation.deleteSavedActionEnabled("   "))
        assertTrue(HttpCollectionPresentation.deleteSavedActionEnabled("req-1"))
    }

    @Test
    fun saveAndCurlImportRequireNonBlankTrimmed() {
        assertFalse(HttpCollectionPresentation.saveCollectionActionEnabled(""))
        assertFalse(HttpCollectionPresentation.saveCollectionActionEnabled("  "))
        assertTrue(HttpCollectionPresentation.saveCollectionActionEnabled(" demo "))
        assertFalse(HttpCollectionPresentation.curlImportActionEnabled("\n"))
        assertTrue(HttpCollectionPresentation.curlImportActionEnabled("curl https://example.com"))
    }
}
