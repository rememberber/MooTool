import {
  CheckCircle2,
  Copy,
  Eraser,
  Play,
  Replace,
  Search,
  TriangleAlert
} from 'lucide-react'
import { useMemo, useState } from 'react'
import { useLocalizedMessages } from '../../app/localizedMessages'
import { clipboardApi } from '../../platform/api/clipboardApi'
import { CodeEditor } from '../../shared/CodeEditor'
import { contentFingerprint } from '../../shared/fingerprint'
import { useToolSessionReport } from '../toolWebview/useToolSessionReport'
import { ToolFavoriteBar } from '../favorites/ToolFavoriteBar'
import { useOperationHistory } from '../history/useOperationHistory'
import { parseOperationMetadata, useOperationRestore } from '../history/operationRestore'
import {
  commonRegexes,
  matchRegex,
  regexFlags,
  replaceRegex,
  type RegexMatch,
  type RegexOptions
} from './regexTools'
import { regexMessages } from './regexMessages'

type RegexTab = 'test' | 'common'

const defaultOptions: RegexOptions = {
  global: true,
  ignoreCase: false,
  multiline: false,
  dotAll: false,
  unicode: true
}

export function RegexSurface() {
  const { t } = useLocalizedMessages(regexMessages)
  const [tab, setTab] = useState<RegexTab>('test')
  const [pattern, setPattern] = useState('(?<name>moo)(\\d+)')
  const [source, setSource] = useState('moo1\nMOO22\nmoo333\nMooTool Next Tauri')
  const [replacement, setReplacement] = useState('$<name>-$2')
  const [options, setOptions] = useState(defaultOptions)
  const [matches, setMatches] = useState<RegexMatch[]>([])
  const [error, setError] = useState('')
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

  function run(): void {
    try {
      const result = matchRegex(pattern, source, options)
      setMatches(result)
      setError('')
      setHasRun(true)
      recordOperation(t('action.run'), `/${pattern}/${regexFlags(options)} · ${result.length}`, 'success', {
        inputText: source,
        outputText: replacementPreview,
        metadata: { pattern, replacement, options }
      })
    } catch (cause) {
      setMatches([])
      setError(cause instanceof Error ? cause.message : String(cause))
      setHasRun(true)
      recordOperation(t('action.run'), `/${pattern}/${regexFlags(options)} · ${cause instanceof Error ? cause.message : String(cause)}`, 'error', {
        inputText: source,
        metadata: { pattern, replacement, options }
      })
    }
  }

  function setOption(key: keyof RegexOptions, checked: boolean): void {
    setOptions((current) => ({ ...current, [key]: checked }))
    setHasRun(false)
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
                  <button className="primary-button" type="button" onClick={run}>
                    <Play />{t('action.run')}
                  </button>
                </div>
              </div>
              <div className="regex-flags" aria-label={t('aria.flags')}>
                <Flag label={t('flag.global')} checked={options.global} onChange={(value) => setOption('global', value)} />
                <Flag label={t('flag.ignoreCase')} checked={options.ignoreCase} onChange={(value) => setOption('ignoreCase', value)} />
                <Flag label={t('flag.multiline')} checked={options.multiline} onChange={(value) => setOption('multiline', value)} />
                <Flag label={t('flag.dotAll')} checked={options.dotAll} onChange={(value) => setOption('dotAll', value)} />
                <Flag label="u · Unicode" checked={options.unicode} onChange={(value) => setOption('unicode', value)} />
              </div>
              <div className="regex-grid">
                <section className="utility-editor-card regex-source">
                  <header><span>{t('pane.source')}</span><code>{t('pane.characters', { count: source.length })}</code></header>
                  <CodeEditor
                    ariaLabel={t('aria.source')}
                    value={source}
                    onChange={(value) => {
                      setSource(value)
                      setHasRun(false)
                    }}
                    className="utility-code-editor"
                  />
                </section>
                <section className="regex-results">
                  <header className={error ? 'regex-results__status regex-results__status--error' : 'regex-results__status'}>
                    {error
                      ? <><TriangleAlert />{t('result.invalid', { error })}</>
                      : <><CheckCircle2 />{hasRun ? t('result.matches', { count: matches.length }) : t('result.waiting')}</>}
                  </header>
                  <div className="regex-match-list">
                    {hasRun && !error && matches.length === 0 && (
                      <p className="regex-empty">{t('result.empty')}</p>
                    )}
                    {!hasRun && <p className="regex-empty">{t('result.instructions')}</p>}
                    {matches.map((match, index) => (
                      <article key={`${match.index}:${match.end}:${index}`}>
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
