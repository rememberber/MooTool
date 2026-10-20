import { describe, expect, it } from 'vitest'
import { ensureImageDataUrl, overwriteImageName, processedImageName, resizeGeometry, scaledDimensions, type ResizeImageOptions } from './imageTools'

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

  it('names resized copies with the selected format', () => {
    expect(processedImageName('photo.jpg', 'resized')).toBe('photo_resized.jpg')
    expect(processedImageName('logo.png', 'resized', 'jpeg')).toBe('logo_resized.jpg')
  })

  it('overwrites the original name unless the format changes the extension', () => {
    expect(overwriteImageName('photo.jpg')).toBe('photo.jpg')
    expect(overwriteImageName('photo.jpeg')).toBe('photo.jpeg')
    expect(overwriteImageName('logo.png', 'jpeg')).toBe('logo.jpg')
    expect(overwriteImageName('scan.webp')).toBe('scan.png')
    expect(overwriteImageName('scan.webp', 'png')).toBe('scan.png')
  })
})

describe('image resize geometry', () => {
  const options: ResizeImageOptions = { width: 800, height: 600, fit: 'fit', shrinkOnly: false, format: 'auto' }

  it('fits landscape and portrait images without cropping or distortion', () => {
    expect(resizeGeometry(1600, 900, options)).toMatchObject({ width: 800, height: 450, source: { x: 0, y: 0, width: 1600, height: 900 } })
    expect(resizeGeometry(900, 1600, options)).toMatchObject({ width: 338, height: 600 })
  })

  it('fills the target with a centered crop', () => {
    const result = resizeGeometry(1600, 800, { ...options, fit: 'fill' })
    expect(result).toMatchObject({ width: 800, height: 600, source: { y: 0, height: 800 } })
    expect(result.source.x).toBeCloseTo(800 / 3)
    expect(result.source.width).toBeCloseTo(3200 / 3)
    expect(resizeGeometry(400, 800, { ...options, fit: 'fill' }).source).toEqual({ x: 0, y: 250, width: 400, height: 300 })
  })

  it('stretches directly and calculates a missing dimension proportionally', () => {
    expect(resizeGeometry(1600, 900, { ...options, fit: 'stretch' })).toMatchObject({ width: 800, height: 600 })
    expect(resizeGeometry(1600, 900, { ...options, height: null })).toMatchObject({ width: 800, height: 450 })
    expect(resizeGeometry(1600, 900, { ...options, width: null })).toMatchObject({ width: 1067, height: 600 })
  })

  it('never enlarges pixels in any shrink-only mode', () => {
    expect(resizeGeometry(400, 200, { ...options, shrinkOnly: true })).toMatchObject({ width: 400, height: 200 })
    expect(resizeGeometry(400, 200, { ...options, fit: 'fill', shrinkOnly: true })).toMatchObject({ width: 267, height: 200 })
    expect(resizeGeometry(400, 800, { ...options, fit: 'stretch', shrinkOnly: true })).toMatchObject({ width: 400, height: 600 })
  })

  it('rejects missing, non-finite, fractional or out-of-range targets', () => {
    for (const value of [0, -1, 0.5, Number.NaN, Number.POSITIVE_INFINITY, 16385]) {
      expect(() => resizeGeometry(1600, 900, { ...options, width: value })).toThrow('resizeInvalid')
    }
    expect(() => resizeGeometry(1600, 900, { ...options, width: null, height: null })).toThrow('resizeInvalid')
  })

  it('bounds actual output dimensions and area, including automatic dimensions', () => {
    expect(() => resizeGeometry(200, 1, { ...options, width: null, height: 1000 })).toThrow('resizeTooLarge')
    expect(() => resizeGeometry(8000, 8000, { ...options, width: 8000, height: 8000 })).toThrow('resizeTooLarge')
    expect(resizeGeometry(400, 200, { ...options, width: 16384, height: 16384, shrinkOnly: true })).toMatchObject({ width: 400, height: 200 })
    expect(resizeGeometry(1, 10000, { ...options, width: 1, height: 1, fit: 'fill', shrinkOnly: true })).toMatchObject({ width: 1, height: 1 })
  })
})
