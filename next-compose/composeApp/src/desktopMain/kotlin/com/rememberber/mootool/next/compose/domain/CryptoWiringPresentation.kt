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
}
