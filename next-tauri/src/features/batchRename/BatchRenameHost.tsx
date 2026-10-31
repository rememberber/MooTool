import { lazy, Suspense } from 'react'
import { useLocalizedMessages } from '../../app/localizedMessages'
import { ManagedToolHost } from '../toolWebview/ManagedToolHost'
import { batchRenameMessages } from './batchRenameMessages'

const BatchRenameSurface = lazy(async () => ({ default: (await import('./BatchRenameSurface')).BatchRenameSurface }))

export function BatchRenameHost({ active }: { active: boolean }) {
  const { t } = useLocalizedMessages(batchRenameMessages)
  return (
    <ManagedToolHost active={active} toolId="batch-rename" title={t('title')}>
      <Suspense fallback={<div className="tool-webview-placeholder">{t('host.loading')}</div>}>
        <BatchRenameSurface />
      </Suspense>
    </ManagedToolHost>
  )
}
