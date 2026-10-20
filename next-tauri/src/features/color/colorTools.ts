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

export type ColorOperation = 'invert' | 'intersect' | 'add' | 'difference' | 'average'
export type ColorThemeId = 'default' | 'theme1' | 'theme2' | 'theme3' | 'theme4' | 'theme5' | 'china'

export const standardColors = ['#C00000', '#FF0000', '#FFC000', '#FFFF00', '#92D050', '#00B050', '#00B0F0', '#0070C0', '#002060', '#7030A0']

export const colorThemes: ReadonlyArray<{
  id: ColorThemeId
  main: readonly string[]
  shades: ReadonlyArray<readonly string[]>
}> = [
  {
    id: 'default',
    main: ['#000000', '#FFFFFF', '#880015', '#ED1C24', '#FF7F27', '#FFF200', '#22B14C', '#00A2E8', '#3F48CC', '#A349A4'],
    shades: [
      ['#808080', '#595959', '#404040', '#262626', '#0D0D0D'],
      ['#F2F2F2', '#D9D9D9', '#BFBFBF', '#A6A6A6', '#808080'],
      ['#E7B9C0', '#CF7C89', '#B84A5B', '#660010', '#44000A'],
      ['#FBCFD0', '#F8A1A4', '#F47378', '#B21016', '#77070B'],
      ['#FFE5D4', '#FFCCA9', '#FFB27D', '#BF5B16', '#803A0A'],
      ['#FFFCCC', '#FFFA99', '#FFF766', '#BFB500', '#807900'],
      ['#C8EFD4', '#98E0AD', '#6BD089', '#138535', '#085820'],
      ['#C8EBFA', '#94D8F6', '#60C5F1', '#007AAE', '#005174'],
      ['#D3D5F5', '#AAAEEB', '#8389E0', '#232B99', '#101566'],
      ['#EDD3ED', '#DAAADB', '#C785C8', '#7A297B', '#511252']
    ]
  },
  {
    id: 'theme1',
    main: ['#FFFFFF', '#000000', '#1F497D', '#EEECE1', '#4F81BD', '#C0504D', '#9BBB59', '#8064A2', '#4BACC6', '#F79646'],
    shades: [
      ['#F2F2F2', '#D9D9D9', '#BFBFBF', '#A6A6A6', '#808080'],
      ['#808080', '#595959', '#404040', '#262626', '#0D0D0D'],
      ['#C3D2E5', '#8EA9CB', '#6185B1', '#11345E', '#08203E'],
      ['#D6D1B6', '#B2AA7E', '#776D38', '#3C350E', '#181502'],
      ['#D6E3F2', '#B0C8E5', '#8CAED7', '#2D598E', '#14365F'],
      ['#F2D6D5', '#E6B0AF', '#D98D8B', '#902E2B', '#601513'],
      ['#E9F1D8', '#D4E4B4', '#C0D693', '#6F8C32', '#465D16'],
      ['#E2DAEC', '#C8B9DA', '#AE99C7', '#553879', '#321951'],
      ['#D6EEF4', '#AEDCE8', '#8BCCDD', '#2A7E95', '#135263'],
      ['#FDE9D9', '#FCD4B4', '#FABF8F', '#B96927', '#7C4212']
    ]
  },
  {
    id: 'theme2',
    main: ['#FFFFFF', '#000000', '#69676D', '#C9C2D1', '#CEB966', '#9CB084', '#6BB1C9', '#6585CF', '#7E6BC9', '#A379BB'],
    shades: [
      ['#F2F2F2', '#D9D9D9', '#BFBFBF', '#A6A6A6', '#808080'],
      ['#808080', '#595959', '#404040', '#262626', '#0D0D0D'],
      ['#E0E0E2', '#C2C1C5', '#A3A1A7', '#423A52', '#231A36'],
      ['#AB9DBC', '#846D9D', '#4A3068', '#1F0C34', '#0B0215'],
      ['#F5F0DC', '#EBE1BC', '#E2D49E', '#9B873A', '#67571A'],
      ['#EAEFE3', '#D5DFC9', '#C2D0B1', '#6A844A', '#3F5821'],
      ['#DDEEF4', '#BDDEE9', '#A0CFDF', '#3C8097', '#1B5164'],
      ['#DCE3F5', '#BCCAEC', '#9DB2E2', '#39569B', '#193168'],
      ['#E2DDF4', '#C6BDE9', '#ADA0DF', '#4F3C97', '#291B64'],
      ['#EBE0F1', '#D8C4E4', '#C6A9D6', '#72448C', '#461E5D']
    ]
  },
  {
    id: 'theme3',
    main: ['#FFFFFF', '#000000', '#323232', '#E3DED1', '#F07F09', '#9F2936', '#1B587C', '#4E8542', '#604878', '#C19859'],
    shades: [
      ['#F2F2F2', '#D9D9D9', '#BFBFBF', '#A6A6A6', '#808080'],
      ['#808080', '#595959', '#404040', '#262626', '#0D0D0D'],
      ['#D6D6D6', '#ADADAD', '#848484', '#262626', '#191919'],
      ['#CCC2A9', '#AA9B75', '#716034', '#392D0D', '#171102'],
      ['#FCE4CB', '#F9CA99', '#F6B168', '#B45E05', '#783E02'],
      ['#ECC9CD', '#D999A0', '#C56D77', '#771722', '#500A12'],
      ['#C1D8E5', '#8BB3CB', '#5D91B0', '#0F405D', '#07293E'],
      ['#D4E7D0', '#ACCEA4', '#89B67F', '#306425', '#1A4311'],
      ['#DBD2E4', '#B9A9C9', '#9984AE', '#41285A', '#27123C'],
      ['#F3E9D9', '#E6D2B4', '#DABE94', '#916C32', '#604316']
    ]
  },
  {
    id: 'theme4',
    main: ['#FFFFFF', '#000000', '#646B86', '#C5D1D7', '#D16349', '#CCB400', '#8CADAE', '#8C7B70', '#8FB08C', '#D19049'],
    shades: [
      ['#F2F2F2', '#D9D9D9', '#BFBFBF', '#A6A6A6', '#808080'],
      ['#808080', '#595959', '#404040', '#262626', '#0D0D0D'],
      ['#DBDEE7', '#BABECF', '#9AA0B6', '#394265', '#192243'],
      ['#9FB6C1', '#6F90A1', '#31586C', '#0C2836', '#020F16'],
      ['#F6DCD6', '#EDBBAF', '#E39B8A', '#9D3F29', '#682312'],
      ['#F5EFC4', '#EBE08D', '#E0D05A', '#998700', '#665A00'],
      ['#E6EFEF', '#CEDEDF', '#B6CDCE', '#4E8082', '#235557'],
      ['#E8E2DF', '#D1C7C0', '#BAACA4', '#69503F', '#462D1C'],
      ['#E6EFE5', '#CEDFCD', '#B9D0B6', '#53844F', '#275823'],
      ['#F6E7D6', '#EDD0AF', '#E3B98A', '#9D6629', '#683F12']
    ]
  },
  {
    id: 'theme5',
    main: ['#FFFFFF', '#000000', '#464646', '#DEF5FA', '#2DA2BF', '#DA1F28', '#EB641B', '#39639D', '#474B78', '#7D3C4A'],
    shades: [
      ['#F2F2F2', '#D9D9D9', '#BFBFBF', '#A6A6A6', '#808080'],
      ['#808080', '#595959', '#404040', '#262626', '#0D0D0D'],
      ['#DADADA', '#B5B5B5', '#909090', '#353535', '#232323'],
      ['#B4D9E1', '#7DB0BB', '#37717D', '#0E353E', '#021519'],
      ['#CDEBF2', '#9FD7E5', '#75C5D9', '#19788F', '#0B4F60'],
      ['#F8CDD0', '#F09EA2', '#E97177', '#A31118', '#6D080D'],
      ['#FBDECF', '#F7BEA0', '#F39F72', '#B0480F', '#752D07'],
      ['#CDDAEB', '#A1B8D8', '#7999C4', '#204476', '#0E294F'],
      ['#D1D3E4', '#A8ABC9', '#8387AE', '#282C5A', '#12153C'],
      ['#E5CDD2', '#CBA1AA', '#B17A86', '#5E222F', '#3E0F19']
    ]
  },
  {
    id: 'china',
    main: ['#FFFEF9', '#3D3B4F', '#9D2933', '#FF461F', '#C91F37', '#CA6924', '#F0C239', '#789262', '#177CB0', '#815463'],
    shades: [
      ['#FFFEFD', '#FFFEF9', '#FFF8EF', '#FFF2E5', '#FFE8D5'],
      ['#D5D4DC', '#ACABB9', '#828190', '#59576C', '#3D3B4F'],
      ['#EBD4D6', '#D7A9AD', '#C37E84', '#AF535C', '#7A1E26'],
      ['#FFE2D9', '#FFBFAD', '#FF9C81', '#FF7955', '#CC2E08'],
      ['#F2D0D4', '#E5A1A9', '#D8727E', '#B83D4D', '#8F1525'],
      ['#F5E6D9', '#EBCCB3', '#E1B28D', '#C48542', '#8F5118'],
      ['#FCF3D4', '#F8E7A9', '#F4DB7E', '#D9AE2A', '#A67E15'],
      ['#E5EDE0', '#CBDBC1', '#B1C9A2', '#93A97E', '#5A7348'],
      ['#D5EBF4', '#ABDAEA', '#81C9E0', '#45A8CC', '#0E5F85'],
      ['#E5D8DD', '#CBACB8', '#B18099', '#946B7B', '#5C3445']
    ]
  }
]

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

