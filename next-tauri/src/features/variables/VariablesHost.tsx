import { lazy, Suspense } from 'react'
import { useLocalizedMessages } from '../../app/localizedMessages'
import { ManagedToolHost } from '../toolWebview/ManagedToolHost'
import { variablesMessages } from './variablesMessages'

const VariablesSurface = lazy(async () => {
  const module = await import('./VariablesSurface')
  return { default: module.VariablesSurface }
})

export function VariablesHost({ active }: { active: boolean }) {
  const { t } = useLocalizedMessages(variablesMessages)
  return (
    <ManagedToolHost active={active} toolId="variables" title={t('title')}>
      <Suspense fallback={<div className="tool-webview-placeholder">{t('host.loading')}</div>}>
        <VariablesSurface />
      </Suspense>
    </ManagedToolHost>
  )
}
