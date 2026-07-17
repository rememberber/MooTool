import {
  ArchiveRestore,
  Code2,
  Command,
  Database,
  Download,
  ExternalLink,
  FolderGit2,
  FolderOpen,
  Info,
  Network,
  PanelLeft,
  RefreshCw,
  Settings2,
  SlidersHorizontal,
  SquareTerminal,
  SunMedium,
  X,
  type LucideIcon
} from 'lucide-react'
import DOMPurify from 'dompurify'
import { marked } from 'marked'
import { useEffect, useMemo, useState, type ReactNode } from 'react'
import { toolById, toolGroups, type ToolId } from '@/app/toolRegistry'
import { BrandIcon } from '@/shared/components/BrandIcon'
import {
  accentColorPresets,
  type AppSettings,
  type SecretKey,
  type SecretStatus,
  type SettingsPatch
} from '@/shared/contracts/settings'
import type { AppPaths, RuntimeId, RuntimeStatus } from '@/shared/contracts/app'
import { translationLanguageCodes } from '@/shared/contracts/network'
import type { BackupInfo, BackupKind, BackupLocation } from '@/shared/contracts/backup'
import type { LegacyMigrationPreview, LegacyMigrationWarning } from '@/shared/contracts/migration'
import type { UpdateCheckResult } from '@/shared/contracts/update'
import { useUpdateState } from '@/shared/hooks/useUpdateState'
import { Dialog } from '@/shared/components/Dialog'
import { useToast } from '@/shared/feedback/ToastProvider'
import { ResizableColumns } from '@/shared/components/ResizableColumns'
import { useI18n } from '@/shared/i18n/I18nProvider'
import type { MessageKey } from '@/shared/i18n/messages'
import { useSettings } from './SettingsProvider'

type SettingsCategory = 'general' | 'appearance' | 'layout' | 'editor' | 'network' | 'data' | 'vault' | 'runtime' | 'tools' | 'shortcuts' | 'about'

const categories: Array<{ id: SettingsCategory; labelKey: MessageKey; icon: LucideIcon }> = [
  { id: 'general', labelKey: 'settings.category.general', icon: Settings2 },
  { id: 'appearance', labelKey: 'settings.category.appearance', icon: SunMedium },
  { id: 'layout', labelKey: 'settings.category.layout', icon: PanelLeft },
  { id: 'editor', labelKey: 'settings.category.editor', icon: Code2 },
  { id: 'network', labelKey: 'settings.category.network', icon: Network },
  { id: 'data', labelKey: 'settings.category.data', icon: Database },
  { id: 'vault', labelKey: 'settings.category.vault', icon: FolderGit2 },
  { id: 'runtime', labelKey: 'settings.category.runtime', icon: SquareTerminal },
  { id: 'tools', labelKey: 'settings.category.tools', icon: SlidersHorizontal },
  { id: 'shortcuts', labelKey: 'settings.category.shortcuts', icon: Command },
  { id: 'about', labelKey: 'settings.category.about', icon: Info }
]

export function SettingsWindow() {
  const { settings, ready, updateSettings } = useSettings()
  const { t } = useI18n()
  const toast = useToast()
  const [activeCategory, setActiveCategory] = useState<SettingsCategory>('general')
  const category = categories.find((item) => item.id === activeCategory) ?? categories[0]

  function commit(patch: SettingsPatch): void {
    void updateSettings(patch).catch(() => toast.error(t('settings.saveFailed')))
  }

  return (
    <main className="settings-window">
      <header className="settings-titlebar window-drag">
        <span>{t('settings.title')}</span>
        <button className="icon-ghost" type="button" aria-label={t('settings.close')} onClick={() => window.mootool.closeSettings()}>
          <X size={16} />
        </button>
      </header>

      <ResizableColumns className="settings-layout" columns={2} defaultSizes={[220, 780]} minPaneWidths={[180, 420]} storageKey="settings-window">
        <nav className="settings-nav" aria-label={t('settings.title')}>
          {categories.map((item) => {
            const Icon = item.icon
            return (
              <button
                className={item.id === activeCategory ? 'settings-nav__item settings-nav__item--active' : 'settings-nav__item'}
                type="button"
                key={item.id}
                aria-current={item.id === activeCategory ? 'page' : undefined}
                onClick={() => setActiveCategory(item.id)}
              >
                <Icon size={16} />
                <span>{t(item.labelKey)}</span>
              </button>
            )
          })}
        </nav>

        <section className="settings-content">
          <header className="settings-content__header">
            <category.icon size={20} />
            <h1>{t(category.labelKey)}</h1>
          </header>
          <div className="settings-scroll">
            {ready ? (
              <SettingsCategoryContent category={activeCategory} settings={settings} commit={commit} />
            ) : (
              <div className="settings-loading">{t('common.loading')}</div>
            )}
          </div>
        </section>
      </ResizableColumns>
    </main>
  )
}

