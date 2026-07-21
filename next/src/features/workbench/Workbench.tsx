import { ChevronDown, FolderCog, Languages, LocateFixed, PanelLeftClose, PanelLeftOpen, PanelTopClose, Search, Settings } from 'lucide-react'
import { Suspense, useEffect, useLayoutEffect, useRef, useState } from 'react'
import { useAppStore } from '@/app/appStore'
import { toolById, toolGroups, type ToolId } from '@/app/toolRegistry'
import { CommandPalette } from '@/features/search/CommandPalette'
import { BrandIcon } from '@/shared/components/BrandIcon'
import { ToolButton } from '@/shared/components/ToolButton'
import { Tooltip } from '@/shared/components/Tooltip'
import { ToolActivityProvider } from '@/shared/components/ToolActivity'
import type { ToolWindowSnapshot } from '@/shared/contracts/app'
import { useI18n } from '@/shared/i18n/I18nProvider'
import { LegacyMigrationHintDialog } from '@/features/settings/LegacyMigrationHintDialog'
import { useSettings } from '@/features/settings/SettingsProvider'
import { CustomGroupManager } from './CustomGroupManager'
import { UpdateReadyAction } from './UpdateReadyAction'

export function Workbench() {
  const { language, languageLabels, languages, setLanguage, t } = useI18n()
  const { settings, updateSettings } = useSettings()
  const activeToolId = useAppStore((state) => state.activeToolId)
  const recentToolIds = useAppStore((state) => state.recentToolIds)
  const hydrate = useAppStore((state) => state.hydrate)
  const hydrated = useAppStore((state) => state.hydrated)
  const openTool = useAppStore((state) => state.openTool)
  const searchOpen = useAppStore((state) => state.searchOpen)
  const setSearchOpen = useAppStore((state) => state.setSearchOpen)
  const [recentCollapsed, setRecentCollapsed] = useState(false)
  const [groupManagerOpen, setGroupManagerOpen] = useState(false)
  const [toolWindows, setToolWindows] = useState<ToolWindowSnapshot>({ enabled: window.mootool.toolWindowsEnabled, activeToolId: 'mootool', tools: [] })
  const [mountedToolIds, setMountedToolIds] = useState<ToolId[]>([])
  const workspaceRef = useRef<HTMLElement>(null)
  const activeTool = toolById.get(activeToolId) ?? toolById.get('mootool')!
  const activeToolWindow = activeTool.id === 'mootool' ? undefined : toolWindows.tools.find((item) => item.toolId === activeTool.id)
  const renderedToolIds = mountedToolIds.includes(activeTool.id) ? mountedToolIds : [...mountedToolIds, activeTool.id]
  const hiddenNavigationToolIds = new Set(settings.layout.hiddenNavigationToolIds)
  const visibleBuiltinToolCount = toolGroups.reduce((count, group) => count + group.toolIds.filter((toolId) => !hiddenNavigationToolIds.has(toolId)).length, 0)
  const workspaceOverlayOpen = searchOpen || groupManagerOpen
  const workspaceOverlayReady = !toolWindows.enabled || toolWindows.activeToolId === 'mootool'
  const dockedToolViewActive = toolWindows.enabled
    && !workspaceOverlayOpen
    && activeTool.id !== 'mootool'
    && toolWindows.activeToolId === activeTool.id
    && activeToolWindow?.ready === true
    && !activeToolWindow.detached

  useEffect(() => {
    void hydrate()
    const unsubscribeNavigation = window.mootool.onNavigate((event) => {
      if (event === 'focus-search') {
        setSearchOpen(true)
      }
    })
    const unsubscribeToolWindows = window.mootool.onToolWindowSnapshotChange(setToolWindows)
    void window.mootool.getToolWindowSnapshot().then(setToolWindows)
    return () => {
      unsubscribeNavigation()
      unsubscribeToolWindows()
    }
  }, [hydrate, setSearchOpen])

  useEffect(() => {
    const handleKeyDown = (event: KeyboardEvent) => {
      if ((event.metaKey || event.ctrlKey) && event.key.toLocaleLowerCase() === 'k') {
        event.preventDefault()
        setSearchOpen(true)
      }
    }
    window.addEventListener('keydown', handleKeyDown)
    return () => window.removeEventListener('keydown', handleKeyDown)
  }, [setSearchOpen])

  useEffect(() => {
    if (!hydrated || !toolWindows.enabled) return
    void window.mootool.activateToolView(workspaceOverlayOpen ? 'mootool' : activeTool.id).then(setToolWindows)
  }, [activeTool.id, hydrated, toolWindows.enabled, workspaceOverlayOpen])

  useLayoutEffect(() => {
    if (toolWindows.enabled) return
    setMountedToolIds((current) => current.includes(activeTool.id) ? current : [...current, activeTool.id])
  }, [activeTool.id, toolWindows.enabled])

  useLayoutEffect(() => {
    const workspace = workspaceRef.current
    if (!workspace) return
    const updateBounds = () => {
      const bounds = workspace.getBoundingClientRect()
      if (bounds.width <= 0 || bounds.height <= 0) return
      void window.mootool.setToolWorkspaceBounds({
        x: bounds.x,
        y: bounds.y,
        width: bounds.width,
        height: bounds.height
      }).then(setToolWindows)
    }
    updateBounds()
    const observer = new ResizeObserver(updateBounds)
    observer.observe(workspace)
    window.addEventListener('resize', updateBounds)
    return () => {
      observer.disconnect()
      window.removeEventListener('resize', updateBounds)
    }
  }, [])

  const shellClassName = [
    'app-shell',
    settings.layout.compactNavigation ? 'app-shell--compact-nav' : '',
    settings.layout.showSeparators ? 'app-shell--nav-separators' : '',
    settings.layout.hideNavigationTitles ? 'app-shell--hide-nav-titles' : '',
    settings.appearance.unifiedBackground ? 'app-shell--unified-background' : '',
    dockedToolViewActive ? 'app-shell--tool-view-docked' : '',
    `app-shell--nav-${settings.layout.navigationStyle}`
  ].filter(Boolean).join(' ')

  return (
    <main className={shellClassName}>
      <div className="window-drag window-drag-region" aria-hidden="true" />

      <aside className="sidebar">
        <div className="window-drag toolbar-spacer" />

        <div className="sidebar-actions">
          <Tooltip content={settings.layout.hideNavigationTitles ? t('app.nav.expand') : t('app.nav.collapse')} side="bottom">
            <button
              className="icon-ghost"
              type="button"
              aria-label={settings.layout.hideNavigationTitles ? t('app.nav.expand') : t('app.nav.collapse')}
              aria-pressed={settings.layout.hideNavigationTitles}
              onClick={() => { void updateSettings({ layout: { hideNavigationTitles: !settings.layout.hideNavigationTitles } }).catch(() => undefined) }}
            >
              {settings.layout.hideNavigationTitles ? <PanelLeftOpen size={17} /> : <PanelLeftClose size={17} />}
            </button>
          </Tooltip>
          <Tooltip content={`${t('app.nav.search')} · ⌘K`} side="bottom">
            <button className="icon-ghost" type="button" aria-label={t('app.nav.search')} onClick={() => setSearchOpen(true)}>
              <Search size={17} />
            </button>
          </Tooltip>
          <Tooltip content={t('app.nav.manageGroups')} side="bottom">
            <button className="icon-ghost sidebar-group-manage" type="button" aria-label={t('app.nav.manageGroups')} onClick={() => setGroupManagerOpen(true)}>
              <FolderCog size={17} />
            </button>
          </Tooltip>
        </div>

        <div className="sidebar-scroll">
          <nav className="tool-nav" aria-label={t('app.nav.tools')}>
            <ToolButton
              icon={toolById.get('mootool')!.icon}
              label={t('app.nav.home')}
              active={activeToolId === 'mootool'}
              onClick={() => openTool('mootool')}
            />

            {settings.layout.customGroups.map((group) => {
              const tools = group.toolIds.flatMap((toolId) => {
                const tool = toolById.get(toolId)
                return tool ? [tool] : []
              })
              if (tools.length === 0) return null
              return (
                <section className="tool-group tool-group--custom" key={group.id}>
                  <h2>{group.name}</h2>
                  {tools.map((tool) => (
                    <ToolButton
                      key={tool.id}
                      icon={tool.icon}
                      label={t(tool.titleKey)}
                      active={activeToolId === tool.id}
                      detached={toolWindows.tools.some((item) => item.toolId === tool.id && item.detached)}
                      onClick={() => openTool(tool.id)}
                    />
                  ))}
                </section>
              )
            })}

            {settings.layout.customGroups.length > 0 && visibleBuiltinToolCount > 0 && <div className="tool-group-divider">{t('app.group.all')}</div>}

            {toolGroups.map((group) => {
              const visibleToolIds = group.toolIds.filter((toolId) => !hiddenNavigationToolIds.has(toolId))
              if (visibleToolIds.length === 0) return null
              return (
                <section className="tool-group" key={group.id}>
                  <h2>{t(group.titleKey)}</h2>
                  {visibleToolIds.map((toolId) => {
                    const tool = toolById.get(toolId)!
                    return (
                      <ToolButton
                        key={tool.id}
                        icon={tool.icon}
                        label={t(tool.titleKey)}
                        active={activeToolId === tool.id}
                        detached={toolWindows.tools.some((item) => item.toolId === tool.id && item.detached)}
                        onClick={() => openTool(tool.id)}
                      />
                    )
                  })}
                </section>
              )
            })}
          </nav>

          {settings.layout.showRecent && <section className="recent-section">
            <button className="section-title" type="button" aria-expanded={!recentCollapsed} onClick={() => setRecentCollapsed((collapsed) => !collapsed)}>
              <span>{t('app.nav.recent')}</span>
              <ChevronDown size={14} />
            </button>
            {!recentCollapsed && <div className="recent-list">
              {recentToolIds.length === 0 ? (
                <p className="recent-empty">{t('app.recent.empty')}</p>
              ) : recentToolIds.map((toolId, index) => {
                const tool = toolById.get(toolId)
                if (!tool) {
                  return null
                }
                return (
                  <button className="recent-item" type="button" key={tool.id} onClick={() => openTool(tool.id)}>
                    <span className={index === 0 ? 'recent-dot recent-dot--blue' : 'recent-dot'} />
                    <span>{t(tool.titleKey)}</span>
                  </button>
                )
              })}
            </div>}
          </section>}
        </div>

        <UpdateReadyAction />

        <div className="sidebar-footer">
          <div className="brand-mark">
            <BrandIcon size={24} />
            <span>MooTool</span>
          </div>
          <div className="footer-controls">
            <label className="language-menu">
              <Languages size={14} aria-hidden="true" />
              <select aria-label={t('settings.language')} value={language} onChange={(event) => setLanguage(event.target.value as typeof language)}>
                {languages.map((item) => <option value={item} key={item}>{languageLabels[item]}</option>)}
              </select>
            </label>
            <Tooltip content={t('app.nav.settings')} side="top">
              <button className="icon-ghost" type="button" aria-label={t('app.nav.settings')} onClick={() => window.mootool.openSettings()}>
                <Settings size={17} />
              </button>
            </Tooltip>
          </div>
        </div>
      </aside>

      <section className="workspace" ref={workspaceRef}>
        {!toolWindows.enabled ? renderedToolIds.map((toolId) => {
          const definition = toolById.get(toolId)
          const ToolComponent = definition?.component
          const active = toolId === activeTool.id
          if (!definition) return null
          return (
            <ToolActivityProvider active={active} key={toolId}>
              <div className="workspace-tool-session" hidden={!active}>
                <Suspense fallback={active ? <div className="workspace-loading">{t('common.loading')}</div> : null}>
                  {ToolComponent ? <ToolComponent /> : null}
                </Suspense>
              </div>
            </ToolActivityProvider>
          )
        }) : activeTool.id === 'mootool' ? (
          <Suspense fallback={<div className="workspace-loading">{t('common.loading')}</div>}>
            {activeTool.component ? <activeTool.component /> : null}
          </Suspense>
        ) : activeToolWindow?.detached ? (
          <section className="detached-tool-placeholder">
            <span className="detached-tool-placeholder__icon"><activeTool.icon size={32} /><PanelTopClose size={16} /></span>
            <h1>{t('toolWindow.detachedTitle', { tool: t(activeTool.titleKey) })}</h1>
            <p>{t('toolWindow.detachedDescription')}</p>
            <div className="detached-tool-placeholder__actions">
              <button className="toolbar-button toolbar-button--primary" type="button" onClick={() => { void window.mootool.focusToolWindow(activeTool.id as Exclude<typeof activeTool.id, 'mootool'>) }}>
                <LocateFixed size={15} />{t('toolWindow.focus')}
              </button>
              <button className="toolbar-button" type="button" onClick={() => { void window.mootool.dockToolWindow(activeTool.id as Exclude<typeof activeTool.id, 'mootool'>) }}>
                <PanelTopClose size={15} />{t('toolWindow.dock')}
              </button>
            </div>
          </section>
        ) : (
          <div className="workspace-loading">{t('common.loading')}</div>
        )}
      </section>

      <CommandPalette />
      <CustomGroupManager open={groupManagerOpen && workspaceOverlayReady} onClose={() => setGroupManagerOpen(false)} />
      <LegacyMigrationHintDialog />
    </main>
  )
}
