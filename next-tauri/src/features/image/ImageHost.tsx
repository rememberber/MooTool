import { lazy, Suspense } from 'react'
import { useLocalizedMessages } from '../../app/localizedMessages'
import { ManagedToolHost } from '../toolWebview/ManagedToolHost'
import { imageMessages } from './imageMessages'

const ImageSurface = lazy(async () => ({ default: (await import('./ImageSurface')).ImageSurface }))

export function ImageHost({ active }: { active: boolean }) {
  const { t } = useLocalizedMessages(imageMessages)
  return <ManagedToolHost active={active} toolId="image" title={t('title')}><Suspense fallback={<div className="tool-webview-placeholder">{t('host.loading')}</div>}><ImageSurface /></Suspense></ManagedToolHost>
}
