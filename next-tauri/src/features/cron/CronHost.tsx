import { lazy, Suspense } from 'react'
import { useLocalizedMessages } from '../../app/localizedMessages'
import { ManagedToolHost } from '../toolWebview/ManagedToolHost'
import { cronMessages } from './cronMessages'

const CronSurface = lazy(async () => {
  const module = await import('./CronSurface')
  return { default: module.CronSurface }
})

export function CronHost({ active }: { active: boolean }) {
  const { t } = useLocalizedMessages(cronMessages)
  return (
    <ManagedToolHost active={active} toolId="cron" title="Cron">
      <Suspense fallback={<div className="tool-webview-placeholder">{t('host.loading')}</div>}>
        <CronSurface />
      </Suspense>
    </ManagedToolHost>
  )
}
