import { Clock3, Code2, Copy, FileInput, History, Plus, Save, Search, Send, Square, Trash2, X } from 'lucide-react'
import { useCallback, useEffect, useRef, useState } from 'react'
import { Dialog } from '@/shared/components/Dialog'
import { ResizableColumns } from '@/shared/components/ResizableColumns'
import { ToolPageHeader, ToolTabs } from '@/shared/components/ToolPage'
import { TextCodeEditor } from '@/shared/components/TextCodeEditor'
import { httpMethods, type HttpCookieEntry, type HttpRequestDraft, type HttpRequestHistory, type HttpResponseResult, type KeyValueEntry, type SavedHttpRequest } from '@/shared/contracts/network'
import { useToolActions } from '@/shared/hooks/useToolActions'
import { useI18n } from '@/shared/i18n/I18nProvider'
import { useSettings } from '@/features/settings/SettingsProvider'
import { cookie, emptyHttpRequest, entry, parseCurlCommand, toCurlCommand } from './httpTools'

type RequestTab = 'params' | 'headers' | 'cookies' | 'body'
type ResponseTab = 'body' | 'headers' | 'cookies'

export function HttpTool() {
  const { t } = useI18n()
  const { settings } = useSettings()
  const actions = useToolActions('http')
  const [saved, setSaved] = useState<SavedHttpRequest[]>([])
  const [search, setSearch] = useState('')
  const [request, setRequest] = useState<HttpRequestDraft>(() => emptyHttpRequest(t('http.untitled')))
  const [response, setResponse] = useState<HttpResponseResult | null>(null)
  const [requestTab, setRequestTab] = useState<RequestTab>('params')
  const [responseTab, setResponseTab] = useState<ResponseTab>('body')
  const [sending, setSending] = useState(false)
  const [historyOpen, setHistoryOpen] = useState(false)
  const [saveOpen, setSaveOpen] = useState(false)
  const [saveName, setSaveName] = useState('')
  const [curlOpen, setCurlOpen] = useState(false)
  const [curlValue, setCurlValue] = useState('')
  const activeRequestId = useRef('')

  const loadSaved = useCallback(async () => setSaved(await window.mootool.listHttpRequests(search)), [search])
  useEffect(() => { const timer = window.setTimeout(() => { void loadSaved() }, 100); return () => window.clearTimeout(timer) }, [loadSaved])

  function openSaved(item: SavedHttpRequest): void {
    setRequest(item)
    setResponse({ requestId: 'saved', ok: true, status: 0, statusText: '', url: item.url, durationMs: 0, body: item.responseBody, headers: item.responseHeaders, cookies: item.responseCookies })
  }

  async function sendRequest(): Promise<void> {
    if (!request.url.trim()) { actions.toast.error(t('http.urlRequired')); return }
    activeRequestId.current = `http-${Date.now()}-${Math.random().toString(36).slice(2)}`
    setSending(true)
    try {
      const result = await window.mootool.sendHttpRequest({ requestId: activeRequestId.current, request, timeoutMs: settings.network.requestTimeoutMs })
      setResponse(result)
      if (!result.ok) actions.toast.error(result.statusText || t(`http.error.${result.errorCode ?? 'NETWORK'}` as 'http.error.NETWORK'))
    } catch (error) { actions.reportError(error) } finally { setSending(false) }
  }

  async function stopRequest(): Promise<void> {
    await window.mootool.cancelNetworkRequest(activeRequestId.current)
    setSending(false)
  }

  async function saveRequest(): Promise<void> {
    const name = saveName.trim()
    if (!name) return
    try {
      const next = await window.mootool.saveHttpRequest({ ...request, name }, response ?? undefined)
      setRequest(next)
      await loadSaved()
      setSaveOpen(false)
      actions.toast.success(t('common.saved'))
    } catch (error) { actions.reportError(error) }
  }

  async function deleteRequest(): Promise<void> {
    if (!request.id || !window.confirm(t('http.confirmDelete'))) return
    try { await window.mootool.deleteHttpRequest(request.id); setRequest(emptyHttpRequest(t('http.untitled'))); setResponse(null); await loadSaved() } catch (error) { actions.reportError(error) }
  }

  function importCurl(): void {
    if (!curlValue.trim()) return
    try {
      setRequest(parseCurlCommand(curlValue))
      setResponse(null)
      setCurlOpen(false)
      setCurlValue('')
    } catch (error) { actions.reportError(error) }
  }

  const responseValue = responseTab === 'body' ? response?.body : responseTab === 'headers' ? response?.headers : response?.cookies
  return (
    <section className="tool-page p5-tool http-tool-page">
      <ToolPageHeader title={t('http.title')} actions={<button className="toolbar-button" type="button" onClick={() => setHistoryOpen(true)}><History size={14} />{t('common.action.history')}</button>} />
      <ResizableColumns className="local-tool-shell http-workspace" columns={2} defaultSizes={[230, 770]} minPaneWidths={[180, 420]} storageKey="http-workspace">
        <aside className="http-collection">
          <header><div className="compact-search"><Search size={13} /><input value={search} aria-label={t('common.search')} placeholder={t('common.search')} onChange={(event) => setSearch(event.target.value)} /></div><button className="icon-button" type="button" aria-label={t('common.new')} onClick={() => { setRequest(emptyHttpRequest(t('http.untitled'))); setResponse(null) }}><Plus size={14} /></button></header>
          <div className="http-saved-list">{saved.length === 0 ? <div className="history-empty">{t('http.savedEmpty')}</div> : saved.map((item) => <button className={item.id === request.id ? 'http-saved-item http-saved-item--active' : 'http-saved-item'} type="button" key={item.id} onClick={() => openSaved(item)}><strong>{item.name}</strong><span><em>{item.method}</em>{item.url || t('http.noUrl')}</span></button>)}</div>
          <footer><button className="icon-button" type="button" aria-label={t('http.importCurl')} onClick={() => setCurlOpen(true)}><FileInput size={14} /></button><button className="icon-button" type="button" aria-label={t('http.copyCurl')} onClick={() => { void actions.copy(toCurlCommand(request)) }}><Code2 size={14} /></button><button className="icon-button" type="button" aria-label={t('common.save')} onClick={() => { setSaveName(request.name || t('http.untitled')); setSaveOpen(true) }}><Save size={14} /></button><button className="icon-button icon-button--danger" type="button" disabled={!request.id} aria-label={t('common.action.delete')} onClick={() => { void deleteRequest() }}><Trash2 size={14} /></button></footer>
        </aside>
        <main className="http-editor">
          <div className="http-url-bar"><select aria-label={t('http.method')} value={request.method} onChange={(event) => setRequest({ ...request, method: event.target.value as HttpRequestDraft['method'] })}>{httpMethods.map((method) => <option key={method}>{method}</option>)}</select><input data-testid="http-url" value={request.url} placeholder="https://api.example.com" spellCheck={false} onChange={(event) => setRequest({ ...request, url: event.target.value })} onKeyDown={(event) => { if (event.key === 'Enter' && !sending) void sendRequest() }} />{sending ? <button className="toolbar-button" type="button" onClick={() => { void stopRequest() }}><Square size={13} />{t('common.stop')}</button> : <button className="toolbar-button toolbar-button--primary" data-testid="http-send" type="button" onClick={() => { void sendRequest() }}><Send size={13} />{t('http.send')}</button>}</div>
          <div className="http-request-pane"><ToolTabs tabs={(['params', 'headers', 'cookies', 'body'] as RequestTab[]).map((id) => ({ id, label: t(`http.tab.${id}` as 'http.tab.params') }))} active={requestTab} onChange={setRequestTab} />
            {requestTab === 'params' && <KeyValueEditor entries={request.params} onChange={(params) => setRequest({ ...request, params })} />}
            {requestTab === 'headers' && <KeyValueEditor entries={request.headers} onChange={(headers) => setRequest({ ...request, headers })} />}
            {requestTab === 'cookies' && <CookieEditor entries={request.cookies} onChange={(cookies) => setRequest({ ...request, cookies })} />}
            {requestTab === 'body' && <div className="http-body-editor"><select aria-label={t('http.bodyType')} value={request.bodyType} onChange={(event) => setRequest({ ...request, bodyType: event.target.value })}>{['application/json', 'text/plain', 'application/xml', 'text/xml', 'text/html', 'application/javascript'].map((type) => <option key={type}>{type}</option>)}</select><TextCodeEditor className="http-body-code-editor" testId="http-body" ariaLabel={t('http.tab.body')} value={request.body} onChange={(body) => setRequest({ ...request, body })} /></div>}
          </div>
          <div className="http-response-pane"><header><ToolTabs tabs={(['body', 'headers', 'cookies'] as ResponseTab[]).map((id) => ({ id, label: t(`http.response.${id}` as 'http.response.body') }))} active={responseTab} onChange={setResponseTab} /><div className={response?.ok ? 'http-status http-status--ok' : 'http-status'}>{response && <><span>{response.status || response.errorCode}</span><span>{response.durationMs} ms</span><button className="icon-button" type="button" aria-label={t('common.action.copy')} onClick={() => { void actions.copy(responseValue || '') }}><Copy size={13} /></button></>}</div></header><pre data-testid="http-response">{responseValue || t('http.responseEmpty')}</pre></div>
        </main>
      </ResizableColumns>
      <HttpHistoryDialog open={historyOpen} onClose={() => setHistoryOpen(false)} onApply={(item) => { setRequest(item); setResponse({ requestId: 'history', ok: item.status.startsWith('2'), status: Number(item.status.split(' ')[0]) || 0, statusText: item.status, url: item.url, durationMs: item.costTime, body: item.responseBody, headers: item.responseHeaders, cookies: item.responseCookies }) }} />
      <Dialog title={t('http.saveName')} open={saveOpen} width={420} onClose={() => setSaveOpen(false)} footer={<><button className="dialog-button" type="button" onClick={() => setSaveOpen(false)}>{t('common.cancel')}</button><button className="dialog-button dialog-button--primary" type="button" disabled={!saveName.trim()} onClick={() => { void saveRequest() }}><Save size={14} />{t('common.save')}</button></>}><label className="vault-new-field"><span>{t('http.saveName')}</span><input autoFocus value={saveName} onChange={(event) => setSaveName(event.target.value)} onKeyDown={(event) => { if (event.key === 'Enter' && saveName.trim()) void saveRequest() }} /></label></Dialog>
      <Dialog title={t('http.importCurl')} open={curlOpen} width={720} onClose={() => setCurlOpen(false)} footer={<><button className="dialog-button" type="button" onClick={() => setCurlOpen(false)}>{t('common.cancel')}</button><button className="dialog-button dialog-button--primary" type="button" disabled={!curlValue.trim()} onClick={importCurl}><FileInput size={14} />{t('common.import')}</button></>}><label className="dialog-editor-label"><span>{t('http.curlPrompt')}</span><textarea autoFocus value={curlValue} spellCheck={false} onChange={(event) => setCurlValue(event.target.value)} /></label></Dialog>
    </section>
  )
}

