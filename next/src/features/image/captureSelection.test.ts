import { describe, expect, it } from 'vitest'
import { captureRectFromPoints, clampCaptureRect, moveCaptureRect, resizeCaptureRect } from './captureSelection'

const bounds = { width: 1920, height: 1080 }

describe('capture selection geometry', () => {
  it('creates a normalized rectangle regardless of drag direction', () => {
    expect(captureRectFromPoints({ x: 900, y: 700 }, { x: 200, y: 100 }, bounds)).toEqual({
      x: 200, y: 100, width: 700, height: 600
    })
  })

  it('clamps numeric crop fields to the captured image', () => {
    expect(clampCaptureRect({ x: 1900, y: -20, width: 200, height: 0 }, bounds)).toEqual({
      x: 1900, y: 0, width: 20, height: 1
    })
  })

  it('moves a selection without allowing it outside the image', () => {
    expect(moveCaptureRect({ x: 100, y: 100, width: 500, height: 300 }, { x: 2000, y: -500 }, bounds)).toEqual({
      x: 1420, y: 0, width: 500, height: 300
    })
  })

  it('resizes from a corner while preserving a valid selection', () => {
    expect(resizeCaptureRect({ x: 100, y: 100, width: 500, height: 300 }, 'nw', { x: 700, y: 500 }, bounds)).toEqual({
      x: 599, y: 399, width: 1, height: 1
    })
    expect(resizeCaptureRect({ x: 100, y: 100, width: 500, height: 300 }, 'se', { x: 2000, y: 2000 }, bounds)).toEqual({
      x: 100, y: 100, width: 1820, height: 980
    })
  })
})
