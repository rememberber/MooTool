import { describe, expect, it } from 'vitest'
import { generateQrDataUrl, normalizeQrSize } from './qrTools'

describe('QR tools', () => {
  it('generates a PNG data URL at all correction levels', async () => {
    for (const level of ['L', 'M', 'Q', 'H'] as const) {
      await expect(generateQrDataUrl('https://github.com/rememberber/MooTool', 240, level)).resolves.toMatch(/^data:image\/png;base64,/)
    }
  })

  it('normalizes QR sizes to the settings boundary', () => {
    expect(normalizeQrSize(20)).toBe(120)
    expect(normalizeQrSize(360.4)).toBe(360)
    expect(normalizeQrSize(9999)).toBe(2000)
  })
})
