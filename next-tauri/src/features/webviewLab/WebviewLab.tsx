import {
  CircleCheck,
  CircleX,
  ExternalLink,
  PanelTop,
  Play,
  Power,
  RefreshCw
} from 'lucide-react'
import { useToolWebviewSession } from '../toolWebview/useToolWebviewSession'
import type { ToolWebviewSnapshot } from '../../platform/contracts/toolWebview'

export function WebviewLab({ active }: { active: boolean }) {
  const {
    api,
    busy,
    error,
    nativeRuntime,
    readBounds,
    run,
    slotRef,
    snapshot
  } = useToolWebviewSession({ toolId: 'webview-probe', active })

  return (
    <section className="webview-lab">
      <header className="tool-header webview-lab__header">
        <div>
          <span className="eyebrow">P0 ARCHITECTURE PROBE</span>
          <h1>WebView 实验台</h1>
        </div>
        <div className="webview-lab__actions">
          <button
            className="primary-button"
            type="button"
            disabled={busy !== '' || snapshot.exists}
            onClick={() => void run('正在创建', () => api.open(readBounds()))}
          >
            <Power />创建
          </button>
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
            <Play />100 次验证
          </button>
          <button
            className="icon-button webview-lab__close"
            type="button"
            aria-label="关闭工具 WebView"
            disabled={busy !== '' || !snapshot.exists}
            onClick={() => void run('正在关闭', () => api.close())}
          >
            <CircleX />
          </button>
        </div>
      </header>

      <section className="webview-metrics" aria-label="WebView 运行状态">
        <Metric label="位置" value={placementLabel(snapshot.placement)} />
        <Metric label="页面加载" value={`${snapshot.pageLoads} 次`} />
        <Metric label="重挂载操作" value={`${snapshot.reparentOperations} 次`} />
        <Metric label="状态版本" value={String(snapshot.stateRevision)} />
        <Metric
          label="压力结论"
          value={stressLabel(snapshot)}
          passed={snapshot.lastStressPassed}
        />
      </section>

      <div className="tool-webview-frame">
        <div ref={slotRef} className="tool-webview-slot" aria-label="原生工具 WebView 停靠区域">
          {!snapshot.exists && (
            <div className="tool-webview-placeholder">
              <RefreshCw />
              <strong>等待创建独立工具 WebView</strong>
              <span>
                {nativeRuntime
                  ? '创建后，这个区域由 Rust 管理的原生子 WebView 覆盖。'
                  : '浏览器预览只展示控制层；原生重挂载需在 Tauri 桌面运行。'}
              </span>
            </div>
          )}
          {snapshot.placement === 'detached' && (
            <div className="tool-webview-placeholder">
              <ExternalLink />
              <strong>工具 WebView 已分离</strong>
              <span>同一个页面实例正在独立原生窗口中运行。</span>
            </div>
          )}
        </div>
        <footer>
          <span>
            会话：<code>{snapshot.sessionId ?? '等待探针上报'}</code>
          </span>
          <span>
            状态：<code>{snapshot.stateSummary || '—'}</code>
          </span>
          {busy && <strong>{busy}…</strong>}
          {error && <strong className="webview-lab__error">{error}</strong>}
        </footer>
      </div>
    </section>
  )
}

function Metric({ label, value, passed }: {
  label: string
  value: string
  passed?: boolean | null
}) {
  return (
    <div className={passed == null ? '' : passed ? 'metric--passed' : 'metric--failed'}>
      <span>{label}</span>
      <strong>
        {passed === true && <CircleCheck />}
        {passed === false && <CircleX />}
        {value}
      </strong>
    </div>
  )
}

function placementLabel(placement: ToolWebviewSnapshot['placement']): string {
  return { closed: '未创建', docked: '已停靠', detached: '独立窗口' }[placement]
}

function stressLabel(snapshot: ToolWebviewSnapshot): string {
  if (snapshot.lastStressPassed === null) return '未执行'
  return snapshot.lastStressPassed
    ? `${snapshot.lastStressCycles} 次通过`
    : `${snapshot.lastStressCycles} 次失败`
}
