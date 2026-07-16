import { describe, expect, it } from 'vitest'
import { applyColorOperation, bestTextColor, formatColor, parseColor } from './colorTools'

describe('color tools', () => {
  it('parses short/long hex and RGB strings', () => {
    expect(parseColor('#0f8')).toEqual({ r: 0, g: 255, b: 136 })
    expect(parseColor('12, 34, 56')).toEqual({ r: 12, g: 34, b: 56 })
    expect(formatColor({ r: 10, g: 187, b: 204 }, 'HEX_UPPER')).toBe('#0ABBCC')
    expect(formatColor({ r: 10, g: 187, b: 204 }, 'RGB')).toBe('10, 187, 204')
  })

  it('performs all Java color operations', () => {
    const a = { r: 100, g: 150, b: 200 }
    const b = { r: 50, g: 200, b: 100 }
    expect(applyColorOperation('invert', a, b)).toEqual({ r: 155, g: 105, b: 55 })
    expect(applyColorOperation('intersect', a, b)).toEqual({ r: 19, g: 117, b: 78 })
    expect(applyColorOperation('add', a, b)).toEqual({ r: 150, g: 255, b: 255 })
    expect(applyColorOperation('difference', a, b)).toEqual({ r: 50, g: 50, b: 100 })
    expect(applyColorOperation('average', a, b)).toEqual({ r: 75, g: 175, b: 150 })
  })

  it('chooses readable preview text', () => {
    expect(bestTextColor(parseColor('#ffffff'))).toBe('#000000')
    expect(bestTextColor(parseColor('#111111'))).toBe('#FFFFFF')
  })
})
