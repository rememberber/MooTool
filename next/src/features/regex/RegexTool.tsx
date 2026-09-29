import { History, Play, Star } from 'lucide-react'
import { useEffect, useMemo, useRef, useState } from 'react'
import { ManagedFavoritesDialog } from '@/features/favorites/ManagedFavoritesDialog'
import type { RegexEngine, RegexResult } from '@/shared/contracts/regex'
import { regexMessages } from './regexMessages'
import { HistoryDialog } from '@/features/history/HistoryDialog'
import { ToolPageHeader, ToolTabs } from '@/shared/components/ToolPage'
import { ResizableColumns } from '@/shared/components/ResizableColumns'
import { TextCodeEditor, type TextCodeEditorHandle } from '@/shared/components/TextCodeEditor'
import { useToolActions } from '@/shared/hooks/useToolActions'
import { useI18n } from '@/shared/i18n/I18nProvider'
import { commonRegexes, type RegexMatch, type RegexOptions } from './regexTools'

type RegexTab = 'test' | 'common'

export function RegexTool() {
  const { t, language } = useI18n()
  const words = regexMessages[language]
  const editor = useRef<TextCodeEditorHandle>(null)
  const worker = useRef<Worker | null>(null)
  const generation = useRef(0)
  const debounceTimer = useRef<ReturnType<typeof setTimeout> | undefined>(undefined)
  const [engine, setEngine] = useState<RegexEngine>(() => readDraft().engine)
  const [busy, setBusy] = useState(false)
  const [limited, setLimited] = useState(false)
  const [selected, setSelected] = useState(-1)
  const actions = useToolActions('regex')
  const [tab, setTab] = useState<RegexTab>('test')
  const [pattern, setPattern] = useState(() => readDraft().pattern)
  const [source, setSource] = useState(() => readDraft().source)
  const [options, setOptions] = useState<RegexOptions>(() => readDraft().options)
  const [matches, setMatches] = useState<RegexMatch[]>([])
  const [error, setError] = useState('')
  const [historyOpen, setHistoryOpen] = useState(false)
  const [favoritesOpen, setFavoritesOpen] = useState(false)

  const decorations = useMemo(() => matches.filter(match => match.value.length).map(match => ({ from: match.index, to: match.index + match.value.length, type: 'mark' as const, className: 'regex-match-highlight' })), [matches])

  function focusMatch(index: number): void {
    if (!matches.length) return
    const next = (index + matches.length) % matches.length
    setSelected(next)
    const match = matches[next]
    editor.current?.selectRange(match.index, match.index + match.value.length)
    editor.current?.focus()
  }

  async function run(saveHistory = true): Promise<void> {
    clearTimeout(debounceTimer.current)
    const version = ++generation.current
    worker.current?.terminate()
    setBusy(true); setError(''); setMatches([]); setLimited(false); setSelected(-1)
    try {
      await window.mootool.cancelJavaRegex()
      if (version !== generation.current) return
      if (source.length > 1_000_000 || pattern.length > 16000) throw new Error('Pattern ≤ 16000, text ≤ 1000000 characters')
      let result: RegexResult
      if (engine === 'java') result = await window.mootool.matchJavaRegex({ pattern, source, options })
      else result = await new Promise<RegexResult>((resolve, reject) => {
        const task = new Worker(new URL('./regex.worker.ts', import.meta.url), { type: 'module' })
        worker.current = task
        const timer = setTimeout(() => { task.terminate(); reject(new Error('REGEX_TIMEOUT')) }, 2000)
        task.onmessage = event => { clearTimeout(timer); task.terminate(); event.data.error ? reject(new Error(event.data.error)) : resolve(event.data) }
        task.onerror = event => { clearTimeout(timer); task.terminate(); reject(new Error(event.message)) }
        task.postMessage({ pattern, source, options })
      })
      if (version !== generation.current) return
      setMatches(result.matches); setLimited(result.limited)
      if (saveHistory) void actions.saveHistory(t('regex.matches', { count: String(result.matches.length) }), pattern, source, JSON.stringify({ options, engine, matchCount: result.matches.length }))
    } catch (caught) {
      if (version !== generation.current) return
      const message = String(caught)
      setError(message.includes('REGEX_TIMEOUT') ? words.timeout : t('regex.invalid', { message }))
    } finally { if (version === generation.current) setBusy(false) }
  }

  useEffect(() => {
    setMatches([]); setSelected(-1); setError(''); setLimited(false)
    try { localStorage.setItem('mootool.regex.draft', JSON.stringify({ pattern, source, options, engine })) } catch { /* Storage may be full. */ }
    debounceTimer.current = setTimeout(() => { void run(false) }, 300)
    return () => {
      clearTimeout(debounceTimer.current); generation.current++; worker.current?.terminate()
      void window.mootool.cancelJavaRegex().catch(() => undefined)
    }
  }, [pattern, source, options, engine])

  function setOption(key: keyof RegexOptions, value: boolean): void {
    setOptions((current) => ({ ...current, [key]: value }))
  }

  return (
    <section className="tool-page p3-tool">
      <ToolPageHeader title={t('regex.title')} />
      <div className="local-tool-shell regex-workspace">
        <ToolTabs tabs={[{ id: 'test', label: t('regex.tab.test') }, { id: 'common', label: t('regex.tab.common') }]} active={tab} onChange={setTab} windowDrag actions={<><button className="toolbar-button" type="button" onClick={() => setFavoritesOpen(true)}><Star size={14} />{t('favorite.title')}</button><button className="toolbar-button" type="button" onClick={() => setHistoryOpen(true)}><History size={14} />{t('common.action.history')}</button></>} />
        {tab === 'test' ? (
          <ResizableColumns className="regex-test-layout" columns={2} defaultSizes={[710, 290]} minPaneWidths={[320, 220]} paneSelector=".regex-source, .regex-results" storageKey="regex-test">
            <section className="regex-controls">
              <label htmlFor="regex-expression">{t('regex.expression')}</label>
              <div className="regex-expression-row"><input id="regex-expression" value={pattern} spellCheck={false} onChange={(event) => setPattern(event.target.value)} onKeyDown={(event) => { if (event.key === 'Enter') void run() }} /><button className="primary-command" type="button" onClick={() => void run()}><Play size={14} />{t('regex.tab.test')}</button></div>
              <label className="regex-engine">{words.engine}<select aria-label={words.engine} value={engine} onChange={event => setEngine(event.target.value as RegexEngine)}><option value="java">{words.java}</option><option value="javascript">{words.js}</option></select></label>
              {engine === 'java' && <p className="field-hint">{words.hint}</p>}
              <div className="checkbox-row">
                <Check label={t('regex.flag.global')} checked={options.global} onChange={(value) => setOption('global', value)} />
                <Check label={t('regex.flag.ignoreCase')} checked={options.ignoreCase} onChange={(value) => setOption('ignoreCase', value)} />
                <Check label={t('regex.flag.multiline')} checked={options.multiline} onChange={(value) => setOption('multiline', value)} />
                <Check label={t('regex.flag.dotAll')} checked={options.dotAll} onChange={(value) => setOption('dotAll', value)} />
              </div>
            </section>
            <div className="regex-source"><span>{t('regex.source')}</span><TextCodeEditor ref={editor} decorations={decorations} ariaLabel={t('regex.source')} value={source} onChange={setSource} /></div>
            <section className="regex-results">
              <header className={error ? 'result-status result-status--error' : 'result-status'}>{busy ? words.working : error || t('regex.matches', { count: String(matches.length) })}</header>
              <div className="regex-navigation"><button type="button" className="toolbar-button" disabled={!matches.length} onClick={() => focusMatch(selected < 0 ? matches.length - 1 : selected - 1)}>{words.previous}</button><button type="button" className="toolbar-button" disabled={!matches.length} onClick={() => focusMatch(selected + 1)}>{words.next}</button></div>
              {limited && <p role="status">{words.limited}</p>}
              {matches.length === 0 && !error && !busy ? <p className="empty-state">{t('regex.noMatches')}</p> : matches.map((match, index) => <article key={`${match.index}-${index}`} role="button" tabIndex={0} aria-label={`#${index + 1}: ${match.value || '∅'}`} onClick={() => focusMatch(index)} onKeyDown={event => { if (event.key === 'Enter' || event.key === ' ') { event.preventDefault(); focusMatch(index) } }}><span>#{index + 1} · {match.index}</span><strong>{match.value || '∅'}</strong>{match.groups.length > 0 && <code>{match.groups.join(' · ')}</code>}</article>)}
            </section>
          </ResizableColumns>
        ) : (
          <div className="common-pattern-list">{commonRegexes.map((item) => <button type="button" key={item.id} onClick={() => { setPattern(item.pattern); setTab('test') }}><strong>{t(item.labelKey)}</strong><code>{item.pattern}</code></button>)}</div>
        )}
      </div>
      <ManagedFavoritesDialog kind="regex" open={favoritesOpen} currentValue={pattern} onClose={() => setFavoritesOpen(false)} onApply={(value) => { setPattern(value); setTab('test') }} />
      <HistoryDialog funcType="regex" open={historyOpen} onClose={() => setHistoryOpen(false)} onApply={(value) => setSource(value)} onApplyRecord={(record) => {
        setPattern(record.inputText)
        setSource(record.outputText)
        try {
          const meta = JSON.parse(record.extraData ?? '{}') as { options?: RegexOptions; engine?: RegexEngine }
          if (meta.options) setOptions(meta.options)
          setEngine(meta.engine === 'javascript' ? 'javascript' : 'java')
        } catch { setMatches([]) }
      }} />
    </section>
  )
}

function Check({ label, checked, onChange }: { label: string; checked: boolean; onChange: (value: boolean) => void }) {
  return <label><input type="checkbox" checked={checked} onChange={(event) => onChange(event.target.checked)} />{label}</label>
}

function readDraft(): { pattern: string; source: string; options: RegexOptions; engine: RegexEngine } {
  const fallback = { pattern: '(moo)(\\d+)', source: 'moo1\nMOO22\nmoo333', options: { global: true, ignoreCase: false, multiline: false, dotAll: false }, engine: 'java' as const }
  try {
    const value = JSON.parse(localStorage.getItem('mootool.regex.draft') ?? 'null')
    if (!value || typeof value.pattern !== 'string' || typeof value.source !== 'string') return fallback
    return { pattern: value.pattern, source: value.source, engine: value.engine === 'javascript' ? 'javascript' : 'java', options: { global: value.options?.global !== false, ignoreCase: value.options?.ignoreCase === true, multiline: value.options?.multiline === true, dotAll: value.options?.dotAll === true } }
  } catch { return fallback }
}
