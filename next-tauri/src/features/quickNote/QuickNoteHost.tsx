import { lazy, Suspense } from 'react'
import { useLocalizedMessages } from '../../app/localizedMessages'
import { ManagedToolHost } from '../toolWebview/ManagedToolHost'
import { quickNoteMessages } from './quickNoteMessages'

const QuickNoteSurface = lazy(async () => {
  const module = await import('./QuickNoteSurface')
  return { default: module.QuickNoteSurface }
})

export function QuickNoteHost({ active }: { active: boolean }) {
  const { t } = useLocalizedMessages(quickNoteMessages)
  return (
    <ManagedToolHost active={active} toolId="quick-note" title={t('title')}>
      <Suspense fallback={<div className="tool-webview-placeholder">{t('host.loading')}</div>}>
        <QuickNoteSurface />
      </Suspense>
    </ManagedToolHost>
  )
}
