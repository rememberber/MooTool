import {
  Braces,
  ChevronDown,
  ChevronUp,
  CheckCircle2,
  Clipboard,
  Copy,
  Eraser,
  History,
  Import,
  Plus,
  Save,
  Search,
  Send,
  Square,
  Trash2,
  TriangleAlert,
  X
} from 'lucide-react'
import { EditorView } from '@codemirror/view'
import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import { useLocalizedMessages, type LocalizedMessageKey, type MessageValues } from '../../app/localizedMessages'
import { clipboardApi } from '../../platform/api/clipboardApi'
import { httpApi } from '../../platform/api/httpApi'
import type {
  HttpCookieEntry,
  HttpEntry,
  HttpRequestHistory,
  HttpRequestSpec,
  HttpResponseData,
  SavedHttpRequest
} from '../../platform/contracts/http'
import { CodeEditor } from '../../shared/CodeEditor'
import { ResizableColumns } from '../../shared/ResizableColumns'
import { useDesktopDialog } from '../../shared/DesktopDialogProvider'
import { contentFingerprint } from '../../shared/fingerprint'
import { useSettings } from '../settings/SettingsProvider'
import { useToolSessionReport } from '../toolWebview/useToolSessionReport'
import { useOperationHistory } from '../history/useOperationHistory'
import { useOperationRestore } from '../history/operationRestore'
import { httpMessages } from './httpMessages'
import { buildCurl, cookie, emptyHttpRequest, entry, HttpToolError, parseCurl, type HttpMethod } from './httpTools'

type RequestTab = 'params' | 'headers' | 'cookies' | 'body'
type ResponseTab = 'body' | 'headers' | 'cookies'
type HttpMessageKey = LocalizedMessageKey<typeof httpMessages>
type HttpNotice = { key: HttpMessageKey; values?: MessageValues } | { raw: string }

class HttpLocalizedError extends Error {
  constructor(readonly key: HttpMessageKey, readonly values?: MessageValues) {
    super(key)
    this.name = 'HttpLocalizedError'
  }
}

