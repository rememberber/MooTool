package com.rememberber.mootool.next.compose.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CryptoWiringPresentationTest {
    @Test
    fun symmetricCryptRequiresValidKey() {
        assertTrue(CryptoWiringPresentation.canSymmetricCrypt(keyValid = true))
        assertFalse(CryptoWiringPresentation.canSymmetricCrypt(keyValid = false))
    }

    @Test
    fun rsaPrivateReverseOnlyForRsa() {
        assertTrue(CryptoWiringPresentation.rsaPrivateReverseEnabled(AsymmetricAlgorithm.RSA))
        assertFalse(CryptoWiringPresentation.rsaPrivateReverseEnabled(AsymmetricAlgorithm.SM2))
    }

    @Test
    fun runDigestTextUsesCryptoEngine() {
        val outcome = CryptoWiringPresentation.runDigestText(DigestAlgorithm.SHA256, "mootool")
        assertTrue(outcome is CryptoWiringPresentation.DigestOutcome.Success)
        assertEquals(64, (outcome as CryptoWiringPresentation.DigestOutcome.Success).output.length)
    }

    @Test
    fun shouldToastFailures() {
        assertTrue(CryptoWiringPresentation.shouldToastOperationFailure(IllegalStateException()))
        assertTrue(CryptoWiringPresentation.shouldToastVerifyFailure())
    }
}
