import {
  Braces,
  CalendarClock,
  ChevronDown,
  Clock3,
  Code2,
  Globe,
  Home,
  Image,
  Palette,
  QrCode,
  Regex,
  Search,
  Settings,
  Shuffle,
  Sparkle
} from 'lucide-react'
import { useState } from 'react'
import { JsonTool } from '@/features/json/JsonTool'
import { ToolButton } from '@/shared/components/ToolButton'
import { useI18n } from '@/shared/i18n/I18nProvider'
import type { MessageKey } from '@/shared/i18n/messages'

const primaryTools = [
  { id: 'home', icon: Home, labelKey: 'app.nav.home' },
  { id: 'json', icon: Braces, labelKey: 'app.nav.json' },
  { id: 'time', icon: Clock3, labelKey: 'app.nav.time' },
  { id: 'encode', icon: Shuffle, labelKey: 'app.nav.encode' },
  { id: 'qrcode', icon: QrCode, labelKey: 'app.nav.qrcode' },
  { id: 'http', icon: Globe, labelKey: 'app.nav.http' },
  { id: 'diff', icon: Code2, labelKey: 'app.nav.diff' },
  { id: 'regex', icon: Regex, labelKey: 'app.nav.regex' },
  { id: 'color', icon: Palette, labelKey: 'app.nav.color' },
  { id: 'image', icon: Image, labelKey: 'app.nav.image' },
  { id: 'cron', icon: CalendarClock, labelKey: 'app.nav.cron' }
] satisfies Array<{
  id: string
  icon: typeof Home
  labelKey: MessageKey
}>

const recentItems = [
  'JSON format snippet',
  'HTTP draft',
  'Base64 conversion',
  'Saved color',
  'Regex test'
]

export function Workbench() {
  const [activeTool, setActiveTool] = useState('home')
  const { language, languageLabels, languages, setLanguage, t } = useI18n()
  const activeToolLabel = t(primaryTools.find((tool) => tool.id === activeTool)?.labelKey ?? 'app.nav.home')

  return (
    <main className="app-shell">
      <div className="window-drag window-drag-region" aria-hidden="true" />

      <aside className="sidebar">
        <div className="window-drag toolbar-spacer" />

        <div className="sidebar-actions">
          <button className="icon-ghost" aria-label={t('app.nav.search')}>
            <Search size={17} />
          </button>
        </div>

        <nav className="tool-nav" aria-label={t('app.nav.tools')}>
          {primaryTools.map((tool) => (
            <ToolButton
              key={tool.id}
              icon={tool.icon}
              label={t(tool.labelKey)}
              active={activeTool === tool.id}
              onClick={() => setActiveTool(tool.id)}
            />
          ))}
        </nav>

        <section className="recent-section">
          <div className="section-title">
            <span>{t('app.nav.recent')}</span>
            <ChevronDown size={15} />
          </div>
          <div className="recent-list">
            {recentItems.map((item, index) => (
              <button className="recent-item" key={item}>
                <span className={index === 1 ? 'recent-dot recent-dot--blue' : 'recent-dot'} />
                <span>{item}</span>
              </button>
            ))}
          </div>
        </section>

        <div className="sidebar-footer">
          <div className="brand-mark">
            <Sparkle size={18} />
            <span>MooTool</span>
          </div>
          <div className="footer-controls">
            <div className="language-switch" aria-label="Language">
              {languages.map((item) => (
                <button
                  className={item === language ? 'language-switch__item language-switch__item--active' : 'language-switch__item'}
                  key={item}
                  onClick={() => setLanguage(item)}
                >
                  {languageLabels[item]}
                </button>
              ))}
            </div>
            <button className="icon-ghost" aria-label={t('app.nav.settings')}>
              <Settings size={17} />
            </button>
          </div>
        </div>
      </aside>

      <section className="workspace">
        {activeTool === 'home' ? (
          <>
            <div className="hero-pattern" aria-hidden="true" />

            <div className="home-panel">
              <div className="headline-row">
                <Sparkle className="headline-icon" size={28} />
                <h1>{t('app.home.title')}</h1>
              </div>
              <p className="subtle-link">{t('app.home.subtitle')}</p>

              <div className="command-box">
                <div className="command-input">{t('app.home.prompt')}</div>
                <div className="command-controls">
                  <button className="round-button">+</button>
                  <button className="pill-button" onClick={() => setActiveTool('json')}>
                    JSON
                    <ChevronDown size={16} />
                  </button>
                  <button className="send-button" onClick={() => setActiveTool('json')}>
                    ↑
                  </button>
                </div>
              </div>

              <div className="quick-grid">
                <button className="quick-card" onClick={() => setActiveTool('json')}>
                  <Braces size={30} />
                  <strong>JSON</strong>
                  <span>{t('app.home.json.desc')}</span>
                </button>
                <button className="quick-card">
                  <Globe size={30} />
                  <strong>HTTP</strong>
                  <span>{t('app.home.http.desc')}</span>
                </button>
                <button className="quick-card">
                  <Code2 size={30} />
                  <strong>Diff</strong>
                  <span>{t('app.home.diff.desc')}</span>
                </button>
              </div>
            </div>
          </>
        ) : activeTool === 'json' ? (
          <JsonTool />
        ) : (
          <section className="placeholder-page">
            <h1>{activeToolLabel}</h1>
            <p>{t('app.placeholder')}</p>
          </section>
        )}
      </section>

    </main>
  )
}
