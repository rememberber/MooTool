import { describe, expect, it } from 'vitest'
import {
  decryptAesGcm,
  encryptAesGcm,
  generateRandom,
  hashText,
  hmacSha256,
  decodeBase,
  encodeBase,
  generateRsaKeyPair,
  rsaDecrypt,
  rsaEncrypt,
  rsaSign,
  rsaVerify,
  CryptoToolError
} from './cryptoTools'

describe('crypto tools', () => {
  it('calculates standard message digests', () => {
    expect(hashText('abc', 'md5')).toBe('900150983cd24fb0d6963f7d28e17f72')
    expect(hashText('abc', 'sha1')).toBe('a9993e364706816aba3e25717850c26c9cd0d89d')
    expect(hashText('abc', 'sha256')).toBe(
      'ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad'
    )
  })

  it('calculates HMAC-SHA256', () => {
    expect(hmacSha256('The quick brown fox jumps over the lazy dog', 'key')).toBe(
      'f7bc83f430538424b13298e6aa6fb143ef4d59a14946175997479dbc2d1a3cd8'
    )
  })

  it('round-trips an authenticated AES-GCM envelope', async () => {
    const encrypted = await encryptAesGcm('MooTool 密文 🐮', 'correct horse battery staple')
    expect(encrypted).toMatch(/^mootool:aes-gcm:v1:/)
    await expect(decryptAesGcm(encrypted, 'correct horse battery staple'))
      .resolves.toBe('MooTool 密文 🐮')
    await expect(decryptAesGcm(encrypted, 'wrong')).rejects.toThrow(CryptoToolError)
  })

  it('generates constrained cryptographic random values', () => {
    expect(generateRandom('uuid', 1)).toMatch(
      /^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/
    )
    expect(generateRandom('digits', 64)).toMatch(/^\d{64}$/)
    expect(generateRandom('hex', 64)).toMatch(/^[0-9a-f]{64}$/)
    expect(() => generateRandom('password', 0)).toThrow('CRYPTO_TOOL_randomLength')
  })

  it('round-trips UTF-8 with strict Base64 and Base32 codecs', () => {
    expect(decodeBase(encodeBase('MooTool 牛', 'base64'), 'base64')).toBe('MooTool 牛')
    expect(encodeBase('foo', 'base32')).toBe('MZXW6===')
    expect(decodeBase('MZXW6===', 'base32')).toBe('foo')
    expect(() => decodeBase('***', 'base64')).toThrow('CRYPTO_TOOL_baseInvalid')
  })

  it('uses RSA-OAEP encryption and RSA-PSS signatures', async () => {
    const keys = await generateRsaKeyPair(2048)
    const encrypted = await rsaEncrypt('MooTool RSA', keys.publicKey)
    await expect(rsaDecrypt(encrypted, keys.privateKey)).resolves.toBe('MooTool RSA')
    const signature = await rsaSign('signed content', keys.privateKey)
    await expect(rsaVerify('signed content', signature, keys.publicKey)).resolves.toBe(true)
    await expect(rsaVerify('changed', signature, keys.publicKey)).resolves.toBe(false)
  })
})
