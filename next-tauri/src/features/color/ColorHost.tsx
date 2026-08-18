import { lazy, Suspense } from 'react'
import { useLocalizedMessages } from '../../app/localizedMessages'
import { ManagedToolHost } from '../toolWebview/ManagedToolHost'
import { colorMessages } from './colorMessages'

const ColorSurface = lazy(async () => {
  const module = await import('./ColorSurface')
  return { default: module.ColorSurface }
})

export function ColorHost({ active }: { active: boolean }) {
  const { t } = useLocalizedMessages(colorMessages)
  return (
    <ManagedToolHost active={active} toolId="color" title={t('title')}>
      <Suspense fallback={<div className="tool-webview-placeholder">{t('host.loading')}</div>}>
        <ColorSurface />
      </Suspense>
    </ManagedToolHost>
  )
}
