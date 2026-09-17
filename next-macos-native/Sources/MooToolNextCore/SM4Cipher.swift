import Foundation

enum SM4Cipher {
    enum Operation { case encrypt, decrypt }

    static func ecb(operation: Operation, data: Data, key: Data, language: AppLanguage = AppLocalization.preferredLanguage()) throws -> Data {
        guard key.count == 16 else { throw err("crypto.error.sm4KeyInvalid", language) }
        let blocks = pkcs7Pad(data, blockSize: 16, encrypt: operation == .encrypt)
        var output = Data()
        output.reserveCapacity(blocks.count)
        let roundKeys = operation == .encrypt ? expandEncryptKey(key) : expandDecryptKey(key)
        for offset in stride(from: 0, to: blocks.count, by: 16) {
            let block = blocks.subdata(in: offset..<(offset + 16))
            output.append(cryptBlock(block, roundKeys: roundKeys))
        }
        if operation == .decrypt {
            return try pkcs7Unpad(output, blockSize: 16, language: language)
        }
        return output
    }

    private static func err(_ key: String, _ language: AppLanguage) -> ToolError {
        ToolError(AppLocalization.string(key, language: language))
    }

    private static func pkcs7Pad(_ data: Data, blockSize: Int, encrypt: Bool) -> Data {
        if !encrypt { return data }
        let padding = blockSize - (data.count % blockSize)
        return data + Data(repeating: UInt8(padding), count: padding)
    }

    private static func pkcs7Unpad(_ data: Data, blockSize: Int, language: AppLanguage) throws -> Data {
        guard let last = data.last, last > 0, last <= blockSize, data.count >= last else {
            throw err("crypto.error.sm4PaddingInvalid", language)
        }
        let pad = Int(last)
        guard data.suffix(pad).allSatisfy({ $0 == last }) else { throw err("crypto.error.sm4PaddingInvalid", language) }
        return data.dropLast(pad)
    }

    private static func cryptBlock(_ block: Data, roundKeys: [UInt32]) -> Data {
        var x0 = block.uint32(at: 0)
        var x1 = block.uint32(at: 4)
        var x2 = block.uint32(at: 8)
        var x3 = block.uint32(at: 12)
        for index in 0..<32 {
            let temp = x1 ^ x2 ^ x3 ^ roundKeys[index]
            let t = tau(l(temp))
            x0 ^= t
            (x0, x1, x2, x3) = (x1, x2, x3, x0)
        }
        return Data.uint32Bytes(x3, x2, x1, x0)
    }

    private static func l(_ value: UInt32) -> UInt32 {
        value ^ rotateLeft(value, 2) ^ rotateLeft(value, 10) ^ rotateLeft(value, 18) ^ rotateLeft(value, 24)
    }

    private static func lPrime(_ value: UInt32) -> UInt32 {
        value ^ rotateLeft(value, 13) ^ rotateLeft(value, 23)
    }

    private static func tau(_ value: UInt32) -> UInt32 {
        let bytes = [
            sbox[Int((value >> 24) & 0xFF)],
            sbox[Int((value >> 16) & 0xFF)],
            sbox[Int((value >> 8) & 0xFF)],
            sbox[Int(value & 0xFF)]
        ]
        return UInt32(bytes[0]) << 24 | UInt32(bytes[1]) << 16 | UInt32(bytes[2]) << 8 | UInt32(bytes[3])
    }

    private static func rotateLeft(_ value: UInt32, _ bits: UInt32) -> UInt32 {
        (value << bits) | (value >> (32 - bits))
    }

    private static func expandEncryptKey(_ key: Data) -> [UInt32] {
        let mk = (0..<4).map { key.uint32(at: $0 * 4) }
        var k = (0..<4).map { mk[$0] ^ fk[$0] }
        var roundKeys = [UInt32](repeating: 0, count: 32)
        for index in 0..<32 {
            let next = k[0] ^ lPrime(tau(k[1] ^ k[2] ^ k[3] ^ ck[index]))
            k.append(next)
            k.removeFirst()
            roundKeys[index] = next
        }
        return roundKeys
    }

    private static func expandDecryptKey(_ key: Data) -> [UInt32] {
        Array(expandEncryptKey(key).reversed())
    }

