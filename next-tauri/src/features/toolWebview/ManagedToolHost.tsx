import { ExternalLink, MoreHorizontal, PanelTop, Power, X } from 'lucide-react'
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
      <header className="managed-tool-host__toolbar">
        <strong>{title}</strong>
        <span className={error ? 'managed-tool-host__notice managed-tool-host__notice--error' : 'managed-tool-host__notice'} role="status" aria-live="polite">
          {error || (busy ? `${busy}…` : '')}
        </span>
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
        <details className="managed-tool-host__menu">
          <summary aria-label={t('action.manage', { title })} title={t('action.manage', { title })}>
            <MoreHorizontal />
          </summary>
          <div>
            <button
              type="button"
              disabled={busy !== '' || !snapshot.exists || snapshot.placement === 'detached'}
              onClick={() => void run(t('busy.detach'), () => api.detach())}
            >
              <ExternalLink />{t('action.detach')}
            </button>
            <button
              type="button"
              disabled={busy !== '' || snapshot.placement !== 'detached'}
              onClick={() => void run(t('busy.dock'), () => api.dock(readBounds()))}
            >
              <PanelTop />{t('action.dock')}
            </button>
            <button
              className="managed-tool-host__danger"
              type="button"
              disabled={busy !== '' || !snapshot.exists}
              onClick={() => void run(t('busy.close'), () => api.close())}
            >
              <X />{t('action.close', { title })}
            </button>
            {import.meta.env.DEV && (
              <dl className="managed-tool-host__diagnostics">
                <div><dt>{t('debug.placement')}</dt><dd>{t(`placement.${snapshot.placement}`)}</dd></div>
                <div><dt>{t('debug.loads')}</dt><dd>{snapshot.pageLoads}</dd></div>
                <div><dt>{t('debug.session')}</dt><dd>{snapshot.sessionId ?? '—'}</dd></div>
              </dl>
            )}
          </div>
        </details>
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

    </section>
  )
}
