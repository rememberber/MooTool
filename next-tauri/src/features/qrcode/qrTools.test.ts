import { describe, expect, it } from 'vitest'
import { QrToolError, defaultQrOptions, generateQrPngDataUrl, generateQrSvg, normalizeQrSize, svgDataUrl } from './qrTools'

describe('QR code tools', () => {
  it('generates a deterministic SVG QR symbol', async () => {
    const svg = await generateQrSvg('https://mootool.app/?from=tauri', defaultQrOptions)
    expect(svg).toContain('<svg')
    expect(svg).toContain('width="300"')
    expect(svg).toContain('#111111')
    expect(svg).toContain('shape-rendering="crispEdges"')
  })

  it('encodes SVG as a browser-safe data URL', () => {
    expect(svgDataUrl('<svg><text>牛 & 🐮</text></svg>')).toContain(
      'data:image/svg+xml;charset=utf-8,'
    )
    expect(svgDataUrl('<svg/>')).toContain('%3Csvg%2F%3E')
  })

  it('rejects empty, oversized and invalid options', async () => {
    await expectQrError(generateQrSvg('', defaultQrOptions), 'empty')
    await expectQrError(generateQrSvg('x', { ...defaultQrOptions, size: 119 }), 'size')
    expect(normalizeQrSize(12)).toBe(120)
    expect(normalizeQrSize(9000)).toBe(2000)
    expect(await generateQrPngDataUrl('MooTool', defaultQrOptions)).toMatch(/^data:image\/png/)
    await expectQrError(generateQrSvg('x', { ...defaultQrOptions, dark: 'black' }), 'color')
  })
})

async function expectQrError(run: Promise<unknown>, code: QrToolError['code']) {
  try {
    await run
    throw new Error('expected QR validation to fail')
  } catch (cause) {
    expect(cause).toBeInstanceOf(QrToolError)
    expect((cause as QrToolError).code).toBe(code)
  }
}
