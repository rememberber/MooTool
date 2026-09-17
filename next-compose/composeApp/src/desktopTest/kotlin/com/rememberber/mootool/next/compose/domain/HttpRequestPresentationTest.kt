package com.rememberber.mootool.next.compose.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HttpRequestPresentationTest {
    @Test
    fun applyResponseMatchesActiveRequestId() {
        assertTrue(HttpRequestPresentation.shouldApplyResponse("http-1", "http-1"))
        assertFalse(HttpRequestPresentation.shouldApplyResponse("http-1", "http-2"))
        assertFalse(HttpRequestPresentation.shouldApplyResponse("", "http-1"))
    }

    @Test
    fun previousResponseBeforeSendDelegatesToHttpResponsePresentation() {
        val ok = HttpResponseResult(
            requestId = "a",
            ok = true,
            status = 200,
            statusText = "OK",
            url = "",
            durationMs = 1,
            body = "x",
            headers = "",
            cookies = "",
        )
        val aborted = ok.copy(ok = false, errorCode = HttpErrorCode.ABORTED)
        assertEquals(ok, HttpRequestPresentation.previousResponseBeforeSend(ok, null))
        assertEquals(ok, HttpRequestPresentation.previousResponseBeforeSend(aborted, ok))
    }

    @Test
    fun canSendRequiresUrlAndNotSending() {
        assertTrue(HttpRequestPresentation.canSend("https://example.com", sending = false))
        assertFalse(HttpRequestPresentation.canSend("  ", sending = false))
        assertFalse(HttpRequestPresentation.canSend("https://example.com", sending = true))
    }
}
