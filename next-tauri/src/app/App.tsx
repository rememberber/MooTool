import {
  Boxes,
  ChevronDown,
  Command,
  FolderCog,
  Languages,
  PanelLeftClose,
  Search,
  Star,
  Settings
} from 'lucide-react'
import { useEffect, useMemo, useState } from 'react'
import brandLogo from '../assets/brand/mootool-logo.png'
import { CalculatorHost } from '../features/calculator/CalculatorHost'
import { ColorHost } from '../features/color/ColorHost'
import { CryptoHost } from '../features/crypto/CryptoHost'
import { ConfigHost } from '../features/config/ConfigHost'
import { CronHost } from '../features/cron/CronHost'
import { EncodeHost } from '../features/encode/EncodeHost'
import { EditorLabHost } from '../features/editorLab/EditorLabHost'
import { HomePage } from '../features/home/HomePage'
import { HostHost } from '../features/host/HostHost'
import { CustomGroupDialog } from '../features/groups/CustomGroupDialog'
import { HttpHost } from '../features/http/HttpHost'
import { ImageHost } from '../features/image/ImageHost'
import { JsonHost } from '../features/json/JsonHost'
import { MessageBoardHost } from '../features/messageBoard/MessageBoardHost'
import { NetworkHost } from '../features/network/NetworkHost'
import { PdfHost } from '../features/pdf/PdfHost'
import { ProtobufHost } from '../features/protobuf/ProtobufHost'
import { QrcodeHost } from '../features/qrcode/QrcodeHost'
import { QuickNoteHost } from '../features/quickNote/QuickNoteHost'
import { ReformatHost } from '../features/reformat/ReformatHost'
import { RegexHost } from '../features/regex/RegexHost'
import { RuntimeHost } from '../features/runtime/RuntimeHost'
import { useSettings } from '../features/settings/SettingsProvider'
import { TextDiffHost } from '../features/textDiff/TextDiffHost'
import { TimestampHost } from '../features/timestamp/TimestampHost'
import { TranslationHost } from '../features/translation/TranslationHost'
import { UaHost } from '../features/ua/UaHost'
import { VariablesHost } from '../features/variables/VariablesHost'
import { SystemHost } from '../features/system/SystemHost'
import { WebviewLab } from '../features/webviewLab/WebviewLab'
import { runtimeApi } from '../platform/api/runtimeApi'
import { historyApi } from '../platform/api/historyApi'
import { desktopApi } from '../platform/api/desktopApi'
import { productUpdateApi } from '../platform/api/updateApi'
import type { DesktopCloseDecision, DesktopCloseRequest } from '../platform/contracts/desktop'
import type { RuntimeInfo } from '../platform/contracts/runtime'
import type { AppLanguage } from '../platform/contracts/settings'
import { errorMessage, reportProductError, toProductError } from '../shared/errors'
import { useI18n } from './i18n'
import {
  homeTool,
  navigationToolCatalog,
  productToolCatalog,
  toolCatalog,
  toolGroups,
  type ToolId
} from './toolCatalog'

