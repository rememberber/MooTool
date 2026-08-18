import { ExternalLink, PanelTop, Power, X } from 'lucide-react'
import type { ReactNode } from 'react'
import { useLocalizedMessages } from '../../app/localizedMessages'
import type { ManagedToolId } from '../../platform/contracts/toolWebview'
import { useToolWebviewSession } from './useToolWebviewSession'
import { toolWebviewMessages } from './toolWebviewMessages'

interface ManagedToolHostProps {
  active: boolean
  toolId: ManagedToolId
  title: string
  children: ReactNode
}

export function ManagedToolHost({ active, toolId, title, children }: ManagedToolHostProps) {
  const { t } = useLocalizedMessages(toolWebviewMessages)
  const nativeRuntime = typeof window !== 'undefined' && Boolean(window.__TAURI_INTERNALS__)
  const {
    api,
    busy,
    error,
    readBounds,
    run,
    slotRef,
    snapshot
  } = useToolWebviewSession({
    toolId,
    active,
    autoOpen: nativeRuntime,
    containerNotReadyMessage: t('error.containerNotReady')
  })

  if (!nativeRuntime) return children

  return (
    <section className="calculator-host managed-tool-host">
      <header className="calculator-host__toolbar">
        <div className="calculator-host__status">
          <strong>{title}</strong>
          <span>{t(`placement.${snapshot.placement}`)}</span>
          <span>{t('loads', { count: snapshot.pageLoads })}</span>
          <span>{snapshot.stateSummary || t('waiting.state')}</span>
        </div>
        <div className="calculator-host__actions">
          {!snapshot.exists && (
            <button
              className="primary-button"
              type="button"
              disabled={busy !== ''}
              onClick={() => void run(t('busy.start'), () => api.open(readBounds()))}
            >
              <Power />{t('action.start')}
            </button>
          )}
          <button
            className="secondary-button"
            type="button"
            disabled={busy !== '' || !snapshot.exists || snapshot.placement === 'detached'}
            onClick={() => void run(t('busy.detach'), () => api.detach())}
          >
            <ExternalLink />{t('action.detach')}
          </button>
          <button
            className="secondary-button"
            type="button"
            disabled={busy !== '' || snapshot.placement !== 'detached'}
            onClick={() => void run(t('busy.dock'), () => api.dock(readBounds()))}
          >
            <PanelTop />{t('action.dock')}
          </button>
          <button
            className="icon-button calculator-host__close"
            type="button"
            aria-label={t('action.close', { title })}
            disabled={busy !== '' || !snapshot.exists}
            onClick={() => void run(t('busy.close'), () => api.close())}
          >
            <X />
          </button>
        </div>
      </header>

      <div ref={slotRef} className="calculator-webview-slot" aria-label={t('area', { title })}>
        {snapshot.placement === 'detached' && (
          <div className="tool-webview-placeholder">
            <ExternalLink />
            <strong>{t('detached.title', { title })}</strong>
            <span>{t('detached.detail')}</span>
          </div>
        )}
        {!snapshot.exists && (
          <div className="tool-webview-placeholder">
            <Power />
            <strong>{t('closed.title', { title })}</strong>
            <span>{t('closed.detail')}</span>
          </div>
        )}
      </div>

      <footer className="calculator-host__footer">
        <span>{t('session.label')}<code>{snapshot.sessionId ?? t('session.waiting', { title })}</code></span>
        <span>{t('state.label')}<code>{snapshot.stateSummary || '—'}</code></span>
        {busy && <strong>{busy}…</strong>}
        {error && <strong className="calculator-host__failed">{error}</strong>}
      </footer>
    </section>
  )
}
