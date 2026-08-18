import { lazy, Suspense } from 'react'
import { useLocalizedMessages } from '../../app/localizedMessages'
import { ManagedToolHost } from '../toolWebview/ManagedToolHost'
import { textDiffMessages } from './textDiffMessages'

const TextDiffSurface = lazy(async () => {
  const module = await import('./TextDiffSurface')
  return { default: module.TextDiffSurface }
})

export function TextDiffHost({ active }: { active: boolean }) {
  const { t } = useLocalizedMessages(textDiffMessages)
  const surface = (
    <Suspense fallback={<div className="tool-webview-placeholder">{t('host.loading')}</div>}>
      <TextDiffSurface />
    </Suspense>
  )
  return (
    <ManagedToolHost active={active} toolId="text-diff" title={t('title')}>
      {surface}
    </ManagedToolHost>
  )
}
