import { lazy, Suspense } from 'react'
import { useLocalizedMessages } from '../../app/localizedMessages'
import { ManagedToolHost } from '../toolWebview/ManagedToolHost'
import { networkMessages } from './networkMessages'

const NetworkSurface = lazy(async () => {
  const module = await import('./NetworkSurface')
  return { default: module.NetworkSurface }
})

export function NetworkHost({ active }: { active: boolean }) {
  const { t } = useLocalizedMessages(networkMessages)
  return (
    <ManagedToolHost active={active} toolId="network" title={t('title')}>
      <Suspense fallback={<div className="tool-webview-placeholder">{t('host.loading')}</div>}>
        <NetworkSurface />
      </Suspense>
    </ManagedToolHost>
  )
}