function SettingsCategoryContent({ category, settings, commit }: {
  category: SettingsCategory
  settings: AppSettings
  commit: (patch: SettingsPatch) => void
}) {
  switch (category) {
    case 'general':
      return <GeneralSettings settings={settings} commit={commit} />
    case 'appearance':
      return <AppearanceSettings settings={settings} commit={commit} />
    case 'layout':
      return <LayoutSettings settings={settings} commit={commit} />
    case 'editor':
      return <EditorSettings settings={settings} commit={commit} />
    case 'network':
      return <NetworkSettings settings={settings} commit={commit} />
    case 'data':
      return <DataSettings settings={settings} commit={commit} />
    case 'vault':
      return <VaultSettings settings={settings} commit={commit} />
    case 'runtime':
      return <RuntimeSettings settings={settings} commit={commit} />
    case 'tools':
      return <ToolDefaults settings={settings} commit={commit} />
    case 'shortcuts':
      return <ShortcutSettings settings={settings} />
    case 'about':
      return <AboutSettings />
  }
}

function GeneralSettings({ settings, commit }: SettingsPanelProps) {
  const { languageLabels, languages, t } = useI18n()
  return (
    <SettingsGroup title={t('settings.group.application')}>
      <SettingRow label={t('settings.language')}>
        <select value={settings.general.language} onChange={(event) => commit({ general: { language: event.target.value as AppSettings['general']['language'] } })}>
          {languages.map((language) => <option value={language} key={language}>{languageLabels[language]}</option>)}
        </select>
      </SettingRow>
      <SettingRow label={t('settings.autoCheckUpdates')}>
        <Toggle checked={settings.general.autoCheckUpdates} label={t('settings.autoCheckUpdates')} onChange={(value) => commit({ general: { autoCheckUpdates: value } })} />
      </SettingRow>
      <SettingRow label={t('settings.autoDownloadUpdates')}>
        <Toggle checked={settings.general.autoDownloadUpdates} label={t('settings.autoDownloadUpdates')} onChange={(value) => commit({ general: { autoDownloadUpdates: value } })} />
      </SettingRow>
      <SettingRow label={t('settings.startMaximized')}>
        <Toggle checked={settings.general.startMaximized} label={t('settings.startMaximized')} onChange={(value) => commit({ general: { startMaximized: value } })} />
      </SettingRow>
      <SettingRow label={t('settings.trayEnabled')}>
        <Toggle checked={settings.general.trayEnabled} label={t('settings.trayEnabled')} onChange={(value) => commit({ general: { trayEnabled: value } })} />
      </SettingRow>
      <SettingRow label={t('settings.closeBehavior')}>
        <Segmented
          value={settings.general.closeBehavior}
          options={[
            { value: 'ask', label: t('settings.close.ask') },
            { value: 'hide', label: t('settings.close.hide') },
            { value: 'quit', label: t('settings.close.quit') }
          ]}
          onChange={(value) => commit({ general: { closeBehavior: value } })}
        />
      </SettingRow>
    </SettingsGroup>
  )
}

function AppearanceSettings({ settings, commit }: SettingsPanelProps) {
  const { t } = useI18n()
  return (
    <SettingsGroup title={t('settings.group.theme')}>
      <SettingRow label={t('settings.interfaceStyle')}>
        <Segmented
          value={settings.appearance.interfaceStyle}
          options={[
            { value: 'modern', label: t('settings.interfaceStyle.modern') },
            { value: 'quiet', label: t('settings.interfaceStyle.quiet') }
          ]}
          onChange={(value) => commit({ appearance: { interfaceStyle: value } })}
        />
      </SettingRow>
      <SettingRow label={t('settings.theme')}>
        <Segmented
          value={settings.appearance.theme}
          options={[
            { value: 'system', label: t('settings.theme.system') },
            { value: 'light', label: t('settings.theme.light') },
            { value: 'dark', label: t('settings.theme.dark') }
          ]}
          onChange={(value) => commit({ appearance: { theme: value } })}
        />
      </SettingRow>
      <SettingRow label={t('settings.accentColor')}>
        <div className="color-swatches">
          {accentColorPresets.map((preset) => (
            <button
              className={settings.appearance.accentColor === preset.id ? 'color-swatch color-swatch--active' : 'color-swatch'}
              type="button"
              aria-label={`${t('settings.accentColor')} ${preset.id}`}
              aria-pressed={settings.appearance.accentColor === preset.id}
              key={preset.id}
              style={{ '--swatch-color': preset.value } as React.CSSProperties}
              onClick={() => commit({ appearance: { accentColor: preset.id } })}
            />
          ))}
        </div>
      </SettingRow>
      <SettingRow label={t('settings.fontSize')}>
        <div className="range-control">
          <input
            type="range"
            min="12"
            max="18"
            value={settings.appearance.fontSize}
            aria-label={t('settings.fontSize')}
            onChange={(event) => commit({ appearance: { fontSize: Number(event.target.value) } })}
          />
          <output>{settings.appearance.fontSize}</output>
        </div>
      </SettingRow>
      <SettingRow label={t('settings.unifiedBackground')}>
        <Toggle checked={settings.appearance.unifiedBackground} label={t('settings.unifiedBackground')} onChange={(value) => commit({ appearance: { unifiedBackground: value } })} />
      </SettingRow>
    </SettingsGroup>
  )
}

