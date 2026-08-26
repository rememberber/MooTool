import { describe, expect, it } from 'vitest'
import { isWindowControlsHoverTarget } from './windowControlsHover'

const bounds = { x: 200, y: 100, width: 1100, height: 760 }

describe('detached window controls hover target', () => {
  it('keeps the controls visible across the complete native traffic-light area', () => {
    expect(isWindowControlsHoverTarget({ x: 200, y: 100 }, bounds)).toBe(true)
    expect(isWindowControlsHoverTarget({ x: 218, y: 118 }, bounds)).toBe(true)
    expect(isWindowControlsHoverTarget({ x: 304, y: 158 }, bounds)).toBe(true)
  })

  it('keeps the existing draggable brand region as a reveal target', () => {
    expect(isWindowControlsHoverTarget({ x: 420, y: 130 }, bounds)).toBe(true)
  })

  it('hides the controls after the cursor leaves both top-left regions', () => {
    expect(isWindowControlsHoverTarget({ x: 700, y: 130 }, bounds)).toBe(false)
    expect(isWindowControlsHoverTarget({ x: 250, y: 180 }, bounds)).toBe(false)
  })
})
