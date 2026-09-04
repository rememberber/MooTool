import { md5, sha1 } from '@noble/hashes/legacy.js'
import { sha256, sha384, sha512 } from '@noble/hashes/sha2.js'
import { hmac } from '@noble/hashes/hmac.js'
import { Buffer } from 'buffer'
import CryptoJS from 'crypto-js'
import { sm2, sm3, sm4 } from 'sm-crypto'

export type HashAlgorithm = 'md5' | 'sha1' | 'sha256' | 'sha384' | 'sha512' | 'sm3'
export type BaseAlgorithm = 'base64' | 'base32'
export type CryptoToolErrorCode = 'hmacSecret' | 'invalidCiphertext' | 'decryptFailed' | 'randomLength' | 'passphraseEmpty' | 'passphraseLong' | 'baseInvalid' | 'rsaKey' | 'rsaOperation'
export type CompatibilityAlgorithm = 'des' | 'sm4' | 'sm2'

export class CryptoToolError extends Error {
  constructor(readonly code: CryptoToolErrorCode) {
    super(`CRYPTO_TOOL_${code}`)
    this.name = 'CryptoToolError'
  }
}

const encoder = new TextEncoder()
const hashers = { md5, sha1, sha256, sha384, sha512 } as const
const AES_ITERATIONS = 210_000
const AES_PREFIX = 'mootool:aes-gcm:v1:'

interface AesEnvelope {
  ciphertext: string
  iterations: number
  iv: string
  salt: string
}

export function hashText(value: string, algorithm: HashAlgorithm): string {
  if (algorithm === 'sm3') return sm3(value)
  return Buffer.from(hashers[algorithm](encoder.encode(value))).toString('hex')
}

export function compatibilityEncrypt(algorithm: Exclude<CompatibilityAlgorithm, 'sm2'>, value: string, key: string): string {
  if (algorithm === 'sm4') return String(sm4.encrypt(value, utf8KeyHex(key, 16), { mode: 'ecb', padding: 'pkcs#7' }))
  const keyWords = CryptoJS.enc.Utf8.parse(normalizeCompatibilityKey(key, 8))
  return CryptoJS.DES.encrypt(CryptoJS.enc.Utf8.parse(value), keyWords, {
    mode: CryptoJS.mode.ECB,
    padding: CryptoJS.pad.Pkcs7
  }).ciphertext.toString(CryptoJS.enc.Hex)
}

export function compatibilityDecrypt(algorithm: Exclude<CompatibilityAlgorithm, 'sm2'>, value: string, key: string): string {
  const cleaned = cleanHex(value)
  if (algorithm === 'sm4') return String(sm4.decrypt(cleaned, utf8KeyHex(key, 16), { mode: 'ecb', padding: 'pkcs#7' }))
  const keyWords = CryptoJS.enc.Utf8.parse(normalizeCompatibilityKey(key, 8))
  const params = CryptoJS.lib.CipherParams.create({ ciphertext: CryptoJS.enc.Hex.parse(cleaned) })
  const output = CryptoJS.DES.decrypt(params, keyWords, {
    mode: CryptoJS.mode.ECB,
    padding: CryptoJS.pad.Pkcs7
  }).toString(CryptoJS.enc.Utf8)
  if (!output && value.trim()) throw new CryptoToolError('decryptFailed')
  return output
}

export function generateSm2KeyPair(): RsaKeyPair {
  const pair = sm2.generateKeyPairHex()
  return { publicKey: hexToBase64(pair.publicKey), privateKey: hexToBase64(pair.privateKey) }
}

export function sm2Encrypt(value: string, publicKey: string): string {
  return hexToBase64(sm2.doEncrypt(value, base64ToHex(publicKey), 1))
}

export function sm2Decrypt(value: string, privateKey: string): string {
  return String(sm2.doDecrypt(base64ToHex(value), base64ToHex(privateKey), 1))
}

export function sm2Sign(value: string, privateKey: string, publicKey: string): string {
  return hexToBase64(sm2.doSignature(value, base64ToHex(privateKey), {
    hash: true,
    der: true,
    publicKey: base64ToHex(publicKey)
  }))
}