function LayoutSettings({ settings, commit }: SettingsPanelProps) {
  const { t } = useI18n()
  const hiddenToolIds = new Set(settings.layout.hiddenNavigationToolIds)
  const navigationToolIds = toolGroups.flatMap((group) => group.toolIds)

  function setToolVisible(toolId: ToolId, visible: boolean): void {
    const nextHiddenToolIds = visible
      ? settings.layout.hiddenNavigationToolIds.filter((id) => id !== toolId)
      : navigationToolIds.filter((id) => id === toolId || hiddenToolIds.has(id))
    commit({ layout: { hiddenNavigationToolIds: nextHiddenToolIds } })
  }

  return (
    <>
      <SettingsGroup title={t('settings.group.navigation')}>
        <SettingRow label={t('settings.navigationStyle')}>
          <Segmented
            value={settings.layout.navigationStyle}
            options={[
              { value: 'classic', label: t('settings.navigation.classic') },
              { value: 'card', label: t('settings.navigation.card') },
              { value: 'grouped', label: t('settings.navigation.grouped') }
            ]}
            onChange={(value) => commit({ layout: { navigationStyle: value } })}
          />
        </SettingRow>
        <SettingRow label={t('settings.showRecent')}>
          <Toggle checked={settings.layout.showRecent} label={t('settings.showRecent')} onChange={(value) => commit({ layout: { showRecent: value } })} />
        </SettingRow>
        <SettingRow label={t('settings.compactNavigation')}>
          <Toggle checked={settings.layout.compactNavigation} label={t('settings.compactNavigation')} onChange={(value) => commit({ layout: { compactNavigation: value } })} />
        </SettingRow>
        <SettingRow label={t('settings.showSeparators')}>
          <Toggle checked={settings.layout.showSeparators} label={t('settings.showSeparators')} onChange={(value) => commit({ layout: { showSeparators: value } })} />
        </SettingRow>
        <SettingRow label={t('settings.hideNavigationTitles')}>
          <Toggle checked={settings.layout.hideNavigationTitles} label={t('settings.hideNavigationTitles')} onChange={(value) => commit({ layout: { hideNavigationTitles: value } })} />
        </SettingRow>
      </SettingsGroup>

      <SettingsGroup title={t('settings.navigation.toolsTitle')}>
        <div className="navigation-tool-visibility">
          <div className="navigation-tool-visibility__intro">
            <p>{t('settings.navigation.toolsDescription')}</p>
            <div>
              <button className="settings-command settings-command--quiet" type="button" disabled={hiddenToolIds.size === 0} onClick={() => commit({ layout: { hiddenNavigationToolIds: [] } })}>
                {t('settings.navigation.showAll')}
              </button>
              <button className="settings-command settings-command--quiet" type="button" disabled={hiddenToolIds.size === navigationToolIds.length} onClick={() => commit({ layout: { hiddenNavigationToolIds: navigationToolIds } })}>
                {t('settings.navigation.hideAll')}
              </button>
            </div>
          </div>
          <div className="navigation-tool-visibility__groups">
            {toolGroups.map((group) => (
              <section key={group.id}>
                <h3>{t(group.titleKey)}</h3>
                {group.toolIds.map((toolId) => {
                  const tool = toolById.get(toolId)!
                  const Icon = tool.icon
                  return (
                    <label key={toolId}>
                      <input type="checkbox" checked={!hiddenToolIds.has(toolId)} onChange={(event) => setToolVisible(toolId, event.target.checked)} />
                      <Icon size={15} />
                      <span>{t(tool.titleKey)}</span>
                    </label>
                  )
                })}
              </section>
            ))}
          </div>
        </div>
      </SettingsGroup>
    </>
  )
}

function EditorSettings({ settings, commit }: SettingsPanelProps) {
  const { t } = useI18n()
  return (
    <SettingsGroup title={t('settings.group.editor')}>
      <SettingRow label={t('settings.sqlDialect')}>
        <select value={settings.editor.sqlDialect} onChange={(event) => commit({ editor: { sqlDialect: event.target.value } })}>
          {['Standard SQL', 'MySQL', 'PostgreSQL', 'Oracle PL/SQL', 'SQL Server Transact-SQL'].map((dialect) => <option key={dialect}>{dialect}</option>)}
        </select>
      </SettingRow>
      <RangeSetting label={t('settings.jsonFontSize')} value={settings.editor.jsonFontSize} onChange={(value) => commit({ editor: { jsonFontSize: value } })} />
      <RangeSetting label={t('settings.quickNoteFontSize')} value={settings.editor.quickNoteFontSize} onChange={(value) => commit({ editor: { quickNoteFontSize: value } })} />
      <SettingRow label={t('settings.softWrap')}>
        <Toggle checked={settings.editor.softWrap} label={t('settings.softWrap')} onChange={(value) => commit({ editor: { softWrap: value } })} />
      </SettingRow>
    </SettingsGroup>
  )
}

