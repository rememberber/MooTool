export interface RgbaColor {
  a: number
  b: number
  g: number
  r: number
}

export interface HslColor {
  h: number
  l: number
  s: number
}

export interface HsvColor {
  h: number
  s: number
  v: number
}

export interface CmykColor {
  c: number
  k: number
  m: number
  y: number
}

export interface ColorFormats {
  cmyk: string
  hex: string
  hsl: string
  hsv: string
  rgb: string
}

export class ColorToolError extends Error {
  readonly code = 'invalidHex' as const

  constructor() {
    super('COLOR_TOOL_invalidHex')
    this.name = 'ColorToolError'
  }
}

export function parseHexColor(value: string): RgbaColor {
  const compact = value.trim().replace(/^#/, '')
  if (!/^(?:[0-9a-f]{3}|[0-9a-f]{4}|[0-9a-f]{6}|[0-9a-f]{8})$/i.test(compact)) {
    throw new ColorToolError()
  }
  const expanded = compact.length <= 4
    ? [...compact].map((character) => `${character}${character}`).join('')
    : compact
  return {
    r: Number.parseInt(expanded.slice(0, 2), 16),
    g: Number.parseInt(expanded.slice(2, 4), 16),
    b: Number.parseInt(expanded.slice(4, 6), 16),
    a: expanded.length === 8 ? Number.parseInt(expanded.slice(6, 8), 16) / 255 : 1
  }
}

export function rgbToHex({ r, g, b, a }: RgbaColor, includeAlpha = a < 1): string {
  const components = [r, g, b, ...(includeAlpha ? [a * 255] : [])]
  return `#${components.map((value) => clampByte(value).toString(16).padStart(2, '0')).join('')}`.toUpperCase()
}

export function rgbToHsl({ r, g, b }: RgbaColor): HslColor {
  const [red, green, blue] = [r, g, b].map((value) => clampByte(value) / 255)
  const max = Math.max(red, green, blue)
  const min = Math.min(red, green, blue)
  const delta = max - min
  let hue = 0
  if (delta) {
    if (max === red) hue = 60 * (((green - blue) / delta) % 6)
    else if (max === green) hue = 60 * ((blue - red) / delta + 2)
    else hue = 60 * ((red - green) / delta + 4)
  }
  if (hue < 0) hue += 360
  const lightness = (max + min) / 2
  const saturation = delta ? delta / (1 - Math.abs(2 * lightness - 1)) : 0
  return { h: hue, s: saturation * 100, l: lightness * 100 }
}

export function hslToRgb({ h, s, l }: HslColor, alpha = 1): RgbaColor {
  const hue = ((h % 360) + 360) % 360
  const saturation = clamp(s, 0, 100) / 100
  const lightness = clamp(l, 0, 100) / 100
  const chroma = (1 - Math.abs(2 * lightness - 1)) * saturation
  const x = chroma * (1 - Math.abs(((hue / 60) % 2) - 1))
  const match = lightness - chroma / 2
  const [red, green, blue] = hue < 60 ? [chroma, x, 0]
    : hue < 120 ? [x, chroma, 0]
      : hue < 180 ? [0, chroma, x]
        : hue < 240 ? [0, x, chroma]
          : hue < 300 ? [x, 0, chroma]
            : [chroma, 0, x]
  return { r: (red + match) * 255, g: (green + match) * 255, b: (blue + match) * 255, a: alpha }
}

export function rgbToHsv({ r, g, b, a }: RgbaColor): HsvColor {
  const hsl = rgbToHsl({ r, g, b, a })
  const [red, green, blue] = [r, g, b].map((value) => clampByte(value) / 255)
  const value = Math.max(red, green, blue)
  const delta = value - Math.min(red, green, blue)
  return { h: hsl.h, s: value ? (delta / value) * 100 : 0, v: value * 100 }
}

export function rgbToCmyk({ r, g, b }: RgbaColor): CmykColor {
  const [red, green, blue] = [r, g, b].map((value) => clampByte(value) / 255)
  const key = 1 - Math.max(red, green, blue)
  if (key === 1) return { c: 0, m: 0, y: 0, k: 100 }
  return {
    c: ((1 - red - key) / (1 - key)) * 100,
    m: ((1 - green - key) / (1 - key)) * 100,
    y: ((1 - blue - key) / (1 - key)) * 100,
    k: key * 100
  }
}

export function colorFormats(color: RgbaColor): ColorFormats {
  const hsl = rgbToHsl(color)
  const hsv = rgbToHsv(color)
  const cmyk = rgbToCmyk(color)
  return {
    hex: rgbToHex(color),
    rgb: color.a < 1
      ? `rgba(${clampByte(color.r)}, ${clampByte(color.g)}, ${clampByte(color.b)}, ${round(color.a, 3)})`
      : `rgb(${clampByte(color.r)}, ${clampByte(color.g)}, ${clampByte(color.b)})`,
    hsl: `hsl(${round(hsl.h)}, ${round(hsl.s)}%, ${round(hsl.l)}%)`,
    hsv: `hsv(${round(hsv.h)}, ${round(hsv.s)}%, ${round(hsv.v)}%)`,
    cmyk: `cmyk(${round(cmyk.c)}%, ${round(cmyk.m)}%, ${round(cmyk.y)}%, ${round(cmyk.k)}%)`
  }
}

export function createColorScale(color: RgbaColor): string[] {
  const hsl = rgbToHsl(color)
  return [96, 90, 80, 70, 60, 50, 40, 30, 20, 12].map((lightness) => (
    rgbToHex(hslToRgb({ ...hsl, l: lightness }))
  ))
}

export function contrastRatio(foreground: RgbaColor, background: RgbaColor): number {
  const composed = composite(foreground, background)
  const first = luminance(composed)
  const second = luminance(background)
  return (Math.max(first, second) + 0.05) / (Math.min(first, second) + 0.05)
}

export function bestTextColor(background: RgbaColor): '#000000' | '#FFFFFF' {
  const black = contrastRatio(parseHexColor('#000000'), background)
  const white = contrastRatio(parseHexColor('#FFFFFF'), background)
  return black >= white ? '#000000' : '#FFFFFF'
}

export function randomColor(): string {
  const bytes = crypto.getRandomValues(new Uint8Array(3))
  return rgbToHex({ r: bytes[0], g: bytes[1], b: bytes[2], a: 1 })
}

function composite(foreground: RgbaColor, background: RgbaColor): RgbaColor {
  const alpha = clamp(foreground.a, 0, 1)
  return {
    r: foreground.r * alpha + background.r * (1 - alpha),
    g: foreground.g * alpha + background.g * (1 - alpha),
    b: foreground.b * alpha + background.b * (1 - alpha),
    a: 1
  }
}

function luminance(color: RgbaColor): number {
  const channels = [color.r, color.g, color.b].map((value) => {
    const normalized = clampByte(value) / 255
    return normalized <= 0.04045
      ? normalized / 12.92
      : ((normalized + 0.055) / 1.055) ** 2.4
  })
  return channels[0] * 0.2126 + channels[1] * 0.7152 + channels[2] * 0.0722
}

function clampByte(value: number): number {
  return Math.round(clamp(value, 0, 255))
}

function clamp(value: number, min: number, max: number): number {
  return Math.min(max, Math.max(min, value))
}

function round(value: number, precision = 1): number {
  return Number(value.toFixed(precision))
}
