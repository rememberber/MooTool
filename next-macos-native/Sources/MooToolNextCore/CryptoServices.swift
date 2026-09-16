import Foundation
import Security
import CryptoKit

public enum CryptoServices {
    public static func sm3(_ text: String) -> String { SM3Digest.hash(text) }

    public static func rsaGenerateKeyPair(
        bits: Int = 2048,
        language: AppLanguage = AppLocalization.preferredLanguage()
    ) throws -> (publicKey: String, privateKey: String) {
        guard bits == 2048 || bits == 3072 || bits == 4096 else { throw err("crypto.error.rsaKeyBits", language) }
        var error: Unmanaged<CFError>?
        let attributes: [String: Any] = [
            kSecAttrKeyType as String: kSecAttrKeyTypeRSA,
            kSecAttrKeySizeInBits as String: bits,
            kSecAttrIsPermanent as String: false
        ]
        guard let privateKey = SecKeyCreateRandomKey(attributes as CFDictionary, &error),
              let publicKey = SecKeyCopyPublicKey(privateKey) else { throw ToolError(rsaError(error, language: language)) }
        return (try exportKey(publicKey, class: .public), try exportKey(privateKey, class: .private))
    }

    public static func rsaEncrypt(
        _ plaintext: String,
        publicKeyBase64: String,
        language: AppLanguage = AppLocalization.preferredLanguage()
    ) throws -> String {
        let key = try importKey(publicKeyBase64, class: .public, language: language)
        let algorithm = SecKeyAlgorithm.rsaEncryptionPKCS1
        guard SecKeyIsAlgorithmSupported(key, .encrypt, algorithm) else { throw err("crypto.error.rsaEncryptUnsupported", language) }
        var error: Unmanaged<CFError>?
        guard let cipher = SecKeyCreateEncryptedData(key, algorithm, Data(plaintext.utf8) as CFData, &error) as Data? else {
            throw ToolError(rsaError(error, language: language))
        }
        return cipher.base64EncodedString()
    }

    public static func rsaDecrypt(
        _ ciphertextBase64: String,
        privateKeyBase64: String,
        language: AppLanguage = AppLocalization.preferredLanguage()
    ) throws -> String {
        let key = try importKey(privateKeyBase64, class: .private, language: language)
        let algorithm = SecKeyAlgorithm.rsaEncryptionPKCS1
        guard SecKeyIsAlgorithmSupported(key, .decrypt, algorithm) else { throw err("crypto.error.rsaDecryptUnsupported", language) }
        guard let cipher = Data(base64Encoded: ciphertextBase64.replacingOccurrences(of: "\\s", with: "", options: .regularExpression)) else {
            throw err("crypto.error.cipherBase64", language)
        }
        var error: Unmanaged<CFError>?
        guard let plain = SecKeyCreateDecryptedData(key, algorithm, cipher as CFData, &error) as Data? else {
            throw ToolError(rsaError(error, language: language))
        }
        guard let text = String(data: plain, encoding: .utf8) else { throw err("crypto.error.decryptNotUtf8", language) }
        return text
    }

    public static func rsaSign(
        _ content: String,
        privateKeyBase64: String,
        language: AppLanguage = AppLocalization.preferredLanguage()
    ) throws -> String {
        let key = try importKey(privateKeyBase64, class: .private, language: language)
        let algorithm = SecKeyAlgorithm.rsaSignatureMessagePKCS1v15SHA256
        guard SecKeyIsAlgorithmSupported(key, .sign, algorithm) else { throw err("crypto.error.rsaSignUnsupported", language) }
        var error: Unmanaged<CFError>?
        guard let signature = SecKeyCreateSignature(key, algorithm, Data(content.utf8) as CFData, &error) as Data? else {
            throw ToolError(rsaError(error, language: language))
        }
        return signature.base64EncodedString()
    }

    public static func sm2GenerateKeyPair(language: AppLanguage = AppLocalization.preferredLanguage()) throws -> (publicKey: String, privateKey: String) {
        try SM2JSCrypto.generateKeyPair(language: language)
    }

    public static func sm2Encrypt(
        _ plaintext: String,
        publicKeyBase64: String,
        language: AppLanguage = AppLocalization.preferredLanguage()
    ) throws -> String {
        try SM2JSCrypto.encrypt(plaintext: plaintext, publicKeyBase64: publicKeyBase64, language: language)
    }

    public static func sm2Decrypt(
        _ ciphertextBase64: String,
        privateKeyBase64: String,
        language: AppLanguage = AppLocalization.preferredLanguage()
    ) throws -> String {
        try SM2JSCrypto.decrypt(ciphertextBase64: ciphertextBase64, privateKeyBase64: privateKeyBase64, language: language)
    }

