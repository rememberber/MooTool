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

    @Test
    fun cipherCopyActionEnabled() {
        assertFalse(CryptoWiringPresentation.cipherCopyActionEnabled(""))
        assertTrue(CryptoWiringPresentation.cipherCopyActionEnabled("deadbeef"))
    }

    @Test
    fun asymmetricAndBaseActionEnabled() {
        val missing = AsymmetricKeyStatus(publicReady = false, privateReady = false)
        val ready = AsymmetricKeyStatus(publicReady = true, privateReady = true)
        assertFalse(CryptoWiringPresentation.publicEncryptActionEnabled(missing, asymBusy = false))
        assertTrue(CryptoWiringPresentation.publicEncryptActionEnabled(ready, asymBusy = false))
        assertFalse(CryptoWiringPresentation.publicEncryptActionEnabled(ready, asymBusy = true))
        assertFalse(CryptoWiringPresentation.verifyActionEnabled(ready, "plain", "", asymBusy = false))
        assertTrue(CryptoWiringPresentation.verifyActionEnabled(ready, "plain", "sig", asymBusy = false))
        assertFalse(
            CryptoWiringPresentation.privateEncryptActionEnabled(AsymmetricAlgorithm.SM2, ready, asymBusy = false),
        )
        assertTrue(
            CryptoWiringPresentation.privateEncryptActionEnabled(AsymmetricAlgorithm.RSA, ready, asymBusy = false),
        )
        assertFalse(CryptoWiringPresentation.encodeBaseActionEnabled(""))
        assertTrue(CryptoWiringPresentation.decodeBaseActionEnabled("YWJj"))
    }

    @Test
    fun randomTabActionEnabled() {
        assertFalse(CryptoWiringPresentation.randomCopyActionEnabled(""))
        assertTrue(CryptoWiringPresentation.randomCopyActionEnabled("abc"))
        assertTrue(CryptoWiringPresentation.randomGenerateActionEnabled(RandomKind.Uuid, 0))
        assertTrue(CryptoWiringPresentation.randomGenerateActionEnabled(RandomKind.String, 16))
        assertFalse(CryptoWiringPresentation.randomGenerateActionEnabled(RandomKind.String, 0))
        assertFalse(
            CryptoWiringPresentation.randomGenerateActionEnabled(
                RandomKind.Password,
                CryptoEngine.MAX_RANDOM_LENGTH + 1,
            ),
        )
    }
}
