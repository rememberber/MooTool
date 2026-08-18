import { lazy, Suspense } from 'react'
import { useLocalizedMessages } from '../../app/localizedMessages'
import { ManagedToolHost } from '../toolWebview/ManagedToolHost'
import { qrcodeMessages } from './qrcodeMessages'

const QrcodeSurface = lazy(async () => {
  const module = await import('./QrcodeSurface')
  return { default: module.QrcodeSurface }
})

export function QrcodeHost({ active }: { active: boolean }) {
  const { t } = useLocalizedMessages(qrcodeMessages)
  return (
    <ManagedToolHost active={active} toolId="qrcode" title={t('title')}>
      <Suspense fallback={<div className="tool-webview-placeholder">{t('host.loading')}</div>}>
        <QrcodeSurface />
      </Suspense>
    </ManagedToolHost>
  )
}