export function sm2Verify(value: string, signature: string, publicKey: string): boolean {
  return sm2.doVerifySignature(value, base64ToHex(signature), base64ToHex(publicKey), { hash: true, der: true })
}

export function hmacSha256(value: string, secret: string): string {
  if (!secret) throw new CryptoToolError('hmacSecret')
  return Buffer.from(hmac(sha256, encoder.encode(secret), encoder.encode(value))).toString('hex')
}

export function encodeBase(value: string, algorithm: BaseAlgorithm): string {
  const bytes = encoder.encode(value)
  return algorithm === 'base64' ? Buffer.from(bytes).toString('base64') : encodeBase32(bytes)
}

export function decodeBase(value: string, algorithm: BaseAlgorithm): string {
  try {
    const bytes = algorithm === 'base64' ? decodeBase64(value) : decodeBase32(value)
    return new TextDecoder('utf-8', { fatal: true }).decode(bytes)
  } catch {
    throw new CryptoToolError('baseInvalid')
  }
}

export interface RsaKeyPair { publicKey: string; privateKey: string }

export async function generateRsaKeyPair(bits: 2048 | 3072 | 4096 = 2048): Promise<RsaKeyPair> {
  const pair = await crypto.subtle.generateKey(
    { name: 'RSA-OAEP', modulusLength: bits, publicExponent: new Uint8Array([1, 0, 1]), hash: 'SHA-256' },
    true,
    ['encrypt', 'decrypt']
  )
  const [publicDer, privateDer] = await Promise.all([
    crypto.subtle.exportKey('spki', pair.publicKey),
    crypto.subtle.exportKey('pkcs8', pair.privateKey)
  ])
  return { publicKey: pem('PUBLIC KEY', publicDer), privateKey: pem('PRIVATE KEY', privateDer) }
}

export async function rsaEncrypt(value: string, publicKey: string): Promise<string> {
  try {
    const key = await crypto.subtle.importKey('spki', pemBytes(publicKey, 'PUBLIC KEY'), { name: 'RSA-OAEP', hash: 'SHA-256' }, false, ['encrypt'])
    return Buffer.from(await crypto.subtle.encrypt({ name: 'RSA-OAEP' }, key, encoder.encode(value))).toString('base64')
  } catch {
    throw new CryptoToolError(publicKey.trim() ? 'rsaOperation' : 'rsaKey')
  }
}

export async function rsaDecrypt(value: string, privateKey: string): Promise<string> {
  try {
    const key = await crypto.subtle.importKey('pkcs8', pemBytes(privateKey, 'PRIVATE KEY'), { name: 'RSA-OAEP', hash: 'SHA-256' }, false, ['decrypt'])
    const plaintext = await crypto.subtle.decrypt({ name: 'RSA-OAEP' }, key, decodeBase64(value))
    return new TextDecoder('utf-8', { fatal: true }).decode(plaintext)
  } catch {
    throw new CryptoToolError(privateKey.trim() ? 'rsaOperation' : 'rsaKey')
  }
}

export async function rsaSign(value: string, privateKey: string): Promise<string> {
  try {
    const key = await crypto.subtle.importKey('pkcs8', pemBytes(privateKey, 'PRIVATE KEY'), { name: 'RSA-PSS', hash: 'SHA-256' }, false, ['sign'])
    return Buffer.from(await crypto.subtle.sign({ name: 'RSA-PSS', saltLength: 32 }, key, encoder.encode(value))).toString('base64')
  } catch {
    throw new CryptoToolError(privateKey.trim() ? 'rsaOperation' : 'rsaKey')
  }
}

export async function rsaVerify(value: string, signature: string, publicKey: string): Promise<boolean> {
  try {
    const key = await crypto.subtle.importKey('spki', pemBytes(publicKey, 'PUBLIC KEY'), { name: 'RSA-PSS', hash: 'SHA-256' }, false, ['verify'])
    return crypto.subtle.verify({ name: 'RSA-PSS', saltLength: 32 }, key, decodeBase64(signature), encoder.encode(value))
  } catch {
    throw new CryptoToolError(publicKey.trim() ? 'rsaOperation' : 'rsaKey')
  }
}

