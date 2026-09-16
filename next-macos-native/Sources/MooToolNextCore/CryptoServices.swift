import Foundation
import Security
import CryptoKit

public enum CryptoServices {
    public static func sm3(_ text: String) -> String { SM3Digest.hash(text) }

    public static func rsaGenerateKeyPair(bits: Int = 2048) throws -> (publicKey: String, privateKey: String) {
        guard bits == 2048 || bits == 3072 || bits == 4096 else { throw ToolError("RSA 密钥长度仅支持 2048、3072 或 4096 位。") }
        var error: Unmanaged<CFError>?
        let attributes: [String: Any] = [
            kSecAttrKeyType as String: kSecAttrKeyTypeRSA,
            kSecAttrKeySizeInBits as String: bits,
            kSecAttrIsPermanent as String: false
        ]
        guard let privateKey = SecKeyCreateRandomKey(attributes as CFDictionary, &error),
              let publicKey = SecKeyCopyPublicKey(privateKey) else { throw ToolError(rsaError(error)) }
        return (try exportKey(publicKey, class: .public), try exportKey(privateKey, class: .private))
    }

    public static func rsaEncrypt(_ plaintext: String, publicKeyBase64: String) throws -> String {
        let key = try importKey(publicKeyBase64, class: .public)
        let algorithm = SecKeyAlgorithm.rsaEncryptionPKCS1
        guard SecKeyIsAlgorithmSupported(key, .encrypt, algorithm) else { throw ToolError("当前密钥不支持 RSA PKCS#1 加密。") }
        var error: Unmanaged<CFError>?
        guard let cipher = SecKeyCreateEncryptedData(key, algorithm, Data(plaintext.utf8) as CFData, &error) as Data? else { throw ToolError(rsaError(error)) }
        return cipher.base64EncodedString()
    }

    public static func rsaDecrypt(_ ciphertextBase64: String, privateKeyBase64: String) throws -> String {
        let key = try importKey(privateKeyBase64, class: .private)
        let algorithm = SecKeyAlgorithm.rsaEncryptionPKCS1
        guard SecKeyIsAlgorithmSupported(key, .decrypt, algorithm) else { throw ToolError("当前密钥不支持 RSA PKCS#1 解密。") }
        guard let cipher = Data(base64Encoded: ciphertextBase64.replacingOccurrences(of: "\\s", with: "", options: .regularExpression)) else { throw ToolError("密文需为 Base64。") }
        var error: Unmanaged<CFError>?
        guard let plain = SecKeyCreateDecryptedData(key, algorithm, cipher as CFData, &error) as Data? else { throw ToolError(rsaError(error)) }
        guard let text = String(data: plain, encoding: .utf8) else { throw ToolError("解密结果不是 UTF-8 文本。") }
        return text
    }

    public static func rsaSign(_ content: String, privateKeyBase64: String) throws -> String {
        let key = try importKey(privateKeyBase64, class: .private)
        let algorithm = SecKeyAlgorithm.rsaSignatureMessagePKCS1v15SHA256
        guard SecKeyIsAlgorithmSupported(key, .sign, algorithm) else { throw ToolError("当前密钥不支持 RSA 签名。") }
        var error: Unmanaged<CFError>?
        guard let signature = SecKeyCreateSignature(key, algorithm, Data(content.utf8) as CFData, &error) as Data? else { throw ToolError(rsaError(error)) }
        return signature.base64EncodedString()
    }

    public static func sm2GenerateKeyPair() throws -> (publicKey: String, privateKey: String) {
        try SM2JSCrypto.generateKeyPair()
    }

    public static func sm2Encrypt(_ plaintext: String, publicKeyBase64: String) throws -> String {
        try SM2JSCrypto.encrypt(plaintext: plaintext, publicKeyBase64: publicKeyBase64)
    }

    public static func sm2Decrypt(_ ciphertextBase64: String, privateKeyBase64: String) throws -> String {
        try SM2JSCrypto.decrypt(ciphertextBase64: ciphertextBase64, privateKeyBase64: privateKeyBase64)
    }

    public static func sm2Sign(_ content: String, privateKeyBase64: String) throws -> String {
        try SM2JSCrypto.sign(content: content, privateKeyBase64: privateKeyBase64)
    }

    public static func sm2Verify(_ content: String, signatureBase64: String, publicKeyBase64: String) throws -> String {
        try SM2JSCrypto.verify(content: content, signatureBase64: signatureBase64, publicKeyBase64: publicKeyBase64) ? "验签通过" : "验签失败"
    }

    public static func rsaPrivateEncrypt(_ plaintext: String, privateKeyBase64: String) throws -> String {
        try RSAOpenSSLBridge.privateEncrypt(plaintext: plaintext, privateKeyBase64: privateKeyBase64)
    }

    public static func rsaPublicDecrypt(_ ciphertextBase64: String, publicKeyBase64: String) throws -> String {
        try RSAOpenSSLBridge.publicDecrypt(ciphertextBase64: ciphertextBase64, publicKeyBase64: publicKeyBase64)
    }

    public static func rsaVerify(_ content: String, signatureBase64: String, publicKeyBase64: String) throws -> String {
        let key = try importKey(publicKeyBase64, class: .public)
        let algorithm = SecKeyAlgorithm.rsaSignatureMessagePKCS1v15SHA256
        guard let signature = Data(base64Encoded: signatureBase64.replacingOccurrences(of: "\\s", with: "", options: .regularExpression)) else { throw ToolError("签名需为 Base64。") }
        var error: Unmanaged<CFError>?
        let ok = SecKeyVerifySignature(key, algorithm, Data(content.utf8) as CFData, signature as CFData, &error)
        if ok { return "验签通过" }
        if let error { throw ToolError(rsaError(error)) }
        return "验签失败"
    }

    private enum KeyClass { case `public`, `private` }

    private static func exportKey(_ key: SecKey, class: KeyClass) throws -> String {
        var error: Unmanaged<CFError>?
        guard let data = SecKeyCopyExternalRepresentation(key, &error) as Data? else { throw ToolError(rsaError(error)) }
        return data.base64EncodedString()
    }

    private static func importKey(_ base64: String, class: KeyClass) throws -> SecKey {
        let cleaned = base64.replacingOccurrences(of: "\\s", with: "", options: .regularExpression)
        guard let data = Data(base64Encoded: cleaned), !data.isEmpty else { throw ToolError("密钥需为 Base64 DER。") }
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
        throw ToolError("无法导入 RSA 密钥，请确认 Base64 DER 格式。")
    }

    private static func rsaError(_ error: Unmanaged<CFError>?) -> String {
        if let error { return CFErrorCopyDescription(error.takeRetainedValue()) as String? ?? "RSA 操作失败。" }
        return "RSA 操作失败。"
    }
}
