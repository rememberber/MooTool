import { lazy, Suspense } from 'react'
import { useLocalizedMessages } from '../../app/localizedMessages'
import { ManagedToolHost } from '../toolWebview/ManagedToolHost'
import { jsonMessages } from './jsonMessages'

const JsonToolSurface = lazy(async () => {
  const module = await import('./JsonToolSurface')
  return { default: module.JsonToolSurface }
})

export function JsonHost({ active }: { active: boolean }) {
  const { t } = useLocalizedMessages(jsonMessages)
  const surface = (
    <Suspense fallback={<div className="tool-webview-placeholder">{t('host.loading')}</div>}>
      <JsonToolSurface />
    </Suspense>
  )
  return (
    <ManagedToolHost active={active} toolId="json" title={t('host.title')}>
      {surface}
    </ManagedToolHost>
  )
}
