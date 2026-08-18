import { lazy, Suspense } from 'react'
import { useLocalizedMessages } from '../../app/localizedMessages'
import { ManagedToolHost } from '../toolWebview/ManagedToolHost'
import { regexMessages } from './regexMessages'

const RegexSurface = lazy(async () => {
  const module = await import('./RegexSurface')
  return { default: module.RegexSurface }
})

export function RegexHost({ active }: { active: boolean }) {
  const { t } = useLocalizedMessages(regexMessages)
  return (
    <ManagedToolHost active={active} toolId="regex" title="Regex">
      <Suspense fallback={<div className="tool-webview-placeholder">{t('host.loading')}</div>}>
        <RegexSurface />
      </Suspense>
    </ManagedToolHost>
  )
}
