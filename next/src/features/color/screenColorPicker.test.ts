import { describe, expect, it } from 'vitest'
import { rgbToHex, sampleScreenColor } from './screenColorPicker'

describe('screen color picker', () => {
  it('samples and formats an RGB pixel', () => {
    const pixels = new Uint8ClampedArray([
      10, 20, 30, 255,
      222, 143, 125, 255
    ])
    expect(sampleScreenColor(pixels, 2, 1, 1, 0)).toEqual({
      hex: '#DE8F7D', r: 222, g: 143, b: 125
    })
  })

  it('clamps sample coordinates and RGB values', () => {
    const pixels = new Uint8ClampedArray([1, 2, 3, 255])
    expect(sampleScreenColor(pixels, 1, 1, 99, -4)?.hex).toBe('#010203')
    expect(rgbToHex(-10, 16.4, 300)).toBe('#0010FF')
  })

  it('rejects incomplete image data', () => {
    expect(sampleScreenColor(new Uint8ClampedArray(3), 1, 1, 0, 0)).toBeNull()
  })
})
