import {
  CircleCheck,
  CircleX,
  ExternalLink,
  PanelTop,
  Play,
  Power,
  RefreshCw
} from 'lucide-react'
import { useCallback, useEffect, useRef, useState } from 'react'
import { toolWebviewApi } from '../../platform/api/toolWebviewApi'
import type {
  ToolWebviewBounds,
  ToolWebviewSnapshot
} from '../../platform/contracts/toolWebview'

const initialSnapshot: ToolWebviewSnapshot = {
  exists: false,
  visible: false,
  placement: 'closed',
  reparentOperations: 0,
  pageLoads: 0,
  sessionId: null,
  counter: 0,
  draft: '',
  lastStressCycles: 0,
  lastStressPassed: null
}

export function WebviewLab({ active }: { active: boolean }) {
  const slotRef = useRef<HTMLDivElement>(null)
  const [snapshot, setSnapshot] = useState(initialSnapshot)
  const [busy, setBusy] = useState('')
  const [error, setError] = useState('')
  const nativeRuntime = typeof window !== 'undefined' && Boolean(window.__TAURI_INTERNALS__)

  const readBounds = useCallback((): ToolWebviewBounds => {
    const slot = slotRef.current
    if (!slot) {
      throw new Error('工具 WebView 容器尚未就绪')
    }
    const bounds = slot.getBoundingClientRect()
    return {
      x: Math.round(bounds.x),
      y: Math.round(bounds.y),
      width: Math.round(bounds.width),
      height: Math.round(bounds.height)
    }
  }, [])

  const run = useCallback(async (
    label: string,
    operation: () => Promise<ToolWebviewSnapshot>
  ): Promise<void> => {
    setBusy(label)
    setError('')
    try {
      setSnapshot(await operation())
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : String(cause))
    } finally {
      setBusy('')
    }
  }, [])

  useEffect(() => {
    let cancelled = false
    void toolWebviewApi.getSnapshot().then(async (current) => {
      if (cancelled) return
      let next = current
      if (current.exists && current.placement === 'docked') {
        if (active) {
          next = await toolWebviewApi.updateBounds(readBounds())
          next = await toolWebviewApi.setVisible(true)
        } else {
          next = await toolWebviewApi.setVisible(false)
        }
      }
      if (!cancelled) setSnapshot(next)
    }).catch((cause: unknown) => {
      if (!cancelled) setError(cause instanceof Error ? cause.message : String(cause))
    })
    return () => {
      cancelled = true
    }
  }, [active, readBounds])

  useEffect(() => {
    if (!active) return
    const timer = window.setInterval(() => {
      void toolWebviewApi.getSnapshot().then(setSnapshot).catch(() => undefined)
    }, 750)
    return () => window.clearInterval(timer)
  }, [active])

  useEffect(() => {
    if (!active || !slotRef.current) return
    let frame = 0
    const update = () => {
      window.cancelAnimationFrame(frame)
      frame = window.requestAnimationFrame(() => {
        void toolWebviewApi.getSnapshot().then((current) => {
          if (current.exists && current.placement === 'docked') {
            return toolWebviewApi.updateBounds(readBounds()).then(setSnapshot)
          }
          return undefined
        }).catch(() => undefined)
      })
    }
    const observer = new ResizeObserver(update)
    observer.observe(slotRef.current)
    window.addEventListener('resize', update)
    return () => {
      window.cancelAnimationFrame(frame)
      observer.disconnect()
      window.removeEventListener('resize', update)
    }
  }, [active, readBounds])

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
            onClick={() => void run('正在创建', () => toolWebviewApi.open(readBounds()))}
          >
            <Power />创建
          </button>
          <button
            className="secondary-button"
            type="button"
            disabled={busy !== '' || !snapshot.exists || snapshot.placement === 'detached'}
            onClick={() => void run('正在分离', () => toolWebviewApi.detach())}
          >
            <ExternalLink />分离
          </button>
          <button
            className="secondary-button"
            type="button"
            disabled={busy !== '' || snapshot.placement !== 'detached'}
            onClick={() => void run('正在收回', () => toolWebviewApi.dock(readBounds()))}
          >
            <PanelTop />收回
          </button>
          <button
            className="secondary-button"
            type="button"
            disabled={busy !== '' || !snapshot.exists}
            onClick={() => void run('100 次验证中', () => toolWebviewApi.stress(readBounds(), 100))}
          >
            <Play />100 次验证
          </button>
          <button
            className="icon-button webview-lab__close"
            type="button"
            aria-label="关闭工具 WebView"
            disabled={busy !== '' || !snapshot.exists}
            onClick={() => void run('正在关闭', () => toolWebviewApi.close())}
          >
            <CircleX />
          </button>
        </div>
      </header>

      <section className="webview-metrics" aria-label="WebView 运行状态">
        <Metric label="位置" value={placementLabel(snapshot.placement)} />
        <Metric label="页面加载" value={`${snapshot.pageLoads} 次`} />
        <Metric label="重挂载操作" value={`${snapshot.reparentOperations} 次`} />
        <Metric label="探针计数" value={String(snapshot.counter)} />
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
            草稿：<code>{snapshot.draft || '—'}</code>
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