function NetworkSettings({ settings, commit }: SettingsPanelProps) {
  const { t } = useI18n()
  return (
    <>
      <SettingsGroup title={t('settings.group.proxy')}>
        <SettingRow label={t('settings.proxyEnabled')}>
          <Toggle checked={settings.network.proxyEnabled} label={t('settings.proxyEnabled')} onChange={(value) => commit({ network: { proxyEnabled: value } })} />
        </SettingRow>
        <TextSetting label={t('settings.proxyHost')} value={settings.network.proxyHost} disabled={!settings.network.proxyEnabled} onCommit={(value) => commit({ network: { proxyHost: value } })} />
        <TextSetting label={t('settings.proxyPort')} value={settings.network.proxyPort} disabled={!settings.network.proxyEnabled} onCommit={(value) => commit({ network: { proxyPort: value } })} />
        <TextSetting label={t('settings.proxyUsername')} value={settings.network.proxyUsername} disabled={!settings.network.proxyEnabled} onCommit={(value) => commit({ network: { proxyUsername: value } })} />
        <SecretSetting label={t('settings.proxyPassword')} secretKey="proxyPassword" disabled={!settings.network.proxyEnabled} />
      </SettingsGroup>
      <SettingsGroup title={t('settings.group.timeouts')}>
        <NumberSetting label={t('settings.requestTimeout')} value={settings.network.requestTimeoutMs} min={1_000} max={120_000} onCommit={(value) => commit({ network: { requestTimeoutMs: value } })} />
        <NumberSetting label={t('settings.translationTimeout')} value={settings.network.translationTimeoutMs} min={1_000} max={120_000} onCommit={(value) => commit({ network: { translationTimeoutMs: value } })} />
      </SettingsGroup>
    </>
  )
}

function DataSettings({ settings, commit }: SettingsPanelProps) {
  const { t } = useI18n()
  const toast = useToast()
  const [paths, setPaths] = useState<AppPaths | null>(null)
  const [backup, setBackup] = useState<BackupInfo | null>(null)
  const [busyKind, setBusyKind] = useState<BackupKind | null>(null)
  useEffect(() => { void window.mootool.getAppPaths().then(setPaths) }, [])
  useEffect(() => { void window.mootool.getBackupInfo().then(setBackup) }, [settings.data.directory])

  function exportBackup(kind: BackupKind): void {
    setBusyKind(kind)
    void window.mootool.exportBackup(kind)
      .then((result) => { if (result) toast.success(t('settings.backup.success')) })
      .catch((error) => toast.error(error instanceof Error ? error.message : String(error)))
      .finally(() => setBusyKind(null))
  }

  return (
    <>
      <SettingsGroup title={t('settings.group.storage')}>
        <DirectorySetting
          label={t('settings.dataDirectory')}
          value={settings.data.directory || paths?.userData || ''}
          onCommit={(value) => commit({ data: { directory: value } })}
        />
      </SettingsGroup>
      <SettingsGroup
        title={t('settings.group.backup')}
        action={<button className="settings-command" type="button" disabled={busyKind !== null} onClick={() => exportBackup('all')}><Download size={14} />{t('settings.backup.all')}</button>}
      >
        <BackupRow label={t('settings.backup.database')} path={backup?.databasePath ?? ''} kind="database" location="data" busy={busyKind} onExport={exportBackup} />
        <BackupRow label={t('settings.backup.settings')} path={backup?.settingsPath ?? ''} kind="settings" location="data" busy={busyKind} onExport={exportBackup} />
        <BackupRow label={t('settings.backup.images')} path={backup?.imagesPath ?? ''} kind="images" location="images" busy={busyKind} onExport={exportBackup} />
      </SettingsGroup>
      <LegacyMigrationSettings />
    </>
  )
}

