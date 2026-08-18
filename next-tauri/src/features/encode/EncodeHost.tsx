import { lazy, Suspense } from 'react'
import { useLocalizedMessages } from '../../app/localizedMessages'
import { ManagedToolHost } from '../toolWebview/ManagedToolHost'
import { encodeMessages } from './encodeMessages'

const EncodeSurface = lazy(async () => {
  const module = await import('./EncodeSurface')
  return { default: module.EncodeSurface }
})

export function EncodeHost({ active }: { active: boolean }) {
  const { t } = useLocalizedMessages(encodeMessages)
  return (
    <ManagedToolHost active={active} toolId="encode" title={t('title')}>
      <Suspense fallback={<div className="tool-webview-placeholder">{t('host.loading')}</div>}>
        <EncodeSurface />
      </Suspense>
    </ManagedToolHost>
  )
}
