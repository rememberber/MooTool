import { describe, expect, it } from 'vitest'
import { ensureImageDataUrl, processedImageName, scaledDimensions } from './imageTools'

describe('image tools', () => {
  it('creates deterministic processed names', () => {
    expect(processedImageName('moo.JPG', 'compressed', 'auto')).toBe('moo_compressed.jpg')
    expect(processedImageName('moo', 'watermarked', 'png')).toBe('moo_watermarked.png')
  })

  it('clamps scaled dimensions and normalizes base64', () => {
    expect(scaledDimensions(1000, 500, 0.25)).toEqual({ width: 250, height: 125 })
    expect(scaledDimensions(5, 5, 0)).toEqual({ width: 1, height: 1 })
    expect(ensureImageDataUrl('iVBORw0KGgo=')).toBe('data:image/png;base64,iVBORw0KGgo=')
    expect(() => ensureImageDataUrl('not base64!')).toThrow('Base64')
  })
})