function LegacyMigrationSettings() {
  const { t } = useI18n()
  const toast = useToast()
  const [sourceDirectory, setSourceDirectory] = useState('')
  const [preview, setPreview] = useState<LegacyMigrationPreview | null>(null)
  const [scanning, setScanning] = useState(false)
  const [migrating, setMigrating] = useState(false)
  const [confirmOpen, setConfirmOpen] = useState(false)

  useEffect(() => {
    let cancelled = false
    void window.mootool.getDefaultLegacySource().then((path) => {
      if (!cancelled) setSourceDirectory((current) => current || path)
    })
    return () => { cancelled = true }
  }, [])

  function scan(): void {
    if (!sourceDirectory.trim()) return
    setScanning(true)
    setPreview(null)
    void window.mootool.previewLegacyMigration({ sourceDirectory })
      .then(setPreview)
      .catch((error) => toast.error(error instanceof Error ? error.message : String(error)))
      .finally(() => setScanning(false))
  }

  function migrate(): void {
    setMigrating(true)
    void window.mootool.runLegacyMigration({ sourceDirectory })
      .then((result) => {
        const count = Object.values(result.imported).reduce((sum, value) => sum + value, 0)
        toast.success(result.alreadyMigrated
          ? t('settings.migration.alreadyMigrated')
          : t('settings.migration.success', { count: String(count) }))
        setConfirmOpen(false)
        scan()
      })
      .catch((error) => toast.error(error instanceof Error ? error.message : String(error)))
      .finally(() => setMigrating(false))
  }

  return (
    <SettingsGroup
      title={t('settings.group.migration')}
      action={<button className="settings-command" type="button" disabled={scanning || migrating || !sourceDirectory.trim()} onClick={scan}><RefreshCw size={14} />{t('settings.migration.scan')}</button>}
    >
      <div className="setting-row legacy-migration-row">
        <label>{t('settings.migration.source')}</label>
        <div className="legacy-migration-source">
          <input
            type="text"
            value={sourceDirectory}
            aria-label={t('settings.migration.source')}
            onChange={(event) => { setSourceDirectory(event.target.value); setPreview(null) }}
          />
          <button
            className="icon-button"
            type="button"
            aria-label={t('settings.chooseDirectory')}
            onClick={() => { void window.mootool.chooseDirectory(sourceDirectory).then((path) => { if (path) { setSourceDirectory(path); setPreview(null) } }) }}
          >
            <FolderOpen size={16} />
          </button>
        </div>
      </div>
      {preview && (
        <div className="legacy-migration-summary" aria-live="polite">
          <div>
            <strong>{preview.alreadyMigrated ? t('settings.migration.alreadyMigrated') : t('settings.migration.ready', { count: String(preview.totalItems) })}</strong>
            <span>{preview.databaseFound ? t('settings.migration.databaseFound') : t('settings.migration.databaseMissing')}</span>
            <span>{preview.configFound ? t('settings.migration.configFound') : t('settings.migration.configMissing')}</span>
          </div>
          {!preview.alreadyMigrated && (
            <button className="settings-command" type="button" disabled={migrating} onClick={() => setConfirmOpen(true)}>
              <ArchiveRestore size={14} />{t('settings.migration.import')}
            </button>
          )}
          {preview.warnings.map((warning) => <p key={warning}>{t(migrationWarningKeys[warning])}</p>)}
        </div>
      )}
      <Dialog
        title={t('settings.migration.confirmTitle')}
        open={confirmOpen}
        width={520}
        onClose={() => { if (!migrating) setConfirmOpen(false) }}
        footer={(
          <>
            <button className="settings-command settings-command--quiet" type="button" disabled={migrating} onClick={() => setConfirmOpen(false)}>{t('common.cancel')}</button>
            <button className="settings-command" type="button" disabled={migrating} onClick={migrate}><ArchiveRestore size={14} />{migrating ? t('settings.migration.importing') : t('settings.migration.import')}</button>
          </>
        )}
      >
        <p className="legacy-migration-confirm">{t('settings.migration.confirmBody', { count: String(preview?.totalItems ?? 0) })}</p>
      </Dialog>
    </SettingsGroup>
  )
}

const migrationWarningKeys: Record<LegacyMigrationWarning, MessageKey> = {
  differentVaultRemotes: 'settings.migration.warning.remotes',
  secretsSkipped: 'settings.migration.warning.secrets'
}

function BackupRow({ label, path, kind, location, busy, onExport }: {
  label: string
  path: string
  kind: BackupKind
  location: BackupLocation
  busy: BackupKind | null
  onExport: (kind: BackupKind) => void
}) {
  const { t } = useI18n()
  return (
    <div className="setting-row backup-row">
      <label>{label}</label>
      <div className="backup-row__control">
        <code title={path}>{path}</code>
        <button className="settings-command settings-command--quiet" type="button" aria-label={`${t('settings.backup.open')} ${label}`} onClick={() => { void window.mootool.openBackupLocation(location) }}><FolderOpen size={14} /></button>
        <button className="settings-command" type="button" disabled={busy !== null} onClick={() => onExport(kind)}><Download size={14} />{t('settings.backup.export')}</button>
      </div>
    </div>
  )
}

function VaultSettings({ settings, commit }: SettingsPanelProps) {
  const { t } = useI18n()
  return (
    <>
      <SettingsGroup title={t('settings.group.vaultPaths')}>
        <DirectorySetting label={t('settings.quickNoteVault')} value={settings.vault.quickNotePath} onCommit={(value) => commit({ vault: { quickNotePath: value } })} />
        <DirectorySetting label={t('settings.jsonVault')} value={settings.vault.jsonPath} onCommit={(value) => commit({ vault: { jsonPath: value } })} />
      </SettingsGroup>
      <SettingsGroup title={t('settings.group.git')}>
        <TextSetting label={t('settings.gitRemote')} value={settings.vault.gitRemote} onCommit={(value) => commit({ vault: { gitRemote: value } })} />
        <TextSetting label={t('settings.gitUsername')} value={settings.vault.gitUsername} onCommit={(value) => commit({ vault: { gitUsername: value } })} />
        <SecretSetting label={t('settings.gitToken')} secretKey="gitToken" />
        <SettingRow label={t('settings.autoCommit')}>
          <Toggle checked={settings.vault.autoCommit} label={t('settings.autoCommit')} onChange={(value) => commit({ vault: { autoCommit: value } })} />
        </SettingRow>
        <NumberSetting label={t('settings.autoCommitIdleSeconds')} value={settings.vault.autoCommitIdleSeconds} min={5} max={3600} onCommit={(value) => commit({ vault: { autoCommitIdleSeconds: value } })} />
        <NumberSetting label={t('settings.autoCommitInactiveSeconds')} value={settings.vault.autoCommitInactiveSeconds} min={5} max={3600} onCommit={(value) => commit({ vault: { autoCommitInactiveSeconds: value } })} />
        <NumberSetting label={t('settings.autoPullMinutes')} value={settings.vault.autoPullMinutes} min={0} max={1440} onCommit={(value) => commit({ vault: { autoPullMinutes: value } })} />
        <SettingRow label={t('settings.hideGitignoredFiles')}>
          <Toggle checked={settings.vault.hideGitignoredFiles} label={t('settings.hideGitignoredFiles')} onChange={(value) => commit({ vault: { hideGitignoredFiles: value } })} />
        </SettingRow>
      </SettingsGroup>
    </>
  )
}

