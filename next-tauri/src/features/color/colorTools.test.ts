import { describe, expect, it } from 'vitest'
import {
  bestTextColor,
  ColorToolError,
  colorFormats,
  contrastRatio,
  createColorScale,
  hslToRgb,
  parseHexColor,
  rgbToHex,
  rgbToHsl
} from './colorTools'

describe('color tools', () => {
  it('parses short and alpha hex colors', () => {
    expect(parseHexColor('#0f8')).toEqual({ r: 0, g: 255, b: 136, a: 1 })
    expect(parseHexColor('#33669980')).toMatchObject({ r: 51, g: 102, b: 153 })
    expect(parseHexColor('#33669980').a).toBeCloseTo(0.502)
    expect(rgbToHex(parseHexColor('#33669980'))).toBe('#33669980')
  })

  it('round-trips HSL and exposes common formats', () => {
    const source = parseHexColor('#2F6FED')
    const hsl = rgbToHsl(source)
    expect(rgbToHex(hslToRgb(hsl))).toBe('#2F6FED')
    expect(colorFormats(source)).toMatchObject({
      hex: '#2F6FED',
      rgb: 'rgb(47, 111, 237)'
    })
  })

  it('generates a ten-step scale', () => {
    const scale = createColorScale(parseHexColor('#2F6FED'))
    expect(scale).toHaveLength(10)
    expect(new Set(scale).size).toBe(10)
    expect(scale.every((value) => /^#[0-9A-F]{6}$/.test(value))).toBe(true)
  })

  it('computes WCAG contrast and the best text color', () => {
    expect(contrastRatio(parseHexColor('#000'), parseHexColor('#fff'))).toBeCloseTo(21)
    expect(bestTextColor(parseHexColor('#FFFFFF'))).toBe('#000000')
    expect(bestTextColor(parseHexColor('#101828'))).toBe('#FFFFFF')
  })

  it('rejects malformed colors', () => {
    expect(() => parseHexColor('blue')).toThrow(ColorToolError)
  })
})
