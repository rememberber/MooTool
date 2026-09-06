import { Check, Database, FileDown, FolderArchive, FolderInput, Languages, MonitorCog, RotateCcw, Save, SlidersHorizontal, Terminal, Wrench, X } from 'lucide-react'
import { useEffect, useMemo, useRef, useState } from 'react'
import { useI18n } from '../../app/i18n'
import { productToolCatalog } from '../../app/toolCatalog'
import { backupApi } from '../../platform/api/backupApi'
import { credentialApi, type ProxyCredentialStatus } from '../../platform/api/credentialApi'
import { diagnosticsApi } from '../../platform/api/diagnosticsApi'
import type {
  AccentColor,
  AppLanguage,
  AppSettings,
  CloseBehavior,
  InterfaceDensity,
  ProxyMode,
  ThemePreference
} from '../../platform/contracts/settings'
import { errorMessage } from '../../shared/errors'
import { useDesktopDialog } from '../../shared/DesktopDialogProvider'
import { useSettings } from './SettingsProvider'
import { UpdateCard } from './UpdateCard'
import { ProductImportCard } from './ProductImportCard'

type SettingsCategory = 'general' | 'appearance' | 'editor' | 'runtime' | 'tools' | 'data'

const settingsCategories = [
  { id: 'general', label: 'settings.general', icon: Languages },
  { id: 'appearance', label: 'settings.appearance', icon: MonitorCog },
  { id: 'editor', label: 'settings.editor', icon: SlidersHorizontal },
  { id: 'runtime', label: 'settings.runtime', icon: Terminal },
  { id: 'tools', label: 'settings.tools', icon: Wrench },
  { id: 'data', label: 'settings.data', icon: Database }
] as const