function RuntimeSettings({ settings, commit }: SettingsPanelProps) {
  const { t } = useI18n()
  const [statuses, setStatuses] = useState<RuntimeStatus[]>([])
  const [detecting, setDetecting] = useState(false)
  type RuntimePathKey = 'javaPath' | 'groovyPath' | 'pythonPath' | 'nodePath'
  const pathKeys: Record<RuntimeId, RuntimePathKey> = {
    java: 'javaPath',
    groovy: 'groovyPath',
    python: 'pythonPath',
    node: 'nodePath'
  }

  function detect(): void {
    setDetecting(true)
    void window.mootool.detectRuntimes().then(setStatuses).finally(() => setDetecting(false))
  }

  useEffect(detect, [])

  return (
    <SettingsGroup
      className="runtime-settings-group"
      title={t('settings.group.runtimes')}
      action={(
        <button className="settings-command" type="button" disabled={detecting} onClick={detect}>
          <RefreshCw size={14} className={detecting ? 'spin' : undefined} />
          {t('settings.runtime.detect')}
        </button>
      )}
    >
      {(['java', 'groovy', 'python', 'node'] as RuntimeId[]).map((id) => {
        const key = pathKeys[id]
        const status = statuses.find((item) => item.id === id)
        return (
          <div className="runtime-row" key={id}>
            <div className="runtime-row__label">
              <strong>{runtimeLabels[id]}</strong>
              <span className={status?.available ? 'runtime-status runtime-status--ok' : 'runtime-status'}>
                {status?.available ? status.version : t('settings.runtime.notFound')}
              </span>
            </div>
            <TextInput
              value={settings.runtime[key]}
              placeholder={status?.command || t('settings.runtime.auto')}
              ariaLabel={`${runtimeLabels[id]} ${t('settings.runtime.path')}`}
              onCommit={(value) => commit({ runtime: { [key]: value } })}
            />
          </div>
        )
      })}
    </SettingsGroup>
  )
}

function ToolDefaults({ settings, commit }: SettingsPanelProps) {
  const { t } = useI18n()
  return (
    <SettingsGroup title={t('settings.group.toolDefaults')}>
      <NumberSetting label={t('settings.qrCodeSize')} value={settings.tools.qrCodeSize} min={120} max={2000} onCommit={(value) => commit({ tools: { qrCodeSize: value } })} />
      <SettingRow label={t('settings.qrErrorCorrection')}>
        <select value={settings.tools.qrErrorCorrection} onChange={(event) => commit({ tools: { qrErrorCorrection: event.target.value as AppSettings['tools']['qrErrorCorrection'] } })}>
          {['L', 'M', 'Q', 'H'].map((level) => <option key={level}>{level}</option>)}
        </select>
      </SettingRow>
      <NumberSetting label={t('settings.randomStringLength')} value={settings.tools.randomStringLength} min={1} max={4096} onCommit={(value) => commit({ tools: { randomStringLength: value } })} />
      <DirectorySetting label={t('settings.exportDirectory')} value={settings.tools.exportDirectory} onCommit={(value) => commit({ tools: { exportDirectory: value } })} />
      <SettingRow label={t('settings.translationProvider')}>
        <select value={settings.tools.translationProvider} onChange={(event) => commit({ tools: { translationProvider: event.target.value as AppSettings['tools']['translationProvider'] } })}>
          <option value="google">Google</option><option value="bing">Bing</option>
        </select>
      </SettingRow>
      <SettingRow label={t('settings.translationSource')}>
        <select value={settings.tools.translationSourceLang} onChange={(event) => commit({ tools: { translationSourceLang: event.target.value } })}>
          {translationLanguageCodes.map((code) => <option key={code} value={code}>{t(`translation.lang.${code}` as MessageKey)}</option>)}
        </select>
      </SettingRow>
      <SettingRow label={t('settings.translationTarget')}>
        <select value={settings.tools.translationTargetLang} onChange={(event) => commit({ tools: { translationTargetLang: event.target.value } })}>
          {translationLanguageCodes.filter((code) => code !== 'auto').map((code) => <option key={code} value={code}>{t(`translation.lang.${code}` as MessageKey)}</option>)}
        </select>
      </SettingRow>
    </SettingsGroup>
  )
}

