import CryptoJS from 'crypto-js'
import * as base32 from 'hi-base32'
import forge from 'node-forge'
import { sm2, sm3, sm4 } from 'sm-crypto'

export const symmetricAlgorithms = ['AES', 'DES', 'SM4'] as const
export const asymmetricAlgorithms = ['RSA', 'SM2'] as const
export const digestAlgorithms = ['MD5', 'SHA-1', 'SHA-256', 'SHA-384', 'SHA-512', 'SM3'] as const
export const baseAlgorithms = ['Base64', 'Base32'] as const

export type SymmetricAlgorithm = (typeof symmetricAlgorithms)[number]
export type AsymmetricAlgorithm = (typeof asymmetricAlgorithms)[number]
export type DigestAlgorithm = (typeof digestAlgorithms)[number]
export type BaseAlgorithm = (typeof baseAlgorithms)[number]
export type AsymmetricKeyPair = { publicKey: string; privateKey: string }

export function symmetricEncrypt(algorithm: SymmetricAlgorithm, content: string, key: string): string {
  if (algorithm === 'SM4') {
    return String(sm4.encrypt(content, utf8KeyHex(key, 16), { mode: 'ecb', padding: 'pkcs#7' }))
  }
  const keySize = algorithm === 'DES' ? 8 : 16
  const keyWords = CryptoJS.enc.Utf8.parse(normalizeKey(key, keySize))
  const encrypted = algorithm === 'DES'
    ? CryptoJS.DES.encrypt(CryptoJS.enc.Utf8.parse(content), keyWords, { mode: CryptoJS.mode.ECB, padding: CryptoJS.pad.Pkcs7 })
    : CryptoJS.AES.encrypt(CryptoJS.enc.Utf8.parse(content), keyWords, { mode: CryptoJS.mode.ECB, padding: CryptoJS.pad.Pkcs7 })
  return encrypted.ciphertext.toString(CryptoJS.enc.Hex)
}

export function symmetricDecrypt(algorithm: SymmetricAlgorithm, cipherHex: string, key: string): string {
  if (algorithm === 'SM4') {
    return String(sm4.decrypt(cleanHex(cipherHex), utf8KeyHex(key, 16), { mode: 'ecb', padding: 'pkcs#7' }))
  }
  const keySize = algorithm === 'DES' ? 8 : 16
  const keyWords = CryptoJS.enc.Utf8.parse(normalizeKey(key, keySize))
  const params = CryptoJS.lib.CipherParams.create({ ciphertext: CryptoJS.enc.Hex.parse(cleanHex(cipherHex)) })
  const decrypted = algorithm === 'DES'
    ? CryptoJS.DES.decrypt(params, keyWords, { mode: CryptoJS.mode.ECB, padding: CryptoJS.pad.Pkcs7 })
    : CryptoJS.AES.decrypt(params, keyWords, { mode: CryptoJS.mode.ECB, padding: CryptoJS.pad.Pkcs7 })
  const output = decrypted.toString(CryptoJS.enc.Utf8)
  if (!output && cipherHex.trim()) throw new Error('Unable to decrypt with the supplied key')
  return output
}

export function digestText(algorithm: DigestAlgorithm, content: string): string {
  switch (algorithm) {
    case 'MD5': return CryptoJS.MD5(content).toString()
    case 'SHA-1': return CryptoJS.SHA1(content).toString()
    case 'SHA-256': return CryptoJS.SHA256(content).toString()
    case 'SHA-384': return CryptoJS.SHA384(content).toString()
    case 'SHA-512': return CryptoJS.SHA512(content).toString()
    case 'SM3': return sm3(content)
  }
}

export function encodeBase(algorithm: BaseAlgorithm, content: string): string {
  return algorithm === 'Base64'
    ? CryptoJS.enc.Base64.stringify(CryptoJS.enc.Utf8.parse(content))
    : base32.encode(content)
}

export function decodeBase(algorithm: BaseAlgorithm, content: string): string {
  if (algorithm === 'Base64') {
    const output = CryptoJS.enc.Base64.parse(content.replace(/\s+/g, '')).toString(CryptoJS.enc.Utf8)
    if (!output && content.trim()) throw new Error('Invalid Base64 content')
    return output
  }
  return base32.decode(content.replace(/\s+/g, ''))
}

export function randomUuid(): string {
  return globalThis.crypto.randomUUID()
}

export function randomDigits(length: number): string {
  return randomFromAlphabet('0123456789', length)
}

export function randomString(length: number): string {
  return randomFromAlphabet('abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789', length)
}

