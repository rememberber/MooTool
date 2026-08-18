import { lazy, Suspense } from 'react'
import { useLocalizedMessages } from '../../app/localizedMessages'
import { ManagedToolHost } from '../toolWebview/ManagedToolHost'
import { protobufMessages } from './protobufMessages'

const ProtobufSurface = lazy(async () => {
  const module = await import('./ProtobufSurface')
  return { default: module.ProtobufSurface }
})

export function ProtobufHost({ active }: { active: boolean }) {
  const { t } = useLocalizedMessages(protobufMessages)
  return (
    <ManagedToolHost active={active} toolId="protobuf" title="Protobuf">
      <Suspense fallback={<div className="tool-webview-placeholder">{t('host.loading')}</div>}>
        <ProtobufSurface />
      </Suspense>
    </ManagedToolHost>
  )
}
