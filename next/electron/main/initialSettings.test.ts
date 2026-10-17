import { mkdtemp, readFile, rm, writeFile } from 'node:fs/promises'
import { tmpdir } from 'node:os'
import { join } from 'node:path'
import Store from 'electron-store'
import { describe, expect, it } from 'vitest'
import { defaultAppSettings, mergeSettings, type AppSettings, type SettingsPatch } from '../../src/shared/contracts/settings'
import { createInitialSettings } from './initialSettings'

describe('initial settings', () => {
  it.each([
    ['zh-CN', 'zh-CN'], ['zh-TW', 'zh-CN'], ['zh-Hant-HK', 'zh-CN'],
    ['en', 'en-US'], ['en-GB', 'en-US'], ['ja', 'ja-JP'], ['ja-JP', 'ja-JP'],
    [' JA_jp ', 'ja-JP'], ['de-DE', 'en-US'], ['', 'en-US']
  ])('maps system language %j to %s', (locale, expected) => {
    expect(createInitialSettings([locale]).general.language).toBe(expected)
  })

  it('uses the first supported system preference and falls back to English', () => {
    expect(createInitialSettings(['fr-FR', 'ja-JP', 'zh-CN']).general.language).toBe('ja-JP')
    expect(createInitialSettings(['en-AU', 'zh-CN']).general.language).toBe('en-US')
    expect(createInitialSettings([]).general.language).toBe('en-US')
    expect(createInitialSettings(['fr-FR', 'de-DE']).general.language).toBe('en-US')
    expect(createInitialSettings(['ja-JP'])).toEqual({
      ...defaultAppSettings,
      general: { ...defaultAppSettings.general, language: 'ja-JP' }
    })
    expect(defaultAppSettings.general.language).toBe('zh-CN')
  })

  it('persists the first-run language and preserves a manual choice after the system language changes', async () => {
    const directory = await mkdtemp(join(tmpdir(), 'mootool-initial-settings-'))
    const openStore = (languages: string[]) => {
      const store = new Store<{ settings: AppSettings }>({
        cwd: directory,
        name: 'mootool-next',
        defaults: { settings: createInitialSettings(languages) }
      })
      store.set('settings', mergeSettings(defaultAppSettings, store.get('settings') as SettingsPatch))
      return store
    }
    try {
      const first = openStore(['ja-JP'])
      expect(JSON.parse(await readFile(first.path, 'utf8')).settings.general.language).toBe('ja-JP')
      expect(openStore(['en-US']).get('settings').general.language).toBe('ja-JP')
      first.set('settings', mergeSettings(first.get('settings'), { general: { language: 'zh-CN' } }))
      expect(openStore(['en-US']).get('settings').general.language).toBe('zh-CN')

      // Existing installs retain their language while upgrading an older settings schema.
      await writeFile(first.path, JSON.stringify({ settings: { schemaVersion: 10, general: { language: 'en-US' } } }))
      expect(openStore(['ja-JP']).get('settings').general.language).toBe('en-US')
    } finally {
      await rm(directory, { recursive: true, force: true })
    }
  })
})
