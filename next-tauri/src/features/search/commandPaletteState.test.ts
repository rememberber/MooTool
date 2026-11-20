import { describe, expect, it } from 'vitest'
import { trayCommandActions } from './commandPaletteState'

describe('tray command actions', () => {
  it('uses the Electron defaults when nothing is pinned', () => {
    expect(trayCommandActions(null)).toEqual(['uuid', 'json-format', 'base64-decode'])
    expect(trayCommandActions({ pinnedActionIds: ['missing', 3], recentActionIds: ['uuid'] })).toEqual([
      'uuid',
      'json-format',
      'base64-decode'
    ])
  })

  it('keeps pinned actions in order and drops unknown ids', () => {
    expect(trayCommandActions({ pinnedActionIds: ['base64-decode', 'nope', 'base64-decode', 'timestamp'] })).toEqual([
      'base64-decode',
      'timestamp'
    ])
  })
})