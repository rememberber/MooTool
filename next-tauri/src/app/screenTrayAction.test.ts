import { describe, expect, it } from 'vitest'
import { claimScreenAction, publishScreenAction, screenTrayAction, subscribeScreenAction } from './screenTrayAction'

describe('tray screen actions', () => {
  it('accepts only the color and capture actions', () => {
    expect(screenTrayAction('color')).toBe('color')
    expect(screenTrayAction('capture')).toBe('capture')
    expect(screenTrayAction('image')).toBeNull()
    expect(screenTrayAction(null)).toBeNull()
  })

  it('delivers a pending capture once and ignores the other tool', () => {
    const request = publishScreenAction('capture')
    const seen: string[] = []
    const stopImage = subscribeScreenAction((item) => {
      if (item.kind !== 'capture' || !claimScreenAction(item)) return
      seen.push(`image:${item.sequence}`)
    })
    const stopColor = subscribeScreenAction((item) => {
      if (item.kind !== 'color' || !claimScreenAction(item)) return
      seen.push(`color:${item.sequence}`)
    })
    expect(seen).toEqual([`image:${request.sequence}`])
    expect(claimScreenAction(request)).toBe(false)
    stopImage()
    stopColor()
  })
})
