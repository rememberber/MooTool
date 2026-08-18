import { Minus, Plus, RotateCcw, ShieldCheck } from 'lucide-react'
import { useEffect, useRef, useState } from 'react'
import { toolWebviewApis } from '../../platform/api/toolWebviewApi'

export function ToolProbePage() {
  const sessionId = useRef(crypto.randomUUID())
  const revision = useRef(0)
  const [counter, setCounter] = useState(0)
  const [draft, setDraft] = useState('state survives reparent')
  const [reportError, setReportError] = useState('')

  useEffect(() => {
    revision.current += 1
    void toolWebviewApis['webview-probe'].report({
      sessionId: sessionId.current,
      stateRevision: revision.current,
      stateDigest: JSON.stringify({ counter, draft }),
      stateSummary: `计数 ${counter} · 草稿 ${draft}`
    }).then(() => setReportError('')).catch((error: unknown) => {
      setReportError(error instanceof Error ? error.message : String(error))
    })
  }, [counter, draft])

  return (
    <main className="tool-probe">
      <header className="tool-probe__header">
        <div>
          <span className="eyebrow">OWNED CHILD WEBVIEW</span>
          <h1>工具 WebView 状态探针</h1>
        </div>
        <span className="probe-status"><ShieldCheck />已连接 Rust Manager</span>
      </header>

      <section className="probe-grid">
        <article className="probe-card">
          <span>会话 ID</span>
          <strong data-testid="probe-session">{sessionId.current}</strong>
          <p>停靠、分离和收回后，此 ID 必须保持不变。</p>
        </article>
        <article className="probe-card probe-card--counter">
          <span>内存计数器</span>
          <output data-testid="probe-counter">{counter}</output>
          <div>
            <button type="button" aria-label="减少计数" onClick={() => setCounter((value) => value - 1)}>
              <Minus />
            </button>
            <button type="button" aria-label="重置计数" onClick={() => setCounter(0)}>
              <RotateCcw />
            </button>
            <button type="button" aria-label="增加计数" onClick={() => setCounter((value) => value + 1)}>
              <Plus />
            </button>
          </div>
        </article>
      </section>

      <label className="probe-draft">
        <span>页面内存草稿</span>
        <input
          value={draft}
          data-testid="probe-draft"
          onChange={(event) => setDraft(event.target.value)}
        />
        <small>输入一段唯一文本，再执行 100 次压力验证；文本不应被重置。</small>
      </label>

      {reportError && <p className="probe-error" role="alert">{reportError}</p>}
    </main>
  )
}
