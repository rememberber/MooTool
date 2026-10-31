import {
  Check,
  Command,
  Database,
  FileDown,
  FolderArchive,
  FolderGit2,
  FolderInput,
  Globe2,
  Info,
  Languages,
  MonitorCog,
  PanelLeft,
  RefreshCw,
  RotateCcw,
  Save,
  SlidersHorizontal,
  Terminal,
  Wrench,
  X
} from 'lucide-react'
import { useEffect, useMemo, useRef, useState } from 'react'
import brandLogo from '../../assets/brand/mootool-logo.png'
import { FontSelect } from '../../shared/components/FontSelect'
import { useI18n } from '../../app/i18n'
import { productToolCatalog } from '../../app/toolCatalog'
import { backupApi } from '../../platform/api/backupApi'
import { credentialApi, type ProxyCredentialStatus } from '../../platform/api/credentialApi'
import { diagnosticsApi } from '../../platform/api/diagnosticsApi'
import { runtimeApi } from '../../platform/api/runtimeApi'
import type { CodeRuntimeStatus } from '../../platform/contracts/runtime'
import type {
  AccentColor,
  AppLanguage,
  AppSettings,
  CloseBehavior,
  NavigationStyle,
  InterfaceStyle,
  ThemePreference
} from '../../platform/contracts/settings'
import { sqlDialects } from '../../platform/contracts/settings'
import { translationLanguageCodes, translationLanguageLabel } from '../../platform/contracts/translationLanguages'
import { errorMessage } from '../../shared/errors'
import { useDesktopDialog } from '../../shared/DesktopDialogProvider'
import { ResizableColumns } from '../../shared/ResizableColumns'
import { GlobalShortcutSettings } from './GlobalShortcutSettings'
import { useSettings } from './SettingsProvider'
import { UpdateCard } from './UpdateCard'
import { ProductImportCard } from './ProductImportCard'

type SettingsCategory =
  | 'general'
  | 'appearance'
  | 'layout'
  | 'editor'
  | 'network'
  | 'data'
  | 'vault'
  | 'runtime'
  | 'tools'
  | 'shortcuts'
  | 'about'

const SETTINGS_CATEGORY_KEY = 'mootool-next-tauri:settings-category'

const settingsCategories = [
  { id: 'general', label: 'settings.general', icon: Languages },
  { id: 'appearance', label: 'settings.appearance', icon: MonitorCog },
  { id: 'layout', label: 'settings.layout', icon: PanelLeft },
  { id: 'editor', label: 'settings.editor', icon: SlidersHorizontal },
  { id: 'network', label: 'settings.network', icon: Globe2 },
  { id: 'data', label: 'settings.data', icon: Database },
  { id: 'vault', label: 'settings.vault', icon: FolderGit2 },
  { id: 'runtime', label: 'settings.runtimeOnly', icon: Terminal },
  { id: 'tools', label: 'settings.tools', icon: Wrench },
  { id: 'shortcuts', label: 'settings.shortcuts', icon: Command },
  { id: 'about', label: 'settings.about', icon: Info }
] as const

function readInitialCategory(): SettingsCategory {
  const requested = new URLSearchParams(window.location.search).get('category')
    ?? window.localStorage.getItem(SETTINGS_CATEGORY_KEY)
  window.localStorage.removeItem(SETTINGS_CATEGORY_KEY)
  return settingsCategories.some((category) => category.id === requested) ? requested as SettingsCategory : 'general'
}