function ShortcutSettings({ settings }: { settings: AppSettings }) {
  const { t } = useI18n()
  return (
    <SettingsGroup title={t('settings.group.shortcuts')}>
      <SettingRow label={t('settings.shortcut.search')}><kbd>{formatShortcut(settings.shortcuts.search)}</kbd></SettingRow>
      <SettingRow label={t('settings.shortcut.settings')}><kbd>{formatShortcut(settings.shortcuts.settings)}</kbd></SettingRow>
    </SettingsGroup>
  )
}

function AboutSettings() {
  const { t } = useI18n()
  const toast = useToast()
  const [version, setVersion] = useState('')
  const [checking, setChecking] = useState(false)
  const [result, setResult] = useState<UpdateCheckResult | null>(null)
  const updateState = useUpdateState()
  const releaseNotesHtml = useMemo(() => result?.releaseNotes
    ? DOMPurify.sanitize(marked.parse(result.releaseNotes, { async: false }) as string)
    : '', [result?.releaseNotes])
  useEffect(() => { void window.mootool.getAppVersion().then(setVersion) }, [])

  function check(): void {
    setChecking(true)
    void window.mootool.checkForUpdates()
      .then(setResult)
      .catch(() => toast.error(t('settings.update.failed')))
      .finally(() => setChecking(false))
  }

  function downloadUpdate(): void {
    void window.mootool.downloadUpdate().catch(() => toast.error(t('settings.update.downloadFailed')))
  }

  function installUpdate(): void {
    void window.mootool.installUpdate().catch(() => toast.error(t('settings.update.installFailed')))
  }

  return (
    <div className="settings-about-page">
      <div className="settings-about">
        <BrandIcon size={52} />
        <div>
          <h2>MooTool</h2>
          <p>{t('settings.version', { version: version || '…' })}</p>
          <p>MIT License</p>
        </div>
      </div>
      <div className="settings-about-actions">
        <button className="settings-command" type="button" disabled={checking} onClick={check}>
          <RefreshCw size={14} className={checking ? 'spin' : undefined} />
          {checking ? t('settings.update.checking') : t('settings.update.check')}
        </button>
        <button className="settings-command settings-command--quiet" type="button" onClick={() => { void window.mootool.openProjectPage() }}>
          <ExternalLink size={14} />{t('settings.update.project')}
        </button>
      </div>
      {result && (
        <section className="settings-update-result" aria-live="polite">
          <strong>{result.status === 'available' ? t('settings.update.available', { version: result.latestVersion }) : t('settings.update.latest')}</strong>
          <span className="settings-update-result__target">
            {t('settings.update.target', {
              product: result.productName,
              platform: result.target.platform,
              architecture: result.target.architecture
            })}
          </span>
          {result.status === 'available' && (
            <>
              <div className="settings-update-result__actions">
                {result.download && updateState.status === 'ready' && (
                  <button className="settings-command" type="button" onClick={installUpdate}>
                    {updateState.installMode === 'manual' ? <FolderOpen size={14} /> : <RefreshCw size={14} />}
                    {t(updateState.installMode === 'manual' ? 'settings.update.openDownloaded' : 'settings.update.installRestart')}
                  </button>
                )}
                {result.download && updateState.status !== 'ready' && (
                  <button className="settings-command" type="button" disabled={updateState.status === 'downloading'} onClick={downloadUpdate}>
                    <Download size={14} />
                    {updateState.status === 'downloading'
                      ? t('settings.update.downloading', { percent: String(updateState.percent ?? 0) })
                      : t('settings.update.download')}
                  </button>
                )}
                <button className="settings-command settings-command--quiet" type="button" onClick={() => { void window.mootool.openReleasePage() }}>
                  <ExternalLink size={14} />{t('settings.update.openRelease')}
                </button>
              </div>
              {result.download
                ? <code className="settings-update-result__file">{result.download.fileName}</code>
                : <span className="settings-update-result__missing">{t('settings.update.noDownload')}</span>}
              {updateState.status === 'error' && updateState.message && (
                <span className="settings-update-result__error">{updateState.message}</span>
              )}
              {releaseNotesHtml && (
                <article
                  className="settings-update-result__notes"
                  dangerouslySetInnerHTML={{ __html: releaseNotesHtml }}
                />
              )}
            </>
          )}
        </section>
      )}
    </div>
  )
}

type SettingsPanelProps = {
  settings: AppSettings
  commit: (patch: SettingsPatch) => void
}

function SettingsGroup({ title, action, className = '', children }: { title: string; action?: ReactNode; className?: string; children: ReactNode }) {
  return (
    <section className={['settings-group', className].filter(Boolean).join(' ')}>
      <header><h2>{title}</h2>{action}</header>
      <div className="settings-group__rows">{children}</div>
    </section>
  )
}

function SettingRow({ label, children }: { label: string; children: ReactNode }) {
  return <div className="setting-row"><label>{label}</label><div className="setting-row__control">{children}</div></div>
}

function Toggle({ checked, label, onChange }: { checked: boolean; label: string; onChange: (value: boolean) => void }) {
  return (
    <button className={checked ? 'toggle toggle--checked' : 'toggle'} type="button" role="switch" aria-checked={checked} aria-label={label} onClick={() => onChange(!checked)}>
      <span />
    </button>
  )
}

