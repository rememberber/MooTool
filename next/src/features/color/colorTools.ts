export type RgbColor = { r: number; g: number; b: number }
export type ColorFormat = 'HEX_UPPER' | 'HEX_LOWER' | 'RGB'
export type ColorOperation = 'invert' | 'intersect' | 'add' | 'difference' | 'average'

export const standardColors = ['#C00000', '#FF0000', '#FFC000', '#FFFF00', '#92D050', '#00B050', '#00B0F0', '#0070C0', '#002060', '#7030A0']

const themeMain = {
  default: ['#000000', '#FFFFFF', '#880015', '#ED1C24', '#FF7F27', '#FFF200', '#22B14C', '#00A2E8', '#3F48CC', '#A349A4'],
  theme1: ['#FFFFFF', '#000000', '#1F497D', '#EEECE1', '#4F81BD', '#C0504D', '#9BBB59', '#8064A2', '#4BACC6', '#F79646'],
  theme2: ['#FFFFFF', '#000000', '#69676D', '#C9C2D1', '#CEB966', '#9CB084', '#6BB1C9', '#6585CF', '#7E6BC9', '#A379BB'],
  theme3: ['#FFFFFF', '#000000', '#323232', '#E3DED1', '#F07F09', '#9F2936', '#1B587C', '#4E8542', '#604878', '#C19859'],
  theme4: ['#FFFFFF', '#000000', '#646B86', '#C5D1D7', '#D16349', '#CCB400', '#8CADAE', '#8C7B70', '#8FB08C', '#D19049'],
  theme5: ['#FFFFFF', '#000000', '#464646', '#DEF5FA', '#2DA2BF', '#DA1F28', '#EB641B', '#39639D', '#474B78', '#7D3C4A'],
  china: ['#FFFEF9', '#3D3B4F', '#9D2933', '#FF461F', '#C91F37', '#CA6924', '#F0C239', '#789262', '#177CB0', '#815463']
} as const

export type ColorThemeId = keyof typeof themeMain

export const colorThemes: Array<{ id: ColorThemeId; main: string[]; shades: string[][] }> = Object.entries(themeMain).map(([id, main]) => ({
  id: id as ColorThemeId,
  main: [...main],
  shades: main.map((color) => createShades(color))
}))

export function parseColor(input: string): RgbColor {
  const value = input.trim().replace(/，/g, ',')
  if (value.includes(',')) {
    const parts = value.replace(/^rgba?\(|\)$/gi, '').split(',').map((part) => Number(part.trim()))
    if (parts.length < 3 || parts.slice(0, 3).some((part) => !Number.isInteger(part) || part < 0 || part > 255)) throw new Error('RGB values must be integers from 0 to 255')
    return { r: parts[0], g: parts[1], b: parts[2] }
  }
  const hex = value.replace(/^#/, '')
  if (!/^(?:[0-9a-fA-F]{3}|[0-9a-fA-F]{6})$/.test(hex)) throw new Error('Color must be #RGB, #RRGGBB or R,G,B')
  const expanded = hex.length === 3 ? [...hex].map((char) => `${char}${char}`).join('') : hex
  return { r: Number.parseInt(expanded.slice(0, 2), 16), g: Number.parseInt(expanded.slice(2, 4), 16), b: Number.parseInt(expanded.slice(4, 6), 16) }
}

export function formatColor(color: RgbColor, format: ColorFormat): string {
  if (format === 'RGB') return `${color.r}, ${color.g}, ${color.b}`
  const hex = `#${[color.r, color.g, color.b].map((channel) => clamp(channel).toString(16).padStart(2, '0')).join('')}`
  return format === 'HEX_UPPER' ? hex.toUpperCase() : hex.toLowerCase()
}

export function applyColorOperation(operation: ColorOperation, primary: RgbColor, secondary: RgbColor): RgbColor {
  switch (operation) {
    case 'invert': return { r: 255 - primary.r, g: 255 - primary.g, b: 255 - primary.b }
    case 'intersect': return { r: Math.floor(primary.r * secondary.r / 255), g: Math.floor(primary.g * secondary.g / 255), b: Math.floor(primary.b * secondary.b / 255) }
    case 'add': return { r: clamp(primary.r + secondary.r), g: clamp(primary.g + secondary.g), b: clamp(primary.b + secondary.b) }
    case 'difference': return { r: Math.abs(primary.r - secondary.r), g: Math.abs(primary.g - secondary.g), b: Math.abs(primary.b - secondary.b) }
    case 'average': return { r: Math.floor((primary.r + secondary.r) / 2), g: Math.floor((primary.g + secondary.g) / 2), b: Math.floor((primary.b + secondary.b) / 2) }
  }
}

export function bestTextColor(color: RgbColor): '#000000' | '#FFFFFF' {
  const luminance = (0.2126 * color.r + 0.7152 * color.g + 0.0722 * color.b) / 255
  return luminance > 0.55 ? '#000000' : '#FFFFFF'
}

function createShades(hex: string): string[] {
  const color = parseColor(hex)
  return [0.78, 0.55, 0.3].map((ratio) => formatColor(blend(color, { r: 255, g: 255, b: 255 }, ratio), 'HEX_UPPER'))
    .concat([0.25, 0.5].map((ratio) => formatColor(blend(color, { r: 0, g: 0, b: 0 }, ratio), 'HEX_UPPER')))
}

function blend(color: RgbColor, target: RgbColor, ratio: number): RgbColor {
  return {
    r: Math.round(color.r + (target.r - color.r) * ratio),
    g: Math.round(color.g + (target.g - color.g) * ratio),
    b: Math.round(color.b + (target.b - color.b) * ratio)
  }
}

function clamp(value: number): number {
  return Math.min(255, Math.max(0, Math.round(value)))
}