function KeyValueEditor({ entries, onChange }: { entries: KeyValueEntry[]; onChange: (entries: KeyValueEntry[]) => void }) {
  const { t } = useI18n()
  function update(id: string, patch: Partial<KeyValueEntry>) { onChange(entries.map((item) => item.id === id ? { ...item, ...patch } : item)) }
  return <div className="http-entry-table"><div className="http-entry-head"><span /><span>{t('http.name')}</span><span>{t('http.value')}</span><button className="icon-button" type="button" aria-label={t('common.add')} onClick={() => onChange([...entries, entry()])}><Plus size={13} /></button></div>{entries.length === 0 ? <button className="http-add-empty" type="button" onClick={() => onChange([entry()])}><Plus size={14} />{t('http.addEntry')}</button> : entries.map((item) => <div className="http-entry-row" key={item.id}><input type="checkbox" checked={item.enabled} aria-label={t('http.enabled')} onChange={(event) => update(item.id, { enabled: event.target.checked })} /><input value={item.name} aria-label={t('http.name')} onChange={(event) => update(item.id, { name: event.target.value })} /><input value={item.value} aria-label={t('http.value')} onChange={(event) => update(item.id, { value: event.target.value })} /><button className="icon-button" type="button" aria-label={t('common.action.delete')} onClick={() => onChange(entries.filter((entryItem) => entryItem.id !== item.id))}><X size={13} /></button></div>)}</div>
}

