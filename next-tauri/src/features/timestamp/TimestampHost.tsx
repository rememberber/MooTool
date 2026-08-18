import { lazy, Suspense } from 'react'
import { useLocalizedMessages } from '../../app/localizedMessages'
import { ManagedToolHost } from '../toolWebview/ManagedToolHost'
import { timestampMessages } from './timestampMessages'

const TimestampSurface = lazy(async () => {
  const module = await import('./TimestampSurface')
  return { default: module.TimestampSurface }
})

export function TimestampHost({ active }: { active: boolean }) {
  const { t } = useLocalizedMessages(timestampMessages)
  return (
    <ManagedToolHost active={active} toolId="timestamp" title={t('title')}>
      <Suspense fallback={<div className="tool-webview-placeholder">{t('host.loading')}</div>}>
        <TimestampSurface />
      </Suspense>
    </ManagedToolHost>
  )
}
