import { describe, expect, it } from 'vitest'
import { actionRegistry } from '@/app/actionRegistry'
import { commandActionIds, commandActionTitles, defaultCommandPaletteState, normalizeCommandPaletteState, orderedCommandActions, trayCommandActions, updateCommandPaletteState } from './commandPalette'

describe('command palette preferences', () => {
  it('normalizes stored metadata and drops invalid ids, duplicates and content', () => {
    expect(normalizeCommandPaletteState({ pinnedActionIds: ['uuid', 'uuid', 'bad'], recentActionIds: ['json-format', null, 'bad'], input: 'secret', output: 'secret' }))
      .toEqual({ pinnedActionIds: ['uuid'], recentActionIds: ['json-format'] })
    expect(normalizeCommandPaletteState(null)).toEqual(defaultCommandPaletteState)
    expect(commandActionIds).toEqual(actionRegistry.map((action) => action.id))
    for (const action of actionRegistry) expect(commandActionTitles[action.id]).toBe(action.titleKey)
  })

  it('records at most five distinct actions, with repeat executions moved to the front', () => {
    let state = defaultCommandPaletteState
    for (const actionId of commandActionIds.slice(0, 6)) state = updateCommandPaletteState(state, { type: 'record', actionId, argument: 'secret' })
    expect(state.recentActionIds).toEqual(['base64-decode', 'base64-encode', 'json-minify', 'json-format', 'timestamp'])
    state = updateCommandPaletteState(state, { type: 'record', actionId: 'json-format' })
    expect(state.recentActionIds).toEqual(['json-format', 'base64-decode', 'base64-encode', 'json-minify', 'timestamp'])
    expect(JSON.stringify(state)).not.toContain('secret')
  })

  it('pins independently of recent history, never duplicates entries and clears only recents', () => {
    let state = updateCommandPaletteState(defaultCommandPaletteState, { type: 'pin', actionId: 'uuid', pinned: true })
    state = updateCommandPaletteState(state, { type: 'pin', actionId: 'uuid', pinned: true })
    state = updateCommandPaletteState(state, { type: 'record', actionId: 'uuid' })
    state = updateCommandPaletteState(state, { type: 'record', actionId: 'timestamp' })
    const ordered = orderedCommandActions(state)
    expect(ordered.slice(0, 2)).toEqual([{ actionId: 'uuid', section: 'pinned' }, { actionId: 'timestamp', section: 'recent' }])
    expect(ordered).toHaveLength(commandActionIds.length)
    expect(trayCommandActions(state)).toEqual(['uuid'])
    expect(updateCommandPaletteState(state, { type: 'clear-recent' })).toEqual({ pinnedActionIds: ['uuid'], recentActionIds: [] })
    expect(updateCommandPaletteState(state, { type: 'pin', actionId: 'uuid', pinned: false }).pinnedActionIds).toEqual([])
    expect(trayCommandActions(null)).toEqual(['uuid', 'json-format', 'base64-decode'])
  })

  it('rejects arbitrary actions and unsupported operations at the persistence boundary', () => {
    for (const operation of [null, { type: 'record', actionId: 'shell' }, { type: 'pin', actionId: 'uuid', pinned: 'yes' }, { type: 'run', actionId: 'uuid' }]) {
      expect(() => updateCommandPaletteState(defaultCommandPaletteState, operation)).toThrow()
    }
  })
})