export function HttpSurface() {
  const { settings, save: saveSettings } = useSettings()
  const dialog = useDesktopDialog()
  const { t } = useLocalizedMessages(httpMessages)
  const [request, setRequest] = useState<HttpRequestSpec>(() => emptyHttpRequest(settings.network.timeoutSeconds * 1_000))
  const [saved, setSaved] = useState<SavedHttpRequest[]>([])
  const [savedQuery, setSavedQuery] = useState('')
  const [activeSavedId, setActiveSavedId] = useState('')
  const [response, setResponse] = useState<HttpResponseData>()
  const [running, setRunning] = useState(false)
  const [progress, setProgress] = useState<HttpNotice>({ key: 'progress.ready' })
  const [notice, setNotice] = useState<HttpNotice>({ key: 'notice.ready' })
  const [failed, setFailed] = useState(false)
  const [requestTab, setRequestTab] = useState<RequestTab>('params')
  const [responseTab, setResponseTab] = useState<ResponseTab>('body')
  const [copied, setCopied] = useState(false)
  const [historyOpen, setHistoryOpen] = useState(false)
  const [responseSearch, setResponseSearch] = useState('')
  const [responseMatchIndex, setResponseMatchIndex] = useState(-1)
  const responseEditor = useRef<EditorView | undefined>(undefined)
  const progressText = localize(progress, t)
  const noticeText = localize(notice, t)

  const loadSaved = useCallback(async (query = savedQuery) => {
    setSaved(await httpApi.listSaved(query))
  }, [savedQuery])

  useEffect(() => {
    const timer = window.setTimeout(() => { void loadSaved().catch(fail) }, 120)
    return () => window.clearTimeout(timer)
  }, [loadSaved])

  const session = useMemo(() => ({
    digest: JSON.stringify({
      method: request.method,
      urlHash: contentFingerprint(request.url),
      bodyHash: contentFingerprint(request.body),
      status: response?.status,
      running
    }),
    summary: `${request.method} · ${response ? `${response.status} · ${response.durationMs} ms` : running ? progressText : t('session.pending')}`
  }), [progressText, request.body, request.method, request.url, response, running, t])
  const { sessionId, reportError } = useToolSessionReport('http', session.digest, session.summary)
  const recordOperation = useOperationHistory('http')
  useOperationRestore('http', (entry) => {
    try {
      const restored = JSON.parse(entry.inputText) as HttpRequestSpec
      setRequest({ ...restored, requestId: crypto.randomUUID() })
      setResponse(undefined)
      setActiveSavedId('')
      setRequestTab('params')
      setResponseTab('body')
      setFailed(false)
    } catch (cause) { fail(cause) }
  })

  const responseOutput = responseTab === 'headers'
    ? response?.headers.map(([name, value]) => `${name}: ${value}`).join('\n') ?? ''
    : responseTab === 'cookies'
      ? response?.headers.filter(([name]) => name.toLowerCase() === 'set-cookie').map(([, value]) => value).join('\n') ?? ''
      : response?.bodyText || (response?.bodyBase64 ? `${t('response.binary')}\n${response.bodyBase64}` : '')
  const responseMatches = useMemo(() => findMatches(responseOutput, responseSearch), [responseOutput, responseSearch])

  useEffect(() => {
    setResponseMatchIndex(-1)
  }, [responseOutput, responseSearch])

  function navigateResponseMatch(direction: 1 | -1) {
    if (!responseMatches.length || !responseEditor.current) return
    const nextIndex = responseMatchIndex < 0
      ? direction > 0 ? 0 : responseMatches.length - 1
      : (responseMatchIndex + direction + responseMatches.length) % responseMatches.length
    const match = responseMatches[nextIndex]
    setResponseMatchIndex(nextIndex)
    responseEditor.current.dispatch({
      selection: { anchor: match.from, head: match.to },
      effects: EditorView.scrollIntoView(match.from, { y: 'center' })
    })
    responseEditor.current.focus()
  }

  async function send() {
    if (running) return
    if (!request.url.trim()) { fail(new HttpLocalizedError('error.url')); return }
    const next: HttpRequestSpec = {
      ...request,
      requestId: crypto.randomUUID(),
      url: request.url.trim(),
      timeoutMs: clampTimeout(request.timeoutMs)
    }
    setRequest(next)
    setRunning(true)
    setResponse(undefined)
    setFailed(false)
    try {
      const result = await httpApi.execute(next, (event) => {
        setProgress(event.kind === 'download'
          ? { key: 'progress.download', values: { size: formatBytes(event.receivedBytes) } }
          : event.kind === 'headers' ? { raw: `HTTP ${event.status}` } : { key: 'progress.connecting' })
      })
      setResponse(result)
      succeed('notice.complete', { status: result.status })
      const safeRequest = redactRequest(next)
      recordOperation(t('operation.send'), `${next.method} ${next.url} · HTTP ${result.status} · ${result.durationMs} ms`, result.status < 400 ? 'success' : 'error', {
        inputText: JSON.stringify(safeRequest, null, 2),
        metadata: { status: result.status, durationMs: result.durationMs, redacted: JSON.stringify(safeRequest) !== JSON.stringify(next) }
      })
    } catch (cause) {
      fail(cause)
      recordOperation(t('operation.send'), `${next.method} ${next.url} · ${errorText(cause, t)}`, 'error', {
        inputText: JSON.stringify(redactRequest(next), null, 2), metadata: { redacted: true }
      })
    } finally {
      setRunning(false)
    }
  }

  async function cancel() {
    if (request.requestId) {
      await httpApi.cancel(request.requestId)
      succeed('notice.cancelling')
    }
  }

  function createNew() {
    setRequest(emptyHttpRequest(settings.network.timeoutSeconds * 1_000))
    setActiveSavedId('')
    setResponse(undefined)
    setRequestTab('params')
    setResponseTab('body')
    succeed('notice.new')
  }

  function openSaved(item: SavedHttpRequest) {
    setActiveSavedId(item.id)
    setRequest({ ...item.request, requestId: crypto.randomUUID(), name: item.name })
    setResponse(item.response)
    succeed('notice.opened', { name: item.name })
  }

  async function saveRequest() {
    const name = (await dialog.prompt(t('prompt.requestName'), request.name || t('prompt.unnamed')))?.trim()
    if (!name) return
    const existing = saved.find((item) => item.id === activeSavedId)
    const now = Date.now()
    try {
      const item = await httpApi.save({
        id: existing?.id ?? crypto.randomUUID(),
        name,
        request: { ...request, name },
        response,
        createdAt: existing?.createdAt ?? now,
        updatedAt: now
      })
      setActiveSavedId(item.id)
      setRequest({ ...item.request, name: item.name })
      await loadSaved()
      succeed('notice.saved', { name: item.name })
    } catch (cause) { fail(cause) }
  }

  async function removeSaved() {
    if (!activeSavedId) return
    const item = saved.find((value) => value.id === activeSavedId)
    if (!await dialog.confirm(t('confirm.deleteSaved', { name: item?.name ?? (request.name || t('prompt.unnamed')) }), { dangerous: true })) return
    try {
      await httpApi.deleteSaved(activeSavedId)
      createNew()
      await loadSaved()
      succeed('notice.deleted')
    } catch (cause) { fail(cause) }
  }

  async function importCurl() {
    const command = await dialog.prompt(t('prompt.curl'))
    if (!command) return
    try {
      setRequest(parseCurl(command, request.timeoutMs))
      setActiveSavedId('')
      setResponse(undefined)
      succeed('notice.curlImported')
    } catch (cause) { fail(cause) }
  }

  async function copyCurl() {
    try {
      await clipboardApi.writeText(buildCurl(request))
      setCopied(true)
      window.setTimeout(() => setCopied(false), 1_200)
      succeed('notice.curlCopied')
    } catch (cause) { fail(cause) }
  }

  function formatBody() {
    try {
      if (!request.body.trim()) return
      if (!request.bodyType.toLowerCase().includes('json')) throw new HttpLocalizedError('error.jsonOnly')
      setRequest({ ...request, body: JSON.stringify(JSON.parse(request.body), null, 2) })
      succeed('notice.bodyFormatted')
    } catch (cause) { fail(cause) }
  }

  function updateTimeout(value: number) {
    const timeoutMs = clampTimeout(value)
    setRequest({ ...request, timeoutMs })
    const timeoutSeconds = Math.round(timeoutMs / 1_000)
    if (timeoutSeconds !== settings.network.timeoutSeconds) {
      void saveSettings((current) => ({ ...current, network: { ...current.network, timeoutSeconds } })).catch(fail)
    }
  }

  function fail(cause: unknown) {
    setFailed(true)
    if (cause instanceof HttpLocalizedError) {
      setNotice({ key: cause.key, values: cause.values })
      return
    }
    if (cause instanceof HttpToolError) {
      setNotice({ key: `error.${cause.code}`, values: cause.values })
      return
    }
    setNotice({ raw: cause instanceof Error ? cause.message : String(cause) })
  }

  function succeed(key: HttpMessageKey, values?: MessageValues) {
    setNotice({ key, values })
    setFailed(false)
  }

  return (
    <main className="utility-workbench http-workbench">
      <header className="utility-header"><h1 className="visually-hidden">{t('title')}</h1><div className="http-header-actions"><button type="button" onClick={() => setHistoryOpen(true)}><History />{t('action.history')}</button><span className="utility-session">{t('session.label')} <code>{sessionId}</code></span></div></header>
      <ResizableColumns id="http-collection" className="http-workspace-layout" initialPrimary={220} minPrimary={170} minSecondary={430}>
        <aside className="http-collection-panel">
          <header><label><Search /><input value={savedQuery} placeholder={t('search.saved')} onChange={(event) => setSavedQuery(event.target.value)} /></label><button type="button" title={t('action.new')} onClick={createNew}><Plus /></button></header>
          <div>{saved.length ? saved.map((item) => <button className={item.id === activeSavedId ? 'http-saved-request http-saved-request--active' : 'http-saved-request'} type="button" key={item.id} onClick={() => openSaved(item)}><strong>{item.name}</strong><span><em>{item.request.method}</em>{item.request.url || t('saved.noUrl')}</span></button>) : <p>{t('saved.empty')}</p>}</div>
          <footer><button type="button" title={t('action.importCurl')} onClick={() => void importCurl()}><Import /></button><button type="button" title={t('action.copyCurl')} onClick={() => void copyCurl()}>{copied ? <Clipboard /> : <Copy />}</button><button type="button" title={t('action.save')} onClick={() => void saveRequest()}><Save /></button><button type="button" title={t('action.delete')} disabled={!activeSavedId} onClick={() => void removeSaved()}><Trash2 /></button></footer>
        </aside>
        <section className="http-editor-grid">
          <div className="http-url-line">
            <select aria-label={t('aria.method')} value={request.method} onChange={(event) => setRequest({ ...request, method: event.target.value as HttpMethod })}>{['GET', 'POST', 'PUT', 'PATCH', 'DELETE', 'HEAD', 'OPTIONS'].map((item) => <option key={item}>{item}</option>)}</select>
            <input value={request.url} placeholder="https://api.example.com" spellCheck={false} onChange={(event) => setRequest({ ...request, url: event.target.value })} onKeyDown={(event) => { if (event.key === 'Enter' && !running) void send() }} />
            <label className="http-follow"><input type="checkbox" checked={request.followRedirects} onChange={(event) => setRequest({ ...request, followRedirects: event.target.checked })} />{t('option.follow')}</label>
            <label className="http-timeout"><input type="number" min="1000" max="120000" step="1000" value={request.timeoutMs} onChange={(event) => setRequest({ ...request, timeoutMs: event.target.valueAsNumber })} onBlur={() => updateTimeout(request.timeoutMs)} /><span>ms</span></label>
            {running ? <button className="primary-button http-stop" type="button" onClick={() => void cancel()}><Square />{t('action.stop')}</button> : <button className="primary-button" type="button" onClick={() => void send()}><Send />{t('action.send')}</button>}
          </div>
          <section className="http-request-card">
            <header><Tabs values={['params', 'headers', 'cookies', 'body']} active={requestTab} labels={[t('tab.params'), t('tab.headers'), t('tab.cookies'), t('tab.body')]} onChange={(value) => setRequestTab(value as RequestTab)} /><span>{request.name || t('prompt.unnamed')}</span></header>
            {requestTab === 'params' && <EntryEditor values={request.params} onChange={(params) => setRequest({ ...request, params })} />}
            {requestTab === 'headers' && <EntryEditor values={request.headers} onChange={(headers) => setRequest({ ...request, headers })} />}
            {requestTab === 'cookies' && <CookieEditor values={request.cookies} onChange={(cookies) => setRequest({ ...request, cookies })} />}
            {requestTab === 'body' && <div className="http-body-pane"><header><select value={request.bodyType} onChange={(event) => setRequest({ ...request, bodyType: event.target.value })}>{['application/json', 'text/plain', 'application/xml', 'text/xml', 'text/html', 'application/javascript'].map((value) => <option key={value}>{value}</option>)}</select><button type="button" disabled={!request.body.trim()} onClick={formatBody}><Braces />{t('action.format')}</button><button type="button" disabled={!request.body} onClick={() => setRequest({ ...request, body: '' })}><Eraser />{t('action.clear')}</button></header><CodeEditor ariaLabel={t('aria.requestBody')} value={request.body} onChange={(body) => setRequest({ ...request, body })} className="utility-code-editor" lineWrapping={false} /></div>}
          </section>
          <section className="http-response-card">
            <header><Tabs values={['body', 'headers', 'cookies']} active={responseTab} labels={[t('tab.responseBody'), t('tab.responseHeaders'), t('tab.cookies')]} onChange={(value) => setResponseTab(value as ResponseTab)} /><div className="http-response-meta">{response && <><span className={response.status < 400 ? 'http-status http-status--ok' : 'http-status http-status--error'}>{response.status}</span><span>{formatBytes(response.sizeBytes)} · {response.durationMs} ms{response.truncated ? ` · ${t('response.truncated')}` : ''}</span><label><Search /><input value={responseSearch} placeholder={t('search.response')} onChange={(event) => setResponseSearch(event.target.value)} onKeyDown={(event) => { if (event.key === 'Enter') { event.preventDefault(); navigateResponseMatch(event.shiftKey ? -1 : 1) } }} /><em>{responseSearch ? `${responseMatchIndex < 0 ? 0 : responseMatchIndex + 1}/${responseMatches.length}` : ''}</em></label><button type="button" disabled={!responseMatches.length} title={t('action.previousMatch')} aria-label={t('action.previousMatch')} onClick={() => navigateResponseMatch(-1)}><ChevronUp /></button><button type="button" disabled={!responseMatches.length} title={t('action.nextMatch')} aria-label={t('action.nextMatch')} onClick={() => navigateResponseMatch(1)}><ChevronDown /></button><button type="button" title={t('action.copyResponse')} onClick={() => void clipboardApi.writeText(responseOutput).catch(fail)}><Copy /></button></>}</div></header>
            <CodeEditor ariaLabel={t('aria.response')} value={responseOutput} readOnly className="utility-code-editor" lineWrapping={false} onReady={(view) => { responseEditor.current = view }} />
          </section>
        </section>
      </ResizableColumns>
      <footer className={failed ? 'utility-status utility-status--error' : 'utility-status'}><span>{failed ? <TriangleAlert /> : <CheckCircle2 />}{noticeText}</span><span>{t('footer.capabilities')}</span><code>{session.summary}</code></footer>
      {historyOpen && <HttpHistoryDialog onClose={() => setHistoryOpen(false)} onApply={(item) => { setRequest({ ...item.request, requestId: crypto.randomUUID() }); setResponse(item.response); setHistoryOpen(false); succeed('notice.historyRestored') }} />}
      {reportError && <p className="tool-surface-report-error">{t('report.error', { error: reportError })}</p>}
    </main>
  )
}

