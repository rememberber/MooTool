import Foundation

enum Base32Codec {
    private static let alphabet = Array("ABCDEFGHIJKLMNOPQRSTUVWXYZ234567")

    static func encode(_ data: Data) -> String {
        var bits = 0, value = 0
        var output = ""
        for byte in data {
            value = (value << 8) | Int(byte)
            bits += 8
            while bits >= 5 {
                output.append(alphabet[(value >> (bits - 5)) & 31])
                bits -= 5
            }
        }
        if bits > 0 { output.append(alphabet[(value << (5 - bits)) & 31]) }
        let padding = (8 - output.count % 8) % 8
        return output + String(repeating: "=", count: padding)
    }

    static func decode(_ text: String) throws -> Data {
        let normalized = text.uppercased().filter { !$0.isWhitespace }
        let unpadded = normalized.replacingOccurrences(of: "=+$", with: "", options: .regularExpression)
        guard normalized.range(of: #"^[A-Z2-7]*={0,6}$"#, options: .regularExpression) != nil, !unpadded.contains("=") else {
            throw ToolError("无效 Base32。")
        }
        var bits = 0, value = 0
        var bytes: [UInt8] = []
        for character in unpadded {
            guard let index = alphabet.firstIndex(of: character) else { throw ToolError("无效 Base32。") }
            value = (value << 5) | index
            bits += 5
            if bits >= 8 {
                bytes.append(UInt8((value >> (bits - 8)) & 255))
                bits -= 8
            }
        }
        return Data(bytes)
    }
}
