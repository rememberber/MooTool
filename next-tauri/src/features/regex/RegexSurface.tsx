import {
  CheckCircle2,
  Copy,
  Eraser,
  Play,
  Replace,
  Search,
  TriangleAlert
} from 'lucide-react'
import { useEffect, useMemo, useRef, useState } from 'react'
import { useLocalizedMessages } from '../../app/localizedMessages'
import { clipboardApi } from '../../platform/api/clipboardApi'
import { javaRegexApi } from '../../platform/api/javaRegexApi'
import { DiffCodeEditor, type DiffEditorDecoration, type DiffEditorHandle } from '../../shared/DiffCodeEditor'
import { contentFingerprint } from '../../shared/fingerprint'
import { useToolSessionReport } from '../toolWebview/useToolSessionReport'
import { ToolFavoriteBar } from '../favorites/ToolFavoriteBar'
import { useOperationHistory } from '../history/useOperationHistory'
import { parseOperationMetadata, useOperationRestore } from '../history/operationRestore'
import {
  commonRegexes,
  regexFlags,
  replaceRegex,
  type RegexMatch,
  type RegexOptions
} from './regexTools'
import { regexMessages } from './regexMessages'

type RegexTab = 'test' | 'common'
type RegexEngine = 'java' | 'javascript'

const regexDraftKey = 'mootool.regex.draft'
const defaultOptions: RegexOptions = {
  global: true,
  ignoreCase: false,
  multiline: false,
  dotAll: false,
  unicode: true
}

function readRegexDraft(): { pattern: string; source: string; replacement: string; options: RegexOptions; engine: RegexEngine } {
  const fallback = {
    pattern: '(moo)(\\d+)',
    source: 'moo1\nMOO22\nmoo333',
    replacement: '$1-$2',
    options: defaultOptions,
    engine: 'javascript' as const
  }
  try {
    const value = JSON.parse(window.localStorage.getItem(regexDraftKey) ?? 'null') as {
      pattern?: unknown
      source?: unknown
      replacement?: unknown
      engine?: unknown
      options?: Partial<RegexOptions>
    } | null
    if (!value || typeof value.pattern !== 'string' || typeof value.source !== 'string') return fallback
    return {
      pattern: value.pattern,
      source: value.source,
      replacement: typeof value.replacement === 'string' ? value.replacement : fallback.replacement,
      engine: value.engine === 'java' ? 'java' : 'javascript',
      options: {
        global: value.options?.global !== false,
        ignoreCase: value.options?.ignoreCase === true,
        multiline: value.options?.multiline === true,
        dotAll: value.options?.dotAll === true,
        unicode: value.options?.unicode !== false
      }
    }
  } catch {
    return fallback
  }
}