function CookieEditor({ entries, onChange }: { entries: HttpCookieEntry[]; onChange: (entries: HttpCookieEntry[]) => void }) {
  const { t } = useI18n()
  function update(id: string, patch: Partial<HttpCookieEntry>) { onChange(entries.map((item) => item.id === id ? { ...item, ...patch } : item)) }
  return <div className="http-cookie-table"><div className="http-cookie-head"><span /><span>{t('http.name')}</span><span>{t('http.value')}</span><span>{t('http.domain')}</span><span>{t('http.path')}</span><span>{t('http.expires')}</span><button className="icon-button" type="button" aria-label={t('common.add')} onClick={() => onChange([...entries, cookie()])}><Plus size={13} /></button></div>{entries.map((item) => <div className="http-cookie-row" key={item.id}><input type="checkbox" checked={item.enabled} aria-label={t('http.enabled')} onChange={(event) => update(item.id, { enabled: event.target.checked })} />{(['name', 'value', 'domain', 'path', 'expires'] as const).map((key) => <input key={key} value={item[key]} aria-label={t(`http.${key}` as 'http.name')} onChange={(event) => update(item.id, { [key]: event.target.value })} />)}<button className="icon-button" type="button" aria-label={t('common.action.delete')} onClick={() => onChange(entries.filter((entryItem) => entryItem.id !== item.id))}><X size={13} /></button></div>)}</div>
}