const SENSITIVE_FIELD = /authorization|cookie|token|secret|password|api[-_]?key|session/i

function redactRequest(request: HttpRequestSpec): HttpRequestSpec {
  return {
    ...request,
    requestId: '',
    params: request.params.map(redactEntry),
    headers: request.headers.map(redactEntry),
    cookies: request.cookies.map((item) => ({ ...item, value: item.value ? '[REDACTED]' : '' })),
    body: redactBody(request.body, request.bodyType)
  }
}

function redactEntry<T extends HttpEntry>(item: T): T {
  return SENSITIVE_FIELD.test(item.name) ? { ...item, value: item.value ? '[REDACTED]' : '' } : item
}

function redactBody(body: string, bodyType: string): string {
  if (!body.trim()) return body
  if (!bodyType.toLowerCase().includes('json')) return '[REDACTED FROM GLOBAL HISTORY]'
  try {
    const redact = (value: unknown): unknown => {
      if (Array.isArray(value)) return value.map(redact)
      if (!value || typeof value !== 'object') return value
      return Object.fromEntries(Object.entries(value).map(([key, item]) => [key, SENSITIVE_FIELD.test(key) ? '[REDACTED]' : redact(item)]))
    }
    return JSON.stringify(redact(JSON.parse(body)), null, 2)
  } catch {
    return '[REDACTED FROM GLOBAL HISTORY]'
  }
}