export function RegexSurface() {
  const { t } = useLocalizedMessages(regexMessages)
  const initialDraft = useMemo(readRegexDraft, [])
  const [tab, setTab] = useState<RegexTab>('test')
  const [pattern, setPattern] = useState(initialDraft.pattern)
  const [source, setSource] = useState(initialDraft.source)
  const [replacement, setReplacement] = useState(initialDraft.replacement)
  const [options, setOptions] = useState(initialDraft.options)
  const [engine, setEngine] = useState<RegexEngine>(initialDraft.engine)
  const [matches, setMatches] = useState<RegexMatch[]>([])
  const [error, setError] = useState('')
  const [limited, setLimited] = useState(false)
  const [busy, setBusy] = useState(false)
  const [selected, setSelected] = useState(-1)
  const [hasRun, setHasRun] = useState(false)
  const [patternFilter, setPatternFilter] = useState('')
  const [copied, setCopied] = useState(false)
  const [copyFailed, setCopyFailed] = useState(false)
  const replacementPreview = useMemo(() => {
    if (!hasRun || error) return source
    try {
      return replaceRegex(pattern, source, replacement, options)
    } catch {
      return source
    }
  }, [error, hasRun, options, pattern, replacement, source])
  const session = useMemo(() => ({
    digest: JSON.stringify({
      pattern,
      sourceLength: source.length,
      sourceHash: contentFingerprint(source),
      replacement,
      options,
      matches: matches.length,
      error: Boolean(error)
    }),
    summary: t('session.summary', { flags: regexFlags(options), count: matches.length, error: error ? t('session.error') : '' })
  }), [error, matches.length, options, pattern, replacement, source, t])
  const { sessionId, reportError } = useToolSessionReport('regex', session.digest, session.summary)
  const recordOperation = useOperationHistory('regex')
  const editorRef = useRef<DiffEditorHandle>(null)
  const requestSeq = useRef(0)
  const workerRef = useRef<Worker | null>(null)
  const decorations = useMemo<DiffEditorDecoration[]>(() => matches.filter((match) => match.value.length).map((match) => ({
    from: match.index,
    to: match.index + match.value.length,
    type: 'mark',
    className: 'regex-match-highlight'
  })), [matches])
  useEffect(() => {
    const timer = window.setTimeout(() => {
      try {
        window.localStorage.setItem(regexDraftKey, JSON.stringify({ pattern, source, replacement, options, engine }))
      } catch {
        // Draft persistence is best effort when storage is unavailable.
      }
    }, 250)
    return () => window.clearTimeout(timer)
  }, [engine, options, pattern, replacement, source])
  useEffect(() => {
    const timer = window.setTimeout(() => { void run(false) }, 300)
    return () => {
      window.clearTimeout(timer)
      requestSeq.current += 1
      workerRef.current?.terminate()
      workerRef.current = null
      void javaRegexApi.cancel()
    }
  }, [engine, options, pattern, source])
  useOperationRestore('regex', (entry) => {
    const metadata = parseOperationMetadata(entry)
    if (typeof metadata.pattern === 'string') setPattern(metadata.pattern)
    if (typeof metadata.replacement === 'string') setReplacement(metadata.replacement)
    if (metadata.options && typeof metadata.options === 'object') setOptions({ ...defaultOptions, ...metadata.options as Partial<RegexOptions> })
    setSource(entry.inputText)
    setMatches([])
    setHasRun(false)
    setError('')
    setTab('test')
  })
  const filteredPatterns = commonRegexes.filter((item) => (
    !patternFilter.trim()
    || `${t(`common.${item.id}`)} ${item.pattern}`.toLowerCase().includes(patternFilter.trim().toLowerCase())
  ))

  async function run(saveHistory = true): Promise<void> {
    const version = ++requestSeq.current
    setBusy(true)
    try {
      let result: RegexMatch[]
      let reachedLimit = false
      if (engine === 'java') {
        const javaResult = await javaRegexApi.match({ pattern, source, options })
        result = javaResult.matches.map((match) => ({
          index: match.index,
          end: match.index + match.value.length,
          value: match.value,
          groups: match.groups,
          namedGroups: {}
        }))
        reachedLimit = javaResult.limited
      } else {
        const scriptResult = await matchJavaScriptRegex(pattern, source, options, workerRef)
        result = scriptResult.matches
        reachedLimit = scriptResult.limited
      }
      if (version !== requestSeq.current) return
      setMatches(result)
      setSelected(-1)
      setLimited(reachedLimit)
      setError('')
      setHasRun(true)
      if (saveHistory) {
        let outputText = source
        try { outputText = replaceRegex(pattern, source, replacement, options) } catch { outputText = source }
        recordOperation(t('action.run'), `/${pattern}/${regexFlags(options)} · ${result.length}`, 'success', {
          inputText: source,
          outputText,
          metadata: { pattern, replacement, options, engine }
        })
      }
    } catch (cause) {
      if (version !== requestSeq.current) return
      const message = cause instanceof Error ? cause.message : String(cause)
      if (message.includes('REGEX_CANCELLED')) return
      const timedOut = message.includes('REGEX_TIMEOUT')
      const display = timedOut
        ? t('match.timeout')
        : message.includes('浏览器预览不能运行 Java')
          ? t('engine.hint')
          : message
      setMatches([])
      setSelected(-1)
      setLimited(false)
      setError(timedOut ? 'REGEX_TIMEOUT' : display)
      setHasRun(true)
      if (saveHistory) {
        recordOperation(t('action.run'), `/${pattern}/${regexFlags(options)} · ${display}`, 'error', {
          inputText: source,
          metadata: { pattern, replacement, options, engine }
        })
      }
    } finally {
      if (version === requestSeq.current) setBusy(false)
    }
  }

  function focusMatch(index: number): void {
    if (!matches.length) return
    const next = (index + matches.length) % matches.length
    const match = matches[next]
    setSelected(next)
    editorRef.current?.selectRange(match.index, match.index + match.value.length)
  }

  function setOption(key: keyof RegexOptions, checked: boolean): void {
    setOptions((current) => ({ ...current, [key]: checked }))
  }

  async function copyPattern(): Promise<void> {
    try {
      await clipboardApi.writeText(pattern)
      setCopied(true)
      setCopyFailed(false)
      window.setTimeout(() => setCopied(false), 1200)
    } catch {
      setCopyFailed(true)
    }
  }

  return (
    <main className="utility-workbench regex-workbench">
      <header className="utility-header">
        <h1 className="visually-hidden">Regex</h1>
        <span className="utility-session">{t('session.label')} <code>{sessionId}</code></span>
      </header>

      <section className="utility-toolbar regex-tabs">
        <div className="utility-segments" role="tablist">
          <button
            className={tab === 'test' ? 'utility-segment utility-segment--active' : 'utility-segment'}
            type="button"
            role="tab"
            aria-selected={tab === 'test'}
            onClick={() => setTab('test')}
          >
            {t('tab.test')}
          </button>
          <button
            className={tab === 'common' ? 'utility-segment utility-segment--active' : 'utility-segment'}
            type="button"
            role="tab"
            aria-selected={tab === 'common'}
            onClick={() => setTab('common')}
          >
            {t('tab.common')}
          </button>
        </div>
        <ToolFavoriteBar
          toolId="regex"
          defaultName={pattern || 'Regex'}
          payload={{ pattern, source, replacement, options }}
          onApply={(payload) => {
            if (typeof payload.pattern === 'string') setPattern(payload.pattern)
            if (typeof payload.source === 'string') setSource(payload.source)
            if (typeof payload.replacement === 'string') setReplacement(payload.replacement)
            if (payload.options && typeof payload.options === 'object' && !Array.isArray(payload.options)) {
              setOptions({ ...defaultOptions, ...payload.options as Partial<RegexOptions> })
            }
            setMatches([])
            setHasRun(false)
            setError('')
            setTab('test')
          }}
        />
        <button
          className="icon-button"
          type="button"
          aria-label={t('action.clear')}
          onClick={() => {
            setPattern('')
            setSource('')
            setMatches([])
            setHasRun(false)
            setError('')
          }}
        >
          <Eraser />
        </button>
      </section>

      {tab === 'test'
        ? (
            <section className="regex-test">
              <div className="regex-expression">
                <label htmlFor="regex-pattern">{t('field.pattern')}</label>
                <div>
                  <span>/</span>
                  <input
                    id="regex-pattern"
                    value={pattern}
                    spellCheck={false}
                    onChange={(event) => {
                      setPattern(event.target.value)
                      setHasRun(false)
                    }}
                    onKeyDown={(event) => event.key === 'Enter' && run()}
                  />
                  <span>/{regexFlags(options)}</span>
                  <button className="secondary-button" type="button" onClick={() => void copyPattern()}>
                    <Copy />{t(copied ? 'action.copied' : 'action.copy')}
                  </button>
                  <button className="primary-button" type="button" onClick={() => run()}>
                    <Play />{t('action.run')}
                  </button>
                </div>
              </div>
              <div className="regex-flags" aria-label={t('aria.flags')}>
                <label className="regex-engine">{t('engine.label')}
                  <select aria-label={t('engine.label')} value={engine} onChange={(event) => setEngine(event.target.value as RegexEngine)}>
                    <option value="java">{t('engine.java')}</option>
                    <option value="javascript">{t('engine.javascript')}</option>
                  </select>
                </label>
                <Flag label={t('flag.global')} checked={options.global} onChange={(value) => setOption('global', value)} />
                <Flag label={t('flag.ignoreCase')} checked={options.ignoreCase} onChange={(value) => setOption('ignoreCase', value)} />
                <Flag label={t('flag.multiline')} checked={options.multiline} onChange={(value) => setOption('multiline', value)} />
                <Flag label={t('flag.dotAll')} checked={options.dotAll} onChange={(value) => setOption('dotAll', value)} />
                <Flag label="u · Unicode" checked={options.unicode} onChange={(value) => setOption('unicode', value)} />
              </div>
              {engine === 'java' && <p className="regex-engine-hint">{t('engine.hint')}</p>}
              <div className="regex-grid">
                <section className="utility-editor-card regex-source">
                  <header><span>{t('pane.source')}</span><code>{t('pane.characters', { count: source.length })}</code></header>
                  <DiffCodeEditor
                    ref={editorRef}
                    ariaLabel={t('aria.source')}
                    className="utility-code-editor regex-source-editor"
                    decorations={decorations}
                    readOnly={false}
                    value={source}
                    onChange={(value) => setSource(value)}
                  />
                </section>
                <section className="regex-results">
                  <header className={error ? 'regex-results__status regex-results__status--error' : 'regex-results__status'}>
                    {error
                      ? <><TriangleAlert />{error === 'REGEX_TIMEOUT' ? t('match.timeout') : t('result.invalid', { error })}</>
                      : <><CheckCircle2 />{busy ? t('match.working') : hasRun ? t('result.matches', { count: matches.length }) : t('result.waiting')}</>}
                  </header>
                  <div className="regex-navigation">
                    <button type="button" disabled={!matches.length} onClick={() => focusMatch(selected < 0 ? matches.length - 1 : selected - 1)}>{t('match.previous')}</button>
                    <button type="button" disabled={!matches.length} onClick={() => focusMatch(selected + 1)}>{t('match.next')}</button>
                  </div>
                  {limited && <p role="status">{t('match.limited')}</p>}
                  <div className="regex-match-list">
                    {hasRun && !error && matches.length === 0 && (
                      <p className="regex-empty">{t('result.empty')}</p>
                    )}
                    {!hasRun && <p className="regex-empty">{t('result.instructions')}</p>}
                    {matches.map((match, index) => (
                      <article
                        className={selected === index ? 'regex-match--selected' : undefined}
                        key={`${match.index}:${match.end}:${index}`}
                        role="button"
                        tabIndex={0}
                        onClick={() => focusMatch(index)}
                        onKeyDown={(event) => {
                          if (event.key === 'Enter' || event.key === ' ') {
                            event.preventDefault()
                            focusMatch(index)
                          }
                        }}
                      >
                        <span>#{index + 1} · {match.index}–{match.end}</span>
                        <strong>{match.value || '∅'}</strong>
                        {match.groups.length > 0 && (
                          <div className="regex-groups">
                            {match.groups.map((group, groupIndex) => (
                              <code key={groupIndex}>${groupIndex + 1} {group || '∅'}</code>
                            ))}
                            {Object.entries(match.namedGroups).map(([name, group]) => (
                              <code key={name}>${`<${name}>`} {group || '∅'}</code>
                            ))}
                          </div>
                        )}
                      </article>
                    ))}
                  </div>
                </section>
              </div>
              <section className="regex-replace">
                <label>
                  <Replace />{t('field.replace')}
                  <input
                    value={replacement}
                    spellCheck={false}
                    onChange={(event) => setReplacement(event.target.value)}
                  />
                </label>
                <button
                  className="secondary-button"
                  type="button"
                  disabled={!hasRun || Boolean(error)}
                  onClick={() => {
                    setSource(replacementPreview)
                    setHasRun(false)
                  }}
                >
                  {t('action.applyReplace')}
                </button>
                <pre>{replacementPreview}</pre>
              </section>
            </section>
          )
        : (
            <section className="regex-common">
              <label className="regex-common__search">
                <Search />
                <input
                  value={patternFilter}
                  placeholder={t('search.placeholder')}
                  onChange={(event) => setPatternFilter(event.target.value)}
                />
              </label>
              <div className="regex-common__grid">
                {filteredPatterns.map((item) => (
                  <button
                    type="button"
                    key={item.id}
                    onClick={() => {
                      setPattern(item.pattern)
                      setTab('test')
                      setHasRun(false)
                    }}
                  >
                    <strong>{t(`common.${item.id}`)}</strong>
                    <code>{item.pattern}</code>
                  </button>
                ))}
              </div>
            </section>
          )}

      <footer className={error || copyFailed ? 'utility-status utility-status--error' : 'utility-status'}>
        <span>{error || copyFailed ? <TriangleAlert /> : <CheckCircle2 />}
          {copyFailed ? t('error.clipboard') : t(error ? 'status.error' : 'status.ready')}
        </span>
        <code>{session.summary}</code>
      </footer>
      {reportError && <p className="tool-surface-report-error">{t('report.error', { error: reportError })}</p>}
    </main>
  )
}

