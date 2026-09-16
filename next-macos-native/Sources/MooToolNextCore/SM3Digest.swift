import Foundation

/// GM/T 0004-2012 SM3 digest (hex output).
enum SM3Digest {
    static func hash(_ data: Data) -> String { digest(data).hex }
    static func hash(_ text: String) -> String { hash(Data(text.utf8)) }

    private static func digest(_ data: Data) -> Data {
        var message = data
        let bitLength = UInt64(message.count) * 8
        message.append(0x80)
        while (message.count % 64) != 56 { message.append(0) }
        var length = bitLength.bigEndian
        message.append(contentsOf: withUnsafeBytes(of: length) { Array($0) })
        var state = [UInt32](repeating: 0, count: 8)
        state[0] = 0x7380166f; state[1] = 0x4914b2b9; state[2] = 0x172442d7; state[3] = 0xda8a0600
        state[4] = 0xa96f30bc; state[5] = 0x163138aa; state[6] = 0xe38dee4d; state[7] = 0xb0fb0e4e
        for offset in stride(from: 0, to: message.count, by: 64) {
            compress(block: ArraySlice(message[offset..<offset + 64]), state: &state)
        }
        var output = Data()
        for value in state { output.append(contentsOf: withUnsafeBytes(of: value.bigEndian) { Array($0) }) }
        return output
    }

    private static func compress(block: ArraySlice<UInt8>, state: inout [UInt32]) {
        var w = [UInt32](repeating: 0, count: 68)
        for index in 0..<16 {
            let start = block.startIndex + index * 4
            w[index] = UInt32(block[start]) << 24 | UInt32(block[start + 1]) << 16 | UInt32(block[start + 2]) << 8 | UInt32(block[start + 3])
        }
        for index in 16..<68 { w[index] = p1(w[index - 16] ^ w[index - 9] ^ rotateLeft(w[index - 3], 15)) ^ rotateLeft(w[index - 13], 7) ^ w[index - 6] }
        var w1 = (0..<64).map { j in w[j] ^ w[j + 4] }
        var a = state[0], b = state[1], c = state[2], d = state[3], e = state[4], f = state[5], g = state[6], h = state[7]
        for index in 0..<64 {
            let tj: UInt32 = index < 16 ? 0x79cc4519 : 0x7a879d8a
            let ss1 = rotateLeft(rotateLeft(a, 12) &+ e &+ rotateLeft(tj, UInt32(index % 32)), 7)
            let ss2 = ss1 ^ rotateLeft(a, 12)
            let tt1 = (index < 16 ? ff0(a, b, c) : ff1(a, b, c)) &+ d &+ ss2 &+ w1[index]
            let tt2 = (index < 16 ? gg0(e, f, g) : gg1(e, f, g)) &+ h &+ ss1 &+ w[index]
            d = c; c = rotateLeft(b, 9); b = a; a = tt1; h = g; g = rotateLeft(f, 19); f = e; e = p0(tt2)
        }
        state[0] ^= a; state[1] ^= b; state[2] ^= c; state[3] ^= d; state[4] ^= e; state[5] ^= f; state[6] ^= g; state[7] ^= h
    }

    private static func rotateLeft(_ value: UInt32, _ bits: UInt32) -> UInt32 { (value << bits) | (value >> (32 - bits)) }
    private static func p0(_ x: UInt32) -> UInt32 { x ^ rotateLeft(x, 9) ^ rotateLeft(x, 17) }
    private static func p1(_ x: UInt32) -> UInt32 { x ^ rotateLeft(x, 15) ^ rotateLeft(x, 23) }
    private static func ff0(_ x: UInt32, _ y: UInt32, _ z: UInt32) -> UInt32 { x ^ y ^ z }
    private static func ff1(_ x: UInt32, _ y: UInt32, _ z: UInt32) -> UInt32 { (x & y) | (x & z) | (y & z) }
    private static func gg0(_ x: UInt32, _ y: UInt32, _ z: UInt32) -> UInt32 { x ^ y ^ z }
    private static func gg1(_ x: UInt32, _ y: UInt32, _ z: UInt32) -> UInt32 { (x & y) | (~x & z) }
}
