import { useEffect, useState } from 'react'
import { Check, Copy, Plug, RefreshCw, Trash2 } from 'lucide-react'
import { aiClients, type AiClient, type AiDataAccess, type AiInstallMode, type AiInstallPreview, type AiInstallResult, type AiIntegrationStatus } from '@/shared/contracts/aiIntegration'
import { useI18n } from '@/shared/i18n/I18nProvider'
import { useToast } from '@/shared/feedback/ToastProvider'

const clientNames: Record<AiClient, string> = { codex: 'Codex', 'claude-code': 'Claude Code', cursor: 'Cursor' }

export function AiIntegrationSettings() {
  const { t } = useI18n()
  const toast = useToast()
  const [client, setClient] = useState<AiClient>('codex')
  const [mode, setMode] = useState<AiInstallMode>('both')
  const [preview, setPreview] = useState<AiInstallPreview | null>(null)
  const [result, setResult] = useState<AiInstallResult | null>(null)
  const [error, setError] = useState('')
  const [connection, setConnection] = useState<string[]>([])
  const [busy, setBusy] = useState(false)
  const [revision, setRevision] = useState(0)
  const [status, setStatus] = useState<AiIntegrationStatus | null>(null)
  const [access, setAccess] = useState<AiDataAccess | null>(null)
  const [removed, setRemoved] = useState(false)

  useEffect(() => {
    let cancelled = false
    setPreview(null)
    setError('')
    setResult(null)
    setStatus(null)
    setRemoved(false)
    void window.mootool.getAiIntegrationStatus(client).then((value) => { if (!cancelled) setStatus(value) }).catch((reason) => { if (!cancelled) setError(String(reason)) })
    void window.mootool.getAiDataAccess().then((value) => { if (!cancelled) setAccess(value) }).catch((reason) => { if (!cancelled) setError(String(reason)) })
    void window.mootool.previewAiIntegration({ client, mode })
      .then((value) => { if (!cancelled) setPreview(value) })
      .catch((reason) => { if (!cancelled) setError(String(reason)) })
    return () => { cancelled = true }
  }, [client, mode, revision])

  async function install(): Promise<void> {
    if (!preview) return
    setBusy(true)
    setError('')
    try {
      setResult(await window.mootool.installAiIntegration(preview.id))
      setStatus(await window.mootool.getAiIntegrationStatus(client))
      setRemoved(false)
      toast.success(t('settings.ai.installed'))
    } catch (reason) { setError(String(reason)) }
    finally { setBusy(false) }
  }

  async function uninstall(): Promise<void> {
    setBusy(true)
    setError('')
    try {
      const removal = await window.mootool.previewAiIntegration({ client, mode, operation: 'uninstall' })
      setResult(await window.mootool.installAiIntegration(removal.id))
      setRemoved(true)
      setStatus(await window.mootool.getAiIntegrationStatus(client))
      setPreview(await window.mootool.previewAiIntegration({ client, mode }))
      toast.success(t('settings.ai.removed'))
    } catch (reason) { setError(String(reason)) }
    finally { setBusy(false) }
  }

  async function changeAccess(kind: 'notes' | 'json', enabled: boolean): Promise<void> {
    if (!access) return
    setBusy(true)
    setError('')
    try { setAccess(await window.mootool.setAiDataAccess({ notes: Boolean(access.notes), json: Boolean(access.json), [kind]: enabled })) }
    catch (reason) { setError(String(reason)) }
    finally { setBusy(false) }
  }

  async function testConnection(): Promise<void> {
    setBusy(true)
    setError('')
    try {
      const value = await window.mootool.testAiIntegration()
      setConnection(value.tools)
      toast.success(t('settings.ai.connected', { count: String(value.tools.length) }))
    } catch (reason) { setError(String(reason)) }
    finally { setBusy(false) }
  }

  return (
    <section className="settings-group ai-integration">
      <header><h2>{t('settings.ai.title')}</h2></header>
      <div className="ai-integration__body">
        <p>{t('settings.ai.description')}</p>
        <p className="ai-integration__hint">{t('settings.ai.scope')}</p>
        <div className="setting-row">
          <label htmlFor="ai-client">{t('settings.ai.client')}</label>
          <select id="ai-client" value={client} disabled={busy} onChange={(event) => {
            const value = event.target.value as AiClient
            setClient(value)
            if (value === 'cursor') setMode('mcp')
          }}>
            {aiClients.map((value) => <option key={value} value={value}>{clientNames[value]}</option>)}
          </select>
        </div>
        <div className="setting-row">
          <label htmlFor="ai-mode">{t('settings.ai.mode')}</label>
          <select id="ai-mode" value={mode} disabled={busy} onChange={(event) => setMode(event.target.value as AiInstallMode)}>
            {client !== 'cursor' && <option value="both">MCP + Skill</option>}
            <option value="mcp">MCP</option>
            {client !== 'cursor' && <option value="skill">Skill</option>}
          </select>
        </div>
        <p className="ai-integration__hint">{t(mode === 'skill' ? 'settings.ai.skillHint' : 'settings.ai.mcpHint')}</p>
        {status && <p role="status">MCP: {t(`settings.ai.state.${status.mcp}`)}{client !== 'cursor' && <> · Skill: {t(`settings.ai.state.${status.skill}`)}</>}</p>}
        <div className="ai-integration__actions">
          <button type="button" className="settings-command" disabled={busy || !preview || Boolean(result && !removed)} onClick={() => { void install() }}>
            {result && !removed ? <Check size={14} /> : <Plug size={14} />}{t(busy ? 'common.loading' : result && !removed ? 'settings.ai.installed' : status?.mcp === 'needs-repair' || status?.skill === 'needs-repair' ? 'settings.ai.repair' : 'settings.ai.install')}
          </button>
          <button type="button" className="settings-command settings-command--quiet" disabled={busy} onClick={() => { void testConnection() }}>{t('settings.ai.test')}</button>
          <button type="button" className="settings-command settings-command--quiet" disabled={busy} onClick={() => setRevision((value) => value + 1)}><RefreshCw size={14} />{t('settings.ai.refresh')}</button>
          <button type="button" className="settings-command settings-command--quiet" disabled={busy || !status || ((mode === 'skill' || status.mcp === 'not-installed') && (mode === 'mcp' || status.skill === 'not-installed'))} onClick={() => { void uninstall() }}><Trash2 size={14} />{t('settings.ai.uninstall')}</button>
        </div>
        <p className="ai-integration__hint">{t('settings.ai.uninstallHint')}</p>
        {error && <p className="ai-integration__error" role="alert">{error}</p>}
        {result && <div role="status"><p>{t(removed ? 'settings.ai.removed' : 'settings.ai.restart')}</p>{result.backups.length > 0 && <><strong>{t('settings.ai.backups')}</strong>{result.backups.map((path) => <code className="ai-integration__path" key={path}>{path}</code>)}</>}</div>}
        <h3>{t('settings.ai.dataTitle')}</h3>
        <p className="ai-integration__hint">{t('settings.ai.dataHint')}</p>
        {(['notes', 'json'] as const).map((kind) => <div className="setting-row" key={kind}>
          <div><label htmlFor={`ai-access-${kind}`}>{t(`settings.ai.access.${kind}`)}</label>{access?.[kind] && <code className="ai-integration__path">{access[kind]}</code>}</div>
          <input id={`ai-access-${kind}`} type="checkbox" checked={Boolean(access?.[kind])} disabled={busy || !access} onChange={(event) => { void changeAccess(kind, event.target.checked) }} />
        </div>)}
        {connection.length > 0 && <p role="status">{t('settings.ai.connected', { count: String(connection.length) })}<br /><code>{connection.join(' · ')}</code></p>}
        {preview && <>
          <h3>{t('settings.ai.preview')}</h3>
          {preview.files.map((file) => <details key={file.path}>
            <summary><span>{t(`settings.ai.${file.action}`)}</span> <code>{file.path}</code></summary>
            <pre>{file.content}</pre>
          </details>)}
          {mode !== 'skill' && <button type="button" className="settings-command settings-command--quiet" onClick={() => {
            void navigator.clipboard.writeText(preview.configuration).then(() => toast.success(t('settings.ai.copied'))).catch((reason) => setError(String(reason)))
          }}><Copy size={14} />{t('settings.ai.copy')}</button>}
        </>}
      </div>
    </section>
  )
}
