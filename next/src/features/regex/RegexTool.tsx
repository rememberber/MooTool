import { History, Play, Star } from 'lucide-react'
import { useState } from 'react'
import { FavoriteDialog } from '@/features/favorites/FavoriteDialog'
import { HistoryDialog } from '@/features/history/HistoryDialog'
import { ToolPageHeader, ToolTabs } from '@/shared/components/ToolPage'
import { ResizableColumns } from '@/shared/components/ResizableColumns'
import { TextCodeEditor } from '@/shared/components/TextCodeEditor'
import { useToolActions } from '@/shared/hooks/useToolActions'
import { useI18n } from '@/shared/i18n/I18nProvider'
import { commonRegexes, matchRegex, type RegexMatch, type RegexOptions } from './regexTools'

type RegexTab = 'test' | 'common'

export function RegexTool() {
  const { t } = useI18n()
  const actions = useToolActions('regex')
  const [tab, setTab] = useState<RegexTab>('test')
  const [pattern, setPattern] = useState('(moo)(\\d+)')
  const [source, setSource] = useState('moo1\nMOO22\nmoo333')
  const [options, setOptions] = useState<RegexOptions>({ global: true, ignoreCase: false, multiline: false, dotAll: false })
  const [matches, setMatches] = useState<RegexMatch[]>([])
  const [error, setError] = useState('')
  const [historyOpen, setHistoryOpen] = useState(false)
  const [favoritesOpen, setFavoritesOpen] = useState(false)

  function run(): void {
    try {
      const next = matchRegex(pattern, source, options)
      setMatches(next)
      setError('')
      void actions.saveHistory(t('regex.matches', { count: String(next.length) }), pattern, source, JSON.stringify({ options, matchCount: next.length }))
    } catch (caught) {
      const message = caught instanceof Error ? caught.message : String(caught)
      setError(t('regex.invalid', { message }))
      setMatches([])
    }
  }

  function setOption(key: keyof RegexOptions, value: boolean): void {
    setOptions((current) => ({ ...current, [key]: value }))
  }

  return (
    <section className="tool-page p3-tool">
      <ToolPageHeader title={t('regex.title')} actions={<><button className="toolbar-button" type="button" onClick={() => setFavoritesOpen(true)}><Star size={14} />{t('favorite.title')}</button><button className="toolbar-button" type="button" onClick={() => setHistoryOpen(true)}><History size={14} />{t('common.action.history')}</button></>} />
      <div className="local-tool-shell regex-workspace">
        <ToolTabs tabs={[{ id: 'test', label: t('regex.tab.test') }, { id: 'common', label: t('regex.tab.common') }]} active={tab} onChange={setTab} />
        {tab === 'test' ? (
          <ResizableColumns className="regex-test-layout" columns={2} defaultSizes={[710, 290]} minPaneWidths={[320, 220]} paneSelector=".regex-source, .regex-results" storageKey="regex-test">
            <section className="regex-controls">
              <label htmlFor="regex-expression">{t('regex.expression')}</label>
              <div className="regex-expression-row"><input id="regex-expression" value={pattern} spellCheck={false} onChange={(event) => setPattern(event.target.value)} onKeyDown={(event) => { if (event.key === 'Enter') run() }} /><button className="primary-command" type="button" onClick={run}><Play size={14} />{t('regex.tab.test')}</button></div>
              <div className="checkbox-row">
                <Check label={t('regex.flag.global')} checked={options.global} onChange={(value) => setOption('global', value)} />
                <Check label={t('regex.flag.ignoreCase')} checked={options.ignoreCase} onChange={(value) => setOption('ignoreCase', value)} />
                <Check label={t('regex.flag.multiline')} checked={options.multiline} onChange={(value) => setOption('multiline', value)} />
                <Check label={t('regex.flag.dotAll')} checked={options.dotAll} onChange={(value) => setOption('dotAll', value)} />
              </div>
            </section>
            <div className="regex-source"><span>{t('regex.source')}</span><TextCodeEditor ariaLabel={t('regex.source')} value={source} onChange={setSource} /></div>
            <section className="regex-results">
              <header className={error ? 'result-status result-status--error' : 'result-status'}>{error || t('regex.matches', { count: String(matches.length) })}</header>
              {matches.length === 0 && !error ? <p className="empty-state">{t('regex.noMatches')}</p> : matches.map((match, index) => <article key={`${match.index}-${index}`}><span>#{index + 1} · {match.index}</span><strong>{match.value || '∅'}</strong>{match.groups.length > 0 && <code>{match.groups.join(' · ')}</code>}</article>)}
            </section>
          </ResizableColumns>
        ) : (
          <div className="common-pattern-list">{commonRegexes.map((item) => <button type="button" key={item.id} onClick={() => { setPattern(item.pattern); setTab('test') }}><strong>{t(item.labelKey)}</strong><code>{item.pattern}</code></button>)}</div>
        )}
      </div>
      <FavoriteDialog kind="regex" open={favoritesOpen} currentValue={pattern} onClose={() => setFavoritesOpen(false)} onApply={(value) => { setPattern(value); setTab('test') }} />
      <HistoryDialog funcType="regex" open={historyOpen} onClose={() => setHistoryOpen(false)} onApply={(value) => setSource(value)} onApplyRecord={(record) => {
        setPattern(record.inputText)
        setSource(record.outputText)
        try {
          const meta = JSON.parse(record.extraData ?? '{}') as { options?: RegexOptions }
          const nextOptions = meta.options ?? options
          if (meta.options) setOptions(meta.options)
          setMatches(matchRegex(record.inputText, record.outputText, nextOptions))
        } catch { setMatches([]) }
      }} />
    </section>
  )
}

function Check({ label, checked, onChange }: { label: string; checked: boolean; onChange: (value: boolean) => void }) {
  return <label><input type="checkbox" checked={checked} onChange={(event) => onChange(event.target.checked)} />{label}</label>
}
