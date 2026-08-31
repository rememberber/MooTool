import { invoke } from '@tauri-apps/api/core'
import type { HashAlgorithm } from '../../features/crypto/cryptoTools'

export interface UserFileDigest {
  name: string
  path: string
  digest: string
}

export const fileDigestApi = {
  digest: (algorithm: HashAlgorithm): Promise<UserFileDigest | null> => {
    if (!window.__TAURI_INTERNALS__) return Promise.reject(new Error('File digests require the Tauri desktop app'))
    return invoke<UserFileDigest | null>('digest_user_file', { algorithm })
  }
}
