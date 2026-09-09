package com.rememberber.mootool.next.compose.domain

import org.bouncycastle.asn1.gm.GMNamedCurves
import org.bouncycastle.asn1.pkcs.RSAPrivateKey
import org.bouncycastle.asn1.pkcs.RSAPublicKey
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo
import org.bouncycastle.crypto.engines.SM2Engine
import org.bouncycastle.crypto.params.ECDomainParameters
import org.bouncycastle.crypto.params.ECPrivateKeyParameters
import org.bouncycastle.crypto.params.ECPublicKeyParameters
import org.bouncycastle.crypto.params.ParametersWithID
import org.bouncycastle.crypto.params.ParametersWithRandom
import org.bouncycastle.crypto.signers.SM2Signer
import org.bouncycastle.jcajce.provider.asymmetric.ec.BCECPrivateKey
import org.bouncycastle.jcajce.provider.asymmetric.ec.BCECPublicKey
import org.bouncycastle.jce.ECNamedCurveTable
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.jce.spec.ECNamedCurveParameterSpec
import org.bouncycastle.util.BigIntegers
import java.math.BigInteger
import java.nio.file.Files
import java.nio.file.Path
import java.security.KeyFactory
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.MessageDigest
import java.security.SecureRandom
import java.security.Security
import java.security.Signature
import java.security.interfaces.RSAPrivateCrtKey
import java.security.interfaces.RSAPublicKey as JcaRsaPublicKey
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.RSAPrivateCrtKeySpec
import java.security.spec.RSAPublicKeySpec
import java.security.spec.X509EncodedKeySpec
import java.util.Base64
import java.util.UUID
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

enum class SymmetricAlgorithm { AES, DES, SM4 }

enum class AsymmetricAlgorithm { RSA, SM2 }

enum class DigestAlgorithm { MD5, SHA1, SHA256, SHA384, SHA512, SM3 }

enum class BaseAlgorithm { Base64, Base32 }

enum class CryptoTab { Symmetric, Asymmetric, Digest, Base, Random }

enum class RandomKind { Uuid, Digits, String, Password }

data class AsymmetricKeyPair(val publicKey: String, val privateKey: String)

class CryptoException(val code: String, message: String) : RuntimeException(message)

object CryptoEngine {
    const val SAMPLE_PLAIN = "MooTool"
    const val SAMPLE_KEY = "1234567890abcdef"
    const val MAX_TEXT_BYTES = 2 * 1024 * 1024
    const val MAX_FILE_BYTES = 256L * 1024L * 1024L
    const val MIN_RANDOM_LENGTH = 1
    const val MAX_RANDOM_LENGTH = 4096
    private const val RSA_BITS = 2048
    private val sm2UserId = "1234567812345678".toByteArray(Charsets.UTF_8)
    private val digitsAlphabet = "0123456789"
    private val stringAlphabet = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
    private val passwordCategories = listOf(
        "abcdefghijklmnopqrstuvwxyz",
        "ABCDEFGHIJKLMNOPQRSTUVWXYZ",
        "0123456789",
        "`~!@#\$%^&*()_+-=[]{};':,./<>?"
    )
    private val random = SecureRandom()
    private val base64 = Base64.getDecoder()
    private val base64Encoder = Base64.getEncoder()
    private val hexPair = Regex("^[0-9a-fA-F]{2}$")

