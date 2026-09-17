package com.rememberber.mootool.next.compose.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PdfImportPresentationTest {
    @Test
    fun structureNoticeAndEncryptedToast() {
        val plain = PdfFileInfo("/a.pdf", "a.pdf", 1, 2)
        assertFalse(PdfImportPresentation.shouldShowStructureNotice(plain))
        val rich = plain.copy(formFieldCount = 1, bookmarkCount = 2, signatureFieldCount = 3)
        assertTrue(PdfImportPresentation.shouldShowStructureNotice(rich))
        assertEquals(
            mapOf("forms" to "1", "bookmarks" to "2", "signatures" to "3"),
            PdfImportPresentation.structureNoticeArgs(rich),
        )
        assertTrue(PdfImportPresentation.showEncryptedImportToast(PdfException("encrypted", "locked")))
        assertFalse(PdfImportPresentation.showEncryptedImportToast(PdfException("invalid-range", "bad")))
    }
}
