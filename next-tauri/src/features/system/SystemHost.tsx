import { lazy, Suspense } from 'react'
import { useLocalizedMessages } from '../../app/localizedMessages'
import { ManagedToolHost } from '../toolWebview/ManagedToolHost'
import { systemMessages } from './systemMessages'

const SystemSurface = lazy(async () => {
  const module = await import('./SystemSurface')
  return { default: module.SystemSurface }
})

export function SystemHost({ active }: { active: boolean }) {
  const { t } = useLocalizedMessages(systemMessages)
  return (
    <ManagedToolHost active={active} toolId="system" title={t('title')}>
      <Suspense fallback={<div className="tool-webview-placeholder">{t('host.loading')}</div>}>
        <SystemSurface />
      </Suspense>
    </ManagedToolHost>
  )
}