function Tabs({ values, labels, active, onChange }: { values: string[]; labels: string[]; active: string; onChange: (value: string) => void }) {
  return <div className="utility-segments">{values.map((value, index) => <button className={active === value ? 'utility-segment utility-segment--active' : 'utility-segment'} type="button" key={value} onClick={() => onChange(value)}>{labels[index]}</button>)}</div>
}

function EntryEditor({ values, onChange }: { values: HttpEntry[]; onChange: (values: HttpEntry[]) => void }) {
  const { t } = useLocalizedMessages(httpMessages)
  function update(id: string, patch: Partial<HttpEntry>) { onChange(values.map((item) => item.id === id ? { ...item, ...patch } : item)) }
  return <div className="http-entry-editor"><header><span /><span>{t('editor.name')}</span><span>{t('editor.value')}</span><button type="button" aria-label={t('editor.add')} title={t('editor.add')} onClick={() => onChange([...values, entry()])}><Plus /></button></header>{values.length ? values.map((item) => <div key={item.id}><input type="checkbox" checked={item.enabled} onChange={(event) => update(item.id, { enabled: event.target.checked })} /><input value={item.name} placeholder={t('editor.name')} onChange={(event) => update(item.id, { name: event.target.value })} /><input value={item.value} placeholder={t('editor.value')} onChange={(event) => update(item.id, { value: event.target.value })} /><button type="button" aria-label={t('editor.remove')} title={t('editor.remove')} onClick={() => onChange(values.filter((value) => value.id !== item.id))}><X /></button></div>) : <button className="http-add-first" type="button" onClick={() => onChange([entry()])}><Plus />{t('editor.add')}</button>}</div>
}

