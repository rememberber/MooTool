import { describe, expect, it } from 'vitest'
import { defaultAppSettings, mergeSettings } from './settings'

it('uses the MooTool yellow accent by default', () => {
  expect(defaultAppSettings.appearance.accentColor).toBe('yellow')
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

  it('fills runtime drafts and options when migrating schema v2 settings', () => {
    const settings = mergeSettings(defaultAppSettings, {
      schemaVersion: 2,
      runtime: {
        javaPath: '/opt/java',
        groovyPath: '',
        pythonPath: '/opt/python',
        nodePath: '/opt/node'
      }
    })

    expect(settings.schemaVersion).toBe(3)
    expect(settings.runtime.javaPath).toBe('/opt/java')
    expect(settings.runtime.drafts).toEqual(defaultAppSettings.runtime.drafts)
    expect(settings.runtime.options).toEqual(defaultAppSettings.runtime.options)
  })
})
