import { PanelTopClose, PanelTopOpen } from 'lucide-react'
import { Suspense, useEffect, useState } from 'react'
import { toolById } from '@/app/toolRegistry'
import { ToolActivityProvider } from '@/shared/components/ToolActivity'
import { Tooltip } from '@/shared/components/Tooltip'
import { isDetachableToolId, type ToolWindowStatus } from '@/shared/contracts/app'
import { useI18n } from '@/shared/i18n/I18nProvider'
import { isImmersiveToolId } from './immersiveTools'

export function ToolWindow({ requestedToolId }: { requestedToolId: string }) {
  const { t } = useI18n()
  const [active, setActive] = useState(true)
  const [status, setStatus] = useState<ToolWindowStatus | null>(null)
  const toolId = isDetachableToolId(requestedToolId) ? requestedToolId : null
  const tool = toolId ? toolById.get(toolId) : undefined
  const ToolComponent = tool?.component

  useEffect(() => {
    if (!toolId || !tool) return
    const unsubscribeState = window.mootool.onToolWindowStateChange((nextStatus) => {
      if (nextStatus.toolId === toolId) {
        setStatus(nextStatus)
      }
    })
    const unsubscribeActivity = window.mootool.onToolWindowActivityChange(setActive)
    void window.mootool.getToolWindowState(toolId).then(setStatus)
    void window.mootool.setToolWindowTitle(toolId, t(tool.titleKey))
    return () => {
      unsubscribeState()
      unsubscribeActivity()
    }
  }, [t, tool, toolId])

  if (!toolId || !tool || !ToolComponent) {
    return <div className="workspace-loading">{t('toolWindow.invalid')}</div>
  }

  const detached = status?.detached ?? false
  const immersive = isImmersiveToolId(tool.id)
  const shellClassName = [
    'tool-view-shell',
    detached ? 'tool-view-shell--detached' : '',
    immersive ? 'tool-view-shell--immersive' : '',
    window.mootool.platform === 'darwin' ? 'tool-view-shell--macos' : ''
  ].filter(Boolean).join(' ')
  const windowAction = (
    <Tooltip content={detached ? t('toolWindow.dock') : t('toolWindow.detach')} side={immersive ? 'left' : 'bottom'}>
      <button className="toolbar-button toolbar-button--icon tool-window-toggle glass-control" type="button"
        aria-label={detached ? t('toolWindow.dock') : t('toolWindow.detach')}
        onClick={(event) => {
          event.currentTarget.blur()
          if (detached) void window.mootool.dockToolWindow(toolId)
          else void window.mootool.detachToolWindow(toolId)
        }}>
        {detached ? <PanelTopClose size={16} /> : <PanelTopOpen size={16} />}
      </button>
    </Tooltip>
  )
  return (
    <main className={shellClassName}>
      <div className="tool-window-toggle-slot">{windowAction}</div>
      <ToolActivityProvider active={active}>
        <Suspense fallback={<div className="workspace-loading">{t('common.loading')}</div>}>
          <ToolComponent />
        </Suspense>
      </ToolActivityProvider>
    </main>
  )
}
