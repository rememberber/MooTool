package com.rememberber.mootool.next.compose.domain

import kotlin.test.Test
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
}
