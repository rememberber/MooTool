package com.rememberber.mootool.next.compose.domain

import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CryptoEngineTest {
    @Test
    fun matchesElectronSymmetricAndDigestFixtures() {
        val plain = "MooTool 加密"
        val key = "1234567890abcdef"
        assertEquals("504a3eb1fee7af3af9561f37a6f12fa8", CryptoEngine.symmetricEncrypt(SymmetricAlgorithm.AES, plain, key))
        assertEquals("cc169541943882e354b03c72dbf8f8c8", CryptoEngine.symmetricEncrypt(SymmetricAlgorithm.DES, plain, key))
        assertEquals("bced0ef937398d96aadf38aa5aa8e478", CryptoEngine.symmetricEncrypt(SymmetricAlgorithm.SM4, plain, key))
        assertEquals(plain, CryptoEngine.symmetricDecrypt(SymmetricAlgorithm.AES, "504a3eb1fee7af3af9561f37a6f12fa8", key))
        assertEquals(plain, CryptoEngine.symmetricDecrypt(SymmetricAlgorithm.DES, "cc169541943882e354b03c72dbf8f8c8", key))
        assertEquals(plain, CryptoEngine.symmetricDecrypt(SymmetricAlgorithm.SM4, "bced0ef937398d96aadf38aa5aa8e478", key))
        assertEquals("ad1d510fa78f8f8276e4f3ad541f1957", CryptoEngine.symmetricEncrypt(SymmetricAlgorithm.AES, "Moo", "abc"))
        assertEquals("900150983cd24fb0d6963f7d28e17f72", CryptoEngine.digestText(DigestAlgorithm.MD5, "abc"))
        assertEquals("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad", CryptoEngine.digestText(DigestAlgorithm.SHA256, "abc"))
        assertEquals("66c7f0f462eeedd9d1f2d46bdc10e4e24167c4875cf2f7a2297da02b8f4ba8e0", CryptoEngine.digestText(DigestAlgorithm.SM3, "abc"))
    }

    @Test
    fun roundTripsBaseEncodingsAndFileDigest() {
        assertEquals("TW9vIOW3peWFtw==", CryptoEngine.encodeBase(BaseAlgorithm.Base64, "Moo 工具"))
        assertEquals("JVXW6IHFW6S6LBNX", CryptoEngine.encodeBase(BaseAlgorithm.Base32, "Moo 工具"))
        assertEquals("Moo 工具", CryptoEngine.decodeBase(BaseAlgorithm.Base64, "TW9vIOW3peWFtw=="))
        assertEquals("Moo 工具", CryptoEngine.decodeBase(BaseAlgorithm.Base32, "JVXW6IHFW6S6LBNX"))
        val file = Files.createTempFile("next-compose-digest", ".txt")
        Files.writeString(file, "abc")
        try {
            assertEquals("900150983cd24fb0d6963f7d28e17f72", CryptoEngine.digestFile(DigestAlgorithm.MD5, file))
        } finally {
            Files.deleteIfExists(file)
        }
    }

    @Test
    fun consumesElectronRsaAndSm2Samples() {
        val rsaPublic = "MFwwDQYJKoZIhvcNAQEBBQADSwAwSAJBAJxmdw437OuNcTuW2QmB3cZe5qFnn9z0BgzoKsxkFaIimxjbAUe92yHU/N4xIB4cNWooh5FkbeOmF1u0Q8fALskCAwEAAQ=="
        val rsaPrivate = "MIIBOgIBAAJBAJxmdw437OuNcTuW2QmB3cZe5qFnn9z0BgzoKsxkFaIimxjbAUe92yHU/N4xIB4cNWooh5FkbeOmF1u0Q8fALskCAwEAAQJAeFa24G/TkeLA73LACHquI8Y9eo97B82TIjc5Rw7zPk5iKF1lRt0cKMQM64Ox8W/Hk1XS7A8qWSIwpyhGBw4miQIhAM6CrqTJK2hr0XQFEJsiNnRwa2tqT6aJMeWTDHgoz+A/AiEAweGLgFhXmnkCtB1Ix+PGMQ3lfTXib4YsDhPJXfI8rvcCIQCzjz17Ws+7g8QjNSQzP5RJulYsl8uZ6kDQdQOqlxHo0QIgR0N4/Eb8hEoAhWXSL674VWWPOdPJlEaUAOSi+oYkagcCIAkMXn057THqjyJM0dW8S++lHItKv7XtirCJzQTpXRp7"
        assertEquals("moo", CryptoEngine.asymmetricDecrypt(AsymmetricAlgorithm.RSA, "PvjjydFrokOw6E1MjQbiui/GFMew7uzeTGhErzhnE8ufIiB92jJjuN7glmE/cQQdl21eU4eiULrkpCntOFinZw==", rsaPrivate))
        assertTrue(CryptoEngine.verifySignature(AsymmetricAlgorithm.RSA, "moo", "DkPQD820mMKcoivVjSKhQkYeNhu+PBmk4uF/+C3Ua+wtCt9jWB9vOLMaU/bIJ1cD3hBFuk+MjMcxCvCkGZss3Q==", rsaPublic))
        assertEquals("moo", CryptoEngine.publicDecrypt("cuK3mq7ZcgMh52MA2firzCp1+EvvtOPOxFz06wvpRVHpnRzW3DfALV40AED1goAqoqQHa73TwWc2+HWMfaYliQ==", rsaPublic))
        val sm2Public = "BKIlVD33k3zQPoXMFwbYCNlxMYbXkWuGTH95zjETCIP+sRSvl4776aFm2OQbJZUq/KGws7Og5M0kijj+AKlIPrM="
        val sm2Private = "gLwDampri1xBKCTE3NiipyOneBMZ/QxntuSCNoOYlqk="
        assertEquals("moo", CryptoEngine.asymmetricDecrypt(AsymmetricAlgorithm.SM2, "TBZTzJ/HFo1vuyucFmkDcwswEE9cDfPWsD6H22zmvYa1DQtqbkilfn6tlmgmGwfx0Aobx4S2leIFaYvgP9qpXHIyZLninIh/nMsiCV73AXyUd2WFR60T7gSv00x5rUXcpeUH", sm2Private))
        assertTrue(CryptoEngine.verifySignature(AsymmetricAlgorithm.SM2, "moo", "MEQCIBsBrgCe5e7uTx9g5lvc8/d+IMqKc4IZ8F6yQBpAEMytAiAwm32c1OVmneoL/li0abLEaB29bXHvK7TMHwas2UnL3w==", sm2Public))
    }

    @Test
    fun generatesRsaAndSm2RoundTrips() {
        val rsa = CryptoEngine.generateAsymmetricKeyPair(AsymmetricAlgorithm.RSA, 512)
        val rsaCipher = CryptoEngine.asymmetricEncrypt(AsymmetricAlgorithm.RSA, "moo", rsa.publicKey)
        assertEquals("moo", CryptoEngine.asymmetricDecrypt(AsymmetricAlgorithm.RSA, rsaCipher, rsa.privateKey))
        val privateCipher = CryptoEngine.privateEncrypt("moo", rsa.privateKey)
        assertEquals("moo", CryptoEngine.publicDecrypt(privateCipher, rsa.publicKey))
        val signature = CryptoEngine.signContent(AsymmetricAlgorithm.RSA, "moo", rsa.privateKey)
        assertTrue(CryptoEngine.verifySignature(AsymmetricAlgorithm.RSA, "moo", signature, rsa.publicKey))
        assertFalse(CryptoEngine.verifySignature(AsymmetricAlgorithm.RSA, "nope", signature, rsa.publicKey))
        val sm2 = CryptoEngine.generateAsymmetricKeyPair(AsymmetricAlgorithm.SM2)
        val sm2Cipher = CryptoEngine.asymmetricEncrypt(AsymmetricAlgorithm.SM2, "moo", sm2.publicKey)
        assertEquals("moo", CryptoEngine.asymmetricDecrypt(AsymmetricAlgorithm.SM2, sm2Cipher, sm2.privateKey))
        val sm2Signature = CryptoEngine.signContent(AsymmetricAlgorithm.SM2, "moo", sm2.privateKey, sm2.publicKey)
        assertTrue(CryptoEngine.verifySignature(AsymmetricAlgorithm.SM2, "moo", sm2Signature, sm2.publicKey))
    }

    @Test
    fun generatesConstrainedRandomValuesAndRejectsBadInput() {
        assertTrue(CryptoEngine.randomUuid().matches(Regex("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$")))
        assertTrue(CryptoEngine.randomDigits(32).matches(Regex("^\\d{32}$")))
        assertTrue(CryptoEngine.randomString(32).matches(Regex("^[a-zA-Z0-9]{32}$")))
        val password = CryptoEngine.randomPassword(24)
        assertEquals(24, password.length)
        assertTrue(password.any { it.isLowerCase() })
        assertTrue(password.any { it.isUpperCase() })
        assertTrue(password.any { it.isDigit() })
        assertTrue(password.any { !it.isLetterOrDigit() })
        assertFailsWith<CryptoException> { CryptoEngine.symmetricDecrypt(SymmetricAlgorithm.AES, "zz", "1234567890abcdef") }
        assertFailsWith<CryptoException> { CryptoEngine.decodeBase(BaseAlgorithm.Base64, "@@@") }
        assertFailsWith<CryptoException> { CryptoEngine.randomDigits(0) }
        val cjkKey = "你好世界你好世界你好世界你好"
        val error = assertFailsWith<CryptoException> {
            CryptoEngine.symmetricEncrypt(SymmetricAlgorithm.AES, "Moo", cjkKey)
        }
        assertEquals("invalid-key", error.code)
    }
}
