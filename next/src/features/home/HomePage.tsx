import { Coffee, ExternalLink, Github, HeartHandshake, PackageOpen, Users } from 'lucide-react'
import { useEffect, useState } from 'react'
import { BrandIcon } from '@/shared/components/BrandIcon'
import type { ExternalPageId } from '@/shared/contracts/app'
import { useI18n } from '@/shared/i18n/I18nProvider'

const contributors: Array<{ name: string; pageId: ExternalPageId }> = [
  { name: 'CassianFlorin', pageId: 'contributorCassianFlorin' },
  { name: 'felixcn', pageId: 'contributorFelixcn' },
  { name: 'felixnan168', pageId: 'contributorFelixnan168' },
  { name: 'Lyp', pageId: 'contributorLyp' },
  { name: 'sunsence', pageId: 'contributorSunsence' },
  { name: 'rememberber', pageId: 'contributorRememberber' }
]

const thanks: Array<{ name: string; pageId: ExternalPageId }> = [
  { name: 'Darcula', pageId: 'darcula' },
  { name: 'Hutool', pageId: 'hutool' },
  { name: 'vscode-icons', pageId: 'vscodeIcons' }
]

function openPage(pageId: ExternalPageId): void {
  void window.mootool.openExternalPage(pageId)
}

export function HomePage() {
  const { t } = useI18n()
  const [version, setVersion] = useState('')

  useEffect(() => { void window.mootool.getAppVersion().then(setVersion) }, [])

  return (
    <section className="home-page">
      <div className="home-content">
        <header className="home-hero">
          <button className="home-logo-button" type="button" aria-label={t('app.home.website')} onClick={() => openPage('home')}>
            <BrandIcon size={104} />
          </button>
          <div className="home-identity">
            <button className="home-website" type="button" onClick={() => openPage('home')}>
              mootool.luoboduner.com <ExternalLink size={13} />
            </button>
            <div className="home-product-row">
              <h1>MooTool</h1>
              <span className="home-version">{version ? `v${version}` : 'v…'}</span>
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

        <div className="home-columns">
          <section className="home-section">
            <h2>{t('app.home.source.title')}</h2>
            <div className="home-link-list">
              <button type="button" onClick={() => openPage('github')}><Github size={15} />GitHub<ExternalLink size={12} /></button>
              <button type="button" onClick={() => openPage('gitee')}><PackageOpen size={15} />Gitee<ExternalLink size={12} /></button>
            </div>
          </section>

          <section className="home-section">
            <h2>{t('app.home.help.title')}</h2>
            <button className="home-inline-link" type="button" onClick={() => openPage('issues')}>
              {t('app.home.help.issue')} <ExternalLink size={12} />
            </button>
          </section>

          <section className="home-section">
            <h2>{t('app.home.thanks.title')}</h2>
            <div className="home-text-links">
              {thanks.map((item) => <button type="button" key={item.name} onClick={() => openPage(item.pageId)}>{item.name}</button>)}
            </div>
          </section>

          <section className="home-section home-sponsor-section">
            <h2>{t('app.home.sponsor.title')}</h2>
            <div className="home-sponsor-copy">
              <Coffee size={19} />
              <div>
                <p>{t('app.home.sponsor.prompt')}</p>
                <span>{t('app.home.sponsor.tip')}</span>
              </div>
            </div>
          </section>
        </div>

        <section className="home-section">
          <h2>{t('app.home.otherWorks.title')}</h2>
          <div className="home-work-list">
            <button type="button" onClick={() => openPage('wePush')}>
              <span><strong>WePush</strong><small>{t('app.home.wePush.desc')}</small></span>
              <ExternalLink size={14} />
            </button>
            <button type="button" onClick={() => openPage('mooInfo')}>
              <span><strong>MooInfo</strong><small>{t('app.home.mooInfo.desc')}</small></span>
              <ExternalLink size={14} />
            </button>
          </div>
        </section>

        <section className="home-section home-contributor-section">
          <h2><Users size={15} />{t('app.home.contributors.title')}</h2>
          <div className="home-contributors">
            {contributors.map((contributor) => (
              <button type="button" key={contributor.name} onClick={() => openPage(contributor.pageId)}>
                <span className="home-contributor-initial">{contributor.name.slice(0, 1).toUpperCase()}</span>
                {contributor.name}
              </button>
            ))}
          </div>
          <p className="home-contributor-thanks"><HeartHandshake size={15} />{t('app.home.contributors.thanks')}</p>
        </section>
      </div>
    </section>
  )
}
