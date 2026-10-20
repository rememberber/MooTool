import { md5, sha1 } from '@noble/hashes/legacy.js'
import { sm3 } from 'sm-crypto'
import { describe, expect, it } from 'vitest'
import { digestMd5, digestSha1, digestSm3 } from './streamingDigest'

const samples = [
  new Uint8Array(),
  new TextEncoder().encode('abc'),
  new TextEncoder().encode('MooTool'),
  new Uint8Array(100).map((_, index) => index)
]

function hex(bytes: Uint8Array): string {
  return [...bytes].map((byte) => byte.toString(16).padStart(2, '0')).join('')
}

describe('streaming file digests', () => {
  it('matches MD5 and SHA-1', () => {
    for (const sample of samples) {
      expect(digestMd5(sample)).toBe(hex(md5(sample)))
      expect(digestSha1(sample)).toBe(hex(sha1(sample)))
    }
  })

  it('matches SM3', () => {
    expect(digestSm3(new TextEncoder().encode('abc'))).toBe('66c7f0f462eeedd9d1f2d46bdc10e4e24167c4875cf2f7a2297da02b8f4ba8e0')
    for (const sample of samples) {
      expect(digestSm3(sample)).toBe((sm3 as unknown as (input: number[]) => string)(Array.from(sample)))
    }
  })
})
