import { History, Play, Star } from 'lucide-react'
import { useEffect, useMemo, useState } from 'react'
import { CronFavoritesDialog } from './CronFavoritesDialog'
import { CronFieldEditor, cronLabels } from './CronFieldEditor'
import { Dialog } from '@/shared/components/Dialog'
import { HistoryDialog } from '@/features/history/HistoryDialog'
import { ToolPageHeader, WorkspaceDragZone } from '@/shared/components/ToolPage'
import { ResizableColumns } from '@/shared/components/ResizableColumns'
import { useToolActions } from '@/shared/hooks/useToolActions'
import { useI18n } from '@/shared/i18n/I18nProvider'
import { buildCron, commonCronExpressions, cronPresets, defaultCronFields, describeCron, nextCronRuns, splitCron, type CronFields } from './cronTools'

export function CronTool() {
  const { language, t } = useI18n()
  const labels = cronLabels[language]
  const [descriptionLanguage, setDescriptionLanguage] = useState(language)
  const [activeField, setActiveField] = useState<keyof CronFields>('second')
  const [examplesOpen, setExamplesOpen] = useState(false)
  const actions = useToolActions('cron')
  const systemZone = Intl.DateTimeFormat().resolvedOptions().timeZone || 'UTC'
  const [fields, setFields] = useState<CronFields>(defaultCronFields)
  const [expression, setExpression] = useState(buildCron(defaultCronFields))
  const [timeZone, setTimeZone] = useState(systemZone)
  const [runs, setRuns] = useState<string[]>([])
  const [description, setDescription] = useState(() => describeCron(buildCron(defaultCronFields), language))
  const [error, setError] = useState('')
  const [historyOpen, setHistoryOpen] = useState(false)
  const [favoritesOpen, setFavoritesOpen] = useState(false)
  const fieldEntries = useMemo(() => ([
    ['second', t('cron.second')], ['minute', t('cron.minute')], ['hour', t('cron.hour')],
    ['day', t('cron.day')], ['month', t('cron.month')], ['week', t('cron.week')], ['year', t('cron.year')]
  ] as const), [t])

  useEffect(() => {
    const timer = setTimeout(() => {
      try {
        setRuns(nextCronRuns(expression, timeZone))
        setDescription(describeCron(expression, descriptionLanguage))
        setError('')
      } catch (caught) {
        setError(t('cron.invalid', { message: caught instanceof Error ? caught.message : String(caught) }))
        setRuns([])
        setDescription('')
      }
    }, 180)
    return () => clearTimeout(timer)
  }, [expression, timeZone, descriptionLanguage, t])

  function updateField(key: keyof CronFields, value: string): void {
    const next = { ...fields, [key]: value }
    if (key === 'day') next.week = value === '?' ? '*' : '?'
    if (key === 'week') next.day = value === '?' ? '*' : '?'
    setFields(next)
    setExpression([next.second, next.minute, next.hour, next.day, next.month, next.week, next.year].join(' ').trimEnd())
  }

  function updateExpression(value: string): void {
    setExpression(value)
    try { setFields(splitCron(value)) } catch { /* The expression can be partial while editing. */ }
  }

  function parse(): void {
    try {
      const next = nextCronRuns(expression, timeZone)
      setDescription(describeCron(expression, descriptionLanguage))
      setRuns(next)
      setError('')
      void actions.saveHistory(t('cron.nextRuns'), expression, next.join('\n'), JSON.stringify({ timeZone, descriptionLanguage }))
    } catch (caught) {
      const message = caught instanceof Error ? caught.message : String(caught)
      setError(t('cron.invalid', { message }))
      setRuns([])
    }
  }

  return (
    <section className="tool-page p3-tool">
      <ToolPageHeader title={t('cron.title')} />
      <ResizableColumns className="local-tool-shell cron-workspace" columns={2} defaultSizes={[660, 340]} minPaneWidths={[460, 260]} minimumWidth={900} storageKey="cron-builder">
        <section className="cron-builder">
          <div className="embedded-tool-heading">
            <h2>{t('cron.builder')}</h2>
            <WorkspaceDragZone />
            <button className="toolbar-button" type="button" onClick={() => setFavoritesOpen(true)} title={t('favorite.add')}><Star size={14} />{t('favorite.title')}</button>
            <button className="toolbar-button" type="button" onClick={() => setHistoryOpen(true)}><History size={14} />{t('common.action.history')}</button>
          </div>
          <div className="cron-fields">{fieldEntries.map(([key, label]) => <label key={key}><span>{label}</span><input value={fields[key]} onChange={(event) => updateField(key, event.target.value)} /></label>)}</div>
          <div className="cron-field-tabs" role="tablist">{fieldEntries.map(([key, label]) => <button type="button" role="tab" aria-selected={activeField === key} key={key} onClick={() => setActiveField(key)}>{label}</button>)}</div>
          <CronFieldEditor key={activeField} field={activeField} value={fields[activeField]} language={language} onChange={value => updateField(activeField, value)} />
          <button className="toolbar-button" type="button" onClick={() => setExamplesOpen(true)}>{labels.examples}</button>
          <div className="cron-presets"><span>{t('cron.preset')}</span>{cronPresets.map((preset) => <button type="button" key={preset.id} onClick={() => updateExpression(preset.expression)}>{t(`cron.${preset.id === 'minute' ? 'everyMinute' : preset.id === 'hour' ? 'everyHour' : preset.id === 'day' ? 'everyDay' : 'weekdays'}` as 'cron.everyMinute')}</button>)}</div>
        </section>
        <section className="cron-expression-panel">
          <label htmlFor="cron-expression">{t('cron.expression')}</label>
          <input id="cron-expression" value={expression} spellCheck={false} onChange={(event) => updateExpression(event.target.value)} onKeyDown={(event) => { if (event.key === 'Enter') parse() }} />
          <select aria-label={t('time.timezone')} value={timeZone} onChange={(event) => setTimeZone(event.target.value)}>{[...new Set([systemZone, timeZone, 'UTC', 'Asia/Shanghai', 'Asia/Tokyo', 'Europe/London', 'America/New_York'])].map(zone => <option key={zone} value={zone}>{zone}</option>)}</select>
          <select aria-label={labels.locale} value={descriptionLanguage} onChange={event => setDescriptionLanguage(event.target.value as typeof language)}><option value="zh-CN">中文</option><option value="en-US">English</option><option value="ja-JP">日本語</option></select>
          <button className="toolbar-button" type="button" onClick={() => { try { describeCron(expression, descriptionLanguage); setFields(splitCron(expression)); setError('') } catch (caught) { setError(String(caught)) } }}>{labels.resolve}</button>
          <button className="toolbar-button" type="button" onClick={() => { try { const text = describeCron(expression, descriptionLanguage); setDescription(text); void actions.saveHistory(labels.convert, expression, text, JSON.stringify({ timeZone, descriptionLanguage })) } catch (caught) { setError(String(caught)) } }}>{labels.convert}</button>
          <button className="toolbar-button" type="button" onClick={() => void actions.copy(expression)}>{labels.copy}</button>
          <button className="primary-command" type="button" onClick={parse}><Play size={14} />{t('cron.parse')}</button>
          <output><span>{t('cron.humanReadable')}</span>{description}</output>
        </section>
        <section className="cron-runs"><h2>{t('cron.nextRuns')}</h2>{error ? <p className="result-status result-status--error">{error}</p> : runs.length === 0 ? <p className="empty-state">{labels.noRuns}</p> : <ol>{runs.map((run) => <li key={run}><time>{run}</time></li>)}</ol>}</section>
      </ResizableColumns>
      <CronFavoritesDialog open={favoritesOpen} currentValue={expression} onClose={() => setFavoritesOpen(false)} onApply={updateExpression} />
      <Dialog title={labels.examples} open={examplesOpen} width={760} onClose={() => setExamplesOpen(false)}><div className="cron-example-list">{commonCronExpressions.map(value => <button type="button" key={value} onClick={() => { updateExpression(value); setExamplesOpen(false) }}><code>{value}</code><span>{describeCron(value, descriptionLanguage)}</span></button>)}</div></Dialog>
      <HistoryDialog funcType="cron" open={historyOpen} onClose={() => setHistoryOpen(false)} onApply={updateExpression} onApplyRecord={(record) => { updateExpression(record.inputText); try { const meta = JSON.parse(record.extraData ?? '{}') as { timeZone?: string; descriptionLanguage?: typeof language }; if (meta.timeZone) setTimeZone(meta.timeZone); if (meta.descriptionLanguage) setDescriptionLanguage(meta.descriptionLanguage) } catch { /* Older history has no metadata. */ } }} />
    </section>
  )
}
