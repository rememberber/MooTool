import { describe, expect, it } from 'vitest'
import { SafeStorageSnapshotProtector } from '../../electron/main/ai/safeStorageSnapshotProtector'

describe('SafeStorageSnapshotProtector', () => {
  it('round-trips binary snapshots through the platform safe-storage boundary', () => {
    const protector = new SafeStorageSnapshotProtector({
      isEncryptionAvailable: () => true,
      encryptString: (value) => Buffer.from(value, 'utf8').reverse(),
      decryptString: (value) => Buffer.from(value).reverse().toString('utf8')
    })
    const source = Buffer.from([0, 1, 2, 127, 128, 255])

    const encrypted = protector.encrypt(source)

    expect(encrypted).not.toEqual(source)
    expect(protector.decrypt(encrypted)).toEqual(source)
  })

  it('blocks snapshot operations when OS encryption is unavailable', () => {
    const protector = new SafeStorageSnapshotProtector({
      isEncryptionAvailable: () => false,
      encryptString: () => Buffer.alloc(0),
      decryptString: () => ''
    })

    expect(protector.isAvailable()).toBe(false)
    expect(() => protector.encrypt(Buffer.from('secret'))).toThrow('unavailable')
  })
})
