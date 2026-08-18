import { describe, expect, it } from 'vitest'
import { defaultAppSettings, normalizeCustomGroups, SETTINGS_SCHEMA_VERSION } from './settings'

describe('settings contract', () => {
  it('owns the current custom-group schema', () => {
    const settings = defaultAppSettings()

    expect(settings.schemaVersion).toBe(SETTINGS_SCHEMA_VERSION)
    expect(settings.layout.customGroups).toEqual([])
  })

  it('normalizes custom groups to known production tools and safe unique identifiers', () => {
    expect(normalizeCustomGroups([
      { id: 'daily', name: ' Daily ', toolIds: ['calculator', 'calculator', 'unknown'] },
      { id: 'daily', name: 'duplicate', toolIds: ['json'] },
      { id: '../unsafe', name: 'unsafe', toolIds: ['json'] },
      { id: 'empty', name: 'empty', toolIds: [] }
    ])).toEqual([
      { id: 'daily', name: 'Daily', toolIds: ['calculator'] }
    ])
  })
})
