import { invoke } from '@tauri-apps/api/core'
import { EditorView } from '@codemirror/view'
import { useRef, useState } from 'react'
import { useLocalizedMessages } from '../../app/localizedMessages'
import { clipboardApi } from '../../platform/api/clipboardApi'
import type { HttpResponseData } from '../../platform/contracts/http'
import { CodeEditor } from '../../shared/CodeEditor'
import { defaultFindReplaceOptions, findNextMatchWithWrap } from '../../shared/findReplace'
import { httpMessages } from './httpMessages'

export const HTTP_RESPONSE_WINDOW_KEY = 'mootool-next-tauri:http-response-window'

export interface HttpResponseWindowSnapshot {
  url: string
  status: number
  statusText: string
  durationMs: number
  body: string
  headers: string
  cookies: string
}

type ResponsePane = 'body' | 'headers' | 'cookies'

export function snapshotFromResponse(response: HttpResponseData): HttpResponseWindowSnapshot {
  return {
    url: response.finalUrl,
    status: response.status,
    statusText: response.statusText ?? '',
    durationMs: response.durationMs,
    body: response.bodyText || response.bodyBase64,
    headers: response.headers.map(([name, value]) => `${name}: ${value}`).join('\n'),
    cookies: response.headers.filter(([name]) => name.toLowerCase() === 'set-cookie').map(([, value]) => value).join('\n')
  }
}

export async function openHttpResponseWindow(response: HttpResponseData): Promise<void> {
  localStorage.setItem(HTTP_RESPONSE_WINDOW_KEY, JSON.stringify(snapshotFromResponse(response)))
  if ('__TAURI_INTERNALS__' in window) {
    await invoke('open_http_response_window')
    return
  }
  window.open(`${window.location.pathname}?surface=http-response`, 'mootool-http-response', 'popup,width=1000,height=760')
}

function readSnapshot(): HttpResponseWindowSnapshot | null {
  try {
    const raw = localStorage.getItem(HTTP_RESPONSE_WINDOW_KEY)
    if (!raw) return null
    const value = JSON.parse(raw) as HttpResponseWindowSnapshot
    return value && typeof value.body === 'string' && typeof value.url === 'string' ? value : null
  } catch {
    return null
  }
}

export function HttpResponseWindow() {
  const { t } = useLocalizedMessages(httpMessages)
  const [snapshot] = useState(readSnapshot)
  const [pane, setPane] = useState<ResponsePane>('body')
  const [find, setFind] = useState('')
  const editor = useRef<EditorView | undefined>(undefined)
  const text = snapshot?.[pane] ?? ''
  const paneLabel = { body: t('tab.responseBody'), headers: t('tab.responseHeaders'), cookies: t('tab.cookies') }

  function findNext(): void {
    const view = editor.current
    const from = view ? Math.max(view.state.selection.main.anchor, view.state.selection.main.head) : 0
    const result = findNextMatchWithWrap(text, find, defaultFindReplaceOptions, from, true)
    if (!result?.match || !view) return
    view.dispatch({
      selection: { anchor: result.match.start, head: result.match.end },
      scrollIntoView: true
    })
  }

  return (
    <section className="http-response-window">
      <header>
        <strong>{snapshot?.url || t('response.empty')}</strong>
        <span>{snapshot ? `${snapshot.status} ${snapshot.statusText} · ${snapshot.durationMs} ms` : ''}</span>
      </header>
      <div className="protobuf-tabs" role="tablist">
        {(['body', 'headers', 'cookies'] as const).map((id) => (
          <button key={id} type="button" role="tab" aria-selected={pane === id} onClick={() => setPane(id)}>{paneLabel[id]}</button>
        ))}
      </div>
      <div className="http-response-window__actions">
        <input aria-label={t('find.placeholder')} placeholder={t('find.placeholder')} value={find} onChange={(event) => setFind(event.target.value)} onKeyDown={(event) => { if (event.key === 'Enter') findNext() }} />
        <button type="button" disabled={!find} onClick={findNext}>{t('action.find')}</button>
        <button type="button" disabled={!text} onClick={() => void clipboardApi.writeText(text)}>{t('action.copyResponse')}</button>
      </div>
      <CodeEditor ariaLabel={t('aria.response')} value={text} readOnly className="utility-code-editor" searchQuery={find} onReady={(view) => { editor.current = view }} />
    </section>
  )
}
