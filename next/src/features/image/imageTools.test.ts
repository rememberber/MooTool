import { describe, expect, it } from 'vitest'
import { ensureImageDataUrl, processedImageName, scaledDimensions, watermarkAnchor } from './imageTools'

describe('image tools', () => {
  it('calculates bounded output dimensions', () => {
    expect(scaledDimensions(1200, 800, 0.5)).toEqual({ width: 600, height: 400 })
    expect(scaledDimensions(3, 3, 0)).toEqual({ width: 1, height: 1 })
  })

  it('builds keep-original output names', () => {
    expect(processedImageName('photo.jpg', 'compressed')).toBe('photo_compressed.jpg')
    expect(processedImageName('logo.png', 'watermarked', 'jpeg')).toBe('logo_watermarked.jpg')
  })

  it('normalizes raw Base64 and preserves image data URLs', () => {
    expect(ensureImageDataUrl('YWJj')).toBe('data:image/png;base64,YWJj')
    expect(ensureImageDataUrl('data:image/jpeg;base64,YWJj')).toBe('data:image/jpeg;base64,YWJj')
  })

  it('positions watermarks at Java-compatible anchors', () => {
    expect(watermarkAnchor(1000, 600, 200, 40, 'bottom-right', 20)).toEqual({ x: 780, y: 580 })
    expect(watermarkAnchor(1000, 600, 200, 40, 'center', 20)).toEqual({ x: 400, y: 320 })
  })
})
