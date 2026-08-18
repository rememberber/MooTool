import { lazy, Suspense } from 'react'
import { useLocalizedMessages } from '../../app/localizedMessages'
import { ManagedToolHost } from '../toolWebview/ManagedToolHost'
import { httpMessages } from './httpMessages'
const HttpSurface = lazy(async () => ({ default: (await import('./HttpSurface')).HttpSurface }))
export function HttpHost({ active }: { active: boolean }) {
  const { t } = useLocalizedMessages(httpMessages)
  return <ManagedToolHost active={active} toolId="http" title="HTTP"><Suspense fallback={<div className="tool-webview-placeholder">{t('host.loading')}</div>}><HttpSurface /></Suspense></ManagedToolHost>
}
