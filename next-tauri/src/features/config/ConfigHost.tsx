import { lazy, Suspense } from 'react'
import { useLocalizedMessages } from '../../app/localizedMessages'
import { ManagedToolHost } from '../toolWebview/ManagedToolHost'
import { configMessages } from './configMessages'

const ConfigSurface = lazy(async () => {
  const module = await import('./ConfigSurface')
  return { default: module.ConfigSurface }
})

export function ConfigHost({ active }: { active: boolean }) {
  const { t } = useLocalizedMessages(configMessages)
  return (
    <ManagedToolHost active={active} toolId="config" title="YAML / Properties">
      <Suspense fallback={<div className="tool-webview-placeholder">{t('host.loading')}</div>}>
        <ConfigSurface />
      </Suspense>
    </ManagedToolHost>
  )
}
