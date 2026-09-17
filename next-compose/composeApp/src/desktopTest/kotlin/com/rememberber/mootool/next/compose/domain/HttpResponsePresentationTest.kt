package com.rememberber.mootool.next.compose.domain

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