function matchJavaScriptRegex(
  pattern: string,
  source: string,
  options: RegexOptions,
  workerRef: { current: Worker | null }
): Promise<{ matches: RegexMatch[]; limited: boolean }> {
  workerRef.current?.terminate()
  return new Promise((resolve, reject) => {
    const task = new Worker(new URL('./regex.worker.ts', import.meta.url), { type: 'module' })
    workerRef.current = task
    const timer = window.setTimeout(() => {
      task.terminate()
      if (workerRef.current === task) workerRef.current = null
      reject(new Error('REGEX_TIMEOUT'))
    }, 2000)
    task.onmessage = (event: MessageEvent<{ matches?: RegexMatch[]; limited?: boolean; error?: string }>) => {
      window.clearTimeout(timer)
      task.terminate()
      if (workerRef.current === task) workerRef.current = null
      if (event.data.error) reject(new Error(event.data.error))
      else resolve({ matches: event.data.matches ?? [], limited: Boolean(event.data.limited) })
    }
    task.onerror = (event) => {
      window.clearTimeout(timer)
      task.terminate()
      if (workerRef.current === task) workerRef.current = null
      reject(new Error(event.message || 'REGEX_WORKER'))
    }
    task.postMessage({ pattern, source, options })
  })
}

function Flag({ label, checked, onChange }: {
  label: string
  checked: boolean
  onChange(checked: boolean): void
}) {
  return (
    <label>
      <input type="checkbox" checked={checked} onChange={(event) => onChange(event.target.checked)} />
      {label}
    </label>
  )
}
