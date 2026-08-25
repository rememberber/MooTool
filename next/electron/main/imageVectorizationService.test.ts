import { describe, expect, it } from 'vitest'
import { normalizeImageVectorizeOptions, toVTracerOptions, vectorizePng } from './imageVectorizationService'

describe('image vectorization options', () => {
  it('maps the poster preset to seam-free spline tracing', () => {
    const options = normalizeImageVectorizeOptions({ preset: 'poster', colorCount: 16, detail: 'medium', filterSpeckle: 8 })

    expect(toVTracerOptions(options)).toMatchObject({
      preset: 'poster',
      clustering: 'color-cluster',
      hierarchical: 'cutout',
      mode: 'spline',
      maxColors: 16,
      simplify: 1.25,
      optimize: 2
    })
  })

  it('does not apply a color count in black-and-white mode', () => {
    const options = normalizeImageVectorizeOptions({ preset: 'bw', colorCount: 16, detail: 'high', filterSpeckle: 0 })

    expect(toVTracerOptions(options)).toMatchObject({ preset: 'bw', clustering: 'bw', maxColors: undefined, simplify: 0.5 })
  })

  it('rejects parameters outside the supported limits', () => {
    expect(() => normalizeImageVectorizeOptions({ preset: 'poster', colorCount: 1, detail: 'medium', filterSpeckle: 8 })).toThrow('color count')
    expect(() => normalizeImageVectorizeOptions({ preset: 'poster', colorCount: 16, detail: 'ultra', filterSpeckle: 8 })).toThrow('options')
  })

  it('produces real SVG paths without embedding the source bitmap', () => {
    const options = normalizeImageVectorizeOptions({ preset: 'poster', colorCount: 4, detail: 'medium', filterSpeckle: 0 })

    const svg = vectorizePng(sampleBmp(), options)

    expect(svg).toContain('<svg')
    expect(svg).toContain('<path')
    expect(svg).not.toContain('<image')
    expect(svg).not.toContain('data:image/')
  })
})

function sampleBmp(): Uint8Array {
  const width = 16
  const height = 16
  const rowSize = width * 3
  const size = 54 + rowSize * height
  const bytes = Buffer.alloc(size)
  bytes.write('BM')
  bytes.writeUInt32LE(size, 2)
  bytes.writeUInt32LE(54, 10)
  bytes.writeUInt32LE(40, 14)
  bytes.writeInt32LE(width, 18)
  bytes.writeInt32LE(height, 22)
  bytes.writeUInt16LE(1, 26)
  bytes.writeUInt16LE(24, 28)
  bytes.writeUInt32LE(size - 54, 34)
  for (let y = 0; y < height; y += 1) {
    for (let x = 0; x < width; x += 1) {
      const offset = 54 + y * rowSize + x * 3
      bytes[offset] = x < width / 2 ? 20 : 210
      bytes[offset + 1] = 40
      bytes[offset + 2] = x < width / 2 ? 220 : 20
    }
  }
  return bytes
}
