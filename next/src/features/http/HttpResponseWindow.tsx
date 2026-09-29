import { useEffect, useRef, useState } from 'react'
import { TextCodeEditor, type TextCodeEditorHandle } from '@/shared/components/TextCodeEditor'
import { ToolTabs } from '@/shared/components/ToolPage'
import { defaultFindReplaceOptions, findNextMatchWithWrap } from '@/shared/components/findReplace'
import type { HttpResponseResult } from '@/shared/contracts/network'
import { useToolActions } from '@/shared/hooks/useToolActions'
import { useI18n } from '@/shared/i18n/I18nProvider'

export function HttpResponseWindow() {
  const { t } = useI18n()
  const actions = useToolActions('http')
  const [response, setResponse] = useState<HttpResponseResult | null>(null)
  const [tab, setTab] = useState<'body' | 'headers' | 'cookies'>('body')
  const [find, setFind] = useState('')
  const editor = useRef<TextCodeEditorHandle>(null)
  useEffect(() => { void window.mootool.getHttpResponseSnapshot().then(setResponse).catch(actions.reportError) }, [])
  const text = response?.[tab] ?? ''
  function next() {
    const result = findNextMatchWithWrap(text, find, defaultFindReplaceOptions, editor.current?.getSelection().end ?? 0, true)
    if (result?.match) editor.current?.selectRange(result.match.start, result.match.end)
  }
  return <section className="http-response-window">
    <header><strong>{response?.url}</strong><span>{response?.status || response?.errorCode} {response?.statusText} · {response?.durationMs} ms</span></header>
    <ToolTabs tabs={(['body', 'headers', 'cookies'] as const).map(id => ({ id, label: t(`http.response.${id}`) }))} active={tab} onChange={setTab} />
    <div className="http-response-window__actions"><input aria-label={t('http.find')} placeholder={t('http.find')} value={find} onChange={event => setFind(event.target.value)} onKeyDown={event => { if (event.key === 'Enter') next() }} /><button className="toolbar-button" onClick={next}>{t('http.find')}</button><button className="toolbar-button" onClick={() => void actions.copy(text)}>{t('common.action.copy')}</button></div>
    <TextCodeEditor ref={editor} readOnly ariaLabel={t(`http.response.${tab}`)} value={text} language={tab === 'body' && /^[\s]*[\[{]/.test(text) ? 'json' : 'text'} searchQuery={find} />
  </section>
}
