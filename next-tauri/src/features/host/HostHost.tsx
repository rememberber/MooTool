import { lazy, Suspense } from 'react'
import { useLocalizedMessages } from '../../app/localizedMessages'
import { ManagedToolHost } from '../toolWebview/ManagedToolHost'
import { hostMessages } from './hostMessages'

const HostSurface = lazy(async () => {
  const module = await import('./HostSurface')
  return { default: module.HostSurface }
})

export function HostHost({ active }: { active: boolean }) {
  const { t } = useLocalizedMessages(hostMessages)
  return <ManagedToolHost active={active} toolId="host" title={t('title')}><Suspense fallback={<div className="tool-webview-placeholder">{t('host.loading')}</div>}><HostSurface /></Suspense></ManagedToolHost>
}