function HttpHistoryDialog({ open, onClose, onApply }: { open: boolean; onClose: () => void; onApply: (item: HttpRequestHistory) => void }) {
  const { t } = useI18n()
  const [items, setItems] = useState<HttpRequestHistory[]>([])
  const [query, setQuery] = useState('')
  const load = useCallback(async () => setItems(await window.mootool.listHttpHistory(query)), [query])
  useEffect(() => { if (open) void load() }, [load, open])
  return <Dialog title={t('http.history')} open={open} width={820} onClose={onClose} footer={<><button className="dialog-button dialog-button--danger" type="button" disabled={!items.length} onClick={() => { if (window.confirm(t('history.confirmClear'))) void window.mootool.clearHttpHistory().then(load) }}><Trash2 size={14} />{t('history.clearAll')}</button><button className="dialog-button" type="button" onClick={onClose}>{t('common.close')}</button></>}><div className="history-search"><Search size={14} /><input value={query} placeholder={t('history.search')} onChange={(event) => setQuery(event.target.value)} /></div><div className="http-history-list">{items.length === 0 ? <div className="history-empty">{t('history.empty')}</div> : items.map((item) => <article key={item.id}><button type="button" onClick={() => { onApply(item); onClose() }}><strong><em>{item.method}</em>{item.title || item.url}</strong><span>{item.status} · {item.costTime} ms · {item.createTime}</span><p>{item.url}</p></button><button className="icon-button" type="button" aria-label={t('history.delete')} onClick={() => { void window.mootool.deleteHttpHistory(item.id).then(load) }}><Trash2 size={13} /></button></article>)}</div></Dialog>
}
