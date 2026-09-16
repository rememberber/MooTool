import Foundation

/// RSA PKCS#1 type 1 private-encrypt / public-decrypt, matching Electron `node-forge` semantics via system OpenSSL.
enum RSAOpenSSLBridge {
    static func privateEncrypt(plaintext: String, privateKeyBase64: String) throws -> String {
        let directory = try makeWorkspace()
        defer { try? FileManager.default.removeItem(at: directory) }
        let privateDER = try derData(from: privateKeyBase64)
        let privateDERFile = directory.appendingPathComponent("private.der")
        let privatePEM = directory.appendingPathComponent("private.pem")
        let plainFile = directory.appendingPathComponent("plain.txt")
        let cipherFile = directory.appendingPathComponent("cipher.bin")
        try privateDER.write(to: privateDERFile)
        try Data(plaintext.utf8).write(to: plainFile)
        try runOpenSSL(["rsa", "-inform", "DER", "-in", privateDERFile.path, "-out", privatePEM.path])
        try runOpenSSL(["rsautl", "-sign", "-inkey", privatePEM.path, "-in", plainFile.path, "-out", cipherFile.path])
        guard FileManager.default.fileExists(atPath: cipherFile.path) else { throw ToolError("RSA 私钥加密失败。") }
        return try Data(contentsOf: cipherFile).base64EncodedString()
    }

    static func publicDecrypt(ciphertextBase64: String, publicKeyBase64: String) throws -> String {
        let directory = try makeWorkspace()
        defer { try? FileManager.default.removeItem(at: directory) }
        let publicDER = try derData(from: publicKeyBase64)
        let publicDERFile = directory.appendingPathComponent("public.der")
        let publicPEM = directory.appendingPathComponent("public.pem")
        let cipherFile = directory.appendingPathComponent("cipher.bin")
        let plainFile = directory.appendingPathComponent("plain.txt")
        try publicDER.write(to: publicDERFile)
        guard let cipher = Data(base64Encoded: ciphertextBase64.replacingOccurrences(of: "\\s", with: "", options: .regularExpression)) else {
            throw ToolError("密文需为 Base64。")
        }
        try cipher.write(to: cipherFile)
        try importPublicDER(publicDER, pemPath: publicPEM)
        try runOpenSSL(["rsautl", "-verify", "-pubin", "-inkey", publicPEM.path, "-in", cipherFile.path, "-out", plainFile.path])
        guard FileManager.default.fileExists(atPath: plainFile.path) else { throw ToolError("RSA 公钥解密失败。") }
        guard let text = String(data: try Data(contentsOf: plainFile), encoding: .utf8) else {
            throw ToolError("解密结果不是 UTF-8 文本。")
        }
        return text
    }

    private static func derData(from base64: String) throws -> Data {
        let cleaned = base64.replacingOccurrences(of: "\\s", with: "", options: .regularExpression)
        guard let data = Data(base64Encoded: cleaned), !data.isEmpty else { throw ToolError("密钥需为 Base64 DER。") }
        return data
    }

    private static func importPublicDER(_ der: Data, pemPath: URL) throws {
        let derFile = pemPath.deletingLastPathComponent().appendingPathComponent("public-input.der")
        try der.write(to: derFile)
        do {
            try runOpenSSL(["pkey", "-pubin", "-inform", "DER", "-in", derFile.path, "-out", pemPath.path])
            return
        } catch {}
        try runOpenSSL(["rsa", "-RSAPublicKey_in", "-inform", "DER", "-in", derFile.path, "-out", pemPath.path])
    }

    private static func runOpenSSL(_ arguments: [String]) throws {
        let output = try ProcessRunner.runSync(executable: "/usr/bin/openssl", arguments: arguments).lowercased()
        if output.contains("unable to") || output.contains("error:") || output.contains("bad decrypt") {
            throw ToolError(output.trimmingCharacters(in: .whitespacesAndNewlines))
        }
    }

    private static func makeWorkspace() throws -> URL {
        let directory = FileManager.default.temporaryDirectory
            .appendingPathComponent(Product.id + "-rsa-" + UUID().uuidString)
        try FileManager.default.createDirectory(at: directory, withIntermediateDirectories: true, attributes: [.posixPermissions: 0o700])
        return directory
    }
}
