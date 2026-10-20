import { createHash } from 'node:crypto'
import { describe, expect, it } from 'vitest'
import {
  applyColorOperation,
  bestTextColor,
  ColorToolError,
  colorFormats,
  colorThemes,
  contrastRatio,
  createColorScale,
  hslToRgb,
  parseHexColor,
  rgbToHex,
  rgbToHsl,
  standardColors,
  themeShadeColumns
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

  it('applies two-color operations with channel clamping', () => {
    const primary = parseHexColor('#804020')
    const secondary = parseHexColor('#4080FF')
    expect(rgbToHex(applyColorOperation('average', primary, secondary))).toBe('#60608F')
    expect(rgbToHex(applyColorOperation('difference', primary, secondary))).toBe('#4040DF')
    expect(rgbToHex(applyColorOperation('add', primary, secondary))).toBe('#C0C0FF')
    expect(rgbToHex(applyColorOperation('invert', primary, secondary))).toBe('#7FBFDF')
    const a = parseHexColor('#6496C8')
    const b = parseHexColor('#32C864')
    expect(applyColorOperation('intersect', a, b)).toMatchObject({ r: 19, g: 117, b: 78 })
  })

  it('keeps the Java theme tables and standard colors', () => {
    expect(colorThemes.map((theme) => theme.id)).toEqual(['default', 'theme1', 'theme2', 'theme3', 'theme4', 'theme5', 'china'])
    expect(colorThemes[0]?.shades[0]).toEqual(['#808080', '#595959', '#404040', '#262626', '#0D0D0D'])
    expect(createHash('sha256').update(JSON.stringify(colorThemes)).digest('hex')).toBe('72e2218c6dcffce0def99f1317075f2041127035730a0379875f5e875e893575')
    expect(createHash('sha256').update(JSON.stringify(standardColors)).digest('hex')).toBe('1994345359abd00b901c22614eeef6b400775120cebc04083e05bab9875b535c')
  })

  it('builds five shades for every theme color', () => {
    const shades = themeShadeColumns(['#2F6FED', '#FFFFFF'])
    expect(shades).toHaveLength(2)
    expect(shades.every((column) => column.length === 5)).toBe(true)
  })
})