export function SettingsSurface() {
  const { settings, save, reset, error: providerError } = useSettings()
  const { t, toolTitle } = useI18n()
  const dialog = useDesktopDialog()
  const [draft, setDraft] = useState(settings)
  const [status, setStatus] = useState('')
  const [busy, setBusy] = useState(false)
  const [activeCategory, setActiveCategory] = useState<SettingsCategory>('general')
  const [proxyPassword, setProxyPassword] = useState('')
  const [proxyPasswordDirty, setProxyPasswordDirty] = useState(false)
  const [proxyCredential, setProxyCredential] = useState<ProxyCredentialStatus>()
  const allowCloseRef = useRef(false)

  useEffect(() => setDraft(settings), [settings])
  useEffect(() => {
    void credentialApi.getProxyStatus().then(setProxyCredential).catch((cause) => {
      setStatus(`${t('settings.proxyCredentialError')}: ${errorMessage(cause)}`)
    })
  }, [t])
  const dirty = useMemo(
    () => JSON.stringify({ ...draft, revision: 0 }) !== JSON.stringify({ ...settings, revision: 0 }),
    [draft, settings]
  )

  useEffect(() => {
    if (!window.__TAURI_INTERNALS__) return
    let dispose: (() => void) | undefined
    let cancelled = false
    void import('@tauri-apps/api/window').then(async ({ getCurrentWindow }) => {
      const currentWindow = getCurrentWindow()
      dispose = await currentWindow.onCloseRequested(async (event) => {
        if (allowCloseRef.current || !dirty) return
        event.preventDefault()
        if (await dialog.confirm(t('settings.discardChanges'), { dangerous: true })) {
          allowCloseRef.current = true
          await currentWindow.destroy()
        }
      })
      if (cancelled) dispose()
    })
    return () => { cancelled = true; dispose?.() }
  }, [dialog, dirty, t])

  async function handleClose(): Promise<void> {
    if (dirty && !await dialog.confirm(t('settings.discardChanges'), { dangerous: true })) return
    allowCloseRef.current = true
    await closeWindow()
  }

  async function handleSave(): Promise<void> {
    setBusy(true)
    setStatus('')
    try {
      const saved = await save(draft)
      if (proxyPasswordDirty) {
        const credential = await credentialApi.setProxyPassword(proxyPassword)
        setProxyCredential(credential)
        setProxyPassword('')
        setProxyPasswordDirty(false)
      }
      setDraft(saved)
      setStatus(t('settings.saved'))
    } catch (cause) {
      setStatus(`${t('settings.saveError')}: ${errorMessage(cause)}`)
    } finally {
      setBusy(false)
    }
  }

  async function handleReset(): Promise<void> {
    setBusy(true)
    setStatus('')
    try {
      const credential = await credentialApi.setProxyPassword('')
      const restored = await reset()
      setProxyCredential(credential)
      setProxyPassword('')
      setProxyPasswordDirty(false)
      setDraft(restored)
      setStatus(t('settings.saved'))
    } catch (cause) {
      setStatus(`${t('settings.resetError')}: ${errorMessage(cause)}`)
    } finally {
      setBusy(false)
    }
  }

  async function handleBackupExport(): Promise<void> {
    const directory = await backupApi.chooseExportDirectory(draft.tools.exportDirectory)
    if (!directory) return
    setBusy(true)
    setStatus('')
    try {
      const result = await backupApi.exportTo(directory)
      setStatus(t('settings.backupCreated', { path: result.backupPath }))
    } catch (cause) {
      setStatus(`${t('settings.saveError')}: ${errorMessage(cause)}`)
    } finally {
      setBusy(false)
    }
  }

  async function handleBackupImport(): Promise<void> {
    const directory = await backupApi.chooseImportDirectory()
    if (!directory || !await dialog.confirm(t('settings.backupConfirm'))) return
    setBusy(true)
    setStatus('')
    try {
      const result = await backupApi.importFrom(directory)
      setStatus(t('settings.backupRestored', { path: result.rollbackPath }))
    } catch (cause) {
      setStatus(`${t('settings.resetError')}: ${errorMessage(cause)}`)
    } finally {
      setBusy(false)
    }
  }

  async function handleDiagnosticsExport(): Promise<void> {
    const directory = await diagnosticsApi.chooseExportDirectory(draft.tools.exportDirectory)
    if (!directory) return
    setBusy(true)
    setStatus('')
    try {
      const result = await diagnosticsApi.exportBundle(directory)
      setStatus(t('settings.diagnosticsCreated', { path: result.bundlePath }))
    } catch (cause) {
      setStatus(`${t('settings.diagnosticsError')}: ${errorMessage(cause, 'settings.diagnostics.export')}`)
    } finally {
      setBusy(false)
    }
  }

  async function chooseDefaultExportDirectory(): Promise<void> {
    const directory = await diagnosticsApi.chooseExportDirectory(draft.tools.exportDirectory)
    if (directory) setDraft(updateTools(draft, { exportDirectory: directory }))
  }

  return (
    <main className="settings-surface">
      <div className="settings-titlebar-drag" data-tauri-drag-region />
      <header className="settings-header">
        <div>
          <h1>{t('settings.title')}</h1>
          <p>{t('settings.subtitle')}</p>
        </div>
        <button className="icon-button" type="button" aria-label={t('settings.close')} onClick={() => void handleClose()}>
          <X />
        </button>
      </header>

      <div className="settings-body">
        <nav className="settings-category-nav" aria-label={t('settings.title')}>
          {settingsCategories.map((category) => {
            const Icon = category.icon
            return <button className={activeCategory === category.id ? 'settings-category-nav__item settings-category-nav__item--active' : 'settings-category-nav__item'} type="button" key={category.id} aria-current={activeCategory === category.id ? 'page' : undefined} onClick={() => setActiveCategory(category.id)}><Icon /><span>{t(category.label)}</span></button>
          })}
        </nav>
        <div className="settings-sections">
        <SettingsSection active={activeCategory === 'general'} icon={<Languages />} title={t('settings.general')}>
          <SettingRow label={t('settings.language')}>
            <select
              value={draft.general.language}
              onChange={(event) => setDraft(updateGeneral(draft, {
                language: event.target.value as AppLanguage
              }))}
            >
              <option value="zh-CN">{t('settings.languageZh')}</option>
              <option value="en-US">{t('settings.languageEn')}</option>
              <option value="ja-JP">{t('settings.languageJa')}</option>
            </select>
          </SettingRow>
          <SettingRow label={t('settings.closeBehavior')}>
            <select
              value={draft.general.closeBehavior}
              onChange={(event) => setDraft(updateGeneral(draft, {
                closeBehavior: event.target.value as CloseBehavior
              }))}
            >
              <option value="ask">{t('settings.closeAsk')}</option>
              <option value="minimizeToTray">{t('settings.closeTray')}</option>
              <option value="quit">{t('settings.closeQuit')}</option>
            </select>
          </SettingRow>
          <SettingRow label={t('settings.launchAtLogin')}>
            <input
              type="checkbox"
              checked={draft.general.launchAtLogin}
              onChange={(event) => setDraft(updateGeneral(draft, {
                launchAtLogin: event.target.checked
              }))}
            />
          </SettingRow>
          <SettingRow label={t('settings.autoCheckUpdates')}>
            <input
              type="checkbox"
              checked={draft.general.autoCheckUpdates}
              onChange={(event) => setDraft(updateGeneral(draft, {
                autoCheckUpdates: event.target.checked
              }))}
            />
          </SettingRow>
          <SettingRow label={t('settings.autoDownloadUpdates')}>
            <input type="checkbox" checked={draft.general.autoDownloadUpdates} onChange={(event) => setDraft(updateGeneral(draft, { autoDownloadUpdates: event.target.checked }))} />
          </SettingRow>
          <SettingRow label={t('settings.startMaximized')}>
            <input type="checkbox" checked={draft.general.startMaximized} onChange={(event) => setDraft(updateGeneral(draft, { startMaximized: event.target.checked }))} />
          </SettingRow>
          <SettingRow label={t('settings.trayEnabled')}>
            <input type="checkbox" checked={draft.general.trayEnabled} onChange={(event) => setDraft(updateGeneral(draft, { trayEnabled: event.target.checked }))} />
          </SettingRow>
        </SettingsSection>

        <SettingsSection active={activeCategory === 'appearance'} icon={<MonitorCog />} title={t('settings.appearance')}>
          <SettingRow label={t('settings.theme')}>
            <Segmented
              value={draft.appearance.theme}
              options={[
                ['system', t('settings.themeSystem')],
                ['light', t('settings.themeLight')],
                ['dark', t('settings.themeDark')]
              ]}
              onChange={(theme) => setDraft(updateAppearance(draft, { theme: theme as ThemePreference }))}
            />
          </SettingRow>
          <SettingRow label={t('settings.accent')}>
            <div className="accent-options">
              {(['blue', 'indigo', 'teal', 'orange'] as AccentColor[]).map((accent) => (
                <button
                  key={accent}
                  className={`accent-swatch accent-swatch--${accent}`}
                  type="button"
                  aria-label={accent}
                  aria-pressed={draft.appearance.accentColor === accent}
                  onClick={() => setDraft(updateAppearance(draft, { accentColor: accent }))}
                >
                  {draft.appearance.accentColor === accent && <Check />}
                </button>
              ))}
            </div>
          </SettingRow>
          <SettingRow label={t('settings.density')}>
            <Segmented
              value={draft.layout.density}
              options={[
                ['comfortable', t('settings.comfortable')],
                ['compact', t('settings.compact')]
              ]}
              onChange={(density) => setDraft(updateLayout(draft, { density: density as InterfaceDensity }))}
            />
          </SettingRow>
          <SettingRow label={t('settings.sidebarCompact')}>
            <input
              type="checkbox"
              checked={draft.layout.sidebarCompact}
              onChange={(event) => setDraft(updateLayout(draft, { sidebarCompact: event.target.checked }))}
            />
          </SettingRow>
          <SettingRow label={t('settings.showRecent')}>
            <input type="checkbox" checked={draft.layout.showRecent} onChange={(event) => setDraft(updateLayout(draft, { showRecent: event.target.checked }))} />
          </SettingRow>
          <SettingRow label={t('settings.showGroupTitles')}>
            <input type="checkbox" checked={draft.layout.showGroupTitles} onChange={(event) => setDraft(updateLayout(draft, { showGroupTitles: event.target.checked }))} />
          </SettingRow>
          <SettingRow label={t('settings.interfaceFont')}>
            <select value={draft.appearance.fontFamily} onChange={(event) => setDraft(updateAppearance(draft, { fontFamily: event.target.value as 'system' | 'mono' }))}>
              <option value="system">{t('settings.interfaceFontSystem')}</option>
              <option value="mono">{t('settings.interfaceFontMono')}</option>
            </select>
          </SettingRow>
          <SettingRow label={t('settings.uiScale')}>
            <Segmented value={String(draft.appearance.uiScale)} options={[["90", "90%"], ["100", "100%"], ["110", "110%"]]} onChange={(value) => setDraft(updateAppearance(draft, { uiScale: Number(value) as 90 | 100 | 110 }))} />
          </SettingRow>
          <div className="settings-complex-row">
            <span>{t('settings.visibleTools')}</span>
            <div className="settings-tool-visibility">
              {productToolCatalog.map((tool) => <label key={tool.id}><input type="checkbox" checked={!draft.layout.hiddenTools.includes(tool.id)} onChange={(event) => setDraft(updateLayout(draft, { hiddenTools: event.target.checked ? draft.layout.hiddenTools.filter((id) => id !== tool.id) : [...draft.layout.hiddenTools, tool.id] }))} />{toolTitle(tool)}</label>)}
            </div>
          </div>
        </SettingsSection>

        <SettingsSection active={activeCategory === 'editor'} icon={<SlidersHorizontal />} title={t('settings.editor')}>
          <SettingRow label={t('settings.fontSize')}>
            <input
              type="number"
              min="10"
              max="24"
              value={draft.editor.fontSize}
              onChange={(event) => setDraft(updateEditor(draft, { fontSize: event.target.valueAsNumber }))}
            />
          </SettingRow>
          <SettingRow label={t('settings.tabSize')}>
            <select
              value={draft.editor.tabSize}
              onChange={(event) => setDraft(updateEditor(draft, {
                tabSize: Number(event.target.value) as 2 | 4 | 8
              }))}
            >
              <option value="2">2</option>
              <option value="4">4</option>
              <option value="8">8</option>
            </select>
          </SettingRow>
          <SettingRow label={t('settings.wordWrap')}>
            <input
              type="checkbox"
              checked={draft.editor.wordWrap}
              onChange={(event) => setDraft(updateEditor(draft, { wordWrap: event.target.checked }))}
            />
          </SettingRow>
          <SettingRow label={t('settings.jsonFontSize')}>
            <input type="number" min="10" max="24" value={draft.editor.jsonFontSize} onChange={(event) => setDraft(updateEditor(draft, { jsonFontSize: event.target.valueAsNumber }))} />
          </SettingRow>
          <SettingRow label={t('settings.jsonFontFamily')}>
            <select value={draft.editor.jsonFontFamily} onChange={(event) => setDraft(updateEditor(draft, { jsonFontFamily: event.target.value as 'system' | 'mono' }))}><option value="system">{t('settings.interfaceFontSystem')}</option><option value="mono">{t('settings.interfaceFontMono')}</option></select>
          </SettingRow>
          <SettingRow label={t('settings.quickNoteFontSize')}>
            <input type="number" min="10" max="24" value={draft.editor.quickNoteFontSize} onChange={(event) => setDraft(updateEditor(draft, { quickNoteFontSize: event.target.valueAsNumber }))} />
          </SettingRow>
          <SettingRow label={t('settings.quickNoteFontFamily')}>
            <select value={draft.editor.quickNoteFontFamily} onChange={(event) => setDraft(updateEditor(draft, { quickNoteFontFamily: event.target.value as 'system' | 'mono' }))}><option value="system">{t('settings.interfaceFontSystem')}</option><option value="mono">{t('settings.interfaceFontMono')}</option></select>
          </SettingRow>
        </SettingsSection>

        <SettingsSection active={activeCategory === 'runtime'} icon={<Terminal />} title={t('settings.runtime')}>
          <SettingRow label={t('settings.runtimeAutoDetect')}><input type="checkbox" checked={draft.runtime.autoDetect} onChange={(event) => setDraft(updateRuntime(draft, { autoDetect: event.target.checked }))} /></SettingRow>
          <SettingRow label={t('settings.runtimeTimeout')}><input type="number" min="1" max="300" value={draft.runtime.timeoutSeconds} onChange={(event) => setDraft(updateRuntime(draft, { timeoutSeconds: event.target.valueAsNumber }))} /></SettingRow>
          {(['javaPath', 'groovyPath', 'pythonPath', 'nodePath'] as const).map((key) => (
            <SettingRow key={key} label={t(`settings.${key}`)}><input value={draft.runtime[key]} placeholder={t('settings.runtimePathPlaceholder')} onChange={(event) => setDraft(updateRuntime(draft, { [key]: event.target.value }))} /></SettingRow>
          ))}
          <SettingRow label={t('settings.searchShortcut')}><input value={draft.shortcuts.globalSearch} onChange={(event) => setDraft({ ...draft, shortcuts: { ...draft.shortcuts, globalSearch: event.target.value } })} /></SettingRow>
          <SettingRow label={t('settings.settingsShortcut')}><input value={draft.shortcuts.settings} onChange={(event) => setDraft({ ...draft, shortcuts: { ...draft.shortcuts, settings: event.target.value } })} /></SettingRow>
        </SettingsSection>

        <SettingsSection active={activeCategory === 'tools'} icon={<Wrench />} title={t('settings.tools')}>
          <SettingRow label={t('settings.qrCodeSize')}><input type="number" min="120" max="2000" value={draft.tools.qrCodeSize} onChange={(event) => setDraft(updateTools(draft, { qrCodeSize: event.target.valueAsNumber }))} /></SettingRow>
          <SettingRow label={t('settings.qrErrorCorrection')}><select value={draft.tools.qrErrorCorrection} onChange={(event) => setDraft(updateTools(draft, { qrErrorCorrection: event.target.value as AppSettings['tools']['qrErrorCorrection'] }))}>{(['L', 'M', 'Q', 'H'] as const).map((level) => <option key={level}>{level}</option>)}</select></SettingRow>
          <SettingRow label={t('settings.randomStringLength')}><input type="number" min="1" max="4096" value={draft.tools.randomStringLength} onChange={(event) => setDraft(updateTools(draft, { randomStringLength: event.target.valueAsNumber }))} /></SettingRow>
          <SettingRow label={t('settings.translationProvider')}><select value={draft.tools.translationProvider} onChange={(event) => setDraft(updateTools(draft, { translationProvider: event.target.value as AppSettings['tools']['translationProvider'] }))}><option value="google">Google</option><option value="bing">Bing</option></select></SettingRow>
          <SettingRow label={t('settings.translationSourceLang')}><input value={draft.tools.translationSourceLang} onChange={(event) => setDraft(updateTools(draft, { translationSourceLang: event.target.value }))} /></SettingRow>
          <SettingRow label={t('settings.translationTargetLang')}><input value={draft.tools.translationTargetLang} onChange={(event) => setDraft(updateTools(draft, { translationTargetLang: event.target.value }))} /></SettingRow>
          <div className="settings-complex-row"><span>{t('settings.exportDirectory')}</span><div className="settings-directory-field"><code>{draft.tools.exportDirectory || t('settings.exportDirectoryDefault')}</code><button className="secondary-button" type="button" onClick={() => void chooseDefaultExportDirectory()}><FolderInput />{t('settings.chooseDirectory')}</button>{draft.tools.exportDirectory && <button className="secondary-button" type="button" onClick={() => setDraft(updateTools(draft, { exportDirectory: '' }))}>{t('settings.clearDirectory')}</button>}</div></div>
        </SettingsSection>

        <SettingsSection active={activeCategory === 'data'} icon={<Database />} title={t('settings.data')}>
          <SettingRow label={t('settings.historyLimit')}>
            <input
              type="number"
              min="10"
              max="5000"
              value={draft.data.historyLimit}
              onChange={(event) => setDraft({
                ...draft,
                data: { ...draft.data, historyLimit: event.target.valueAsNumber }
              })}
            />
          </SettingRow>
          <SettingRow label={t('settings.timeout')}>
            <input
              type="number"
              min="1"
              max="300"
              value={draft.network.timeoutSeconds}
              onChange={(event) => setDraft({
                ...draft,
                network: { ...draft.network, timeoutSeconds: event.target.valueAsNumber }
              })}
            />
          </SettingRow>
          <SettingRow label={t('settings.translationTimeout')}><input type="number" min="1" max="300" value={draft.network.translationTimeoutSeconds} onChange={(event) => setDraft(updateNetwork(draft, { translationTimeoutSeconds: event.target.valueAsNumber }))} /></SettingRow>
          <SettingRow label={t('settings.proxyMode')}>
            <select value={draft.network.proxyMode} onChange={(event) => setDraft(updateNetwork(draft, { proxyMode: event.target.value as ProxyMode }))}><option value="system">{t('settings.proxySystem')}</option><option value="direct">{t('settings.proxyDirect')}</option><option value="manual">{t('settings.proxyManual')}</option></select>
          </SettingRow>
          {draft.network.proxyMode === 'manual' && <>
            <SettingRow label={t('settings.proxyHost')}><input value={draft.network.proxyHost} onChange={(event) => setDraft(updateNetwork(draft, { proxyHost: event.target.value }))} /></SettingRow>
            <SettingRow label={t('settings.proxyPort')}><input type="number" min="1" max="65535" value={draft.network.proxyPort} onChange={(event) => setDraft(updateNetwork(draft, { proxyPort: event.target.valueAsNumber }))} /></SettingRow>
            <SettingRow label={t('settings.proxyUsername')}><input value={draft.network.proxyUsername} autoComplete="off" onChange={(event) => setDraft(updateNetwork(draft, { proxyUsername: event.target.value }))} /></SettingRow>
            <SettingRow label={t('settings.proxyPassword')}>
              <div className="settings-secret-field">
                <input
                  type="password"
                  value={proxyPassword}
                  autoComplete="new-password"
                  placeholder={proxyCredential?.stored && !proxyPasswordDirty ? t('settings.proxyPasswordStored') : t('settings.proxyPasswordEmpty')}
                  onChange={(event) => { setProxyPassword(event.target.value); setProxyPasswordDirty(true) }}
                />
                {(proxyCredential?.stored || proxyPasswordDirty) && <button className="secondary-button" type="button" onClick={() => { setProxyPassword(''); setProxyPasswordDirty(true) }}>{t('settings.proxyPasswordClear')}</button>}
              </div>
            </SettingRow>
            <div className="settings-security-note"><strong>{t('settings.proxyPasswordSecurity')}</strong><span>{t('settings.proxyPasswordSecurityBody', { store: proxyCredential?.secureStore ?? t('settings.proxyPasswordStore') })}</span></div>
          </>}
          <SettingRow label={t('settings.vaultAutoCommit')}>
            <input
              type="checkbox"
              checked={draft.vault.autoCommit}
              onChange={(event) => setDraft({
                ...draft,
                vault: { ...draft.vault, autoCommit: event.target.checked }
              })}
            />
          </SettingRow>
          <div className="settings-storage-note">
            <strong>{t('settings.storageTitle')}</strong>
            <span>{t('settings.storageBody')}</span>
          </div>
          <div className="settings-backup-card">
            <div><strong>{t('settings.backupTitle')}</strong><span>{t('settings.backupBody')}</span></div>
            <div>
              <button className="secondary-button" type="button" disabled={busy} onClick={() => void handleBackupExport()}><FolderArchive />{t('settings.backupExport')}</button>
              <button className="secondary-button" type="button" disabled={busy} onClick={() => void handleBackupImport()}><FolderInput />{t('settings.backupImport')}</button>
            </div>
          </div>
          <ProductImportCard disabled={busy} />
          <div className="settings-backup-card">
            <div><strong>{t('settings.diagnosticsTitle')}</strong><span>{t('settings.diagnosticsBody')}</span></div>
            <div>
              <button className="secondary-button" type="button" disabled={busy} onClick={() => void handleDiagnosticsExport()}><FileDown />{t('settings.diagnosticsExport')}</button>
            </div>
          </div>
          <UpdateCard disabled={busy} />
        </SettingsSection>
        </div>
      </div>

      <footer className="settings-footer">
        <div className={status || providerError ? 'settings-status settings-status--visible' : 'settings-status'}>
          {status || providerError}
        </div>
        <button className="secondary-button" type="button" disabled={busy} onClick={() => void handleReset()}>
          <RotateCcw /> {t('settings.reset')}
        </button>
        <button className="primary-button" type="button" disabled={busy || !dirty} onClick={() => void handleSave()}>
          <Save /> {t('settings.save')}
        </button>
      </footer>
    </main>
  )
}