    init {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(BouncyCastleProvider())
        }
    }

    fun symmetricEncrypt(algorithm: SymmetricAlgorithm, content: String, key: String): String {
        val input = utf8Bytes(content, "plain")
        val secret = symmetricKey(algorithm, key)
        val cipher = symmetricCipher(algorithm)
        cipher.init(Cipher.ENCRYPT_MODE, secret)
        return toHex(cipher.doFinal(input))
    }

    fun symmetricDecrypt(algorithm: SymmetricAlgorithm, cipherHex: String, key: String): String {
        val secret = symmetricKey(algorithm, key)
        val data = parseHex(cipherHex)
        if (data.isEmpty() && cipherHex.isNotBlank()) {
            throw CryptoException("invalid-hex", "Cipher text is not valid hexadecimal")
        }
        val cipher = symmetricCipher(algorithm)
        cipher.init(Cipher.DECRYPT_MODE, secret)
        val output = runCatching { cipher.doFinal(data) }.getOrElse {
            throw CryptoException("decrypt", "Unable to decrypt with the supplied key")
        }
        val text = String(output, Charsets.UTF_8)
        if (text.isEmpty() && cipherHex.isNotBlank()) {
            throw CryptoException("decrypt", "Unable to decrypt with the supplied key")
        }
        return text
    }

    fun digestText(algorithm: DigestAlgorithm, content: String): String {
        val digest = messageDigest(algorithm)
        digest.update(utf8Bytes(content, "plain"))
        return toHex(digest.digest())
    }

    fun digestFile(algorithm: DigestAlgorithm, path: Path): String {
        val size = Files.size(path)
        if (size > MAX_FILE_BYTES) {
            throw CryptoException("too-large", "File exceeds $MAX_FILE_BYTES bytes")
        }
        val digest = messageDigest(algorithm)
        Files.newInputStream(path).use { stream ->
            val buffer = ByteArray(64 * 1024)
            while (true) {
                val read = stream.read(buffer)
                if (read <= 0) break
                digest.update(buffer, 0, read)
            }
        }
        return toHex(digest.digest())
    }

    fun encodeBase(algorithm: BaseAlgorithm, content: String): String {
        val bytes = utf8Bytes(content, "plain")
        return if (algorithm == BaseAlgorithm.Base64) base64Encoder.encodeToString(bytes) else encodeBase32(bytes)
    }

    fun decodeBase(algorithm: BaseAlgorithm, content: String): String {
        val cleaned = content.replace(Regex("\\s+"), "")
        val bytes = if (algorithm == BaseAlgorithm.Base64) {
            runCatching { base64.decode(cleaned) }.getOrElse {
                throw CryptoException("invalid-base64", "Invalid Base64 content")
            }
        } else {
            decodeBase32(cleaned)
        }
        val text = String(bytes, Charsets.UTF_8)
        if (text.isEmpty() && cleaned.isNotEmpty()) {
            throw CryptoException("invalid-base", "Unable to decode as UTF-8 text")
        }
        return text
    }

    fun generateAsymmetricKeyPair(algorithm: AsymmetricAlgorithm, rsaBits: Int = RSA_BITS): AsymmetricKeyPair {
        return if (algorithm == AsymmetricAlgorithm.SM2) generateSm2Pair() else generateRsaPair(rsaBits)
    }

    fun asymmetricEncrypt(algorithm: AsymmetricAlgorithm, content: String, publicKey: String): String {
        val plain = utf8Bytes(content, "plain")
        return if (algorithm == AsymmetricAlgorithm.SM2) {
            encodeBase64(sm2Encrypt(plain, parseSm2Public(publicKey)))
        } else {
            val cipher = rsaCipher()
            cipher.init(Cipher.ENCRYPT_MODE, parseRsaPublic(publicKey))
            encodeBase64(cipher.doFinal(plain))
        }
    }

    fun asymmetricDecrypt(algorithm: AsymmetricAlgorithm, cipherText: String, privateKey: String): String {
        val data = parseBase64(cipherText)
        val plain = if (algorithm == AsymmetricAlgorithm.SM2) {
            sm2Decrypt(data, parseSm2Private(privateKey))
        } else {
            val cipher = rsaCipher()
            cipher.init(Cipher.DECRYPT_MODE, parseRsaPrivate(privateKey))
            runCatching { cipher.doFinal(data) }.getOrElse {
                throw CryptoException("decrypt", "Unable to decrypt with the supplied key")
            }
        }
        return String(plain, Charsets.UTF_8)
    }

    fun privateEncrypt(content: String, privateKey: String): String {
        val cipher = rsaCipher()
        cipher.init(Cipher.ENCRYPT_MODE, parseRsaPrivate(privateKey))
        return encodeBase64(cipher.doFinal(utf8Bytes(content, "plain")))
    }

    fun publicDecrypt(cipherText: String, publicKey: String): String {
        val cipher = rsaCipher()
        cipher.init(Cipher.DECRYPT_MODE, parseRsaPublic(publicKey))
        val plain = runCatching { cipher.doFinal(parseBase64(cipherText)) }.getOrElse {
            throw CryptoException("decrypt", "Unable to decrypt with the supplied key")
        }
        return String(plain, Charsets.UTF_8)
    }

    fun signContent(algorithm: AsymmetricAlgorithm, content: String, privateKey: String, publicKey: String = ""): String {
        val data = utf8Bytes(content, "plain")
        return if (algorithm == AsymmetricAlgorithm.SM2) {
            val signer = SM2Signer()
            signer.init(true, ParametersWithID(ParametersWithRandom(parseSm2Private(privateKey), random), sm2UserId))
            signer.update(data, 0, data.size)
            encodeBase64(signer.generateSignature())
        } else {
            val signature = Signature.getInstance("SHA256withRSA", BouncyCastleProvider.PROVIDER_NAME)
            signature.initSign(parseRsaPrivate(privateKey))
            signature.update(data)
            encodeBase64(signature.sign())
        }
    }

    fun verifySignature(algorithm: AsymmetricAlgorithm, content: String, signature: String, publicKey: String): Boolean {
        val data = utf8Bytes(content, "plain")
        return if (algorithm == AsymmetricAlgorithm.SM2) {
            val signer = SM2Signer()
            signer.init(false, ParametersWithID(parseSm2Public(publicKey), sm2UserId))
            signer.update(data, 0, data.size)
            runCatching { signer.verifySignature(parseBase64(signature)) }.getOrDefault(false)
        } else {
            val verifier = Signature.getInstance("SHA256withRSA", BouncyCastleProvider.PROVIDER_NAME)
            verifier.initVerify(parseRsaPublic(publicKey))
            verifier.update(data)
            runCatching { verifier.verify(parseBase64(signature)) }.getOrDefault(false)
        }
    }

    fun randomUuid(): String = UUID.randomUUID().toString()

    fun randomDigits(length: Int): String = randomFromAlphabet(digitsAlphabet, length)

    fun randomString(length: Int): String = randomFromAlphabet(stringAlphabet, length)

    fun randomPassword(length: Int): String {
        val size = normalizeLength(length)
        val required = passwordCategories.take(minOf(size, passwordCategories.size)).map { category ->
            randomFromAlphabet(category, 1)
        }
        val rest = randomFromAlphabet(passwordCategories.joinToString(""), maxOf(0, size - required.size))
            .map { it.toString() }
        return shuffle(required + rest).joinToString("")
    }

    internal fun normalizeKey(key: String, length: Int): String {
        val points = key.codePoints().toArray()
        val selected = if (points.size >= length) {
            points.copyOf(length)
        } else {
            IntArray(length).also { padded ->
                points.copyInto(padded)
                for (index in points.size until length) padded[index] = '0'.code
            }
        }
        return String(selected, 0, selected.size)
    }

    private fun symmetricKey(algorithm: SymmetricAlgorithm, key: String): SecretKeySpec {
        val charLength = if (algorithm == SymmetricAlgorithm.DES) 8 else 16
        val normalized = normalizeKey(key, charLength)
        val bytes = normalized.toByteArray(Charsets.UTF_8)
        val expected = if (algorithm == SymmetricAlgorithm.DES) 8 else 16
        if (bytes.size != expected) {
            throw CryptoException(
                "invalid-key",
                "Key UTF-8 length is ${bytes.size} after character trim/pad to $charLength; $algorithm requires $expected bytes"
            )
        }
        val jca = when (algorithm) {
            SymmetricAlgorithm.AES -> "AES"
            SymmetricAlgorithm.DES -> "DES"
            SymmetricAlgorithm.SM4 -> "SM4"
        }
        return SecretKeySpec(bytes, jca)
    }

    private fun symmetricCipher(algorithm: SymmetricAlgorithm): Cipher = when (algorithm) {
        SymmetricAlgorithm.AES -> Cipher.getInstance("AES/ECB/PKCS5Padding")
        SymmetricAlgorithm.DES -> Cipher.getInstance("DES/ECB/PKCS5Padding")
        SymmetricAlgorithm.SM4 -> Cipher.getInstance("SM4/ECB/PKCS5Padding", BouncyCastleProvider.PROVIDER_NAME)
    }

    private fun messageDigest(algorithm: DigestAlgorithm): MessageDigest = when (algorithm) {
        DigestAlgorithm.MD5 -> MessageDigest.getInstance("MD5")
        DigestAlgorithm.SHA1 -> MessageDigest.getInstance("SHA-1")
        DigestAlgorithm.SHA256 -> MessageDigest.getInstance("SHA-256")
        DigestAlgorithm.SHA384 -> MessageDigest.getInstance("SHA-384")
        DigestAlgorithm.SHA512 -> MessageDigest.getInstance("SHA-512")
        DigestAlgorithm.SM3 -> MessageDigest.getInstance("SM3", BouncyCastleProvider.PROVIDER_NAME)
    }

    private fun generateRsaPair(bits: Int): AsymmetricKeyPair {
        val generator = KeyPairGenerator.getInstance("RSA")
        generator.initialize(bits, random)
        val pair = generator.generateKeyPair()
        return AsymmetricKeyPair(encodeRsaPublic(pair), encodeRsaPrivate(pair))
    }

    private fun encodeRsaPublic(pair: KeyPair): String {
        val publicKey = pair.public as JcaRsaPublicKey
        val encoded = RSAPublicKey(publicKey.modulus, publicKey.publicExponent).encoded
        return encodeBase64(encoded)
    }

    private fun encodeRsaPrivate(pair: KeyPair): String {
        val privateKey = pair.private as RSAPrivateCrtKey
        val encoded = org.bouncycastle.asn1.pkcs.RSAPrivateKey(
            privateKey.modulus,
            privateKey.publicExponent,
            privateKey.privateExponent,
            privateKey.primeP,
            privateKey.primeQ,
            privateKey.primeExponentP,
            privateKey.primeExponentQ,
            privateKey.crtCoefficient
        ).encoded
        return encodeBase64(encoded)
    }

    private fun parseRsaPublic(value: String): java.security.PublicKey {
        val der = parseBase64(value)
        val factory = KeyFactory.getInstance("RSA")
        runCatching {
            val key = RSAPublicKey.getInstance(der)
            return factory.generatePublic(RSAPublicKeySpec(key.modulus, key.publicExponent))
        }
        runCatching {
            SubjectPublicKeyInfo.getInstance(der)
            return factory.generatePublic(X509EncodedKeySpec(der))
        }
        throw CryptoException("invalid-key", "Public key is not PKCS#1 or X.509 RSA DER")
    }

    private fun parseRsaPrivate(value: String): java.security.PrivateKey {
        val der = parseBase64(value)
        val factory = KeyFactory.getInstance("RSA")
        runCatching {
            val key = RSAPrivateKey.getInstance(der)
            return factory.generatePrivate(
                RSAPrivateCrtKeySpec(
                    key.modulus,
                    key.publicExponent,
                    key.privateExponent,
                    key.prime1,
                    key.prime2,
                    key.exponent1,
                    key.exponent2,
                    key.coefficient
                )
            )
        }
        runCatching { return factory.generatePrivate(PKCS8EncodedKeySpec(der)) }
        throw CryptoException("invalid-key", "Private key is not PKCS#1 or PKCS#8 RSA DER")
    }

    private fun rsaCipher(): Cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding", BouncyCastleProvider.PROVIDER_NAME)

    private fun generateSm2Pair(): AsymmetricKeyPair {
        val spec = sm2Spec()
        val generator = KeyPairGenerator.getInstance("EC", BouncyCastleProvider.PROVIDER_NAME)
        generator.initialize(spec, random)
        val pair = generator.generateKeyPair()
        val publicKey = pair.public as BCECPublicKey
        val privateKey = pair.private as BCECPrivateKey
        val publicBytes = publicKey.q.getEncoded(false)
        val privateBytes = BigIntegers.asUnsignedByteArray(32, privateKey.d)
        return AsymmetricKeyPair(encodeBase64(publicBytes), encodeBase64(privateBytes))
    }

    private fun parseSm2Public(value: String): ECPublicKeyParameters {
        val raw = parseBase64(value)
        val uncompressed = when {
            raw.size == 65 && raw[0] == 0x04.toByte() -> raw
            raw.size == 64 -> byteArrayOf(0x04) + raw
            else -> throw CryptoException("invalid-key", "SM2 public key must be uncompressed EC point bytes")
        }
        val spec = sm2Spec()
        val point = spec.curve.decodePoint(uncompressed)
        val params = ECDomainParameters(spec.curve, spec.g, spec.n, spec.h, spec.seed)
        return ECPublicKeyParameters(point, params)
    }

    private fun parseSm2Private(value: String): ECPrivateKeyParameters {
        val raw = parseBase64(value)
        if (raw.isEmpty() || raw.size > 32) {
            throw CryptoException("invalid-key", "SM2 private key must be 32-byte scalar")
        }
        val spec = sm2Spec()
        val d = BigInteger(1, raw)
        val params = ECDomainParameters(spec.curve, spec.g, spec.n, spec.h, spec.seed)
        return ECPrivateKeyParameters(d, params)
    }

    private fun sm2Encrypt(plain: ByteArray, publicKey: ECPublicKeyParameters): ByteArray {
        val engine = SM2Engine(SM2Engine.Mode.C1C3C2)
        engine.init(true, ParametersWithRandom(publicKey, random))
        val raw = engine.processBlock(plain, 0, plain.size)
        return if (raw.isNotEmpty() && raw[0] == 0x04.toByte()) raw.copyOfRange(1, raw.size) else raw
    }

    private fun sm2Decrypt(cipher: ByteArray, privateKey: ECPrivateKeyParameters): ByteArray {
        val normalized = normalizeSm2Cipher(cipher)
        val engine = SM2Engine(SM2Engine.Mode.C1C3C2)
        engine.init(false, privateKey)
        return runCatching { engine.processBlock(normalized, 0, normalized.size) }.getOrElse {
            throw CryptoException("decrypt", "Unable to decrypt with the supplied key")
        }
    }

    private fun normalizeSm2Cipher(cipher: ByteArray): ByteArray {
        if (cipher.isEmpty()) throw CryptoException("invalid-cipher", "Cipher text is empty")
        if (cipher[0] == 0x04.toByte() && cipher.size >= 97) return cipher
        if (cipher.size >= 96) return byteArrayOf(0x04) + cipher
        throw CryptoException("invalid-cipher", "SM2 ciphertext is too short")
    }

    private fun sm2Spec(): ECNamedCurveParameterSpec =
        ECNamedCurveTable.getParameterSpec("sm2p256v1") ?: GMNamedCurves.getByName("sm2p256v1").let { params ->
            ECNamedCurveParameterSpec("sm2p256v1", params.curve, params.g, params.n, params.h, params.seed)
        }

    private fun randomFromAlphabet(alphabet: String, length: Int): String {
        val size = normalizeLength(length)
        if (size == 0) return ""
        val output = CharArray(size)
        val bound = alphabet.length
        for (index in 0 until size) {
            output[index] = alphabet[random.nextInt(bound)]
        }
        return String(output)
    }

    private fun shuffle(values: List<String>): List<String> {
        val output = values.toMutableList()
        for (index in output.lastIndex downTo 1) {
            val target = random.nextInt(index + 1)
            val current = output[index]
            output[index] = output[target]
            output[target] = current
        }
        return output
    }

    private fun normalizeLength(length: Int): Int {
        if (length < MIN_RANDOM_LENGTH || length > MAX_RANDOM_LENGTH) {
            throw CryptoException("invalid-length", "Length must be between $MIN_RANDOM_LENGTH and $MAX_RANDOM_LENGTH")
        }
        return length
    }

    private fun utf8Bytes(value: String, label: String): ByteArray {
        val bytes = value.toByteArray(Charsets.UTF_8)
        if (bytes.size > MAX_TEXT_BYTES) {
            throw CryptoException("too-large", "$label exceeds $MAX_TEXT_BYTES bytes")
        }
        return bytes
    }

    private fun parseHex(value: String): ByteArray {
        val cleaned = value.replace(Regex("\\s+"), "")
        if (cleaned.isEmpty()) return ByteArray(0)
        if (cleaned.length % 2 != 0 || !cleaned.chunked(2).all { hexPair.matches(it) }) {
            throw CryptoException("invalid-hex", "Invalid hexadecimal content")
        }
        return ByteArray(cleaned.length / 2) { index ->
            cleaned.substring(index * 2, index * 2 + 2).toInt(16).toByte()
        }
    }

    private fun parseBase64(value: String): ByteArray {
        val cleaned = value.replace(Regex("\\s+"), "")
        if (cleaned.isEmpty()) throw CryptoException("invalid-base64", "Key or cipher text is empty")
        return runCatching { base64.decode(cleaned) }.getOrElse {
            throw CryptoException("invalid-base64", "Invalid Base64 content")
        }
    }

    private fun encodeBase64(bytes: ByteArray): String = base64Encoder.encodeToString(bytes)

    private fun toHex(bytes: ByteArray): String = bytes.joinToString("") { byte ->
        (byte.toInt() and 0xff).toString(16).padStart(2, '0')
    }

    private fun encodeBase32(bytes: ByteArray): String {
        if (bytes.isEmpty()) return ""
        val alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567"
        val output = StringBuilder((bytes.size * 8 + 4) / 5)
        var buffer = 0
        var bits = 0
        for (byte in bytes) {
            buffer = (buffer shl 8) or (byte.toInt() and 0xff)
            bits += 8
            while (bits >= 5) {
                bits -= 5
                output.append(alphabet[(buffer shr bits) and 0x1f])
            }
        }
        if (bits > 0) output.append(alphabet[(buffer shl (5 - bits)) and 0x1f])
        return output.toString()
    }

    private fun decodeBase32(value: String): ByteArray {
        val alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567"
        val cleaned = value.trim().replace("=", "").uppercase()
        if (cleaned.isEmpty()) return ByteArray(0)
        val output = ArrayList<Byte>((cleaned.length * 5) / 8)
        var buffer = 0
        var bits = 0
        for (char in cleaned) {
            val index = alphabet.indexOf(char)
            if (index < 0) throw CryptoException("invalid-base32", "Invalid Base32 content")
            buffer = (buffer shl 5) or index
            bits += 5
            if (bits >= 8) {
                bits -= 8
                output += ((buffer shr bits) and 0xff).toByte()
            }
        }
        return output.toByteArray()
    }
}
