import { lazy, Suspense } from 'react'
import { useLocalizedMessages } from '../../app/localizedMessages'
import { ManagedToolHost } from '../toolWebview/ManagedToolHost'
import { reformatMessages } from './reformatMessages'

const ReformatSurface = lazy(async () => {
  const module = await import('./ReformatSurface')
  return { default: module.ReformatSurface }
})

export function ReformatHost({ active }: { active: boolean }) {
  const { t } = useLocalizedMessages(reformatMessages)
  return (
    <ManagedToolHost active={active} toolId="reformat" title={t('title')}>
      <Suspense fallback={<div className="tool-webview-placeholder">{t('host.loading')}</div>}>
        <ReformatSurface />
      </Suspense>
    </ManagedToolHost>
  )
}
