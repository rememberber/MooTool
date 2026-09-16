import Foundation
import JavaScriptCore

/// SM2 via the same `sm-crypto` bundle as Electron (C1C3C2, default user id for signatures).
enum SM2JSCrypto {
    private static let userID = "1234567812345678"
    private static let lock = NSLock()
    private static var context: JSContext?
    private static var sm2: JSValue?
    private static var loadError: Error?

    private static func err(_ key: String, _ language: AppLanguage) -> ToolError {
        ToolError(AppLocalization.string(key, language: language))
    }

    private static func load(language: AppLanguage) throws -> (JSContext, JSValue) {
        lock.lock()
        defer { lock.unlock() }
        if let context, let sm2 { return (context, sm2) }
        if let loadError { throw loadError }
        guard let url = Bundle.module.url(forResource: "sm2", withExtension: "js") else {
            let error = err("crypto.error.sm2RuntimeMissing", language)
            loadError = error
            throw error
        }
        let ctx = JSContext()!
        ctx.exceptionHandler = { _, value in
            let message = value?.toString() ?? AppLocalization.string("crypto.error.sm2Script", language: language)
            loadError = ToolError(message)
        }
        ctx.evaluateScript(
            """
            (function (g) {
              var crypto = {
                getRandomValues: function (buffer) {
                  for (var i = 0; i < buffer.length; i++) buffer[i] = (Math.random() * 256) | 0;
                  return buffer;
                }
              };
              g.crypto = crypto;
              g.window = g;
              g.globalThis = g;
              g.self = g;
            })(this);
            """)
        let script = try String(contentsOf: url, encoding: .utf8)
        ctx.evaluateScript(script)
        if let loadError { throw loadError }
        guard let value = ctx.objectForKeyedSubscript("sm2"), !value.isUndefined else {
            let error = err("crypto.error.sm2LoadFailed", language)
            loadError = error
            throw error
        }
        context = ctx
        sm2 = value
        return (ctx, value)
    }

    static func generateKeyPair(language: AppLanguage = AppLocalization.preferredLanguage()) throws -> (publicKey: String, privateKey: String) {
        let (_, sm2) = try load(language: language)
        guard let pair = sm2.invokeMethod("generateKeyPairHex", withArguments: [])?.toDictionary(),
              let publicHex = pair["publicKey"] as? String,
              let privateHex = pair["privateKey"] as? String else {
            throw err("crypto.error.sm2GenerateFailed", language)
        }
        return (hexToBase64(publicHex), hexToBase64(privateHex))
    }

    static func encrypt(plaintext: String, publicKeyBase64: String, language: AppLanguage = AppLocalization.preferredLanguage()) throws -> String {
        let (_, sm2) = try load(language: language)
        let hex = sm2.invokeMethod("doEncrypt", withArguments: [plaintext, base64ToHex(publicKeyBase64), 1])?.toString()
        guard let hex, !hex.isEmpty else { throw err("crypto.error.sm2EncryptFailed", language) }
        return hexToBase64(hex)
    }

    static func decrypt(ciphertextBase64: String, privateKeyBase64: String, language: AppLanguage = AppLocalization.preferredLanguage()) throws -> String {
        let (_, sm2) = try load(language: language)
        let plain = sm2.invokeMethod("doDecrypt", withArguments: [base64ToHex(ciphertextBase64), base64ToHex(privateKeyBase64), 1])?.toString()
        guard let plain else { throw err("crypto.error.sm2DecryptFailed", language) }
        return plain
    }

    static func sign(content: String, privateKeyBase64: String, language: AppLanguage = AppLocalization.preferredLanguage()) throws -> String {
        let (ctx, sm2) = try load(language: language)
        let options = JSValue(object: ["hash": true, "der": true, "userId": userID], in: ctx)!
        let signature = sm2.invokeMethod("doSignature", withArguments: [content, base64ToHex(privateKeyBase64), options])?.toString()
        guard let signature, !signature.isEmpty else { throw err("crypto.error.sm2SignFailed", language) }
        return hexToBase64(signature)
    }

    static func verify(
        content: String,
        signatureBase64: String,
        publicKeyBase64: String,
        language: AppLanguage = AppLocalization.preferredLanguage()
    ) throws -> Bool {
        let (ctx, sm2) = try load(language: language)
        let options = JSValue(object: ["hash": true, "der": true], in: ctx)!
        return sm2.invokeMethod(
            "doVerifySignature",
            withArguments: [content, base64ToHex(signatureBase64), base64ToHex(publicKeyBase64), options])?.toBool() ?? false
    }

    private static func base64ToHex(_ base64: String) -> String {
        let cleaned = base64.replacingOccurrences(of: "\\s", with: "", options: .regularExpression)
        guard let data = Data(base64Encoded: cleaned) else { return "" }
        return data.map { String(format: "%02x", $0) }.joined()
    }

    private static func hexToBase64(_ hex: String) -> String {
        let cleaned = hex.replacingOccurrences(of: "\\s", with: "", options: .regularExpression)
        var bytes = Data()
        var index = cleaned.startIndex
        while index < cleaned.endIndex {
            let next = cleaned.index(index, offsetBy: 2, limitedBy: cleaned.endIndex) ?? cleaned.endIndex
            guard next <= cleaned.endIndex, let byte = UInt8(cleaned[index..<next], radix: 16) else { break }
            bytes.append(byte)
            index = next
        }
        return bytes.base64EncodedString()
    }
}