export function App() {
  const { settings, ready: settingsReady, error: settingsError, save, openWindow } = useSettings()
  const { t, toolTitle, groupTitle } = useI18n()
  const [activeTool, setActiveTool] = useState<ToolId>('home')
  const [query, setQuery] = useState('')
  const [runtimeInfo, setRuntimeInfo] = useState<RuntimeInfo>()
  const [notice, setNotice] = useState('')
  const [customGroupsOpen, setCustomGroupsOpen] = useState(false)
  const [closeRequest, setCloseRequest] = useState<DesktopCloseRequest>()
  const [rememberCloseChoice, setRememberCloseChoice] = useState(false)
  const sidebarCompact = settings.layout.sidebarCompact
  const recent = settings.tools.recent.filter(isToolId)
  const favorites = settings.tools.favorites
    .filter(isToolId)
    .map((toolId) => toolCatalog.find((tool) => tool.id === toolId))
    .filter((tool): tool is NonNullable<typeof tool> => Boolean(tool && !tool.engineeringOnly))

  useEffect(() => {
    void runtimeApi.getInfo().then(setRuntimeInfo).catch((error: unknown) => {
      setNotice(error instanceof Error ? error.message : t('shell.readRuntimeError'))
    })
  }, [t])

  useEffect(() => {
    if (!settingsReady || !settings.general.autoCheckUpdates || !window.__TAURI_INTERNALS__) return
    let cancelled = false
    const check = async () => {
      try {
        const result = await productUpdateApi.check()
        if (!cancelled && result.status === 'available') {
          setNotice(t('settings.updateAvailable', { version: result.latestVersion ?? '' }))
        }
      } catch (cause) {
        await reportProductError(toProductError(cause), 'application.update.scheduled-check')
      }
    }
    const startupTimer = window.setTimeout(() => void check(), 10_000)
    const interval = window.setInterval(() => void check(), 60 * 60 * 1_000)
    return () => {
      cancelled = true
      window.clearTimeout(startupTimer)
      window.clearInterval(interval)
    }
  }, [settings.general.autoCheckUpdates, settingsReady, t])

  useEffect(() => {
    const handleKeyDown = (event: KeyboardEvent) => {
      if ((event.metaKey || event.ctrlKey) && event.key.toLowerCase() === 'k') {
        event.preventDefault()
        document.getElementById('tool-search')?.focus()
      }
    }
    window.addEventListener('keydown', handleKeyDown)
    return () => window.removeEventListener('keydown', handleKeyDown)
  }, [])

  useEffect(() => {
    let unlisten: (() => void) | undefined
    let cancelled = false
    void desktopApi.subscribeCloseRequested((request) => {
      if (!cancelled) {
        setRememberCloseChoice(false)
        setCloseRequest(request)
      }
    }).then((dispose) => {
      if (cancelled) dispose()
      else unlisten = dispose
    }).catch((cause: unknown) => setNotice(errorMessage(cause)))
    return () => {
      cancelled = true
      unlisten?.()
    }
  }, [])

  useEffect(() => {
    if (!window.__TAURI_INTERNALS__) return
    let unlisten: (() => void) | undefined
    let cancelled = false
    void import('@tauri-apps/api/window')
      .then(({ getCurrentWindow }) => getCurrentWindow().onCloseRequested((event) => {
        // Rust owns the close policy and resolves it through the custom dialog.
        // Keep Tauri's built-in listener from destroying the WebviewWindow first.
        event.preventDefault()
      }))
      .then((dispose) => {
        if (cancelled) dispose()
        else unlisten = dispose
      })
      .catch((cause: unknown) => setNotice(errorMessage(cause)))
    return () => {
      cancelled = true
      unlisten?.()
    }
  }, [])

  async function resolveClose(decision: DesktopCloseDecision): Promise<void> {
    try {
      if (rememberCloseChoice && decision !== 'cancel') {
        await save((current) => ({
          ...current,
          general: {
            ...current.general,
            closeBehavior: decision === 'quit' ? 'quit' : 'minimizeToTray'
          }
        }))
      }
      await desktopApi.resolveCloseRequest(decision)
      setCloseRequest(undefined)
    } catch (cause) {
      setNotice(errorMessage(cause))
    }
  }

  const visibleGroups = useMemo(() => toolGroups.map((group) => ({
    group,
    tools: navigationToolCatalog.filter((tool) => tool.group === group && (
      !query.trim()
      || `${tool.title} ${toolTitle(tool)} ${tool.keywords.join(' ')}`
        .toLowerCase()
        .includes(query.trim().toLowerCase())
    ))
  })).filter(({ tools }) => tools.length > 0), [query, toolTitle])
  const visibleCustomGroups = useMemo(() => settings.layout.customGroups.map((group) => ({
    ...group,
    tools: group.toolIds
      .map((toolId) => productToolCatalog.find((tool) => tool.id === toolId))
      .filter((tool): tool is NonNullable<typeof tool> => Boolean(tool))
      .filter((tool) => !query.trim() || `${tool.title} ${toolTitle(tool)} ${tool.keywords.join(' ')}`
        .toLowerCase().includes(query.trim().toLowerCase()))
  })).filter((group) => group.tools.length > 0), [query, settings.layout.customGroups, toolTitle])

  function openTool(toolId: ToolId): void {
    const tool = toolCatalog.find((item) => item.id === toolId)
    if (tool && !tool.ready) {
      setNotice(t('shell.queued', { tool: toolTitle(tool) }))
      return
    }
    setActiveTool(toolId)
    setNotice('')
    if (toolId !== 'home' && settingsReady) {
      void historyApi.record({
        id: crypto.randomUUID(),
        toolId,
        action: t('history.action.openTool'),
        summary: t('history.summary.openTool', { tool: tool ? toolTitle(tool) : toolId }),
        status: 'info',
        createdAt: Date.now()
      }, settings.data.historyLimit).catch((cause: unknown) => setNotice(errorMessage(cause)))
      void save((current) => ({
        ...current,
        tools: {
          ...current.tools,
          recent: [toolId, ...current.tools.recent.filter((item) => item !== toolId)].slice(0, 5)
        }
      })).catch((cause: unknown) => setNotice(errorMessage(cause)))
    }
  }

  function toggleSidebar(): void {
    if (!settingsReady) return
    void save((current) => ({
      ...current,
      layout: { ...current.layout, sidebarCompact: !current.layout.sidebarCompact }
    })).catch((cause: unknown) => setNotice(errorMessage(cause)))
  }

  function toggleFavorite(toolId: ToolId): void {
    if (!settingsReady || toolId === 'home') return
    void save((current) => {
      const selected = current.tools.favorites.includes(toolId)
      return {
        ...current,
        tools: {
          ...current.tools,
          favorites: selected
            ? current.tools.favorites.filter((item) => item !== toolId)
            : [toolId, ...current.tools.favorites].slice(0, 50)
        }
      }
    }).catch((cause: unknown) => setNotice(errorMessage(cause)))
  }

  function cycleLanguage(): void {
    const languages: AppLanguage[] = ['zh-CN', 'en-US', 'ja-JP']
    const next = languages[(languages.indexOf(settings.general.language) + 1) % languages.length]
    void save((current) => ({
      ...current,
      general: { ...current.general, language: next }
    })).catch((cause: unknown) => setNotice(errorMessage(cause)))
  }

  return (
    <main className={`app-shell ${sidebarCompact ? 'app-shell--compact' : ''}`}>
      <div className="window-drag-region" data-tauri-drag-region />
      <aside className="sidebar">
        <div className="sidebar-toolbar">
          <button
            className="icon-button"
            type="button"
            aria-label={sidebarCompact ? t('shell.expand') : t('shell.collapse')}
            onClick={toggleSidebar}
          >
            <PanelLeftClose />
          </button>
          <label className="search-control">
            <Search />
            <input
              id="tool-search"
              value={query}
              placeholder={t('shell.search')}
              onChange={(event) => setQuery(event.target.value)}
            />
            <kbd><Command />K</kbd>
          </label>
        </div>

        <nav className="tool-nav" aria-label={t('shell.navigation')}>
          <NavButton
            icon={homeTool.icon}
            label="MooTool"
            active={activeTool === 'home'}
            compact={sidebarCompact}
            onClick={() => openTool('home')}
          />
          {favorites.length > 0 && (
            <section className="nav-group nav-group--favorites">
              <h2><Star />{t('shell.favorites')}</h2>
              {favorites.map((tool) => (
                <NavButton
                  key={`favorite-${tool.id}`}
                  icon={tool.icon}
                  label={toolTitle(tool)}
                  active={activeTool === tool.id}
                  compact={sidebarCompact}
                  favorite
                  onToggleFavorite={() => toggleFavorite(tool.id)}
                  onClick={() => openTool(tool.id)}
                />
              ))}
            </section>
          )}
          {visibleCustomGroups.map((group) => (
            <section className="nav-group nav-group--custom" key={group.id}>
              <h2><FolderCog />{group.name}</h2>
              {group.tools.map((tool) => (
                <NavButton
                  key={`${group.id}-${tool.id}`}
                  icon={tool.icon}
                  label={toolTitle(tool)}
                  active={activeTool === tool.id}
                  compact={sidebarCompact}
                  favorite={settings.tools.favorites.includes(tool.id)}
                  onToggleFavorite={() => toggleFavorite(tool.id)}
                  onClick={() => openTool(tool.id)}
                />
              ))}
            </section>
          ))}
          {visibleGroups.map(({ group, tools }) => (
            <section className="nav-group" key={group}>
              <h2>{groupTitle(group)}</h2>
              {tools.map((tool) => (
                <NavButton
                  key={tool.id}
                  icon={tool.icon}
                  label={toolTitle(tool)}
                  active={activeTool === tool.id}
                  compact={sidebarCompact}
                  planned={!tool.ready}
                  favorite={settings.tools.favorites.includes(tool.id)}
                  onToggleFavorite={() => toggleFavorite(tool.id)}
                  onClick={() => openTool(tool.id)}
                />
              ))}
            </section>
          ))}

          {!query && recent.length > 0 && (
            <section className="recent-group">
              <h2>{t('shell.recent')} <ChevronDown /></h2>
              {recent.map((toolId) => {
                const tool = toolCatalog.find((item) => item.id === toolId)
                return tool && (
                  <button type="button" key={toolId} onClick={() => openTool(toolId)}>
                    {toolTitle(tool)}
                  </button>
                )
              })}
            </section>
          )}
        </nav>

        <footer className="sidebar-footer">
          <div className="brand-lockup">
            <img className="brand-symbol" src={brandLogo} alt="" aria-hidden="true" draggable={false} />
            <span>MooTool <small>Tauri</small></span>
          </div>
          <div className="footer-actions">
            <button className="icon-button" type="button" aria-label={t('shell.groups')} onClick={() => setCustomGroupsOpen(true)}>
              <FolderCog />
            </button>
            <button className="icon-button" type="button" aria-label={t('shell.language')} onClick={cycleLanguage}>
              <Languages />
            </button>
            <button
              className="icon-button"
              type="button"
              aria-label={t('shell.settings')}
              onClick={() => void openWindow().catch((cause: unknown) => setNotice(errorMessage(cause)))}
            >
              <Settings />
            </button>
          </div>
        </footer>
      </aside>

      <section className="workspace">
        <div className={activeTool === 'home' ? 'view-layer' : 'view-layer view-layer--hidden'}>
          <HomePage runtimeInfo={runtimeInfo} onOpenJson={() => openTool('json')} />
        </div>
        <div className={activeTool === 'calculator' ? 'view-layer' : 'view-layer view-layer--hidden'}>
          <CalculatorHost active={activeTool === 'calculator'} />
        </div>
        <div className={activeTool === 'color' ? 'view-layer' : 'view-layer view-layer--hidden'}>
          <ColorHost active={activeTool === 'color'} />
        </div>
        <div className={activeTool === 'json' ? 'view-layer' : 'view-layer view-layer--hidden'}>
          <JsonHost active={activeTool === 'json'} />
        </div>
        <div className={activeTool === 'quick-note' ? 'view-layer' : 'view-layer view-layer--hidden'}>
          <QuickNoteHost active={activeTool === 'quick-note'} />
        </div>
        <div className={activeTool === 'protobuf' ? 'view-layer' : 'view-layer view-layer--hidden'}>
          <ProtobufHost active={activeTool === 'protobuf'} />
        </div>
        <div className={activeTool === 'runtime' ? 'view-layer' : 'view-layer view-layer--hidden'}><RuntimeHost active={activeTool === 'runtime'} /></div>
        <div className={activeTool === 'http' ? 'view-layer' : 'view-layer view-layer--hidden'}><HttpHost active={activeTool === 'http'} /></div>
        <div className={activeTool === 'host' ? 'view-layer' : 'view-layer view-layer--hidden'}><HostHost active={activeTool === 'host'} /></div>
        <div className={activeTool === 'text-diff' ? 'view-layer' : 'view-layer view-layer--hidden'}>
          <TextDiffHost active={activeTool === 'text-diff'} />
        </div>
        <div className={activeTool === 'reformat' ? 'view-layer' : 'view-layer view-layer--hidden'}>
          <ReformatHost active={activeTool === 'reformat'} />
        </div>
        <div className={activeTool === 'encode' ? 'view-layer' : 'view-layer view-layer--hidden'}>
          <EncodeHost active={activeTool === 'encode'} />
        </div>
        <div className={activeTool === 'crypto' ? 'view-layer' : 'view-layer view-layer--hidden'}>
          <CryptoHost active={activeTool === 'crypto'} />
        </div>
        <div className={activeTool === 'qrcode' ? 'view-layer' : 'view-layer view-layer--hidden'}>
          <QrcodeHost active={activeTool === 'qrcode'} />
        </div>
        <div className={activeTool === 'regex' ? 'view-layer' : 'view-layer view-layer--hidden'}>
          <RegexHost active={activeTool === 'regex'} />
        </div>
        <div className={activeTool === 'config' ? 'view-layer' : 'view-layer view-layer--hidden'}>
          <ConfigHost active={activeTool === 'config'} />
        </div>
        <div className={activeTool === 'cron' ? 'view-layer' : 'view-layer view-layer--hidden'}>
          <CronHost active={activeTool === 'cron'} />
        </div>
        <div className={activeTool === 'timestamp' ? 'view-layer' : 'view-layer view-layer--hidden'}>
          <TimestampHost active={activeTool === 'timestamp'} />
        </div>
        <div className={activeTool === 'message-board' ? 'view-layer' : 'view-layer view-layer--hidden'}>
          <MessageBoardHost active={activeTool === 'message-board'} />
        </div>
        <div className={activeTool === 'translation' ? 'view-layer' : 'view-layer view-layer--hidden'}>
          <TranslationHost active={activeTool === 'translation'} />
        </div>
        <div className={activeTool === 'image' ? 'view-layer' : 'view-layer view-layer--hidden'}>
          <ImageHost active={activeTool === 'image'} />
        </div>
        <div className={activeTool === 'pdf' ? 'view-layer' : 'view-layer view-layer--hidden'}>
          <PdfHost active={activeTool === 'pdf'} />
        </div>
        <div className={activeTool === 'network' ? 'view-layer' : 'view-layer view-layer--hidden'}>
          <NetworkHost active={activeTool === 'network'} />
        </div>
        <div className={activeTool === 'ua' ? 'view-layer' : 'view-layer view-layer--hidden'}>
          <UaHost active={activeTool === 'ua'} />
        </div>
        <div className={activeTool === 'variables' ? 'view-layer' : 'view-layer view-layer--hidden'}>
          <VariablesHost active={activeTool === 'variables'} />
        </div>
        <div className={activeTool === 'system' ? 'view-layer' : 'view-layer view-layer--hidden'}>
          <SystemHost active={activeTool === 'system'} />
        </div>
        <div className={activeTool === 'editor-lab' ? 'view-layer' : 'view-layer view-layer--hidden'}>
          <EditorLabHost active={activeTool === 'editor-lab'} />
        </div>
        <div className={activeTool === 'webview-lab' ? 'view-layer' : 'view-layer view-layer--hidden'}>
          <WebviewLab active={activeTool === 'webview-lab'} />
        </div>
        {(notice || settingsError) && (
          <button className="notice-toast" type="button" onClick={() => setNotice('')}>
            {notice || settingsError}
          </button>
        )}
        <CustomGroupDialog open={customGroupsOpen} onClose={() => setCustomGroupsOpen(false)} />
        {closeRequest && (
          <div className="desktop-dialog-backdrop" role="presentation">
            <section className="desktop-dialog" role="dialog" aria-modal="true" aria-labelledby="close-dialog-title">
              <span className="eyebrow">MOOTOOL NEXT TAURI</span>
              <h2 id="close-dialog-title">{t('closeDialog.title')}</h2>
              <p>{t('closeDialog.body')}</p>
              <label className="desktop-dialog-remember">
                <input
                  type="checkbox"
                  checked={rememberCloseChoice}
                  onChange={(event) => setRememberCloseChoice(event.target.checked)}
                />
                {t('closeDialog.remember')}
              </label>
              <footer>
                <button className="secondary-button" type="button" onClick={() => void resolveClose('cancel')}>
                  {t('closeDialog.cancel')}
                </button>
                {closeRequest.canMinimizeToTray && (
                  <button className="secondary-button" type="button" onClick={() => void resolveClose('minimizeToTray')}>
                    {t('closeDialog.tray')}
                  </button>
                )}
                <button className="primary-button" type="button" onClick={() => void resolveClose('quit')}>
                  {t('closeDialog.quit')}
                </button>
              </footer>
            </section>
          </div>
        )}
      </section>
    </main>
  )
}