function CookieEditor({ values, onChange }: { values: HttpCookieEntry[]; onChange: (values: HttpCookieEntry[]) => void }) {
  const { t } = useLocalizedMessages(httpMessages)
  function update(id: string, patch: Partial<HttpCookieEntry>) { onChange(values.map((item) => item.id === id ? { ...item, ...patch } : item)) }
  return <div className="http-cookie-editor"><header><span /><span>{t('editor.name')}</span><span>{t('editor.value')}</span><span>Domain</span><span>Path</span><span>Expires</span><button type="button" aria-label={t('editor.addCookie')} title={t('editor.addCookie')} onClick={() => onChange([...values, cookie()])}><Plus /></button></header>{values.length ? values.map((item) => <div key={item.id}><input type="checkbox" checked={item.enabled} onChange={(event) => update(item.id, { enabled: event.target.checked })} />{(['name', 'value', 'domain', 'path', 'expires'] as const).map((key) => <input key={key} value={item[key]} placeholder={key === 'name' ? t('editor.name') : key === 'value' ? t('editor.value') : key} onChange={(event) => update(item.id, { [key]: event.target.value })} />)}<button type="button" aria-label={t('editor.removeCookie')} title={t('editor.removeCookie')} onClick={() => onChange(values.filter((value) => value.id !== item.id))}><X /></button></div>) : <button className="http-add-first" type="button" onClick={() => onChange([cookie()])}><Plus />{t('editor.addCookie')}</button>}</div>
}