function SettingsSection({ active, icon, title, children }: {
  active: boolean
  icon: React.ReactNode
  title: string
  children: React.ReactNode
}) {
  return (
    <section className="settings-section" hidden={!active}>
      <h2>{icon}{title}</h2>
      <div>{children}</div>
    </section>
  )
}

function SettingRow({ label, children }: { label: string; children: React.ReactNode }) {
  return <label className="settings-row"><span>{label}</span>{children}</label>
}

function Segmented({ value, options, onChange }: {
  value: string
  options: [string, string][]
  onChange(value: string): void
}) {
  return (
    <div className="settings-segmented">
      {options.map(([option, label]) => (
        <button
          key={option}
          className={value === option ? 'settings-segmented--active' : ''}
          type="button"
          onClick={() => onChange(option)}
        >
          {label}
        </button>
      ))}
    </div>
  )
}

function updateGeneral(settings: AppSettings, patch: Partial<AppSettings['general']>): AppSettings {
  return { ...settings, general: { ...settings.general, ...patch } }
}

function updateAppearance(settings: AppSettings, patch: Partial<AppSettings['appearance']>): AppSettings {
  return { ...settings, appearance: { ...settings.appearance, ...patch } }
}

function updateLayout(settings: AppSettings, patch: Partial<AppSettings['layout']>): AppSettings {
  return { ...settings, layout: { ...settings.layout, ...patch } }
}

function updateEditor(settings: AppSettings, patch: Partial<AppSettings['editor']>): AppSettings {
  return { ...settings, editor: { ...settings.editor, ...patch } }
}

function updateNetwork(settings: AppSettings, patch: Partial<AppSettings['network']>): AppSettings {
  return { ...settings, network: { ...settings.network, ...patch } }
}

function updateRuntime(settings: AppSettings, patch: Partial<AppSettings['runtime']>): AppSettings {
  return { ...settings, runtime: { ...settings.runtime, ...patch } }
}

function updateTools(settings: AppSettings, patch: Partial<AppSettings['tools']>): AppSettings {
  return { ...settings, tools: { ...settings.tools, ...patch } }
}

async function closeWindow(): Promise<void> {
  if (window.__TAURI_INTERNALS__) {
    const { getCurrentWindow } = await import('@tauri-apps/api/window')
    await getCurrentWindow().close()
  } else {
    window.close()
  }
}