function isToolId(value: string): value is ToolId {
  return value === 'home' || toolCatalog.some((tool) => tool.id === value)
}


function NavButton({
  icon: Icon,
  label,
  active,
  compact,
  planned = false,
  favorite = false,
  onToggleFavorite,
  onClick
}: {
  icon: typeof Boxes
  label: string
  active: boolean
  compact: boolean
  planned?: boolean
  favorite?: boolean
  onToggleFavorite?(): void
  onClick(): void
}) {
  const { t } = useI18n()
  return (
    <div className={onToggleFavorite && !compact ? 'nav-button-row nav-button-row--favoritable' : 'nav-button-row'}>
      <button
        className={`nav-button ${active ? 'nav-button--active' : ''}`}
        type="button"
        title={compact ? label : undefined}
        onClick={onClick}
      >
        <Icon />
        <span>{label}</span>
        {planned && <i aria-label={t('shell.planned')} />}
      </button>
      {onToggleFavorite && !compact && (
        <button
          className={favorite ? 'nav-favorite nav-favorite--active' : 'nav-favorite'}
          type="button"
          aria-label={t(favorite ? 'shell.unfavoriteTool' : 'shell.favoriteTool', { tool: label })}
          title={t(favorite ? 'shell.unfavorite' : 'shell.favorite')}
          onClick={onToggleFavorite}
        >
          <Star />
        </button>
      )}
    </div>
  )
}