export async function encryptAesGcm(value: string, passphrase: string): Promise<string> {
  requirePassphrase(passphrase)
  const salt = secureBytes(16)
  const iv = secureBytes(12)
  const key = await deriveAesKey(passphrase, salt, AES_ITERATIONS, ['encrypt'])
  const ciphertext = await crypto.subtle.encrypt(
    { name: 'AES-GCM', iv },
    key,
    encoder.encode(value)
  )
  const envelope: AesEnvelope = {
    ciphertext: Buffer.from(ciphertext).toString('base64'),
    iterations: AES_ITERATIONS,
    iv: Buffer.from(iv).toString('base64'),
    salt: Buffer.from(salt).toString('base64')
  }
  return `${AES_PREFIX}${Buffer.from(JSON.stringify(envelope)).toString('base64')}`
}

export async function decryptAesGcm(value: string, passphrase: string): Promise<string> {
  requirePassphrase(passphrase)
  if (!value.startsWith(AES_PREFIX)) throw new CryptoToolError('invalidCiphertext')
  try {
    const envelope = JSON.parse(
      Buffer.from(value.slice(AES_PREFIX.length), 'base64').toString('utf8')
    ) as Partial<AesEnvelope>
    validateEnvelope(envelope)
    const salt = Buffer.from(envelope.salt, 'base64')
    const iv = Buffer.from(envelope.iv, 'base64')
    const key = await deriveAesKey(passphrase, salt, envelope.iterations, ['decrypt'])
    const plaintext = await crypto.subtle.decrypt(
      { name: 'AES-GCM', iv },
      key,
      Buffer.from(envelope.ciphertext, 'base64')
    )
    return new TextDecoder().decode(plaintext)
  } catch (cause) {
    if (cause instanceof CryptoToolError && cause.code === 'invalidCiphertext') throw cause
    throw new CryptoToolError('decryptFailed')
  }
}

export type RandomKind = 'uuid' | 'password' | 'alphanumeric' | 'digits' | 'hex'

export function generateRandom(kind: RandomKind, length: number): string {
  if (kind === 'uuid') return crypto.randomUUID()
  if (!Number.isInteger(length) || length < 1 || length > 4096) {
    throw new CryptoToolError('randomLength')
  }
  const alphabets: Record<Exclude<RandomKind, 'uuid'>, string> = {
    password: 'ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789!@#$%^&*()-_=+',
    alphanumeric: 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789',
    digits: '0123456789',
    hex: '0123456789abcdef'
  }
  return sampleSecure(alphabets[kind], length)
}

async function deriveAesKey(
  passphrase: string,
  salt: Uint8Array<ArrayBuffer>,
  iterations: number,
  usages: KeyUsage[]
): Promise<CryptoKey> {
  const material = await crypto.subtle.importKey(
    'raw',
    encoder.encode(passphrase),
    'PBKDF2',
    false,
    ['deriveKey']
  )
  return crypto.subtle.deriveKey(
    { name: 'PBKDF2', hash: 'SHA-256', salt, iterations },
    material,
    { name: 'AES-GCM', length: 256 },
    false,
    usages
  )
}

function secureBytes(length: number): Uint8Array<ArrayBuffer> {
  return crypto.getRandomValues(new Uint8Array(length))
}

function sampleSecure(alphabet: string, length: number): string {
  const max = Math.floor(256 / alphabet.length) * alphabet.length
  let output = ''
  while (output.length < length) {
    for (const byte of secureBytes(Math.min(256, (length - output.length) * 2))) {
      if (byte < max) output += alphabet[byte % alphabet.length]
      if (output.length === length) break
    }
  }
  return output
}

function requirePassphrase(passphrase: string): void {
  if (!passphrase) throw new CryptoToolError('passphraseEmpty')
  if (passphrase.length > 4096) throw new CryptoToolError('passphraseLong')
}

