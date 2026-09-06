import { describe, expect, it } from 'vitest'
import { defaultQrOptions } from './qrTools'
import { qrcodeSessionDigest } from './qrcodeSession'

describe('QR Code WebView session', () => {
  it('tracks durable input state without asynchronous generated output', () => {
    const digest = qrcodeSessionDigest('https://mootool.app', '', defaultQrOptions)

    expect(JSON.parse(digest)).toEqual({
      sourceHash: expect.any(String),
      decodedHash: expect.any(String),
      options: defaultQrOptions
    })
  })
})
