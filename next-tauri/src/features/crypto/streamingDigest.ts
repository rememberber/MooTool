/** Streaming MD5, SHA-1, and SM3. The Rust file digest uses the same rounds. */

function rotl(value: number, bits: number): number {
  return ((value << bits) | (value >>> (32 - bits))) >>> 0
}

export function digestMd5(bytes: Uint8Array): string {
  const state = Uint32Array.from([0x67452301, 0xefcdab89, 0x98badcfe, 0x10325476])
  const shifts = [
    7, 12, 17, 22, 7, 12, 17, 22, 7, 12, 17, 22, 7, 12, 17, 22,
    5, 9, 14, 20, 5, 9, 14, 20, 5, 9, 14, 20, 5, 9, 14, 20,
    4, 11, 16, 23, 4, 11, 16, 23, 4, 11, 16, 23, 4, 11, 16, 23,
    6, 10, 15, 21, 6, 10, 15, 21, 6, 10, 15, 21, 6, 10, 15, 21
  ]
  const constants = Array.from({ length: 64 }, (_, index) => Math.floor(Math.abs(Math.sin(index + 1)) * 0x100000000) >>> 0)
  const block = new Uint8Array(64)
  const words = new Uint32Array(16)
  let offset = 0
  let length = 0
  const consume = (chunk: Uint8Array) => {
    let cursor = 0
    while (cursor < chunk.length) {
      const count = Math.min(64 - offset, chunk.length - cursor)
      block.set(chunk.subarray(cursor, cursor + count), offset)
      offset += count
      cursor += count
      length += count
      if (offset === 64) {
        md5Block(state, block, words, shifts, constants)
        offset = 0
      }
    }
  }
  consume(bytes)
  const tail = new Uint8Array(64 + (offset >= 56 ? 64 : 0))
  tail.set(block.subarray(0, offset))
  tail[offset] = 0x80
  const bits = BigInt(length) * 8n
  const view = new DataView(tail.buffer)
  view.setUint32(tail.length - 8, Number(bits & 0xffffffffn), true)
  view.setUint32(tail.length - 4, Number(bits >> 32n), true)
  for (let index = 0; index < tail.length; index += 64) md5Block(state, tail.subarray(index, index + 64), words, shifts, constants)
  const output = new Uint8Array(16)
  const outputView = new DataView(output.buffer)
  state.forEach((value, index) => outputView.setUint32(index * 4, value, true))
  return hex(output)
}

function md5Block(state: Uint32Array, block: Uint8Array, words: Uint32Array, shifts: number[], constants: number[]) {
  const view = new DataView(block.buffer, block.byteOffset, 64)
  for (let index = 0; index < 16; index += 1) words[index] = view.getUint32(index * 4, true)
  let [a, b, c, d] = state
  for (let index = 0; index < 64; index += 1) {
    let value = 0
    let word = index
    if (index < 16) value = (b & c) | (~b & d)
    else if (index < 32) {
      value = (d & b) | (~d & c)
      word = (5 * index + 1) % 16
    } else if (index < 48) {
      value = b ^ c ^ d
      word = (3 * index + 5) % 16
    } else {
      value = c ^ (b | ~d)
      word = (7 * index) % 16
    }
    const next = b + rotl((a + value + constants[index] + words[word]) >>> 0, shifts[index])
    a = d
    d = c
    c = b
    b = next >>> 0
  }
  state[0] = (state[0] + a) >>> 0
  state[1] = (state[1] + b) >>> 0
  state[2] = (state[2] + c) >>> 0
  state[3] = (state[3] + d) >>> 0
}

