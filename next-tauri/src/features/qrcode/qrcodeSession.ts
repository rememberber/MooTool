import { contentFingerprint } from '../../shared/fingerprint'
import type { QrOptions } from './qrTools'

export function qrcodeSessionDigest(source: string, decoded: string, options: QrOptions): string {
  return JSON.stringify({
    sourceHash: contentFingerprint(source),
    decodedHash: contentFingerprint(decoded),
    options
  })
}