    public static func sm2Sign(
        _ content: String,
        privateKeyBase64: String,
        language: AppLanguage = AppLocalization.preferredLanguage()
    ) throws -> String {
        try SM2JSCrypto.sign(content: content, privateKeyBase64: privateKeyBase64, language: language)
    }

    public static func sm2Verify(
        _ content: String,
        signatureBase64: String,
        publicKeyBase64: String,
        language: AppLanguage = AppLocalization.preferredLanguage()
    ) throws -> String {
        let pass = AppLocalization.string("crypto.verify.pass", language: language)
        let fail = AppLocalization.string("crypto.verify.fail", language: language)
        return try SM2JSCrypto.verify(content: content, signatureBase64: signatureBase64, publicKeyBase64: publicKeyBase64, language: language) ? pass : fail
    }

    public static func rsaPrivateEncrypt(
        _ plaintext: String,
        privateKeyBase64: String,
        language: AppLanguage = AppLocalization.preferredLanguage()
    ) throws -> String {
        try RSAOpenSSLBridge.privateEncrypt(plaintext: plaintext, privateKeyBase64: privateKeyBase64, language: language)
    }

    public static func rsaPublicDecrypt(
        _ ciphertextBase64: String,
        publicKeyBase64: String,
        language: AppLanguage = AppLocalization.preferredLanguage()
    ) throws -> String {
        try RSAOpenSSLBridge.publicDecrypt(ciphertextBase64: ciphertextBase64, publicKeyBase64: publicKeyBase64, language: language)
    }

    public static func rsaVerify(
        _ content: String,
        signatureBase64: String,
        publicKeyBase64: String,
        language: AppLanguage = AppLocalization.preferredLanguage()
    ) throws -> String {
        let pass = AppLocalization.string("crypto.verify.pass", language: language)
        let fail = AppLocalization.string("crypto.verify.fail", language: language)
        let key = try importKey(publicKeyBase64, class: .public, language: language)
        let algorithm = SecKeyAlgorithm.rsaSignatureMessagePKCS1v15SHA256
        guard let signature = Data(base64Encoded: signatureBase64.replacingOccurrences(of: "\\s", with: "", options: .regularExpression)) else {
            throw err("crypto.error.signatureBase64", language)
        }
        var error: Unmanaged<CFError>?
        let ok = SecKeyVerifySignature(key, algorithm, Data(content.utf8) as CFData, signature as CFData, &error)
        if ok { return pass }
        if let error { throw ToolError(rsaError(error, language: language)) }
        return fail
    }

    private enum KeyClass { case `public`, `private` }

    private static func err(_ key: String, _ language: AppLanguage) -> ToolError {
        ToolError(AppLocalization.string(key, language: language))
    }

    private static func exportKey(_ key: SecKey, class: KeyClass) throws -> String {
        var error: Unmanaged<CFError>?
        guard let data = SecKeyCopyExternalRepresentation(key, &error) as Data? else {
            throw ToolError(rsaError(error, language: AppLocalization.preferredLanguage()))
        }
        return data.base64EncodedString()
    }

    private static func importKey(_ base64: String, class: KeyClass, language: AppLanguage) throws -> SecKey {
        let cleaned = base64.replacingOccurrences(of: "\\s", with: "", options: .regularExpression)
        guard let data = Data(base64Encoded: cleaned), !data.isEmpty else { throw err("crypto.error.keyBase64Der", language) }
        let keyClass = `class` == .public ? kSecAttrKeyClassPublic : kSecAttrKeyClassPrivate
        for bits in [4096, 3072, 2048] {
            let attributes: [String: Any] = [
                kSecAttrKeyType as String: kSecAttrKeyTypeRSA,
                kSecAttrKeyClass as String: keyClass,
                kSecAttrKeySizeInBits as String: bits
            ]
            var error: Unmanaged<CFError>?
            if let key = SecKeyCreateWithData(data as CFData, attributes as CFDictionary, &error) { return key }
        }
        throw err("crypto.error.rsaImportFailed", language)
    }

    private static func rsaError(_ error: Unmanaged<CFError>?, language: AppLanguage) -> String {
        if let error { return CFErrorCopyDescription(error.takeRetainedValue()) as String? ?? AppLocalization.string("crypto.error.rsaOperationFailed", language: language) }
        return AppLocalization.string("crypto.error.rsaOperationFailed", language: language)
    }
}
