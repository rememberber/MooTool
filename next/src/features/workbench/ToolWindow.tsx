import { PanelTopClose, PanelTopOpen } from 'lucide-react'
import { Suspense, useEffect, useState } from 'react'
import { toolById } from '@/app/toolRegistry'
import { BrandIcon } from '@/shared/components/BrandIcon'
import { ToolActivityProvider } from '@/shared/components/ToolActivity'
import { Tooltip } from '@/shared/components/Tooltip'
import { isDetachableToolId, type ToolWindowStatus } from '@/shared/contracts/app'
import { useI18n } from '@/shared/i18n/I18nProvider'

export function ToolWindow({ requestedToolId }: { requestedToolId: string }) {
  const { t } = useI18n()
  const [active, setActive] = useState(true)
  const [status, setStatus] = useState<ToolWindowStatus | null>(null)
  const [windowControlsVisible, setWindowControlsVisible] = useState(false)
  const toolId = isDetachableToolId(requestedToolId) ? requestedToolId : null
  const tool = toolId ? toolById.get(toolId) : undefined
  const ToolComponent = tool?.component

  useEffect(() => {
    if (!toolId || !tool) return
    const unsubscribeState = window.mootool.onToolWindowStateChange((nextStatus) => {
      if (nextStatus.toolId === toolId) {
        setStatus(nextStatus)
        if (!nextStatus.detached) setWindowControlsVisible(false)
      }
    })
    const unsubscribeActivity = window.mootool.onToolWindowActivityChange(setActive)
    const unsubscribeWindowControls = window.mootool.onToolWindowControlsVisibilityChange(setWindowControlsVisible)
    void window.mootool.getToolWindowState(toolId).then(setStatus)
    void window.mootool.setToolWindowTitle(toolId, t(tool.titleKey))
    return () => {
      unsubscribeState()
      unsubscribeActivity()
      unsubscribeWindowControls()
    }
  }, [t, tool, toolId])

  if (!toolId || !tool || !ToolComponent) {
    return <div className="workspace-loading">{t('toolWindow.invalid')}</div>
  }

  const detached = status?.detached ?? false
  const shellClassName = [
    'tool-view-shell',
    detached ? 'tool-view-shell--detached' : '',
    window.mootool.platform === 'darwin' ? 'tool-view-shell--macos' : '',
    windowControlsVisible ? 'tool-view-shell--window-controls-visible' : ''
  ].filter(Boolean).join(' ')
  return (
    <main className={shellClassName}>
      <div className="window-drag window-drag-region" aria-hidden="true" />
      {detached && (
        <div
          className={windowControlsVisible ? 'tool-window-brand-zone tool-window-brand-zone--controls-visible' : 'tool-window-brand-zone'}
          data-window-controls-visible={windowControlsVisible}
        >
          <BrandIcon className="tool-window-brand" size={26} />
        </div>
      )}
      <div className="tool-window-toggle-slot">
        <Tooltip content={detached ? t('toolWindow.dock') : t('toolWindow.detach')} side="bottom">
          <button
            className="toolbar-button toolbar-button--icon tool-window-toggle"
            type="button"
            aria-label={detached ? t('toolWindow.dock') : t('toolWindow.detach')}
            onClick={() => {
              if (detached) {
                void window.mootool.dockToolWindow(toolId)
              }
              else void window.mootool.detachToolWindow(toolId)
            }}
          >
            {detached ? <PanelTopClose size={16} /> : <PanelTopOpen size={16} />}
          </button>
        </Tooltip>
      </div>
      <ToolActivityProvider active={active}>
        <Suspense fallback={<div className="workspace-loading">{t('common.loading')}</div>}>
          <ToolComponent />
        </Suspense>
      </ToolActivityProvider>
    </main>
  )
}
