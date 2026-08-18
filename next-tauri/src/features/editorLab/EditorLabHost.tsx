import {
  CircleCheck,
  CircleX,
  ExternalLink,
  PanelTop,
  Play,
  Power,
  X
} from 'lucide-react'
import { lazy, Suspense } from 'react'
import { useToolWebviewSession } from '../toolWebview/useToolWebviewSession'

const EditorLabToolSurface = lazy(async () => {
  const module = await import('./EditorLabToolSurface')
  return { default: module.EditorLabToolSurface }
})

export function EditorLabHost({ active }: { active: boolean }) {
  const nativeRuntime = typeof window !== 'undefined' && Boolean(window.__TAURI_INTERNALS__)
  const {
    api,
    busy,
    error,
    readBounds,
    run,
    slotRef,
    snapshot
  } = useToolWebviewSession({ toolId: 'editor-lab', active, autoOpen: nativeRuntime })

  if (!nativeRuntime) {
    return (
      <Suspense fallback={<div className="tool-webview-placeholder">正在加载 CodeMirror…</div>}>
        <EditorLabToolSurface />
      </Suspense>
    )
  }

  return (
    <section className="calculator-host editor-lab-host">
      <header className="calculator-host__toolbar">
        <div className="calculator-host__status">
          <strong>CodeMirror WebView</strong>
          <span>{placementLabel(snapshot.placement)}</span>
          <span>加载 {snapshot.pageLoads}</span>
          <span>reparent {snapshot.reparentOperations}</span>
          {snapshot.lastStressPassed === true && (
            <span className="calculator-host__passed">
              <CircleCheck />{snapshot.lastStressCycles} 次通过
            </span>
          )}
          {snapshot.lastStressPassed === false && (
            <span className="calculator-host__failed">
              <CircleX />压力验证失败
            </span>
          )}
        </div>
        <div className="calculator-host__actions">
          {!snapshot.exists && (
            <button
              className="primary-button"
              type="button"
              disabled={busy !== ''}
              onClick={() => void run('正在启动', () => api.open(readBounds()))}
            >
              <Power />启动
            </button>
          )}
          <button
            className="secondary-button"
            type="button"
            disabled={busy !== '' || !snapshot.exists || snapshot.placement === 'detached'}
            onClick={() => void run('正在分离', () => api.detach())}
          >
            <ExternalLink />分离
          </button>
          <button
            className="secondary-button"
            type="button"
            disabled={busy !== '' || snapshot.placement !== 'detached'}
            onClick={() => void run('正在收回', () => api.dock(readBounds()))}
          >
            <PanelTop />收回
          </button>
          <button
            className="secondary-button"
            type="button"
            disabled={busy !== '' || !snapshot.exists || snapshot.sessionId === null}
            onClick={() => void run('100 次验证中', () => api.stress(readBounds(), 100))}
          >
            <Play />P0 × 100
          </button>
          <button
            className="icon-button calculator-host__close"
            type="button"
            aria-label="关闭 CodeMirror WebView"
            disabled={busy !== '' || !snapshot.exists}
            onClick={() => void run('正在关闭', () => api.close())}
          >
            <X />
          </button>
        </div>
      </header>

      <div
        ref={slotRef}
        className="calculator-webview-slot editor-lab-webview-slot"
        aria-label="CodeMirror 原生 WebView 区域"
      >
        {snapshot.placement === 'detached' && (
          <div className="tool-webview-placeholder">
            <ExternalLink />
            <strong>CodeMirror 已分离</strong>
            <span>同一个编辑器实例正在独立原生窗口中运行。</span>
          </div>
        )}
        {!snapshot.exists && (
          <div className="tool-webview-placeholder">
            <Power />
            <strong>CodeMirror WebView 已关闭</strong>
            <span>点击“启动”创建新的独立编辑器会话。</span>
          </div>
        )}
      </div>

      <footer className="calculator-host__footer">
        <span>会话：<code>{snapshot.sessionId ?? '等待 CodeMirror 上报'}</code></span>
        <span>状态：<code>{snapshot.stateSummary || '—'}</code></span>
        {busy && <strong>{busy}…</strong>}
        {error && <strong className="calculator-host__failed">{error}</strong>}
      </footer>
    </section>
  )
}

function placementLabel(placement: 'closed' | 'docked' | 'detached'): string {
  return { closed: '未启动', docked: '已停靠', detached: '独立窗口' }[placement]
}
