import type { SnapshotProtector } from './configChangeService'

type SafeStorageAdapter = {
  isEncryptionAvailable(): boolean
  encryptString(value: string): Buffer
  decryptString(value: Buffer): string
}

export class SafeStorageSnapshotProtector implements SnapshotProtector {
  constructor(private readonly safeStorage: SafeStorageAdapter) {}

  isAvailable(): boolean {
    return this.safeStorage.isEncryptionAvailable()
  }

  encrypt(value: Buffer): Buffer {
    if (!this.isAvailable()) throw new Error('Secure snapshot storage is unavailable')
    return this.safeStorage.encryptString(value.toString('base64'))
  }

  decrypt(value: Buffer): Buffer {
    if (!this.isAvailable()) throw new Error('Secure snapshot storage is unavailable')
    return Buffer.from(this.safeStorage.decryptString(value), 'base64')
  }
}
