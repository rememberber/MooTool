import { describe, expect, it } from 'vitest'
import { defaultAppSettings, mergeSettings } from './settings'

it('uses the MooTool Next visual defaults', () => {
  expect(defaultAppSettings.appearance.accentColor).toBe('blue')
  expect(defaultAppSettings.appearance.interfaceStyle).toBe('modern')
  expect(defaultAppSettings.appearance.fontSize).toBe(13)
  expect(defaultAppSettings.layout.navigationStyle).toBe('classic')
  expect(defaultAppSettings.editor.quickNoteFontSize).toBe(14)
  expect(defaultAppSettings.general.autoDownloadUpdates).toBe(true)
})

describe('mergeSettings', () => {
  it('merges one section without dropping unrelated values', () => {
    const settings = mergeSettings(defaultAppSettings, {
      general: { language: 'en-US' },
      layout: { compactNavigation: true }
    })

    expect(settings.general.language).toBe('en-US')
    expect(settings.general.trayEnabled).toBe(defaultAppSettings.general.trayEnabled)
    expect(settings.layout.compactNavigation).toBe(true)
    expect(settings.appearance).toEqual(defaultAppSettings.appearance)
  })

  it('normalizes numeric settings at their supported boundaries', () => {
    const settings = mergeSettings(defaultAppSettings, {
      appearance: { fontSize: 200 },
      editor: { jsonFontSize: 1 },
      network: { requestTimeoutMs: 50, translationTimeoutMs: 999999 },
      tools: { qrCodeSize: 12, randomStringLength: 99999, translationProvider: 'bing' }
    })

    expect(settings.appearance.fontSize).toBe(18)
    expect(settings.editor.jsonFontSize).toBe(11)
    expect(settings.tools.qrCodeSize).toBe(120)
    expect(settings.tools.randomStringLength).toBe(4096)
    expect(settings.network.requestTimeoutMs).toBe(1_000)
    expect(settings.network.translationTimeoutMs).toBe(120_000)
    expect(settings.tools.translationProvider).toBe('bing')
  })

  it('fills current defaults when migrating schema v2 settings', () => {
    const settings = mergeSettings(defaultAppSettings, {
      schemaVersion: 2,
      runtime: {
        javaPath: '/opt/java',
        groovyPath: '',
        pythonPath: '/opt/python',
        nodePath: '/opt/node'
      }
    })

    expect(settings.schemaVersion).toBe(6)
    expect(settings.appearance.interfaceStyle).toBe('modern')
    expect(settings.runtime.javaPath).toBe('/opt/java')
    expect(settings.runtime.drafts).toEqual(defaultAppSettings.runtime.drafts)
    expect(settings.runtime.options).toEqual(defaultAppSettings.runtime.options)
    expect(settings.layout.paneSizes).toEqual({})
    expect(settings.general.autoDownloadUpdates).toBe(true)
  })

  it('normalizes persisted workspace pane sizes', () => {
    const settings = mergeSettings(defaultAppSettings, {
      layout: { paneSizes: { json: [200, 600, 200], invalid: [0, 1], '../unsafe': [1, 1] } }
    })

    expect(settings.layout.paneSizes.json).toEqual([0.2, 0.6, 0.2])
    expect(settings.layout.paneSizes.invalid).toBeUndefined()
    expect(settings.layout.paneSizes['../unsafe']).toBeUndefined()
  })

  it('normalizes unknown interface styles', () => {
    const settings = mergeSettings(defaultAppSettings, {
      appearance: { interfaceStyle: 'unknown' as 'modern' }
    })

    expect(settings.appearance.interfaceStyle).toBe('modern')
  })
})
