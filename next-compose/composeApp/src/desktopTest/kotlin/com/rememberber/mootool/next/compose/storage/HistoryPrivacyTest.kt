package com.rememberber.mootool.next.compose.storage

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HistoryPrivacyTest {
    @Test
    fun cryptoRedactsPrivateKeyOperationsAndGeneratedPasswords() {
        val signed = HistoryPrivacy.crypto("sign", "-----BEGIN PRIVATE KEY-----\nabc\n-----END PRIVATE KEY-----", "sig")
        assertEquals(HistoryPrivacy.REDACTED, signed.input)
        assertEquals("sig", signed.output)
        val password = HistoryPrivacy.crypto("password", "len=16", "s3cret-pass")
        assertEquals("len=16", password.input)
        assertEquals(HistoryPrivacy.REDACTED, password.output)
        val digest = HistoryPrivacy.crypto("text", "hello", "abc123")
        assertEquals("hello", digest.input)
        assertEquals("abc123", digest.output)
    }

    @Test
    fun httpUrlStripsBasicAuthPassword() {
        assertEquals(
            "https://alice:[redacted]@example.com/v1",
            HistoryPrivacy.httpUrl("https://alice:super-secret@example.com/v1")
        )
        assertEquals("https://example.com/v1", HistoryPrivacy.httpUrl("https://example.com/v1"))
    }

    @Test
    fun privateKeyDetectorDoesNotFlagPublicMaterial() {
        assertTrue(HistoryPrivacy.looksLikePrivateKey("-----BEGIN RSA PRIVATE KEY-----\nMII\n-----END RSA PRIVATE KEY-----"))
        assertFalse(HistoryPrivacy.looksLikePrivateKey("-----BEGIN PUBLIC KEY-----\nMII\n-----END PUBLIC KEY-----"))
    }
}
