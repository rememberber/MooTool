import {
  CheckCircle2,
  Clipboard,
  Copy,
  Download,
  Eye,
  EyeOff,
  Pencil,
  Plus,
  RefreshCw,
  Search,
  ShieldAlert,
  TriangleAlert,
  Trash2,
  Variable
} from 'lucide-react'
import { useEffect, useMemo, useState } from 'react'
import { useLocalizedMessages, type LocalizedMessageKey, type MessageValues } from '../../app/localizedMessages'
import { clipboardApi } from '../../platform/api/clipboardApi'
import { diagnosticsApi } from '../../platform/api/diagnosticsApi'
import { userFilesApi } from '../../platform/api/userFilesApi'
import type { EnvironmentVariable } from '../../platform/contracts/diagnostics'
import { useToolSessionReport } from '../toolWebview/useToolSessionReport'
import { useSettings } from '../settings/SettingsProvider'
import { useDesktopDialog } from '../../shared/DesktopDialogProvider'
import { variablesMessages } from './variablesMessages'

type VariablesMessageKey = LocalizedMessageKey<typeof variablesMessages>
type VariablesNotice = { key: VariablesMessageKey; values?: MessageValues } | { raw: string }

export function VariablesSurface() {
  const { settings, save } = useSettings()
  const dialog = useDesktopDialog()
  const { t } = useLocalizedMessages(variablesMessages)
  const [variables, setVariables] = useState<EnvironmentVariable[]>([])
  const [query, setQuery] = useState('')
  const [revealed, setRevealed] = useState(false)
  const [notice, setNotice] = useState<VariablesNotice>({ key: 'notice.loading' })
  const [failed, setFailed] = useState(false)
  const [copied, setCopied] = useState('')
  const [busy, setBusy] = useState(false)
  const [scope, setScope] = useState<'process' | 'runtime'>('process')
  const runtimeVariables = useMemo<EnvironmentVariable[]>(() => Object.entries(settings.runtime.environment).map(([name, value]) => ({ name, value: isSensitiveName(name) && !revealed ? '••••••••' : value, sensitive: isSensitiveName(name) })).sort((left, right) => left.name.localeCompare(right.name)), [revealed, settings.runtime.environment])
  const scopedVariables = scope === 'process' ? variables : runtimeVariables
  const visible = useMemo(() => {
    const needle = query.trim().toLowerCase()
    return needle
      ? scopedVariables.filter((item) => `${item.name}\n${item.value}`.toLowerCase().includes(needle))
      : scopedVariables
  }, [query, scopedVariables])
  const sensitiveCount = scopedVariables.filter((item) => item.sensitive).length
  const noticeText = 'raw' in notice ? notice.raw : t(notice.key, notice.values)
  const session = useMemo(() => ({
    digest: JSON.stringify({ count: scopedVariables.length, sensitiveCount, revealed, queryLength: query.length, scope }),
    summary: t('session.summary', { count: scopedVariables.length, sensitive: sensitiveCount, state: t(revealed ? 'state.revealed' : 'state.redacted') })
  }), [query.length, revealed, scope, scopedVariables.length, sensitiveCount, t])
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

  async function editRuntime(item?: EnvironmentVariable): Promise<void> {
    const name = (await dialog.prompt(t(item ? 'prompt.editName' : 'prompt.name'), item?.name ?? ''))?.trim()
    if (!name) return
    if (!/^[A-Za-z_][A-Za-z0-9_]{0,127}$/.test(name)) {
      setNotice({ key: 'error.name' }); setFailed(true); return
    }
    const currentValue = item ? settings.runtime.environment[item.name] ?? '' : ''
    const value = await dialog.prompt(t('prompt.value'), currentValue)
    if (value === null) return
    await save((current) => {
      const environment = { ...current.runtime.environment }
      if (item && item.name !== name) delete environment[item.name]
      environment[name] = value
      return { ...current, runtime: { ...current.runtime, environment } }
    })
    setNotice({ key: item ? 'notice.updated' : 'notice.added', values: { name } })
    setFailed(false)
  }

  async function removeRuntime(item: EnvironmentVariable): Promise<void> {
    if (!await dialog.confirm(t('confirm.delete', { name: item.name }), { dangerous: true })) return
    await save((current) => {
      const environment = { ...current.runtime.environment }
      delete environment[item.name]
      return { ...current, runtime: { ...current.runtime, environment } }
    })
    setNotice({ key: 'notice.deleted', values: { name: item.name } })
  }

  async function exportVariables(): Promise<void> {
    const content = scopedVariables.map((item) => `${item.name}=${item.sensitive && !revealed ? '' : item.value}`).join('\n')
    const path = await userFilesApi.exportText(`mootool-${scope}-environment.env`, `${content}\n`)
    if (path) setNotice({ key: 'notice.exported', values: { path } })
  }

  return (
    <main className="utility-workbench variables-workbench">
      <header className="utility-header">
        <h1 className="visually-hidden">{t('title')}</h1>
        <span className="utility-session">{t('session.label')} <code>{sessionId}</code></span>
      </header>

      <nav className="utility-segments variables-tabs" aria-label={t('scope.aria')}><button className={scope === 'process' ? 'utility-segment utility-segment--active' : 'utility-segment'} type="button" onClick={() => setScope('process')}>{t('scope.process')}</button><button className={scope === 'runtime' ? 'utility-segment utility-segment--active' : 'utility-segment'} type="button" onClick={() => setScope('runtime')}>{t('scope.runtime')}</button></nav>

      <section className="variables-toolbar">
        <label><Search /><input value={query} placeholder={t('search.placeholder')} onChange={(event) => setQuery(event.target.value)} /></label>
        <span><ShieldAlert />{t('summary.sensitive', { count: sensitiveCount })}</span>
        <button className="secondary-button" type="button" disabled={busy} onClick={() => void load(!revealed)}>
          {revealed ? <EyeOff /> : <Eye />}{t(revealed ? 'action.redact' : 'action.reveal')}
        </button>
        <button className="secondary-button" type="button" disabled={busy} onClick={() => void load(revealed)}>
          <RefreshCw />{t('action.refresh')}
        </button>
        {scope === 'runtime' && <button className="secondary-button" type="button" onClick={() => void editRuntime()}><Plus />{t('action.add')}</button>}
        <button className="secondary-button" type="button" onClick={() => void exportVariables()}><Download />{t('action.export')}</button>
      </section>

      <section className="variables-table">
        <header><span>{t('column.name')}</span><span>{t('column.value')}</span><span>{t('column.action')}</span></header>
        <div>
          {visible.map((item) => (
            <article key={item.name}>
              <strong><Variable />{item.name}{item.sensitive && <i>{t('badge.sensitive')}</i>}</strong>
              <code>{item.value}</code>
              <div className="variables-actions"><button type="button" onClick={() => void copy(item)}>
                {copied === item.name ? <Clipboard /> : <Copy />}{t(copied === item.name ? 'action.copied' : 'action.copy')}
              </button>{scope === 'runtime' && <><button type="button" aria-label={t('action.edit')} onClick={() => void editRuntime(item)}><Pencil /></button><button type="button" aria-label={t('action.delete')} onClick={() => void removeRuntime(item)}><Trash2 /></button></>}</div>
            </article>
          ))}
          {!visible.length && <p>{t('empty')}</p>}
        </div>
      </section>

      <footer className={failed ? 'utility-status utility-status--error' : 'utility-status'}>
        <span>{failed ? <TriangleAlert /> : <CheckCircle2 />}{noticeText}</span>
        <span>{t(scope === 'process' ? 'footer.capabilities' : 'footer.runtimeCapabilities')}</span>
        <code>{session.summary}</code>
      </footer>
      {reportError && <p className="tool-surface-report-error">{t('report.error', { error: reportError })}</p>}
    </main>
  )
}

function isSensitiveName(name: string): boolean {
  return /(token|secret|password|passwd|credential|private|api[_-]?key|auth)/i.test(name)
}
