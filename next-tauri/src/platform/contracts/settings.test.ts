import { describe, expect, it } from 'vitest'
import { defaultAppSettings, normalizeCloseBehavior, normalizeCustomGroups, normalizeEditorFontName, normalizeEditorSettings, normalizeInterfaceFontFamily, normalizeLayoutSettings, normalizeNetworkSettings, normalizePaneSizes, normalizeShortcutSettings, SETTINGS_SCHEMA_VERSION, storedPaneSizes } from './settings'

describe('settings contract', () => {
  it('owns the current custom-group schema', () => {
    const settings = defaultAppSettings()

    expect(settings.schemaVersion).toBe(SETTINGS_SCHEMA_VERSION)
    expect(settings.layout.customGroups).toEqual([])
    expect(settings.layout.navigationStyle).toBe('classic')
    expect(settings.layout.compactNavigation).toBe(false)
    expect(settings.layout.showSeparators).toBe(true)
    expect(settings.layout.showRecent).toBe(false)
    expect(settings.general.autoDownloadUpdates).toBe(true)
    expect(settings.general.escapeHidesWindow).toBe(true)
    expect(settings.data).toEqual({})
    expect(normalizeCloseBehavior('minimizeToTray')).toBe('hide')
    expect(normalizeCloseBehavior('hide')).toBe('hide')
    expect(normalizeCloseBehavior('quit')).toBe('quit')
    expect(normalizeCloseBehavior('tray')).toBe('ask')
    expect(settings.editor.jsonFontSize).toBe(14)
    expect(settings.editor.quickNoteFontName).toBe('ui-monospace')
    expect(normalizeEditorFontName('mono')).toBe('ui-monospace')
    expect(normalizeEditorFontName('system')).toBe('system-ui')
    expect(normalizeEditorFontName(' PingFang SC ')).toBe('PingFang SC')
    expect(normalizePaneSizes({ json: [200, 600, 200], invalid: [0, 1], '../unsafe': [1, 1], legacy: 260 })).toEqual({
      json: [0.2, 0.6, 0.2]
    })
    expect(storedPaneSizes({ 'http-workspace': [0.2, 0.8], 'http-collection': [0.4, 0.6] }, 'http-workspace', 'http-collection', 2)).toEqual([0.2, 0.8])
    expect(storedPaneSizes({ 'http-collection': [0.4, 0.6] }, 'http-workspace', 'http-collection', 2)).toEqual([0.4, 0.6])
    expect(normalizeNetworkSettings({ proxyMode: 'manual', proxyPort: 7890 })).toMatchObject({ proxyEnabled: true, proxyPort: '7890' })
    expect(normalizeNetworkSettings({ proxyMode: 'system', proxyPort: 8080 })).toMatchObject({ proxyEnabled: false, proxyPort: '' })
    expect(defaultAppSettings().network.proxyEnabled).toBe(false)
    expect(defaultAppSettings().network.proxyPort).toBe('')
    expect(normalizeInterfaceFontFamily('system')).toBe('system-ui')
    expect(normalizeInterfaceFontFamily('mono')).toBe('ui-monospace')
    expect(normalizeInterfaceFontFamily('PingFang SC')).toBe('PingFang SC')
    expect(defaultAppSettings().appearance.fontFamily).toBe('system-ui')
    expect(defaultAppSettings().appearance).not.toHaveProperty('uiScale')
    expect(normalizeLayoutSettings({ density: 'compact', compactNavigation: false })).not.toHaveProperty('density')
    expect(normalizeLayoutSettings({ sidebarCompact: true, showGroupTitles: false, hiddenTools: ['json', '../unsafe'] })).toMatchObject({
      hideNavigationTitles: true,
      hiddenNavigationToolIds: ['json']
    })
    expect(defaultAppSettings().layout.hideNavigationTitles).toBe(false)
    expect(defaultAppSettings().editor.softWrap).toBe(true)
    expect(normalizeShortcutSettings({ globalSearch: 'CommandOrControl+K', settings: 'CommandOrControl+,' })).toEqual({
      search: 'CommandOrControl+K',
      settings: 'CommandOrControl+,',
      globalSearchEnabled: false,
      globalSearch: 'CommandOrControl+Shift+Space'
    })
    expect(normalizeEditorSettings({ wordWrap: false, fontSize: 18, tabSize: 8 }).softWrap).toBe(false)
    expect(settings.network.translationTimeoutMs).toBe(15_000)
    expect(normalizeNetworkSettings({ timeoutSeconds: 45, translationTimeoutSeconds: 20 })).toMatchObject({ requestTimeoutMs: 45_000, translationTimeoutMs: 20_000 })
    expect(normalizeNetworkSettings({ requestTimeoutMs: 50, translationTimeoutMs: 999_999 })).toMatchObject({ requestTimeoutMs: 1_000, translationTimeoutMs: 120_000 })
    expect(settings.vault.quickNoteTreeExpandMode).toBe('expandAll')
    expect(settings.vault.jsonTreeExpandMode).toBe('expandAll')
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
