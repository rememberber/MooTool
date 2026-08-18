import {
  Boxes,
  CheckCircle2,
  Cpu,
  Database,
  ExternalLink,
  GitBranch,
  GitFork,
  HeartHandshake,
  PackageCheck,
  PackageOpen,
  Users
} from 'lucide-react'
import { useI18n } from '../../app/i18n'
import type { RuntimeInfo } from '../../platform/contracts/runtime'
import sponsorQrImage from '../../../../assets/material/wx-zanshang.jpg'

interface HomePageProps {
  runtimeInfo?: RuntimeInfo
  onOpenJson(): void
}

const contributors = [
  { name: 'CassianFlorin', url: 'https://github.com/CassianFlorin' },
  { name: 'felixcn', url: 'https://github.com/felixcn' },
  { name: 'felixnan168', url: 'https://github.com/felixnan168' },
  { name: 'Lyp', url: 'https://github.com/Lyp' },
  { name: 'sunsence', url: 'https://github.com/sunsence' },
  { name: 'rememberber', url: 'https://github.com/rememberber' }
]

function openExternalPage(url: string): void {
  if (typeof window !== 'undefined') {
    window.open(url, '_blank', 'noopener,noreferrer')
  }
}

export function HomePage({ runtimeInfo, onOpenJson }: HomePageProps) {
  const { t } = useI18n()

  return (
    <section className="home-page">
      <div className="home-content">
        <header className="home-hero">
          <button
            className="home-logo-button"
            type="button"
            aria-label={t('app.home.website')}
            onClick={() => openExternalPage('https://mootool.luoboduner.com')}
          >
            <div className="brand-symbol brand-symbol--large"><Boxes aria-hidden="true" /></div>
          </button>

          <div className="home-identity">
            <button
              className="home-website"
              type="button"
              onClick={() => openExternalPage('https://mootool.luoboduner.com')}
            >
              mootool.luoboduner.com <ExternalLink size={13} />
            </button>

            <div className="home-product-row">
              <h1>MooTool</h1>
              <span className="home-version">v{runtimeInfo?.version ?? '…'}</span>
            </div>

            <p className="home-tagline">{t('app.home.tagline')}</p>
            <p className="home-author">{t('app.home.author')}</p>
          </div>
        </header>

        <section className="home-section home-about-section">
          <h2>{t('app.home.about.title')}</h2>
          <div className="home-about-copy">
            <p>{t('app.home.about.line1')}</p>
            <p>{t('app.home.about.line2')}</p>
            <p>{t('app.home.about.line2Note')}</p>
            <p>{t('app.home.about.line3')}</p>
            <p>{t('app.home.about.line4')}</p>
            <p>{t('app.home.about.line5')}</p>
          </div>
        </section>

        <section className="home-section home-contributor-section">
          <h2><Users size={15} />{t('app.home.contributors.title')}</h2>
          <div className="home-contributors">
            {contributors.map((contributor) => (
              <button type="button" key={contributor.name} onClick={() => openExternalPage(contributor.url)}>
                <span className="home-contributor-initial">{contributor.name.slice(0, 1).toUpperCase()}</span>
                {contributor.name}
              </button>
            ))}
          </div>
          <p className="home-contributor-thanks"><HeartHandshake size={15} />{t('app.home.contributors.thanks')}</p>
        </section>

        <section className="home-section home-sponsor-section">
          <h2>{t('app.home.sponsor.title')}</h2>
          <div className="home-sponsor-copy">
            <p>{t('app.home.sponsor.prompt')}</p>
            <img src={sponsorQrImage} alt={t('app.home.sponsor.tip')} title={t('app.home.sponsor.tip')} />
            <span>{t('app.home.sponsor.tip')}</span>
          </div>
        </section>

        <section className="home-section">
          <h2>{t('app.home.source.title')}</h2>
          <div className="home-link-list">
            <button type="button" onClick={() => openExternalPage('https://github.com/rememberber/MooTool')}>
              <GitFork size={15} />GitHub<ExternalLink size={12} />
            </button>
            <button type="button" onClick={() => openExternalPage('https://gitee.com/rememberber/MooTool')}>
              <PackageOpen size={15} />Gitee<ExternalLink size={12} />
            </button>
          </div>
        </section>

        <section className="home-section">
          <h2>{t('app.home.help.title')}</h2>
          <button className="home-inline-link" type="button" onClick={() => openExternalPage('https://github.com/rememberber/MooTool/issues')}>
            {t('app.home.help.issue')} <ExternalLink size={12} />
          </button>
        </section>

        <section className="home-section">
          <h2>{t('app.home.otherWorks.title')}</h2>
          <div className="home-work-list">
            <button type="button" onClick={() => openExternalPage('https://github.com/rememberber/WePush')}>
              <span><strong>WePush</strong><small>{t('app.home.wePush.desc')}</small></span>
              <ExternalLink size={14} />
            </button>
            <button type="button" onClick={() => openExternalPage('https://github.com/rememberber/MooInfo')}>
              <span><strong>MooInfo</strong><small>{t('app.home.mooInfo.desc')}</small></span>
              <ExternalLink size={14} />
            </button>
          </div>
        </section>

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
      </div>
    </section>
  )
}