export function randomPassword(length: number): string {
  const categories = ['abcdefghijklmnopqrstuvwxyz', 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', '0123456789', '`~!@#$%^&*()_+-=[]{};\':,./<>?']
  const size = normalizeLength(length)
  const required = categories.slice(0, Math.min(size, categories.length)).map((alphabet) => randomFromAlphabet(alphabet, 1))
  const rest = randomFromAlphabet(categories.join(''), Math.max(0, size - required.length)).split('')
  return shuffle([...required, ...rest]).join('')
}

export function generateAsymmetricKeyPair(algorithm: AsymmetricAlgorithm, rsaBits = 2048): AsymmetricKeyPair {
  if (algorithm === 'SM2') {
    const pair = sm2.generateKeyPairHex()
    return { publicKey: hexToBase64(pair.publicKey), privateKey: hexToBase64(pair.privateKey) }
  }
  const pair = forge.pki.rsa.generateKeyPair({ bits: rsaBits, e: 0x10001 })
  const publicDer = forge.asn1.toDer(forge.pki.publicKeyToAsn1(pair.publicKey)).getBytes()
  const privateDer = forge.asn1.toDer(forge.pki.privateKeyToAsn1(pair.privateKey)).getBytes()
  return { publicKey: forge.util.encode64(publicDer), privateKey: forge.util.encode64(privateDer) }
}

export function asymmetricEncrypt(algorithm: AsymmetricAlgorithm, content: string, publicKey: string): string {
  if (algorithm === 'SM2') return hexToBase64(sm2.doEncrypt(content, base64ToHex(publicKey), 1))
  const encrypted = readRsaPublicKey(publicKey).encrypt(forge.util.encodeUtf8(content), 'RSAES-PKCS1-V1_5')
  return forge.util.encode64(encrypted)
}

export function asymmetricDecrypt(algorithm: AsymmetricAlgorithm, cipherText: string, privateKey: string): string {
  if (algorithm === 'SM2') return String(sm2.doDecrypt(base64ToHex(cipherText), base64ToHex(privateKey), 1))
  const decrypted = readRsaPrivateKey(privateKey).decrypt(forge.util.decode64(cleanBase64(cipherText)), 'RSAES-PKCS1-V1_5')
  return forge.util.decodeUtf8(decrypted)
}

export function privateEncrypt(content: string, privateKey: string): string {
  const raw = forge.pki.rsa as typeof forge.pki.rsa & {
    encrypt(message: string, key: forge.pki.rsa.PrivateKey, blockType: number): string
  }
  return forge.util.encode64(raw.encrypt(forge.util.encodeUtf8(content), readRsaPrivateKey(privateKey), 0x01))
}

export function publicDecrypt(cipherText: string, publicKey: string): string {
  const raw = forge.pki.rsa as typeof forge.pki.rsa & {
    decrypt(cipher: string, key: forge.pki.rsa.PublicKey, usePublic: boolean): string
  }
  const decoded = raw.decrypt(forge.util.decode64(cleanBase64(cipherText)), readRsaPublicKey(publicKey), true)
  return forge.util.decodeUtf8(decoded)
}

export function signContent(algorithm: AsymmetricAlgorithm, content: string, privateKey: string, publicKey = ''): string {
  if (algorithm === 'SM2') {
    const signature = sm2.doSignature(content, base64ToHex(privateKey), { hash: true, der: true, publicKey: publicKey ? base64ToHex(publicKey) : undefined })
    return hexToBase64(signature)
  }
  const digest = forge.md.sha256.create()
  digest.update(content, 'utf8')
  return forge.util.encode64(readRsaPrivateKey(privateKey).sign(digest))
}

export function verifySignature(algorithm: AsymmetricAlgorithm, content: string, signature: string, publicKey: string): boolean {
  if (algorithm === 'SM2') return sm2.doVerifySignature(content, base64ToHex(signature), base64ToHex(publicKey), { hash: true, der: true })
  const digest = forge.md.sha256.create()
  digest.update(content, 'utf8')
  return readRsaPublicKey(publicKey).verify(digest.digest().bytes(), forge.util.decode64(cleanBase64(signature)))
}

function readRsaPublicKey(value: string): forge.pki.rsa.PublicKey {
  const asn1 = forge.asn1.fromDer(forge.util.decode64(cleanBase64(value)))
  return forge.pki.publicKeyFromAsn1(asn1)
}

function readRsaPrivateKey(value: string): forge.pki.rsa.PrivateKey {
  const asn1 = forge.asn1.fromDer(forge.util.decode64(cleanBase64(value)))
  return forge.pki.privateKeyFromAsn1(asn1)
}

function normalizeKey(key: string, length: number): string {
  const chars = Array.from(key)
  if (chars.length >= length) return chars.slice(0, length).join('')
  return `${key}${'0'.repeat(length)}`.slice(0, length)
}

function utf8KeyHex(key: string, length: number): string {
  return CryptoJS.enc.Utf8.parse(normalizeKey(key, length)).toString(CryptoJS.enc.Hex)
}

function cleanHex(value: string): string {
  const cleaned = value.replace(/\s+/g, '')
  if (!/^(?:[0-9a-fA-F]{2})*$/.test(cleaned)) throw new Error('Invalid hexadecimal content')
  return cleaned
}

function cleanBase64(value: string): string {
  return value.replace(/\s+/g, '')
}

function hexToBase64(hex: string): string {
  return forge.util.encode64(forge.util.hexToBytes(cleanHex(hex)))
}

function base64ToHex(base64: string): string {
  return forge.util.bytesToHex(forge.util.decode64(cleanBase64(base64)))
}

function randomFromAlphabet(alphabet: string, length: number): string {
  const size = normalizeLength(length)
  const output: string[] = []
  const max = Math.floor(256 / alphabet.length) * alphabet.length
  while (output.length < size) {
    const bytes = globalThis.crypto.getRandomValues(new Uint8Array(Math.max(16, size - output.length)))
    for (const byte of bytes) {
      if (byte < max) output.push(alphabet[byte % alphabet.length])
      if (output.length === size) break
    }
  }
  return output.join('')
}

function normalizeLength(length: number): number {
  if (!Number.isFinite(length) || length < 1 || length > 4096) throw new Error('Length must be between 1 and 4096')
  return Math.round(length)
}

function shuffle(values: string[]): string[] {
  const output = [...values]
  for (let index = output.length - 1; index > 0; index -= 1) {
    const byte = globalThis.crypto.getRandomValues(new Uint32Array(1))[0]
    const target = byte % (index + 1)
    ;[output[index], output[target]] = [output[target], output[index]]
  }
  return output
}
