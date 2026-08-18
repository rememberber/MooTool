import { lazy, Suspense } from 'react'
import { useLocalizedMessages } from '../../app/localizedMessages'
import { ManagedToolHost } from '../toolWebview/ManagedToolHost'
import { messageBoardMessages } from './messageBoardMessages'

const MessageBoardSurface = lazy(async () => {
  const module = await import('./MessageBoardSurface')
  return { default: module.MessageBoardSurface }
})

export function MessageBoardHost({ active }: { active: boolean }) {
  const { t } = useLocalizedMessages(messageBoardMessages)
  return (
    <ManagedToolHost active={active} toolId="message-board" title={t('title')}>
      <Suspense fallback={<div className="tool-webview-placeholder">{t('host.loading')}</div>}>
        <MessageBoardSurface />
      </Suspense>
    </ManagedToolHost>
  )
}