function Segmented<Value extends string>({ value, options, onChange }: {
  value: Value
  options: Array<{ value: Value; label: string }>
  onChange: (value: Value) => void
}) {
  return (
    <div className="segmented">
      {options.map((option) => (
        <button className={value === option.value ? 'segmented__item segmented__item--active' : 'segmented__item'} type="button" aria-pressed={value === option.value} key={option.value} onClick={() => onChange(option.value)}>
          {option.label}
        </button>
      ))}
    </div>
  )
}

function TextSetting({ label, value, disabled, onCommit }: { label: string; value: string; disabled?: boolean; onCommit: (value: string) => void }) {
  return <SettingRow label={label}><TextInput value={value} disabled={disabled} ariaLabel={label} onCommit={onCommit} /></SettingRow>
}

function TextInput({ value, placeholder, disabled, ariaLabel, onCommit }: {
  value: string
  placeholder?: string
  disabled?: boolean
  ariaLabel: string
  onCommit: (value: string) => void
}) {
  const [draft, setDraft] = useState(value)
  useEffect(() => setDraft(value), [value])
  return (
    <input
      type="text"
      value={draft}
      placeholder={placeholder}
      disabled={disabled}
      aria-label={ariaLabel}
      onChange={(event) => setDraft(event.target.value)}
      onBlur={() => { if (draft !== value) onCommit(draft.trim()) }}
      onKeyDown={(event) => { if (event.key === 'Enter') event.currentTarget.blur() }}
    />
  )
}

function DirectorySetting({ label, value, onCommit }: { label: string; value: string; onCommit: (value: string) => void }) {
  const { t } = useI18n()
  return (
    <SettingRow label={label}>
      <div className="path-control">
        <TextInput value={value} ariaLabel={label} onCommit={onCommit} />
        <button className="icon-button" type="button" aria-label={t('settings.chooseDirectory')} onClick={() => { void window.mootool.chooseDirectory(value).then((path) => { if (path) onCommit(path) }) }}>
          <FolderOpen size={16} />
        </button>
      </div>
    </SettingRow>
  )
}

function NumberSetting({ label, value, min, max, onCommit }: { label: string; value: number; min: number; max: number; onCommit: (value: number) => void }) {
  return (
    <SettingRow label={label}>
      <input
        className="number-input"
        type="number"
        value={value}
        min={min}
        max={max}
        aria-label={label}
        onChange={(event) => onCommit(Number(event.target.value))}
      />
    </SettingRow>
  )
}

function SecretSetting({ label, secretKey, disabled = false }: { label: string; secretKey: SecretKey; disabled?: boolean }) {
  const { t } = useI18n()
  const toast = useToast()
  const [value, setValue] = useState('')
  const [status, setStatus] = useState<SecretStatus | null>(null)
  const [saving, setSaving] = useState(false)

  useEffect(() => {
    void window.mootool.getSecretStatus(secretKey).then(setStatus)
  }, [secretKey])

  function save(): void {
    if (!value) {
      return
    }
    setSaving(true)
    void window.mootool.setSecret(secretKey, value)
      .then((nextStatus) => {
        setStatus(nextStatus)
        setValue('')
        toast.success(t('settings.secret.saved'))
      })
      .catch(() => toast.error(t('settings.secret.unavailable')))
      .finally(() => setSaving(false))
  }

  function clear(): void {
    setSaving(true)
    void window.mootool.clearSecret(secretKey)
      .then((nextStatus) => {
        setStatus(nextStatus)
        setValue('')
      })
      .catch(() => toast.error(t('settings.secret.unavailable')))
      .finally(() => setSaving(false))
  }

  return (
    <SettingRow label={label}>
      <div className="secret-control">
        <input
          type="password"
          value={value}
          disabled={disabled || saving || status?.encryptionAvailable === false}
          placeholder={status?.stored ? t('settings.secret.stored') : ''}
          aria-label={label}
          autoComplete="new-password"
          onChange={(event) => setValue(event.target.value)}
          onKeyDown={(event) => { if (event.key === 'Enter') save() }}
        />
        <button className="settings-command" type="button" disabled={disabled || saving || !value} onClick={save}>{t('settings.secret.save')}</button>
        {status?.stored && <button className="settings-command settings-command--quiet" type="button" disabled={saving} onClick={clear}>{t('settings.secret.clear')}</button>}
      </div>
    </SettingRow>
  )
}

function RangeSetting({ label, value, onChange }: { label: string; value: number; onChange: (value: number) => void }) {
  return (
    <SettingRow label={label}>
      <div className="range-control">
        <input type="range" min="11" max="24" value={value} aria-label={label} onChange={(event) => onChange(Number(event.target.value))} />
        <output>{value}</output>
      </div>
    </SettingRow>
  )
}

function formatShortcut(value: string): string {
  if (window.mootool.platform === 'darwin') {
    return value.replace('CommandOrControl', '⌘')
  }
  return value.replace('CommandOrControl', 'Ctrl')
}

const runtimeLabels: Record<RuntimeId, string> = {
  java: 'Java',
  groovy: 'Groovy',
  python: 'Python',
  node: 'Node.js'
}