export function applyColorOperation(operation: ColorOperation, primary: RgbaColor, secondary: RgbaColor): RgbaColor {
  if (operation === 'invert') return { r: 255 - primary.r, g: 255 - primary.g, b: 255 - primary.b, a: primary.a }
  if (operation === 'intersect') return { r: Math.floor(primary.r * secondary.r / 255), g: Math.floor(primary.g * secondary.g / 255), b: Math.floor(primary.b * secondary.b / 255), a: 1 }
  if (operation === 'add') return { r: clampByte(primary.r + secondary.r), g: clampByte(primary.g + secondary.g), b: clampByte(primary.b + secondary.b), a: 1 }
  if (operation === 'difference') return { r: Math.abs(primary.r - secondary.r), g: Math.abs(primary.g - secondary.g), b: Math.abs(primary.b - secondary.b), a: 1 }
  return { r: Math.floor((primary.r + secondary.r) / 2), g: Math.floor((primary.g + secondary.g) / 2), b: Math.floor((primary.b + secondary.b) / 2), a: 1 }
}

export function themeShadeColumns(colors: readonly string[]): string[][] {
  return colors.map((color) => {
    const scale = createColorScale(parseHexColor(color))
    return [scale[1], scale[3], scale[5], scale[7], scale[9]]
  })
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