function HttpHistoryDialog({ onClose, onApply }: { onClose: () => void; onApply: (item: HttpRequestHistory) => void }) {
  const { t, locale } = useLocalizedMessages(httpMessages)
  const dialog = useDesktopDialog()
  const [items, setItems] = useState<HttpRequestHistory[]>([])
  const [query, setQuery] = useState('')
  const [error, setError] = useState('')
  const load = useCallback(async () => {
    try { setItems(await httpApi.listHistory(query)); setError('') } catch (cause) { setError(cause instanceof Error ? cause.message : String(cause)) }
  }, [query])
  useEffect(() => { const timer = window.setTimeout(() => { void load() }, 100); return () => window.clearTimeout(timer) }, [load])
  async function clearHistory() {
    if (await dialog.confirm(t('history.clearConfirm'), { dangerous: true })) await httpApi.clearHistory().then(load)
  }
  return <section className="http-history-modal" role="dialog" aria-modal="true" aria-label={t('history.dialog')}><div><header><div><History /><strong>{t('history.title')}</strong><span>{t('history.recent')}</span></div><button type="button" aria-label={t('action.close')} onClick={onClose}><X /></button></header><label><Search /><input autoFocus value={query} placeholder={t('history.search')} onChange={(event) => setQuery(event.target.value)} /></label><main>{error && <p>{error}</p>}{items.length ? items.map((item) => <article key={item.id}><button type="button" onClick={() => onApply(item)}><strong><em>{item.request.method}</em>{item.request.name || item.request.url}</strong><span>HTTP {item.response.status} · {item.response.durationMs} ms · {new Date(item.createdAt).toLocaleString(locale)}</span><small>{item.request.url}</small></button><button type="button" title={t('action.delete')} onClick={() => void httpApi.deleteHistory(item.id).then(load)}><Trash2 /></button></article>) : !error && <p>{t('history.empty')}</p>}</main><footer><button type="button" disabled={!items.length} onClick={() => void clearHistory()}><Trash2 />{t('history.clearAll')}</button><button type="button" onClick={onClose}>{t('action.close')}</button></footer></div></section>
}

function localize(notice: HttpNotice, t: (key: HttpMessageKey, values?: MessageValues) => string): string {
  return 'raw' in notice ? notice.raw : t(notice.key, notice.values)
}

function errorText(cause: unknown, t: (key: HttpMessageKey, values?: MessageValues) => string): string {
  if (cause instanceof HttpLocalizedError) return t(cause.key, cause.values)
  if (cause instanceof HttpToolError) return t(`error.${cause.code}`, cause.values)
  return cause instanceof Error ? cause.message : String(cause)
}

function clampTimeout(value: number) { return Math.max(1_000, Math.min(120_000, Number.isFinite(value) ? Math.round(value) : 30_000)) }
function formatBytes(value: number) { return value < 1024 ? `${value} B` : value < 1024 ** 2 ? `${(value / 1024).toFixed(1)} KiB` : `${(value / 1024 ** 2).toFixed(2)} MiB` }
export function findMatches(value: string, query: string): Array<{ from: number; to: number }> {
  if (!query) return []
  const haystack = value.toLocaleLowerCase()
  const needle = query.toLocaleLowerCase()
  const matches: Array<{ from: number; to: number }> = []
  let index = 0
  while ((index = haystack.indexOf(needle, index)) >= 0) {
    matches.push({ from: index, to: index + query.length })
    index += Math.max(1, needle.length)
  }
  return matches
}
