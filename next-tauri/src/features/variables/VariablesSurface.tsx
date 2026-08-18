import {
  CheckCircle2,
  Clipboard,
  Copy,
  Eye,
  EyeOff,
  RefreshCw,
  Search,
  ShieldAlert,
  TriangleAlert,
  Variable
} from 'lucide-react'
import { useEffect, useMemo, useState } from 'react'
import { useLocalizedMessages, type LocalizedMessageKey, type MessageValues } from '../../app/localizedMessages'
import { clipboardApi } from '../../platform/api/clipboardApi'
import { diagnosticsApi } from '../../platform/api/diagnosticsApi'
import type { EnvironmentVariable } from '../../platform/contracts/diagnostics'
import { useToolSessionReport } from '../toolWebview/useToolSessionReport'
import { variablesMessages } from './variablesMessages'

type VariablesMessageKey = LocalizedMessageKey<typeof variablesMessages>
type VariablesNotice = { key: VariablesMessageKey; values?: MessageValues } | { raw: string }

export function VariablesSurface() {
  const { t } = useLocalizedMessages(variablesMessages)
  const [variables, setVariables] = useState<EnvironmentVariable[]>([])
  const [query, setQuery] = useState('')
  const [revealed, setRevealed] = useState(false)
  const [notice, setNotice] = useState<VariablesNotice>({ key: 'notice.loading' })
  const [failed, setFailed] = useState(false)
  const [copied, setCopied] = useState('')
  const [busy, setBusy] = useState(false)
  const visible = useMemo(() => {
    const needle = query.trim().toLowerCase()
    return needle
      ? variables.filter((item) => `${item.name}\n${item.value}`.toLowerCase().includes(needle))
      : variables
  }, [query, variables])
  const sensitiveCount = variables.filter((item) => item.sensitive).length
  const noticeText = 'raw' in notice ? notice.raw : t(notice.key, notice.values)
  const session = useMemo(() => ({
    digest: JSON.stringify({ count: variables.length, sensitiveCount, revealed, queryLength: query.length }),
    summary: t('session.summary', { count: variables.length, sensitive: sensitiveCount, state: t(revealed ? 'state.revealed' : 'state.redacted') })
  }), [query.length, revealed, sensitiveCount, t, variables.length])
  const { sessionId, reportError } = useToolSessionReport('variables', session.digest, session.summary)

  useEffect(() => {
    void load(false)
  }, [])

  async function load(reveal: boolean): Promise<void> {
    setBusy(true)
    try {
      setVariables(await diagnosticsApi.environment(reveal))
      setRevealed(reveal)
      setNotice({ key: reveal ? 'notice.revealed' : 'notice.redacted' })
      setFailed(false)
    } catch (cause) {
      setNotice({ raw: cause instanceof Error ? cause.message : String(cause) })
      setFailed(true)
    } finally {
      setBusy(false)
    }
  }

  async function copy(item: EnvironmentVariable): Promise<void> {
    if (item.sensitive && !revealed) {
      setNotice({ key: 'error.revealFirst' })
      setFailed(true)
      return
    }
    try {
      await clipboardApi.writeText(item.value)
      setCopied(item.name)
      setNotice({ key: 'notice.copied', values: { name: item.name } })
      setFailed(false)
      window.setTimeout(() => setCopied(''), 1200)
    } catch {
      setNotice({ key: 'error.clipboard' })
      setFailed(true)
    }
  }

  return (
    <main className="utility-workbench variables-workbench">
      <header className="utility-header">
        <div><span className="eyebrow">TAURI PROCESS ENVIRONMENT</span><h1>{t('title')}</h1></div>
        <span className="utility-session">{t('session.label')} <code>{sessionId}</code></span>
      </header>

      <section className="variables-toolbar">
        <label><Search /><input value={query} placeholder={t('search.placeholder')} onChange={(event) => setQuery(event.target.value)} /></label>
        <span><ShieldAlert />{t('summary.sensitive', { count: sensitiveCount })}</span>
        <button className="secondary-button" type="button" disabled={busy} onClick={() => void load(!revealed)}>
          {revealed ? <EyeOff /> : <Eye />}{t(revealed ? 'action.redact' : 'action.reveal')}
        </button>
        <button className="secondary-button" type="button" disabled={busy} onClick={() => void load(revealed)}>
          <RefreshCw />{t('action.refresh')}
        </button>
      </section>

      <section className="variables-table">
        <header><span>{t('column.name')}</span><span>{t('column.value')}</span><span>{t('column.action')}</span></header>
        <div>
          {visible.map((item) => (
            <article key={item.name}>
              <strong><Variable />{item.name}{item.sensitive && <i>{t('badge.sensitive')}</i>}</strong>
              <code>{item.value}</code>
              <button type="button" onClick={() => void copy(item)}>
                {copied === item.name ? <Clipboard /> : <Copy />}{t(copied === item.name ? 'action.copied' : 'action.copy')}
              </button>
            </article>
          ))}
          {!visible.length && <p>{t('empty')}</p>}
        </div>
      </section>

      <footer className={failed ? 'utility-status utility-status--error' : 'utility-status'}>
        <span>{failed ? <TriangleAlert /> : <CheckCircle2 />}{noticeText}</span>
        <span>{t('footer.capabilities')}</span>
        <code>{session.summary}</code>
      </footer>
      {reportError && <p className="tool-surface-report-error">{t('report.error', { error: reportError })}</p>}
    </main>
  )
}
