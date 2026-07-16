import { ChevronDown, Languages, PanelLeftClose, PanelLeftOpen, Search, Settings } from 'lucide-react'
import { Suspense, useEffect } from 'react'
import { useAppStore } from '@/app/appStore'
import { toolById, toolGroups } from '@/app/toolRegistry'
import { CommandPalette } from '@/features/search/CommandPalette'
import { BrandIcon } from '@/shared/components/BrandIcon'
import { ToolButton } from '@/shared/components/ToolButton'
import { Tooltip } from '@/shared/components/Tooltip'
import { useI18n } from '@/shared/i18n/I18nProvider'
import { useSettings } from '@/features/settings/SettingsProvider'

export function Workbench() {
  const { language, languageLabels, languages, setLanguage, t } = useI18n()
  const { settings, updateSettings } = useSettings()
  const activeToolId = useAppStore((state) => state.activeToolId)
  const recentToolIds = useAppStore((state) => state.recentToolIds)
  const hydrate = useAppStore((state) => state.hydrate)
  const openTool = useAppStore((state) => state.openTool)
  const setSearchOpen = useAppStore((state) => state.setSearchOpen)
  const activeTool = toolById.get(activeToolId) ?? toolById.get('mootool')!
  const ActiveComponent = activeTool.component

  useEffect(() => {
    void hydrate()
    return window.mootool.onNavigate((event) => {
      if (event === 'focus-search') {
        setSearchOpen(true)
      }
    })
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

  const shellClassName = [
    'app-shell',
    settings.layout.compactNavigation ? 'app-shell--compact-nav' : '',
    settings.layout.showSeparators ? 'app-shell--nav-separators' : '',
    settings.layout.hideNavigationTitles ? 'app-shell--hide-nav-titles' : '',
    settings.appearance.unifiedBackground ? 'app-shell--unified-background' : '',
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
        </div>

        <div className="sidebar-scroll">
          <nav className="tool-nav" aria-label={t('app.nav.tools')}>
            <ToolButton
              icon={toolById.get('mootool')!.icon}
              label={t('app.nav.home')}
              active={activeToolId === 'mootool'}
              onClick={() => openTool('mootool')}
            />

            {toolGroups.map((group) => (
              <section className="tool-group" key={group.id}>
                <h2>{t(group.titleKey)}</h2>
                {group.toolIds.map((toolId) => {
                  const tool = toolById.get(toolId)!
                  return (
                    <ToolButton
                      key={tool.id}
                      icon={tool.icon}
                      label={t(tool.titleKey)}
                      active={activeToolId === tool.id}
                      onClick={() => openTool(tool.id)}
                    />
                  )
                })}
              </section>
            ))}
          </nav>

          {settings.layout.showRecent && <section className="recent-section">
            <div className="section-title">
              <span>{t('app.nav.recent')}</span>
              <ChevronDown size={14} />
            </div>
            <div className="recent-list">
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
            </div>
          </section>}
        </div>

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

      <section className="workspace">
        <Suspense fallback={<div className="workspace-loading">{t('common.loading')}</div>}>
          {ActiveComponent ? <ActiveComponent /> : (
            <section className="placeholder-page">
              <activeTool.icon size={28} />
              <h1>{t(activeTool.titleKey)}</h1>
              <p>{t('app.placeholder')}</p>
            </section>
          )}
        </Suspense>
      </section>

      <CommandPalette />
    </main>
  )
}
