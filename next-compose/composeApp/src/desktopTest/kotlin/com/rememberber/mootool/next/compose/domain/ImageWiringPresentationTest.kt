package com.rememberber.mootool.next.compose.domain

import java.io.IOException
import kotlin.coroutines.cancellation.CancellationException
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ImageWiringPresentationTest {
    @Test
    fun processRequiresSelectionAndIdle() {
        assertFalse(ImageWiringPresentation.canProcessSelection(selectedCount = 0, busy = false))
        assertFalse(ImageWiringPresentation.canProcessSelection(selectedCount = 2, busy = true))
        assertTrue(ImageWiringPresentation.canProcessSelection(selectedCount = 1, busy = false))
    }

    @Test
    fun runExportAssetsWrapsSuccessAndFailure() {
        assertTrue(ImageWiringPresentation.runExportAssets { } is ImageWiringPresentation.ExportAssetsOutcome.Success)
        val fail = ImageWiringPresentation.runExportAssets { throw IOException("denied") }
        assertTrue(fail is ImageWiringPresentation.ExportAssetsOutcome.Failure)
    }

    @Test
    fun shouldToastProcessFailureSkipsCancel() {
        assertFalse(ImageWiringPresentation.shouldToastProcessFailure(CancellationException()))
        assertFalse(ImageWiringPresentation.shouldToastProcessFailure(ImageException("cancelled", "")))
        assertTrue(ImageWiringPresentation.shouldToastProcessFailure(ImageException("missing", "")))
    }
}
