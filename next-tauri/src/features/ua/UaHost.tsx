import { lazy, Suspense } from 'react'
import { useLocalizedMessages } from '../../app/localizedMessages'
import { ManagedToolHost } from '../toolWebview/ManagedToolHost'
import { uaMessages } from './uaMessages'

const UaSurface = lazy(async () => {
  const module = await import('./UaSurface')
  return { default: module.UaSurface }
})

export function UaHost({ active }: { active: boolean }) {
  const { t } = useLocalizedMessages(uaMessages)
  return (
    <ManagedToolHost active={active} toolId="ua" title={t('title')}>
      <Suspense fallback={<div className="tool-webview-placeholder">{t('host.loading')}</div>}>
        <UaSurface />
      </Suspense>
    </ManagedToolHost>
  )
}