export function SettingsSurface() {
  const { settings, save, reset, error: providerError } = useSettings()
  const { t, toolTitle, locale } = useI18n()
  const dialog = useDesktopDialog()
  const [draft, setDraft] = useState(settings)
  const [status, setStatus] = useState('')
  const [busy, setBusy] = useState(false)
  const [activeCategory, setActiveCategory] = useState<SettingsCategory>(readInitialCategory)
  const [proxyPassword, setProxyPassword] = useState('')
  const [proxyPasswordDirty, setProxyPasswordDirty] = useState(false)
  const [proxyCredential, setProxyCredential] = useState<ProxyCredentialStatus>()
  const [gitToken, setGitToken] = useState('')
  const [gitTokenDirty, setGitTokenDirty] = useState(false)
  const [gitCredential, setGitCredential] = useState<ProxyCredentialStatus>()
  const [deeplKey, setDeeplKey] = useState('')
  const [deeplKeyDirty, setDeeplKeyDirty] = useState(false)
  const [deeplCredential, setDeeplCredential] = useState<ProxyCredentialStatus>()
  const [runtimeStatuses, setRuntimeStatuses] = useState<CodeRuntimeStatus[]>([])
  const [runtimeDetecting, setRuntimeDetecting] = useState(false)
  const [aboutVersion, setAboutVersion] = useState('')
  const allowCloseRef = useRef(false)

  const draftRef = useRef(draft)
  draftRef.current = draft
  useEffect(() => setDraft(settings), [settings])
  useEffect(() => {
    const root = document.documentElement
    const fontSize = Math.min(18, Math.max(12, draft.appearance.fontSize || 13))
    root.dataset.interfaceStyle = draft.appearance.interfaceStyle
    root.style.setProperty('--app-font-size', `${fontSize}px`)
  }, [draft.appearance.fontSize, draft.appearance.interfaceStyle])
  useEffect(() => {
    void credentialApi.getProxyStatus().then(setProxyCredential).catch((cause) => {
      setStatus(`${t('settings.proxyCredentialError')}: ${errorMessage(cause)}`)
    })
    void credentialApi.getGitStatus().then(setGitCredential).catch((cause) => {
      setStatus(`${t('settings.gitCredentialError')}: ${errorMessage(cause)}`)
    })
    void credentialApi.getDeepLStatus().then(setDeeplCredential).catch((cause) => {
      setStatus(`${t('settings.deeplCredentialError')}: ${errorMessage(cause)}`)
    })
  }, [t])
  useEffect(() => {
    void runtimeApi.getInfo().then((info) => setAboutVersion(info.version)).catch(() => undefined)
  }, [])
  useEffect(() => {
    if (activeCategory !== 'runtime') return
    setRuntimeDetecting(true)
    void runtimeApi.detectRuntimes()
      .then(setRuntimeStatuses)
      .catch((cause) => setStatus(`${t('settings.runtimeDetectFailed')}: ${errorMessage(cause)}`))
      .finally(() => setRuntimeDetecting(false))
  }, [activeCategory, t])
  const dirty = useMemo(
    () => JSON.stringify({ ...draft, revision: 0 }) !== JSON.stringify({ ...settings, revision: 0 }),
    [draft, settings]
  )
  const unsaved = dirty || proxyPasswordDirty || gitTokenDirty || deeplKeyDirty

  useEffect(() => {
    if (!window.__TAURI_INTERNALS__) return
    let dispose: (() => void) | undefined
    let cancelled = false
    void import('@tauri-apps/api/window').then(async ({ getCurrentWindow }) => {
      const currentWindow = getCurrentWindow()
      dispose = await currentWindow.onCloseRequested(async (event) => {
        if (allowCloseRef.current || !unsaved) return
        event.preventDefault()
        if (await dialog.confirm(t('settings.discardChanges'), { dangerous: true })) {
          allowCloseRef.current = true
          await currentWindow.destroy()
        }
      })
      if (cancelled) dispose()
    })
    return () => { cancelled = true; dispose?.() }
  }, [dialog, t, unsaved])

  async function handleClose(): Promise<void> {
    if (unsaved && !await dialog.confirm(t('settings.discardChanges'), { dangerous: true })) return
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
      if (gitTokenDirty) {
        const credential = await credentialApi.setGitToken(gitToken)
        setGitCredential(credential)
        setGitToken('')
        setGitTokenDirty(false)
      }
      if (deeplKeyDirty) {
        const credential = await credentialApi.setDeepLApiKey(deeplKey)
        setDeeplCredential(credential)
        setDeeplKey('')
        setDeeplKeyDirty(false)
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
      const git = await credentialApi.setGitToken('')
      const deepl = await credentialApi.setDeepLApiKey('')
      const restored = await reset()
      setProxyCredential(credential)
      setGitCredential(git)
      setDeeplCredential(deepl)
      setProxyPassword('')
      setGitToken('')
      setDeeplKey('')
      setProxyPasswordDirty(false)
      setGitTokenDirty(false)
      setDeeplKeyDirty(false)
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

      <ResizableColumns id="settings-page" className="settings-body" initialPrimary={220} defaultSizes={[220, 780]} minPrimary={180} minSecondary={420}>
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
              <option value="hide">{t('settings.closeHide')}</option>
              <option value="quit">{t('settings.closeQuit')}</option>
            </select>
          </SettingRow>
          <SettingRow label={t('settings.escapeHidesWindow')}>
            <input
              type="checkbox"
              checked={draft.general.escapeHidesWindow}
              onChange={(event) => setDraft(updateGeneral(draft, {
                escapeHidesWindow: event.target.checked
              }))}
            />
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
          <SettingRow label={t('settings.interfaceStyle')}>
            <select
              aria-label={t('settings.interfaceStyle')}
              value={draft.appearance.interfaceStyle}
              onChange={(event) => setDraft(updateAppearance(draft, { interfaceStyle: event.target.value as InterfaceStyle }))}
            >
              <option value="modern">{t('settings.interfaceStyle.modern')}</option>
              <option value="claude">{t('settings.interfaceStyle.claude')}</option>
              <option value="quiet">{t('settings.interfaceStyle.quiet')}</option>
              <option value="hero">{t('settings.interfaceStyle.hero')}</option>
              <option value="smartisan">{t('settings.interfaceStyle.smartisan')}</option>
              <option value="miui-v5">{t('settings.interfaceStyle.miuiV5')}</option>
            </select>
          </SettingRow>
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
              {(['blue', 'yellow', 'coral', 'green', 'red', 'purple', 'indigo', 'teal', 'orange'] as AccentColor[]).map((accent) => (
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
          <SettingRow label={t('settings.interfaceFont')}>
            <FontSelect ariaLabel={t('settings.interfaceFont')} value={draft.appearance.fontFamily} labels={{ 'system-ui': t('settings.interfaceFontSystem'), 'ui-monospace': t('settings.interfaceFontMono') }} onChange={(fontFamily) => setDraft(updateAppearance(draft, { fontFamily }))} />
          </SettingRow>
          <SettingRow label={t('settings.interfaceFontSize')}>
            <label className="settings-range">
              <input
                type="range"
                min={12}
                max={18}
                step={1}
                aria-label={t('settings.interfaceFontSize')}
                value={draft.appearance.fontSize}
                onChange={(event) => setDraft(updateAppearance(draft, { fontSize: Number(event.target.value) }))}
              />
              <output>{draft.appearance.fontSize}</output>
            </label>
          </SettingRow>
          <SettingRow label={t('settings.unifiedBackground')}>
            <input
              type="checkbox"
              checked={draft.appearance.unifiedBackground}
              aria-label={t('settings.unifiedBackground')}
              onChange={(event) => setDraft(updateAppearance(draft, { unifiedBackground: event.target.checked }))}
            />
          </SettingRow>
        </SettingsSection>

        <SettingsSection active={activeCategory === 'layout'} icon={<PanelLeft />} title={t('settings.layout')}>
          <SettingRow label={t('settings.navigationStyle')}>
            <Segmented
              value={draft.layout.navigationStyle}
              options={[
                ['classic', t('settings.navigationClassic')],
                ['card', t('settings.navigationCard')],
                ['grouped', t('settings.navigationGrouped')]
              ]}
              onChange={(value) => setDraft(updateLayout(draft, { navigationStyle: value as NavigationStyle }))}
            />
          </SettingRow>
          <SettingRow label={t('settings.compactNavigation')}>
            <input type="checkbox" checked={draft.layout.compactNavigation} onChange={(event) => setDraft(updateLayout(draft, { compactNavigation: event.target.checked }))} />
          </SettingRow>
          <SettingRow label={t('settings.showSeparators')}>
            <input type="checkbox" checked={draft.layout.showSeparators} onChange={(event) => setDraft(updateLayout(draft, { showSeparators: event.target.checked }))} />
          </SettingRow>
          <SettingRow label={t('settings.hideNavigationTitles')}>
            <input type="checkbox" checked={draft.layout.hideNavigationTitles} onChange={(event) => setDraft(updateLayout(draft, { hideNavigationTitles: event.target.checked }))} />
          </SettingRow>
          <SettingRow label={t('settings.showRecent')}>
            <input type="checkbox" checked={draft.layout.showRecent} onChange={(event) => setDraft(updateLayout(draft, { showRecent: event.target.checked }))} />
          </SettingRow>
          <div className="settings-complex-row">
            <span>{t('settings.visibleTools')}</span>
            <div className="settings-tool-visibility">
              {productToolCatalog.map((tool) => <label key={tool.id}><input type="checkbox" checked={!draft.layout.hiddenNavigationToolIds.includes(tool.id)} onChange={(event) => setDraft(updateLayout(draft, { hiddenNavigationToolIds: event.target.checked ? draft.layout.hiddenNavigationToolIds.filter((id) => id !== tool.id) : [...draft.layout.hiddenNavigationToolIds, tool.id] }))} />{toolTitle(tool)}</label>)}
            </div>
          </div>
        </SettingsSection>

        <SettingsSection active={activeCategory === 'editor'} icon={<SlidersHorizontal />} title={t('settings.editor')}>
          <SettingRow label={t('settings.sqlDialect')}>
            <select value={draft.editor.sqlDialect} onChange={(event) => setDraft(updateEditor(draft, { sqlDialect: event.target.value }))}>
              {sqlDialects.map((dialect) => <option key={dialect} value={dialect}>{dialect}</option>)}
            </select>
          </SettingRow>
          <SettingRow label={t('settings.softWrap')}>
            <input
              type="checkbox"
              checked={draft.editor.softWrap}
              onChange={(event) => setDraft(updateEditor(draft, { softWrap: event.target.checked }))}
            />
          </SettingRow>
          <SettingRow label={t('settings.jsonFontSize')}>
            <input type="number" min="10" max="24" value={draft.editor.jsonFontSize} onChange={(event) => setDraft(updateEditor(draft, { jsonFontSize: event.target.valueAsNumber }))} />
          </SettingRow>
          <SettingRow label={t('settings.jsonFontFamily')}>
            <FontSelect ariaLabel={t('settings.jsonFontFamily')} value={draft.editor.jsonFontName} labels={{ 'ui-monospace': t('settings.interfaceFontMono') }} onChange={(jsonFontName) => setDraft(updateEditor(draft, { jsonFontName }))} />
          </SettingRow>
          <SettingRow label={t('settings.quickNoteFontSize')}>
            <input type="number" min="10" max="24" value={draft.editor.quickNoteFontSize} onChange={(event) => setDraft(updateEditor(draft, { quickNoteFontSize: event.target.valueAsNumber }))} />
          </SettingRow>
          <SettingRow label={t('settings.quickNoteFontFamily')}>
            <FontSelect ariaLabel={t('settings.quickNoteFontFamily')} value={draft.editor.quickNoteFontName} labels={{ 'ui-monospace': t('settings.interfaceFontMono') }} onChange={(quickNoteFontName) => setDraft(updateEditor(draft, { quickNoteFontName }))} />
          </SettingRow>
        </SettingsSection>

        <SettingsSection active={activeCategory === 'network'} icon={<Globe2 />} title={t('settings.network')}>
          <SettingRow label={t('settings.requestTimeout')}>
            <input type="number" min="1000" max="120000" value={draft.network.requestTimeoutMs} onChange={(event) => setDraft(updateNetwork(draft, { requestTimeoutMs: event.target.valueAsNumber }))} />
          </SettingRow>
          <SettingRow label={t('settings.translationTimeout')}><input type="number" min="1000" max="120000" value={draft.network.translationTimeoutMs} onChange={(event) => setDraft(updateNetwork(draft, { translationTimeoutMs: event.target.valueAsNumber }))} /></SettingRow>
          <SettingRow label={t('settings.proxyEnabled')}>
            <input type="checkbox" checked={draft.network.proxyEnabled} onChange={(event) => setDraft(updateNetwork(draft, { proxyEnabled: event.target.checked }))} />
          </SettingRow>
          <SettingRow label={t('settings.proxyHost')}><input value={draft.network.proxyHost} disabled={!draft.network.proxyEnabled} onChange={(event) => setDraft(updateNetwork(draft, { proxyHost: event.target.value }))} /></SettingRow>
          <SettingRow label={t('settings.proxyPort')}><input value={draft.network.proxyPort} disabled={!draft.network.proxyEnabled} inputMode="numeric" onChange={(event) => setDraft(updateNetwork(draft, { proxyPort: event.target.value }))} /></SettingRow>
          <SettingRow label={t('settings.proxyUsername')}><input value={draft.network.proxyUsername} autoComplete="off" disabled={!draft.network.proxyEnabled} onChange={(event) => setDraft(updateNetwork(draft, { proxyUsername: event.target.value }))} /></SettingRow>
          <SettingRow label={t('settings.proxyPassword')}>
            <div className="settings-secret-field">
              <input
                type="password"
                value={proxyPassword}
                autoComplete="new-password"
                disabled={!draft.network.proxyEnabled}
                placeholder={proxyCredential?.stored && !proxyPasswordDirty ? t('settings.proxyPasswordStored') : t('settings.proxyPasswordEmpty')}
                onChange={(event) => { setProxyPassword(event.target.value); setProxyPasswordDirty(true) }}
              />
              {(proxyCredential?.stored || proxyPasswordDirty) && <button className="secondary-button" type="button" disabled={!draft.network.proxyEnabled} onClick={() => { setProxyPassword(''); setProxyPasswordDirty(true) }}>{t('settings.proxyPasswordClear')}</button>}
            </div>
          </SettingRow>
          <div className="settings-security-note"><strong>{t('settings.proxyPasswordSecurity')}</strong><span>{t('settings.proxyPasswordSecurityBody', { store: proxyCredential?.secureStore ?? t('settings.proxyPasswordStore') })}</span></div>
        </SettingsSection>

        <SettingsSection active={activeCategory === 'runtime'} icon={<Terminal />} title={t('settings.runtimeOnly')}>
          <div className="settings-runtime-toolbar">
            <button className="secondary-button" type="button" disabled={runtimeDetecting} onClick={() => void runtimeApi.detectRuntimes().then(setRuntimeStatuses).catch((cause) => setStatus(`${t('settings.runtimeDetectFailed')}: ${errorMessage(cause)}`))}>
              <RefreshCw className={runtimeDetecting ? 'spin' : undefined} />{runtimeDetecting ? t('settings.runtimeDetecting') : t('settings.runtimeDetect')}
            </button>
          </div>
          <SettingRow label={t('settings.runtimeAutoDetect')}><input type="checkbox" checked={draft.runtime.autoDetect} onChange={(event) => setDraft(updateRuntime(draft, { autoDetect: event.target.checked }))} /></SettingRow>
          {(['javaPath', 'groovyPath', 'pythonPath', 'nodePath'] as const).map((key) => {
            const runtimeId = key.replace('Path', '') as CodeRuntimeStatus['id']
            const status = runtimeStatuses.find((item) => item.id === runtimeId)
            return (
              <SettingRow key={key} label={t(`settings.${key}`)}>
                <div className="settings-runtime-path">
                  <input value={draft.runtime[key]} placeholder={status?.command || t('settings.runtimePathPlaceholder')} onChange={(event) => setDraft(updateRuntime(draft, { [key]: event.target.value }))} />
                  {status && <small className={status.available ? 'runtime-status runtime-status--ok' : 'runtime-status'}>{status.available ? status.version || status.command : t('settings.runtimeNotFound')}</small>}
                </div>
              </SettingRow>
            )
          })}
        </SettingsSection>

        <SettingsSection active={activeCategory === 'tools'} icon={<Wrench />} title={t('settings.tools')}>
          <SettingRow label={t('settings.qrCodeSize')}><input type="number" min="120" max="2000" value={draft.tools.qrCodeSize} onChange={(event) => setDraft(updateTools(draft, { qrCodeSize: event.target.valueAsNumber }))} /></SettingRow>
          <SettingRow label={t('settings.qrErrorCorrection')}><select value={draft.tools.qrErrorCorrection} onChange={(event) => setDraft(updateTools(draft, { qrErrorCorrection: event.target.value as AppSettings['tools']['qrErrorCorrection'] }))}>{(['L', 'M', 'Q', 'H'] as const).map((level) => <option key={level}>{level}</option>)}</select></SettingRow>
          <SettingRow label={t('settings.randomStringLength')}><input type="number" min="1" max="4096" value={draft.tools.randomStringLength} onChange={(event) => setDraft(updateTools(draft, { randomStringLength: event.target.valueAsNumber }))} /></SettingRow>
          <SettingRow label={t('settings.translationProvider')}><select value={draft.tools.translationProvider} onChange={(event) => setDraft(updateTools(draft, { translationProvider: event.target.value as AppSettings['tools']['translationProvider'] }))}><option value="google">Google</option><option value="bing">Bing</option><option value="deepl">DeepL</option></select></SettingRow>
          <SettingRow label={t('settings.deeplApiKey')}>
            <div className="settings-secret-field">
              <input
                type="password"
                value={deeplKey}
                autoComplete="new-password"
                placeholder={deeplCredential?.stored && !deeplKeyDirty ? t('settings.deeplApiKeyStored') : t('settings.deeplApiKeyEmpty')}
                onChange={(event) => { setDeeplKey(event.target.value); setDeeplKeyDirty(true) }}
              />
              {(deeplCredential?.stored || deeplKeyDirty) && <button className="secondary-button" type="button" onClick={() => { setDeeplKey(''); setDeeplKeyDirty(true) }}>{t('settings.deeplApiKeyClear')}</button>}
            </div>
          </SettingRow>
          <div className="settings-security-note"><strong>{t('settings.proxyPasswordSecurity')}</strong><span>{t('settings.deeplApiKeySecurityBody', { store: deeplCredential?.secureStore ?? t('settings.proxyPasswordStore') })}</span></div>
          <SettingRow label={t('settings.translationSourceLang')}>
            <select value={draft.tools.translationSourceLang} onChange={(event) => setDraft(updateTools(draft, { translationSourceLang: event.target.value }))}>
              {translationLanguageCodes.map((code) => <option key={code} value={code}>{translationLanguageLabel(code, locale)}</option>)}
            </select>
          </SettingRow>
          <SettingRow label={t('settings.translationTargetLang')}>
            <select value={draft.tools.translationTargetLang} onChange={(event) => setDraft(updateTools(draft, { translationTargetLang: event.target.value }))}>
              {translationLanguageCodes.filter((code) => code !== 'auto').map((code) => <option key={code} value={code}>{translationLanguageLabel(code, locale)}</option>)}
            </select>
          </SettingRow>
          <div className="settings-complex-row"><span>{t('settings.exportDirectory')}</span><div className="settings-directory-field"><code>{draft.tools.exportDirectory || t('settings.exportDirectoryDefault')}</code><button className="secondary-button" type="button" onClick={() => void chooseDefaultExportDirectory()}><FolderInput />{t('settings.chooseDirectory')}</button>{draft.tools.exportDirectory && <button className="secondary-button" type="button" onClick={() => setDraft(updateTools(draft, { exportDirectory: '' }))}>{t('settings.clearDirectory')}</button>}</div></div>
        </SettingsSection>

        <SettingsSection active={activeCategory === 'vault'} icon={<FolderGit2 />} title={t('settings.vault')}>
          <SettingRow label={t('settings.gitRemote')}><input value={draft.vault.gitRemote} placeholder="https://example.com/notes.git" onChange={(event) => setDraft({ ...draft, vault: { ...draft.vault, gitRemote: event.target.value } })} /></SettingRow>
          <SettingRow label={t('settings.gitUsername')}><input value={draft.vault.gitUsername} onChange={(event) => setDraft({ ...draft, vault: { ...draft.vault, gitUsername: event.target.value } })} /></SettingRow>
          <SettingRow label={t('settings.vaultAutoCommit')}>
            <input type="checkbox" checked={draft.vault.autoCommit} onChange={(event) => setDraft({ ...draft, vault: { ...draft.vault, autoCommit: event.target.checked } })} />
          </SettingRow>
          <SettingRow label={t('settings.autoCommitIdleSeconds')}><input type="number" min="5" max="3600" value={draft.vault.autoCommitIdleSeconds} onChange={(event) => setDraft({ ...draft, vault: { ...draft.vault, autoCommitIdleSeconds: event.target.valueAsNumber } })} /></SettingRow>
          <SettingRow label={t('settings.autoCommitInactiveSeconds')}><input type="number" min="5" max="3600" value={draft.vault.autoCommitInactiveSeconds} onChange={(event) => setDraft({ ...draft, vault: { ...draft.vault, autoCommitInactiveSeconds: event.target.valueAsNumber } })} /></SettingRow>
          <SettingRow label={t('settings.autoPullMinutes')}><input type="number" min="0" max="1440" value={draft.vault.autoPullMinutes} onChange={(event) => setDraft({ ...draft, vault: { ...draft.vault, autoPullMinutes: event.target.valueAsNumber } })} /></SettingRow>
          <SettingRow label={t('settings.hideGitignoredFiles')}>
            <input type="checkbox" checked={draft.vault.hideGitignoredFiles} onChange={(event) => setDraft({ ...draft, vault: { ...draft.vault, hideGitignoredFiles: event.target.checked } })} />
          </SettingRow>
          <SettingRow label={t('settings.gitToken')}>
            <div className="settings-secret-field">
              <input
                type="password"
                value={gitToken}
                autoComplete="new-password"
                placeholder={gitCredential?.stored && !gitTokenDirty ? t('settings.gitTokenStored') : t('settings.gitTokenEmpty')}
                onChange={(event) => { setGitToken(event.target.value); setGitTokenDirty(true) }}
              />
              {(gitCredential?.stored || gitTokenDirty) && <button className="secondary-button" type="button" onClick={() => { setGitToken(''); setGitTokenDirty(true) }}>{t('settings.gitTokenClear')}</button>}
            </div>
          </SettingRow>
          <div className="settings-security-note"><strong>{t('settings.proxyPasswordSecurity')}</strong><span>{t('settings.gitTokenSecurityBody', { store: gitCredential?.secureStore ?? t('settings.proxyPasswordStore') })}</span></div>
        </SettingsSection>

        <SettingsSection active={activeCategory === 'shortcuts'} icon={<Command />} title={t('settings.shortcuts')}>
          <h3 className="settings-subsection">{t('settings.group.shortcuts')}</h3>
          <SettingRow label={t('settings.shortcut.search')}><kbd>{formatShortcut(settings.shortcuts.search)}</kbd></SettingRow>
          <SettingRow label={t('settings.shortcut.settings')}><kbd>{formatShortcut(settings.shortcuts.settings)}</kbd></SettingRow>
          <h3 className="settings-subsection">{t('settings.globalShortcut.title')}</h3>
          <GlobalShortcutSettings baseSettings={() => draftRef.current} />
        </SettingsSection>

        <SettingsSection active={activeCategory === 'data'} icon={<Database />} title={t('settings.data')}>
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
        </SettingsSection>

        <SettingsSection active={activeCategory === 'about'} icon={<Info />} title={t('settings.about')}>
          <div className="settings-about-card">
            <img className="brand-symbol" src={brandLogo} alt="" aria-hidden="true" draggable={false} />
            <div>
              <strong>MooTool Next Tauri</strong>
              <span>{t('settings.aboutVersion', { version: aboutVersion || '…' })}</span>
              <small>MIT License</small>
            </div>
          </div>
          <UpdateCard disabled={busy} />
        </SettingsSection>
        </div>
      </ResizableColumns>

      <footer className="settings-footer">
        <div className={status || providerError ? 'settings-status settings-status--visible' : 'settings-status'}>
          {status || providerError}
        </div>
        <button className="secondary-button" type="button" disabled={busy} onClick={() => void handleReset()}>
          <RotateCcw /> {t('settings.reset')}
        </button>
        <button className="primary-button" type="button" disabled={busy || !unsaved} onClick={() => void handleSave()}>
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

function formatShortcut(value: string): string {
  const command = navigator.platform.toLowerCase().includes('mac') ? '⌘' : 'Ctrl'
  return value.replace('CommandOrControl', command)
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
