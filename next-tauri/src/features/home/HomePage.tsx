import { Boxes, CheckCircle2, Cpu, Database, GitBranch, PackageCheck } from 'lucide-react'
import { useI18n } from '../../app/i18n'
import type { RuntimeInfo } from '../../platform/contracts/runtime'

interface HomePageProps {
  runtimeInfo?: RuntimeInfo
  onOpenJson(): void
}

export function HomePage({ runtimeInfo, onOpenJson }: HomePageProps) {
  const { t } = useI18n()
  return (
    <section className="home-page">
      <div className="home-hero">
        <div className="brand-symbol brand-symbol--large"><Boxes aria-hidden="true" /></div>
        <div>
          <span className="eyebrow">{t('home.eyebrow')}</span>
          <div className="home-title-row">
            <h1>MooTool Next Tauri</h1>
            <span className="version-pill">v{runtimeInfo?.version ?? '…'}</span>
          </div>
          <p>{t('home.tagline')}</p>
          <button className="primary-button" type="button" onClick={onOpenJson}>
            {t('home.openJson')}
          </button>
        </div>
      </div>

      <div className="home-grid">
        <article className="home-card home-card--accent">
          <span className="card-icon"><CheckCircle2 /></span>
          <div>
            <h2>{t('home.developmentTitle')}</h2>
            <p>{t('home.developmentBody')}</p>
          </div>
        </article>
        <article className="home-card">
          <span className="card-icon"><GitBranch /></span>
          <div>
            <h2>{t('home.independentTitle')}</h2>
            <p>{t('home.independentBody')}</p>
          </div>
        </article>
        <article className="home-card">
          <span className="card-icon"><Database /></span>
          <div>
            <h2>{t('home.dataTitle')}</h2>
            <p>{t('home.dataBody')}</p>
          </div>
        </article>
        <article className="home-card">
          <span className="card-icon"><PackageCheck /></span>
          <div>
            <h2>{t('home.releaseTitle')}</h2>
            <p>{t('home.releaseBody')}</p>
          </div>
        </article>
      </div>

      <section className="runtime-panel">
        <div className="runtime-title">
          <Cpu />
          <div>
            <h2>{t('home.runtimeTitle')}</h2>
            <p>{t('home.runtimeBody')}</p>
          </div>
        </div>
        <dl>
          <div><dt>{t('home.productId')}</dt><dd>{runtimeInfo?.productId ?? t('home.loading')}</dd></div>
          <div><dt>{t('home.runtime')}</dt><dd>{runtimeInfo?.runtime ?? t('home.loading')}</dd></div>
          <div><dt>{t('home.platform')}</dt><dd>{runtimeInfo ? `${runtimeInfo.platform} · ${runtimeInfo.architecture}` : t('home.loading')}</dd></div>
        </dl>
      </section>
    </section>
  )
}
