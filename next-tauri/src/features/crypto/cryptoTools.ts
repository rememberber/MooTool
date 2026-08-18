import { md5, sha1 } from '@noble/hashes/legacy.js'
import { sha256, sha384, sha512 } from '@noble/hashes/sha2.js'
import { hmac } from '@noble/hashes/hmac.js'
import { Buffer } from 'buffer'

export type HashAlgorithm = 'md5' | 'sha1' | 'sha256' | 'sha384' | 'sha512'
export type CryptoToolErrorCode = 'hmacSecret' | 'invalidCiphertext' | 'decryptFailed' | 'randomLength' | 'passphraseEmpty' | 'passphraseLong'

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
  return Buffer.from(hashers[algorithm](encoder.encode(value))).toString('hex')
}

export function hmacSha256(value: string, secret: string): string {
  if (!secret) throw new CryptoToolError('hmacSecret')
  return Buffer.from(hmac(sha256, encoder.encode(secret), encoder.encode(value))).toString('hex')
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
