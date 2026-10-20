import { lazy, Suspense } from 'react'
import { useLocalizedMessages } from '../../app/localizedMessages'
import { ManagedToolHost } from '../toolWebview/ManagedToolHost'
import { filePreviewMessages } from './filePreviewMessages'

const FilePreviewSurface = lazy(async () => ({ default: (await import('./FilePreviewSurface')).FilePreviewSurface }))

export function FilePreviewHost({ active }: { active: boolean }) {
  const { t } = useLocalizedMessages(filePreviewMessages)
  return (
    <ManagedToolHost active={active} toolId="file-preview" title={t('title')}>
      <Suspense fallback={<div className="tool-webview-placeholder">{t('host.loading')}</div>}>
        <FilePreviewSurface />
      </Suspense>
    </ManagedToolHost>
  )
}
