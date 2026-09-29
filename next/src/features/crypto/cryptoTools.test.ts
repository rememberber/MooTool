import { describe, expect, it } from 'vitest'
import {
  asymmetricDecrypt,
  asymmetricEncrypt,
  decodeBase,
  digestText,
  encodeBase,
  generateAsymmetricKeyPair,
  privateEncrypt,
  publicDecrypt,
  randomDigits,
  randomPassword,
  randomString,
  signContent,
  symmetricDecrypt,
  symmetricEncrypt,
  verifySignature
} from './cryptoTools'

describe('crypto tools', () => {
  it.each(['AES', 'DES', 'SM4'] as const)('round-trips %s symmetric encryption', (algorithm) => {
    const encrypted = symmetricEncrypt(algorithm, 'MooTool 加密', '1234567890abcdef')
    expect(encrypted).toMatch(/^[0-9a-f]+$/i)
    expect(symmetricDecrypt(algorithm, encrypted, '1234567890abcdef')).toBe('MooTool 加密')
  })

  it('computes standard and national digests', () => {
    expect(digestText('MD5', 'abc')).toBe('900150983cd24fb0d6963f7d28e17f72')
    expect(digestText('SHA-256', 'abc')).toBe('ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad')
    expect(digestText('SM3', 'abc')).toBe('66c7f0f462eeedd9d1f2d46bdc10e4e24167c4875cf2f7a2297da02b8f4ba8e0')
  })

  it.each(['Base64', 'Base32'] as const)('round-trips %s text', (algorithm) => {
    expect(decodeBase(algorithm, encodeBase(algorithm, 'Moo 工具'))).toBe('Moo 工具')
  })

  it('generates constrained random values', () => {
    expect(randomDigits(32)).toMatch(/^\d{32}$/)
    expect(randomString(32)).toMatch(/^[a-zA-Z0-9]{32}$/)
    const password = randomPassword(24)
    expect(password).toHaveLength(24)
    expect(password).toMatch(/[a-z]/)
    expect(password).toMatch(/[A-Z]/)
    expect(password).toMatch(/\d/)
    expect(password).toMatch(/[^a-zA-Z0-9]/)
  })

  it('supports RSA encrypt/decrypt, private/public operations and signatures', () => {
    const pair = generateAsymmetricKeyPair('RSA', 512)
    const cipher = asymmetricEncrypt('RSA', 'moo', pair.publicKey)
    expect(asymmetricDecrypt('RSA', cipher, pair.privateKey)).toBe('moo')
    const privateCipher = privateEncrypt('moo', pair.privateKey)
    expect(publicDecrypt(privateCipher, pair.publicKey)).toBe('moo')
    const signature = signContent('RSA', 'moo', pair.privateKey)
    expect(verifySignature('RSA', 'moo', signature, pair.publicKey)).toBe(true)
  })

  it('supports SM2 encryption and signatures', () => {
    const pair = generateAsymmetricKeyPair('SM2')
    const cipher = asymmetricEncrypt('SM2', 'moo', pair.publicKey)
    expect(asymmetricDecrypt('SM2', cipher, pair.privateKey)).toBe('moo')
    const signature = signContent('SM2', 'moo', pair.privateKey, pair.publicKey)
    expect(verifySignature('SM2', 'moo', signature, pair.publicKey)).toBe(true)
  })
})

describe('Java-compatible AES keys', () => {
  it('matches the Java AES-192 fixture and decrypts it', () => {
    const key = '123456789012345678901234'
    const cipher = 'a66c2ff48c6ded0ae657017c86bc4930'
    expect(symmetricEncrypt('AES', 'MooTool parity', key)).toBe(cipher)
    expect(symmetricDecrypt('AES', cipher, key)).toBe('MooTool parity')
  })
  it.each(['1234567890abcdef', '123456789012345678901234', '12345678901234567890123456789012', '中文密钥1234'])('preserves all UTF-8 key bytes: %s', async key => {
    const { createCipheriv } = await import('node:crypto')
    const bytes = Buffer.from(key, 'utf8')
    const cipher = createCipheriv(`aes-${bytes.length * 8}-ecb`, bytes, null)
    const expected = Buffer.concat([cipher.update('跨端 MooTool', 'utf8'), cipher.final()]).toString('hex')
    expect(symmetricEncrypt('AES', '跨端 MooTool', key)).toBe(expected)
    expect(symmetricDecrypt('AES', expected, key)).toBe('跨端 MooTool')
  })
  it('rejects invalid key byte lengths without silent padding', () => {
    for (const key of ['', 'short', 'x'.repeat(17), '中'.repeat(16)]) expect(() => symmetricEncrypt('AES', 'text', key)).toThrow('UTF-8 bytes')
  })
  it('supports empty plaintext and rejects bad ciphertext padding', () => {
    const key = '1234567890abcdef'
    expect(symmetricDecrypt('AES', symmetricEncrypt('AES', '', key), key)).toBe('')
    expect(() => symmetricDecrypt('AES', '00', key)).toThrow('blocks')
    expect(() => symmetricDecrypt('AES', '00'.repeat(16), key)).toThrow('padding')
  })
  it('only reads old truncated-key ciphertext in explicit legacy mode', () => {
    const cipher = '3f4916b141b6d3442df1017170f155e8'
    const key = '123456789012345678901234'
    expect(() => symmetricDecrypt('AES', cipher, key)).toThrow()
    expect(symmetricDecrypt('AES', cipher, key, true)).toBe('MooTool parity')
  })
})
