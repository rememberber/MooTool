import { History, Play, Star } from 'lucide-react'
import { useEffect, useMemo, useState } from 'react'
import { FavoriteDialog } from '@/features/favorites/FavoriteDialog'
import { HistoryDialog } from '@/features/history/HistoryDialog'
import { ToolPageHeader } from '@/shared/components/ToolPage'
import { ResizableColumns } from '@/shared/components/ResizableColumns'
import { useToolActions } from '@/shared/hooks/useToolActions'
import { useI18n } from '@/shared/i18n/I18nProvider'
import { buildCron, cronPresets, defaultCronFields, describeCron, nextCronRuns, splitCron, type CronFields } from './cronTools'

export function CronTool() {
  const { language, t } = useI18n()
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
    try { setDescription(describeCron(expression, language)) } catch { /* Partial expressions are described after parsing. */ }
  }, [expression, language])

  function updateField(key: keyof CronFields, value: string): void {
    const next = { ...fields, [key]: value }
    setFields(next)
    try { setExpression(buildCron(next)) } catch { /* Keep editing partial fields. */ }
  }

  function updateExpression(value: string): void {
    setExpression(value)
    try { setFields(splitCron(value)) } catch { /* The expression can be partial while editing. */ }
  }

  function parse(): void {
    try {
      const next = nextCronRuns(expression, timeZone)
      setDescription(describeCron(expression, language))
      setRuns(next)
      setError('')
      void actions.saveHistory(t('cron.nextRuns'), expression, next.join('\n'), JSON.stringify({ timeZone }))
    } catch (caught) {
      const message = caught instanceof Error ? caught.message : String(caught)
      setError(t('cron.invalid', { message }))
      setRuns([])
    }
  }

  return (
    <section className="tool-page p3-tool">
      <ToolPageHeader title={t('cron.title')} actions={<><button className="toolbar-button" type="button" onClick={() => setFavoritesOpen(true)}><Star size={14} />{t('favorite.title')}</button><button className="toolbar-button" type="button" onClick={() => setHistoryOpen(true)}><History size={14} />{t('common.action.history')}</button></>} />
      <ResizableColumns className="local-tool-shell cron-workspace" columns={2} defaultSizes={[660, 340]} minPaneWidths={[460, 260]} minimumWidth={900} storageKey="cron-builder">
        <section className="cron-builder">
          <h2>{t('cron.builder')}</h2>
          <div className="cron-fields">{fieldEntries.map(([key, label]) => <label key={key}><span>{label}</span><input value={fields[key]} onChange={(event) => updateField(key, event.target.value)} /></label>)}</div>
          <div className="cron-presets"><span>{t('cron.preset')}</span>{cronPresets.map((preset) => <button type="button" key={preset.id} onClick={() => updateExpression(preset.expression)}>{t(`cron.${preset.id === 'minute' ? 'everyMinute' : preset.id === 'hour' ? 'everyHour' : preset.id === 'day' ? 'everyDay' : 'weekdays'}` as 'cron.everyMinute')}</button>)}</div>
        </section>
        <section className="cron-expression-panel">
          <label htmlFor="cron-expression">{t('cron.expression')}</label>
          <input id="cron-expression" value={expression} spellCheck={false} onChange={(event) => updateExpression(event.target.value)} onKeyDown={(event) => { if (event.key === 'Enter') parse() }} />
          <select aria-label={t('time.timezone')} value={timeZone} onChange={(event) => setTimeZone(event.target.value)}><option value={systemZone}>{systemZone}</option><option value="UTC">UTC</option><option value="Asia/Shanghai">Asia/Shanghai</option><option value="Asia/Tokyo">Asia/Tokyo</option><option value="Europe/London">Europe/London</option><option value="America/New_York">America/New_York</option></select>
          <button className="primary-command" type="button" onClick={parse}><Play size={14} />{t('cron.parse')}</button>
          <output><span>{t('cron.humanReadable')}</span>{description}</output>
        </section>
        <section className="cron-runs"><h2>{t('cron.nextRuns')}</h2>{error ? <p className="result-status result-status--error">{error}</p> : runs.length === 0 ? <p className="empty-state">{t('cron.parse')}</p> : <ol>{runs.map((run) => <li key={run}><time>{run}</time></li>)}</ol>}</section>
      </ResizableColumns>
      <FavoriteDialog kind="cron" open={favoritesOpen} currentValue={expression} onClose={() => setFavoritesOpen(false)} onApply={updateExpression} />
      <HistoryDialog funcType="cron" open={historyOpen} onClose={() => setHistoryOpen(false)} onApply={updateExpression} onApplyRecord={(record) => { updateExpression(record.inputText); setRuns(record.outputText.split('\n').filter(Boolean)); try { const meta = JSON.parse(record.extraData ?? '{}') as { timeZone?: string }; if (meta.timeZone) setTimeZone(meta.timeZone) } catch { /* Older history has no metadata. */ } }} />
    </section>
  )
}
