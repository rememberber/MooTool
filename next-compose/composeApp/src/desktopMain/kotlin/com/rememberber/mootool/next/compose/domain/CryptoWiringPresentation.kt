package com.rememberber.mootool.next.compose.domain

/** F14 加解密：操作钮启用守卫（对齐密钥长度/算法与 busy 语义）。 */
object CryptoWiringPresentation {
    fun canGenerateKeyPair(asymBusy: Boolean): Boolean = !asymBusy

    fun canRestorePublicKey(privateKey: String, asymBusy: Boolean): Boolean =
        privateKey.isNotBlank() && !asymBusy

    fun rsaPrivateReverseEnabled(algorithm: AsymmetricAlgorithm): Boolean =
        algorithm == AsymmetricAlgorithm.RSA

    fun canSymmetricCrypt(keyValid: Boolean, busy: Boolean = false): Boolean =
        keyValid && !busy

    fun publicEncryptActionEnabled(status: AsymmetricKeyStatus, asymBusy: Boolean): Boolean =
        !asymBusy && status.publicReady

    fun privateDecryptActionEnabled(status: AsymmetricKeyStatus, asymBusy: Boolean): Boolean =
        !asymBusy && status.privateReady

    fun privateEncryptActionEnabled(
        algorithm: AsymmetricAlgorithm,
        status: AsymmetricKeyStatus,
        asymBusy: Boolean,
    ): Boolean = rsaPrivateReverseEnabled(algorithm) && !asymBusy && status.privateReady

    fun publicDecryptActionEnabled(
        algorithm: AsymmetricAlgorithm,
        status: AsymmetricKeyStatus,
        asymBusy: Boolean,
    ): Boolean = rsaPrivateReverseEnabled(algorithm) && !asymBusy && status.publicReady

    fun signActionEnabled(status: AsymmetricKeyStatus, asymBusy: Boolean): Boolean =
        !asymBusy && status.privateReady

    fun verifyActionEnabled(
        status: AsymmetricKeyStatus,
        plain: String,
        signature: String,
        asymBusy: Boolean,
    ): Boolean =
        !asymBusy && status.publicReady && plain.isNotBlank() && signature.isNotBlank()

    fun encodeBaseActionEnabled(plain: String): Boolean = plain.isNotBlank()

    fun decodeBaseActionEnabled(cipher: String): Boolean = cipher.isNotBlank()

    fun cipherCopyActionEnabled(value: String): Boolean = value.isNotBlank()

    fun randomCopyActionEnabled(value: String): Boolean = cipherCopyActionEnabled(value)

    fun randomGenerateActionEnabled(kind: RandomKind, randomLength: Int): Boolean =
        when (kind) {
            RandomKind.Uuid -> true
            else ->
                randomLength in CryptoEngine.MIN_RANDOM_LENGTH..CryptoEngine.MAX_RANDOM_LENGTH
        }

    sealed interface DigestOutcome {
        data class Success(val output: String) : DigestOutcome
        data class Failure(val error: Throwable) : DigestOutcome
    }

    fun runDigestText(algorithm: DigestAlgorithm, input: String): DigestOutcome =
        runCatching { CryptoEngine.digestText(algorithm, input) }.fold(
            onSuccess = { DigestOutcome.Success(it) },
            onFailure = { DigestOutcome.Failure(it) },
        )

    fun shouldToastOperationFailure(error: Throwable): Boolean = true

    fun shouldToastVerifyFailure(): Boolean = true
}