    private static let fk: [UInt32] = [0xA3B1BAC6, 0x56AA3350, 0x677D9197, 0xB27022DC]
    private static let ck: [UInt32] = [
        0x00070E15, 0x1C232A31, 0x383F464D, 0x545B6269,
        0x70777E85, 0x8C939AA1, 0xA8AFB6BD, 0xC4CBD2D9,
        0xE0E7EEF5, 0xFC030A11, 0x181F262D, 0x343B4249,
        0x50575E65, 0x6C737A81, 0x888F969D, 0xA4ABB2B9,
        0xC0C7CED5, 0xDCE3EAF1, 0xF8FF060D, 0x141B2229,
        0x30373E45, 0x4C535A61, 0x686F767D, 0x848B9299,
        0xA0A7AEB5, 0xBCC3CAD1, 0xD8DFE6ED, 0xF4FB0209,
        0x10171E25, 0x2C333A41, 0x484F565D, 0x646B7279
    ]
    private static let sbox: [UInt8] = [
        0xD6, 0x90, 0xE9, 0xFE, 0xCC, 0xE1, 0x3D, 0xB7, 0x16, 0xB6, 0x14, 0xC2, 0x28, 0xFB, 0x2C, 0x05,
        0x2B, 0x67, 0x9A, 0x76, 0x2A, 0xBE, 0x04, 0xC3, 0xAA, 0x44, 0x13, 0x26, 0x49, 0x86, 0x06, 0x99,
        0x9C, 0x42, 0x50, 0xF4, 0x91, 0xEF, 0x98, 0x7A, 0x33, 0x54, 0x0B, 0x43, 0xED, 0xCF, 0xAC, 0x62,
        0xE4, 0xB1, 0x1C, 0xA9, 0xC9, 0x08, 0xE8, 0x95, 0x80, 0xDF, 0x94, 0xFA, 0x75, 0x8F, 0x3F, 0xA6,
        0x47, 0x07, 0xA7, 0xFC, 0xF3, 0x73, 0x17, 0xBA, 0x83, 0x59, 0x3C, 0x19, 0xE6, 0x85, 0x4F, 0xA8,
        0x68, 0x6B, 0x81, 0xB2, 0x71, 0x64, 0xDA, 0x8B, 0xF8, 0xEB, 0x0F, 0x4B, 0x70, 0x56, 0x9D, 0x35,
        0x1E, 0x24, 0x0E, 0x5E, 0x63, 0x58, 0xD1, 0xA2, 0x25, 0x22, 0x7C, 0x3B, 0x01, 0x21, 0x78, 0x87,
        0xD4, 0x00, 0x46, 0x57, 0x9F, 0xD3, 0x27, 0x52, 0x4C, 0x36, 0x02, 0xE7, 0xA0, 0xC4, 0xC8, 0x9E,
        0xEA, 0xBF, 0x8A, 0xD2, 0x40, 0xC7, 0x38, 0xB5, 0xA3, 0xF7, 0xF2, 0xCE, 0xF9, 0x61, 0x15, 0xA1,
        0xE0, 0xAE, 0x5D, 0xA4, 0x9B, 0x34, 0x1A, 0x55, 0xAD, 0x93, 0x32, 0x30, 0xF5, 0x8C, 0xB1, 0xE3,
        0x1D, 0xF6, 0xE2, 0x2E, 0x82, 0x66, 0xCA, 0x60, 0xC0, 0x29, 0x23, 0xAB, 0x0D, 0x53, 0x4E, 0x6F,
        0xD5, 0xDB, 0x37, 0x45, 0xDE, 0xFD, 0x8E, 0x2F, 0x03, 0xFF, 0x6A, 0x72, 0x6D, 0x6C, 0x5B, 0x51,
        0x8D, 0x1B, 0xAF, 0x92, 0xBB, 0xDD, 0xBC, 0x7F, 0x11, 0xD9, 0x5C, 0x41, 0x1F, 0x10, 0x5A, 0xD8,
        0x0A, 0xC1, 0x31, 0x88, 0xA5, 0xCD, 0x7B, 0xBD, 0x2D, 0x74, 0xD0, 0x12, 0xB8, 0xE5, 0xB4, 0xB0,
        0x89, 0x69, 0x97, 0x4A, 0x0C, 0x96, 0x77, 0x7E, 0x65, 0xB9, 0xF1, 0x09, 0xC5, 0x6E, 0xC6, 0x84,
        0x18, 0xF0, 0x7D, 0xEC, 0x3A, 0xDC, 0x4D, 0x20, 0x79, 0xEE, 0x5F, 0x3E, 0xD7, 0xCB, 0x39, 0x48
    ]
}

private extension Data {
    func uint32(at offset: Int) -> UInt32 {
        let b0 = UInt32(self[offset])
        let b1 = UInt32(self[offset + 1])
        let b2 = UInt32(self[offset + 2])
        let b3 = UInt32(self[offset + 3])
        return (b0 << 24) | (b1 << 16) | (b2 << 8) | b3
    }

    static func uint32Bytes(_ values: UInt32...) -> Data {
        var data = Data()
        for value in values {
            data.append(UInt8((value >> 24) & 0xFF))
            data.append(UInt8((value >> 16) & 0xFF))
            data.append(UInt8((value >> 8) & 0xFF))
            data.append(UInt8(value & 0xFF))
        }
        return data
    }
}
