import { describe, expect, it } from 'vitest'
import { ensureImageDataUrl, processedImageName, resizeGeometry, scaledDimensions, watermarkAnchor, type ResizeImageOptions } from './imageTools'

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
    for (const value of [0, -1, 0.5, NaN, Infinity, 16385]) expect(() => resizeGeometry(1600, 900, { ...options, width: value })).toThrow('image.resize.invalid')
    expect(() => resizeGeometry(1600, 900, { ...options, width: null, height: null })).toThrow('image.resize.invalid')
  })
  it('bounds actual output dimensions and area, including automatic dimensions', () => {
    expect(() => resizeGeometry(200, 1, { ...options, width: null, height: 1000 })).toThrow('image.resize.tooLarge')
    expect(() => resizeGeometry(8000, 8000, { ...options, width: 8000, height: 8000 })).toThrow('image.resize.tooLarge')
    expect(resizeGeometry(400, 200, { ...options, width: 16384, height: 16384, shrinkOnly: true })).toMatchObject({ width: 400, height: 200 })
    expect(resizeGeometry(1, 10000, { ...options, width: 1, height: 1, fit: 'fill', shrinkOnly: true })).toMatchObject({ width: 1, height: 1 })
  })
})
