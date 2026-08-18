import { lazy, Suspense } from 'react'
import { useLocalizedMessages } from '../../app/localizedMessages'
import { ManagedToolHost } from '../toolWebview/ManagedToolHost'
import { cryptoMessages } from './cryptoMessages'

const CryptoSurface = lazy(async () => {
  const module = await import('./CryptoSurface')
  return { default: module.CryptoSurface }
})

export function CryptoHost({ active }: { active: boolean }) {
  const { t } = useLocalizedMessages(cryptoMessages)
  return (
    <ManagedToolHost active={active} toolId="crypto" title={t('title')}>
      <Suspense fallback={<div className="tool-webview-placeholder">{t('host.loading')}</div>}>
        <CryptoSurface />
      </Suspense>
    </ManagedToolHost>
  )
}
