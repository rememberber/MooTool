import { lazy, Suspense } from 'react'
import { useLocalizedMessages } from '../../app/localizedMessages'
import { ManagedToolHost } from '../toolWebview/ManagedToolHost'
import { translationMessages } from './translationMessages'

const TranslationSurface = lazy(async () => ({
  default: (await import('./TranslationSurface')).TranslationSurface
}))

export function TranslationHost({ active }: { active: boolean }) {
  const { t } = useLocalizedMessages(translationMessages)
  return (
    <ManagedToolHost active={active} toolId="translation" title={t('title')}>
      <Suspense fallback={<div className="tool-webview-placeholder">{t('host.loading')}</div>}>
        <TranslationSurface />
      </Suspense>
    </ManagedToolHost>
  )
}
