import {
  CircleCheck,
  CircleX,
  ExternalLink,
  PanelTop,
  Play,
  Power,
  X
} from 'lucide-react'
import { useToolWebviewSession } from '../toolWebview/useToolWebviewSession'
import { useLocalizedMessages } from '../../app/localizedMessages'
import { CalculatorPage } from './CalculatorPage'
import { calculatorMessages } from './calculatorMessages'

export function CalculatorHost({ active }: { active: boolean }) {
  const { t } = useLocalizedMessages(calculatorMessages)
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
    toolId: 'calculator',
    active,
    autoOpen: nativeRuntime,
    containerNotReadyMessage: t('host.error.containerNotReady')
  })

  if (!nativeRuntime) {
    return <CalculatorPage />
  }

  return (
    <section className="calculator-host">
      <header className="calculator-host__toolbar">
        <div className="calculator-host__status">
          <strong>Calculator WebView</strong>
          <span>{t(`host.placement.${snapshot.placement}`)}</span>
          <span>{t('host.loads', { count: snapshot.pageLoads })}</span>
          <span>reparent {snapshot.reparentOperations}</span>
          {snapshot.lastStressPassed === true && (
            <span className="calculator-host__passed">
              <CircleCheck />{t('host.stressPassed', { count: snapshot.lastStressCycles })}
            </span>
          )}
          {snapshot.lastStressPassed === false && (
            <span className="calculator-host__failed">
              <CircleX />{t('host.stressFailed')}
            </span>
          )}
        </div>
        <div className="calculator-host__actions">
          {!snapshot.exists && (
            <button
              className="primary-button"
              type="button"
              disabled={busy !== ''}
              onClick={() => void run(t('host.busy.start'), () => api.open(readBounds()))}
            >
              <Power />{t('host.action.start')}
            </button>
          )}
          <button
            className="secondary-button"
            type="button"
            disabled={busy !== '' || !snapshot.exists || snapshot.placement === 'detached'}
            onClick={() => void run(t('host.busy.detach'), () => api.detach())}
          >
            <ExternalLink />{t('host.action.detach')}
          </button>
          <button
            className="secondary-button"
            type="button"
            disabled={busy !== '' || snapshot.placement !== 'detached'}
            onClick={() => void run(t('host.busy.dock'), () => api.dock(readBounds()))}
          >
            <PanelTop />{t('host.action.dock')}
          </button>
          <button
            className="secondary-button"
            type="button"
            disabled={busy !== '' || !snapshot.exists || snapshot.sessionId === null}
            onClick={() => void run(t('host.busy.stress'), () => api.stress(readBounds(), 100))}
          >
            <Play />P0 × 100
          </button>
          <button
            className="icon-button calculator-host__close"
            type="button"
            aria-label={t('host.action.close')}
            disabled={busy !== '' || !snapshot.exists}
            onClick={() => void run(t('host.busy.close'), () => api.close())}
          >
            <X />
          </button>
        </div>
      </header>

      <div ref={slotRef} className="calculator-webview-slot" aria-label={t('host.area')}>
        {snapshot.placement === 'detached' && (
          <div className="tool-webview-placeholder">
            <ExternalLink />
            <strong>{t('host.detached.title')}</strong>
            <span>{t('host.detached.detail')}</span>
          </div>
        )}
        {!snapshot.exists && (
          <div className="tool-webview-placeholder">
            <Power />
            <strong>{t('host.closed.title')}</strong>
            <span>{t('host.closed.detail')}</span>
          </div>
        )}
      </div>

      <footer className="calculator-host__footer">
        <span>{t('host.session')}<code>{snapshot.sessionId ?? t('host.waiting')}</code></span>
        <span>{t('host.state')}<code>{snapshot.stateSummary || '—'}</code></span>
        {busy && <strong>{busy}…</strong>}
        {error && <strong className="calculator-host__failed">{error}</strong>}
      </footer>
    </section>
  )
}