export function digestSha1(bytes: Uint8Array): string {
  const state = Uint32Array.from([0x67452301, 0xefcdab89, 0x98badcfe, 0x10325476, 0xc3d2e1f0])
  return digestBigEndian(bytes, 64, 56, state, (block, values) => {
    const words = new Uint32Array(80)
    const view = new DataView(block.buffer, block.byteOffset, 64)
    for (let index = 0; index < 16; index += 1) words[index] = view.getUint32(index * 4)
    for (let index = 16; index < 80; index += 1) words[index] = rotl(words[index - 3] ^ words[index - 8] ^ words[index - 14] ^ words[index - 16], 1)
    let [a, b, c, d, e] = values
    for (let index = 0; index < 80; index += 1) {
      const constant = index < 20 ? 0x5a827999 : index < 40 ? 0x6ed9eba1 : index < 60 ? 0x8f1bbcdc : 0xca62c1d6
      const mix = index < 20 ? (b & c) | (~b & d) : index < 40 || index >= 60 ? b ^ c ^ d : (b & c) | (b & d) | (c & d)
      const next = (rotl(a, 5) + mix + e + constant + words[index]) >>> 0
      e = d
      d = c
      c = rotl(b, 30)
      b = a
      a = next
    }
    values[0] = (values[0] + a) >>> 0
    values[1] = (values[1] + b) >>> 0
    values[2] = (values[2] + c) >>> 0
    values[3] = (values[3] + d) >>> 0
    values[4] = (values[4] + e) >>> 0
  })
}

export function digestSm3(bytes: Uint8Array): string {
  const state = Uint32Array.from([0x7380166f, 0x4914b2b9, 0x172442d7, 0xda8a0600, 0xa96f30bc, 0x163138aa, 0xe38dee4d, 0xb0fb0e4e])
  return digestBigEndian(bytes, 64, 56, state, (block, values) => {
    const words = new Uint32Array(68)
    const mixed = new Uint32Array(64)
    const view = new DataView(block.buffer, block.byteOffset, 64)
    for (let index = 0; index < 16; index += 1) words[index] = view.getUint32(index * 4)
    for (let index = 16; index < 68; index += 1) {
      const value = words[index - 16] ^ words[index - 9] ^ rotl(words[index - 3], 15)
      words[index] = (p1(value) ^ rotl(words[index - 13], 7) ^ words[index - 6]) >>> 0
    }
    for (let index = 0; index < 64; index += 1) mixed[index] = (words[index] ^ words[index + 4]) >>> 0
    let [a, b, c, d, e, f, g, h] = values
    for (let index = 0; index < 64; index += 1) {
      const constant = index < 16 ? 0x79cc4519 : 0x7a879d8a
      const ss1 = rotl((rotl(a, 12) + e + rotl(constant, index % 32)) >>> 0, 7)
      const ss2 = ss1 ^ rotl(a, 12)
      const ff = index < 16 ? a ^ b ^ c : (a & b) | (a & c) | (b & c)
      const gg = index < 16 ? e ^ f ^ g : (e & f) | (~e & g)
      const tt1 = (ff + d + ss2 + mixed[index]) >>> 0
      const tt2 = (gg + h + ss1 + words[index]) >>> 0
      d = c
      c = rotl(b, 9)
      b = a
      a = tt1
      h = g
      g = rotl(f, 19)
      f = e
      e = p0(tt2)
    }
    for (let index = 0; index < 8; index += 1) values[index] = (values[index] ^ [a, b, c, d, e, f, g, h][index]) >>> 0
  })
}

function p0(value: number): number {
  return (value ^ rotl(value, 9) ^ rotl(value, 17)) >>> 0
}

function p1(value: number): number {
  return (value ^ rotl(value, 15) ^ rotl(value, 23)) >>> 0
}

function digestBigEndian(bytes: Uint8Array, blockSize: number, lengthOffset: number, state: Uint32Array, compress: (block: Uint8Array, state: Uint32Array) => void): string {
  const block = new Uint8Array(blockSize)
  let offset = 0
  let length = 0
  for (const byte of bytes) {
    block[offset] = byte
    offset += 1
    length += 1
    if (offset === blockSize) {
      compress(block, state)
      offset = 0
    }
  }
  const tail = new Uint8Array(blockSize + (offset >= lengthOffset ? blockSize : 0))
  tail.set(block.subarray(0, offset))
  tail[offset] = 0x80
  const view = new DataView(tail.buffer)
  const bits = BigInt(length) * 8n
  view.setUint32(tail.length - 8, Number(bits >> 32n))
  view.setUint32(tail.length - 4, Number(bits & 0xffffffffn))
  for (let index = 0; index < tail.length; index += blockSize) compress(tail.subarray(index, index + blockSize), state)
  const output = new Uint8Array(state.length * 4)
  const outputView = new DataView(output.buffer)
  state.forEach((value, index) => outputView.setUint32(index * 4, value))
  return hex(output)
}

function hex(bytes: Uint8Array): string {
  return [...bytes].map((byte) => byte.toString(16).padStart(2, '0')).join('')
}