function validateEnvelope(envelope: Partial<AesEnvelope>): asserts envelope is AesEnvelope {
  if (
    typeof envelope.ciphertext !== 'string'
    || typeof envelope.iv !== 'string'
    || typeof envelope.salt !== 'string'
    || envelope.iterations !== AES_ITERATIONS
  ) {
    throw new CryptoToolError('invalidCiphertext')
  }
}

const BASE32_ALPHABET = 'ABCDEFGHIJKLMNOPQRSTUVWXYZ234567'

function encodeBase32(bytes: Uint8Array): string {
  let bits = 0
  let value = 0
  let output = ''
  for (const byte of bytes) {
    value = (value << 8) | byte
    bits += 8
    while (bits >= 5) {
      output += BASE32_ALPHABET[(value >>> (bits - 5)) & 31]
      bits -= 5
    }
  }
  if (bits > 0) output += BASE32_ALPHABET[(value << (5 - bits)) & 31]
  return output.padEnd(Math.ceil(output.length / 8) * 8, '=')
}

function decodeBase32(input: string): Uint8Array {
  const normalized = input.toUpperCase().replace(/\s+/g, '')
  if (!/^[A-Z2-7]*={0,6}$/.test(normalized) || /=/.test(normalized.replace(/=+$/, ''))) throw new Error('invalid base32')
  const unpadded = normalized.replace(/=+$/, '')
  let bits = 0
  let value = 0
  const output: number[] = []
  for (const character of unpadded) {
    const index = BASE32_ALPHABET.indexOf(character)
    if (index < 0) throw new Error('invalid base32')
    value = (value << 5) | index
    bits += 5
    if (bits >= 8) {
      output.push((value >>> (bits - 8)) & 255)
      bits -= 8
    }
  }
  return new Uint8Array(output)
}

function decodeBase64(value: string): Uint8Array<ArrayBuffer> {
  const normalized = value.replace(/\s+/g, '')
  if (!normalized || !/^(?:[A-Za-z0-9+/]{4})*(?:[A-Za-z0-9+/]{2}==|[A-Za-z0-9+/]{3}=)?$/.test(normalized)) throw new Error('invalid base64')
  return Uint8Array.from(Buffer.from(normalized, 'base64'))
}

function normalizeCompatibilityKey(key: string, length: number): string {
  const characters = Array.from(key)
  return characters.length >= length
    ? characters.slice(0, length).join('')
    : `${key}${'0'.repeat(length)}`.slice(0, length)
}

function utf8KeyHex(key: string, length: number): string {
  return CryptoJS.enc.Utf8.parse(normalizeCompatibilityKey(key, length)).toString(CryptoJS.enc.Hex)
}

function cleanHex(value: string): string {
  const normalized = value.replace(/\s+/g, '')
  if (!/^(?:[0-9a-f]{2})*$/i.test(normalized)) throw new CryptoToolError('invalidCiphertext')
  return normalized
}

function hexToBase64(value: string): string {
  return Buffer.from(cleanHex(value), 'hex').toString('base64')
}

function base64ToHex(value: string): string {
  const normalized = value.replace(/\s+/g, '')
  if (!normalized || !/^(?:[A-Za-z0-9+/]{4})*(?:[A-Za-z0-9+/]{2}==|[A-Za-z0-9+/]{3}=)?$/.test(normalized)) {
    throw new CryptoToolError('invalidCiphertext')
  }
  return Buffer.from(normalized, 'base64').toString('hex')
}

function pem(label: string, data: ArrayBuffer): string {
  const body = Buffer.from(data).toString('base64').match(/.{1,64}/g)?.join('\n') ?? ''
  return `-----BEGIN ${label}-----\n${body}\n-----END ${label}-----`
}

function pemBytes(value: string, label: string): ArrayBuffer {
  const match = new RegExp(`-----BEGIN ${label}-----([\\s\\S]+?)-----END ${label}-----`).exec(value.trim())
  if (!match) throw new CryptoToolError('rsaKey')
  const bytes = Uint8Array.from(Buffer.from(match[1].replace(/\s+/g, ''), 'base64'))
  return bytes.buffer
}
