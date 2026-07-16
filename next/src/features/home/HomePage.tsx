import { Braces, ChevronDown, Code2, Globe } from 'lucide-react'
import { useAppStore } from '@/app/appStore'
import { BrandIcon } from '@/shared/components/BrandIcon'
import { useI18n } from '@/shared/i18n/I18nProvider'

export function HomePage() {
  const { t } = useI18n()
  const openTool = useAppStore((state) => state.openTool)

  return (
    <>
      <div className="hero-pattern" aria-hidden="true" />

      <div className="home-panel">
        <div className="headline-row">
          <BrandIcon className="headline-icon" size={28} />
          <h1>{t('app.home.title')}</h1>
        </div>
        <p className="subtle-link">{t('app.home.subtitle')}</p>

        <div className="command-box">
          <div className="command-input">{t('app.home.prompt')}</div>
          <div className="command-controls">
            <button className="round-button" type="button" aria-label={t('app.nav.search')} onClick={() => useAppStore.getState().setSearchOpen(true)}>+</button>
            <button className="pill-button" type="button" onClick={() => openTool('json')}>
              JSON
              <ChevronDown size={16} />
            </button>
            <button className="send-button" type="button" aria-label={t('app.home.openJson')} onClick={() => openTool('json')}>↑</button>
          </div>
        </div>

        <div className="quick-grid">
          <button className="quick-card" type="button" onClick={() => openTool('json')}>
            <Braces size={30} />
            <strong>JSON</strong>
            <span>{t('app.home.json.desc')}</span>
          </button>
          <button className="quick-card" type="button" onClick={() => openTool('http')}>
            <Globe size={30} />
            <strong>HTTP</strong>
            <span>{t('app.home.http.desc')}</span>
          </button>
          <button className="quick-card" type="button" onClick={() => openTool('textDiff')}>
            <Code2 size={30} />
            <strong>Diff</strong>
            <span>{t('app.home.diff.desc')}</span>
          </button>
        </div>
      </div>
    </>
  )
}
