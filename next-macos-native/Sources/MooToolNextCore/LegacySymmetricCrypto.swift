import Foundation
import CommonCrypto

/// AES / DES / SM4 in ECB + PKCS7, matching Electron `cryptoTools.symmetricEncrypt` key normalization.
public enum LegacySymmetricCrypto {
    public enum Algorithm: String {
        case aesECB = "AES-ECB"
        case desECB = "DES-ECB"
        case sm4ECB = "SM4-ECB"
    }

    public static func encrypt(
        algorithm: Algorithm,
        plaintext: String,
        key: String,
        language: AppLanguage = AppLocalization.preferredLanguage()
    ) throws -> String {
        let data = Data(plaintext.utf8)
        let cipher = try crypt(algorithm: algorithm, operation: CCOperation(kCCEncrypt), data: data, key: key, language: language)
        return cipher.hexLowercased
    }

    public static func decrypt(
        algorithm: Algorithm,
        cipherHex: String,
        key: String,
        language: AppLanguage = AppLocalization.preferredLanguage()
    ) throws -> String {
        let cleaned = cipherHex.replacingOccurrences(of: "\\s", with: "", options: .regularExpression)
        guard cleaned.range(of: "^[0-9a-fA-F]*$", options: .regularExpression) != nil, cleaned.count % 2 == 0 else {
            throw err("crypto.error.cipherHex", language)
        }
        var bytes = Data()
        var index = cleaned.startIndex
        while index < cleaned.endIndex {
            let next = cleaned.index(index, offsetBy: 2)
            guard let byte = UInt8(cleaned[index..<next], radix: 16) else { throw err("crypto.error.cipherHex", language) }
            bytes.append(byte)
            index = next
        }
        let plain = try crypt(algorithm: algorithm, operation: CCOperation(kCCDecrypt), data: bytes, key: key, language: language)
        guard let text = String(data: plain, encoding: .utf8) else {
            throw err("crypto.error.symmetricDecryptFailed", language)
        }
        if text.isEmpty && !bytes.isEmpty && !plain.isEmpty {
            throw err("crypto.error.symmetricDecryptFailed", language)
        }
        return text
    }

    static func normalizeKey(_ key: String, charLength: Int) -> Data {
        let chars = Array(key)
        let normalized: String
        if chars.count >= charLength {
            normalized = String(chars.prefix(charLength))
        } else {
            let padded = key + String(repeating: "0", count: charLength)
            normalized = String(padded.prefix(charLength))
        }
        return Data(normalized.utf8)
    }

    private static func crypt(algorithm: Algorithm, operation: CCOperation, data: Data, key: String, language: AppLanguage) throws -> Data {
        switch algorithm {
        case .aesECB:
            let keyData = normalizeKey(key, charLength: 16)
            guard keyData.count == 16 else { throw err("crypto.error.aesKey16", language) }
            return try runCCCryptor(operation: operation, algorithm: CCAlgorithm(kCCAlgorithmAES), key: keyData, data: data, language: language)
        case .desECB:
            let keyData = normalizeKey(key, charLength: 8)
            guard keyData.count == 8 else { throw err("crypto.error.desKey8", language) }
            return try runCCCryptor(operation: operation, algorithm: CCAlgorithm(kCCAlgorithmDES), key: keyData, data: data, language: language)
        case .sm4ECB:
            let keyData = normalizeKey(key, charLength: 16)
            guard keyData.count == 16 else { throw err("crypto.error.sm4Key16", language) }
            return try SM4Cipher.ecb(operation: operation == kCCEncrypt ? .encrypt : .decrypt, data: data, key: keyData, language: language)
        }
    }

    private static func runCCCryptor(operation: CCOperation, algorithm: CCAlgorithm, key: Data, data: Data, language: AppLanguage) throws -> Data {
        let bufferSize = data.count + kCCBlockSizeAES128
        var output = Data(count: bufferSize)
        var moved = 0
        let status = output.withUnsafeMutableBytes { outBytes in
            data.withUnsafeBytes { dataBytes in
                key.withUnsafeBytes { keyBytes in
                    CCCrypt(
                        operation,
                        algorithm,
                        CCOptions(kCCOptionPKCS7Padding | kCCOptionECBMode),
                        keyBytes.baseAddress, key.count,
                        nil,
                        dataBytes.baseAddress, data.count,
                        outBytes.baseAddress, bufferSize,
                        &moved)
                }
            }
        }
        guard status == kCCSuccess else { throw err("crypto.error.symmetricCryptFailed", language) }
        output.count = moved
        return output
    }

    private static func err(_ key: String, _ language: AppLanguage) -> ToolError {
        ToolError(AppLocalization.string(key, language: language))
    }
}

private extension Data {
    var hexLowercased: String { map { String(format: "%02x", $0) }.joined() }
}
