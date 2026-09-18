package com.rememberber.mootool.next.compose.domain

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HttpResponsePresentationTest {
    @Test
    fun failAndCancelKeepPreviousResponseLabel() {
        val ok = sampleResponse(ok = true, status = 200, body = "hello")
        val aborted = sampleResponse(ok = false, status = 0, body = "", error = HttpErrorCode.ABORTED)
        val timeout = sampleResponse(ok = false, status = 0, body = "", error = HttpErrorCode.TIMEOUT)
        val tooLarge = sampleResponse(ok = false, status = 200, body = "partial", error = HttpErrorCode.RESPONSE_TOO_LARGE)
        assertEquals(ok, HttpResponsePresentation.usableResponse(ok, null))
        assertEquals(ok, HttpResponsePresentation.usableResponse(aborted, ok))
        assertTrue(HttpResponsePresentation.showPreviousLabel(sending = true, current = ok, previous = ok))
        assertTrue(HttpResponsePresentation.showPreviousLabel(sending = false, current = aborted, previous = ok))
        assertTrue(HttpResponsePresentation.showPreviousLabel(sending = false, current = timeout, previous = ok))
        assertFalse(HttpResponsePresentation.showPreviousLabel(sending = false, current = ok, previous = ok))
        assertFalse(HttpResponsePresentation.showPreviousLabel(sending = false, current = tooLarge, previous = ok))
        assertEquals(ok, HttpResponsePresentation.visibleResponse(sending = false, current = aborted, previous = ok))
        assertEquals(tooLarge, HttpResponsePresentation.visibleResponse(sending = false, current = tooLarge, previous = ok))
        assertEquals(ok, HttpResponsePresentation.visibleResponse(sending = true, current = null, previous = ok))
    }

    @Test
    fun statusMetaSuccessFollowsOkAndStatus() {
        assertTrue(HttpResponsePresentation.statusMetaSuccess(sampleResponse(ok = true, status = 200, body = "")))
        assertFalse(HttpResponsePresentation.statusMetaSuccess(sampleResponse(ok = false, status = 404, body = "")))
        assertFalse(HttpResponsePresentation.statusMetaSuccess(null))
    }

    @Test
    fun copyAndSaveResponseActionEnabledRequireVisibleAndNotSending() {
        val visible = sampleResponse(ok = true, status = 200, body = "x")
        assertTrue(HttpResponsePresentation.copyResponseActionEnabled(visible, sending = false))
        assertTrue(HttpResponsePresentation.saveResponseActionEnabled(visible, sending = false))
        assertFalse(HttpResponsePresentation.copyResponseActionEnabled(null, sending = false))
        assertFalse(HttpResponsePresentation.saveResponseActionEnabled(null, sending = false))
        assertFalse(HttpResponsePresentation.copyResponseActionEnabled(visible, sending = true))
        assertFalse(HttpResponsePresentation.saveResponseActionEnabled(visible, sending = true))
    }

    @Test
    fun responseFindAndFindBarActionEnabled() {
        val visible = sampleResponse(ok = true, status = 200, body = "x")
        assertFalse(HttpResponsePresentation.responseFindOpenActionEnabled(findOpen = false, visible = null, sending = false))
        assertTrue(HttpResponsePresentation.responseFindOpenActionEnabled(findOpen = false, visible = visible, sending = false))
        assertTrue(HttpResponsePresentation.responseFindOpenActionEnabled(findOpen = true, visible = null, sending = true))
        assertFalse(HttpResponsePresentation.findQueryActionEnabled("  "))
        assertTrue(HttpResponsePresentation.findStepActionEnabled("q"))
    }

    @Test
    fun runWriteResponseTextRoundTripAndMissingParentFails() {
        val dir = kotlin.io.path.createTempDirectory("http-response-write-").toFile()
        try {
            val ok = File(dir, "body.txt")
            assertEquals(
                HttpResponsePresentation.WriteExportOutcome.Success,
                HttpResponsePresentation.runWriteResponseText(ok, "payload579"),
            )
            assertEquals("payload579", ok.readText())
            val bad = File(dir, "missing/nested.txt")
            val fail = HttpResponsePresentation.runWriteResponseText(bad, "x")
            assertTrue(fail is HttpResponsePresentation.WriteExportOutcome.Failure)
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun runWriteResponseBytesRoundTrip() {
        val dir = kotlin.io.path.createTempDirectory("http-response-bytes-").toFile()
        try {
            val ok = File(dir, "bin.dat")
            val bytes = byteArrayOf(0x57, 0x39)
            assertEquals(
                HttpResponsePresentation.WriteExportOutcome.Success,
                HttpResponsePresentation.runWriteResponseBytes(ok, bytes),
            )
            assertTrue(bytes.contentEquals(ok.readBytes()))
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun shouldToastWriteFailure() {
        assertTrue(HttpResponsePresentation.shouldToastWriteFailure(IllegalStateException()))
    }
}

private fun sampleResponse(
    ok: Boolean,
    status: Int,
    body: String,
    error: HttpErrorCode? = null,
): HttpResponseResult = HttpResponseResult(
    requestId = "test",
    ok = ok,
    status = status,
    statusText = if (ok) "OK" else "",
    url = "https://example.com",
    durationMs = 1,
    body = body,
    headers = "",
    cookies = "",
    errorCode = error,
    binary = false,
    bodyBytes = body.toByteArray(),
)
