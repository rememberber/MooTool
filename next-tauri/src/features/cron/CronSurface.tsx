import {
  CalendarClock,
  CheckCircle2,
  Clock3,
  Play,
  Sparkles,
  TriangleAlert
} from 'lucide-react'
import { useEffect, useMemo, useState } from 'react'
import { useLocalizedMessages, type LocalizedMessageKey, type MessageValues } from '../../app/localizedMessages'
import { clipboardApi } from '../../platform/api/clipboardApi'
import type { AppLanguage } from '../../platform/contracts/settings'
import { contentFingerprint } from '../../shared/fingerprint'
import { useSettings } from '../settings/SettingsProvider'
import { useToolSessionReport } from '../toolWebview/useToolSessionReport'
import { useOperationHistory } from '../history/useOperationHistory'
import { parseOperationMetadata, useOperationRestore } from '../history/operationRestore'
import { ToolFavoriteBar } from '../favorites/ToolFavoriteBar'
import {
  buildCron,
  commonCronExpressions,
  cronPresets,
  defaultCronFields,
  describeCron,
  nextCronRuns,
  splitCron,
  CronToolError,
  type CronFields
} from './cronTools'
import { CronFieldEditor } from './CronFieldEditor'
import { cronMessages } from './cronMessages'

type CronMessageKey = LocalizedMessageKey<typeof cronMessages>
type CronError = { key: CronMessageKey; values?: MessageValues } | { raw: string }

const fieldLabels: Array<[keyof CronFields, CronMessageKey, string]> = [
  ['second', 'field.second', '0–59'],
  ['minute', 'field.minute', '0–59'],
  ['hour', 'field.hour', '0–23'],
  ['day', 'field.day', '1–31 / ?'],
  ['month', 'field.month', '1–12 / JAN–DEC'],
  ['week', 'field.week', '0–7 / MON–SUN / ?'],
  ['year', 'field.year', '1970–2199']
]

const timezones = [
  'UTC',
  'Asia/Shanghai',
  'Asia/Tokyo',
  'Asia/Singapore',
  'Europe/London',
  'Europe/Paris',
  'America/New_York',
  'America/Los_Angeles',
  'Australia/Sydney'
]

export function CronSurface() {
  const { settings } = useSettings()
  const { t } = useLocalizedMessages(cronMessages)
  const systemZone = Intl.DateTimeFormat().resolvedOptions().timeZone || 'UTC'
  const [fields, setFields] = useState(defaultCronFields)
  const [expression, setExpression] = useState(() => buildCron(defaultCronFields))
  const [timeZone, setTimeZone] = useState(systemZone)
  const [descriptionLanguage, setDescriptionLanguage] = useState<AppLanguage>(settings.general.language)
  const [activeField, setActiveField] = useState<keyof CronFields>('second')
  const [examplesOpen, setExamplesOpen] = useState(false)
  const [runs, setRuns] = useState<string[]>([])
  const [description, setDescription] = useState('')
  const [error, setError] = useState<CronError>()
  const errorText = error ? ('raw' in error ? error.raw : t(error.key, error.values)) : ''
  const session = useMemo(() => ({
    digest: JSON.stringify({
      expressionHash: contentFingerprint(expression),
      expressionLength: expression.length,
      timeZone,
      runCount: runs.length,
      valid: !error
    }),
    summary: t('session.summary', { expression, count: runs.length })
  }), [error, expression, runs.length, t, timeZone])
  const { sessionId, reportError } = useToolSessionReport('cron', session.digest, session.summary)
  const recordOperation = useOperationHistory('cron')
  useOperationRestore('cron', (entry) => {
    const metadata = parseOperationMetadata(entry)
    const nextZone = typeof metadata.timeZone === 'string' ? metadata.timeZone : systemZone
    setExpression(entry.inputText)
    setTimeZone(nextZone)
    try {
      setFields(splitCron(entry.inputText))
      setDescription(typeof metadata.description === 'string' ? metadata.description : '')
      setRuns(entry.outputText ? entry.outputText.split('\n') : [])
      setError(undefined)
    } catch (cause) { setError(cronError(cause)) }
  })
  const zoneOptions = Array.from(new Set([systemZone, ...timezones]))

  useEffect(() => {
    const timer = window.setTimeout(() => {
      try {
        setRuns(nextCronRuns(expression, timeZone, 10))
        setDescription(describeCron(expression, descriptionLanguage))
        setError(undefined)
      } catch (cause) {
        setError(cronError(cause))
        setRuns([])
        setDescription('')
      }
    }, 180)
    return () => window.clearTimeout(timer)
  }, [descriptionLanguage, expression, timeZone])

  function updateExpression(value: string): void {
    setExpression(value)
    try { setFields(splitCron(value)) } catch { /* The expression can be partial while editing. */ }
  }

  function calculate(nextExpression = expression): void {
    try {
      const nextFields = splitCron(nextExpression)
      const nextDescription = describeCron(nextExpression, settings.general.language)
      const nextRuns = nextCronRuns(nextExpression, timeZone, 10)
      setExpression(nextExpression)
      setFields(nextFields)
      setDescription(nextDescription)
      setRuns(nextRuns)
      setError(undefined)
      recordOperation(t('action.parse'), `${nextExpression} · ${timeZone} · ${nextRuns.length}`, 'success', {
        inputText: nextExpression,
        outputText: nextRuns.join('\n'),
        metadata: { timeZone, description: nextDescription }
      })
    } catch (cause) {
      setRuns([])
      setDescription('')
      setError(cronError(cause))
      recordOperation(t('action.parse'), `${nextExpression} · ${cause instanceof Error ? cause.message : String(cause)}`, 'error', {
        inputText: nextExpression, metadata: { timeZone }
      })
    }
  }

  function updateField(key: keyof CronFields, value: string): void {
    const next = { ...fields, [key]: value }
    if (key === 'day') next.week = value === '?' ? '*' : '?'
    if (key === 'week') next.day = value === '?' ? '*' : '?'
    setFields(next)
    try {
      setExpression(buildCron(next))
      setError(undefined)
    } catch (cause) {
      setError(cronError(cause))
    }
  }

  function resolveFields(): void {
    try {
      setFields(splitCron(expression))
      describeCron(expression, descriptionLanguage)
      setError(undefined)
    } catch (cause) {
      setError(cronError(cause))
    }
  }

  function convertDescription(): void {
    try {
      const text = describeCron(expression, descriptionLanguage)
      setDescription(text)
      setError(undefined)
      recordOperation(t('action.convert'), text, 'success', {
        inputText: expression,
        outputText: text,
        metadata: { timeZone, descriptionLanguage }
      })
    } catch (cause) {
      setError(cronError(cause))
    }
  }

  function cronError(cause: unknown): CronError {
    return cause instanceof CronToolError
      ? { key: `error.${cause.code}`, values: cause.values }
      : { raw: cause instanceof Error ? cause.message : String(cause) }
  }

  return (
    <main className="utility-workbench cron-workbench">
      <header className="utility-header">
        <h1 className="visually-hidden">Cron</h1>
        <span className="utility-session">{t('session.label')} <code>{sessionId}</code></span>
      </header>

      <section className="cron-expression-row">
        <label htmlFor="cron-expression">Quartz Cron</label>
        <div>
          <input
            id="cron-expression"
            value={expression}
            spellCheck={false}
            onChange={(event) => updateExpression(event.target.value)}
            onKeyDown={(event) => event.key === 'Enter' && calculate()}
          />
          <select value={timeZone} onChange={(event) => setTimeZone(event.target.value)}>
            {zoneOptions.map((zone) => <option key={zone} value={zone}>{zone}</option>)}
          </select>
          <select aria-label={t('description.locale')} value={descriptionLanguage} onChange={(event) => setDescriptionLanguage(event.target.value as AppLanguage)}>
            <option value="zh-CN">中文</option>
            <option value="en-US">English</option>
            <option value="ja-JP">日本語</option>
          </select>
          <button className="secondary-button" type="button" onClick={resolveFields}>{t('action.resolve')}</button>
          <button className="secondary-button" type="button" onClick={convertDescription}>{t('action.convert')}</button>
          <button className="secondary-button" type="button" onClick={() => void clipboardApi.writeText(expression)}>{t('action.copy')}</button>
          <button className="primary-button" type="button" onClick={() => calculate()}>
            <Play />{t('action.parse')}
          </button>
        </div>
      </section>

      <ToolFavoriteBar
        toolId="cron"
        defaultName={expression || 'Cron'}
        payload={{ expression, timeZone }}
        onApply={(payload) => {
          const nextExpression = typeof payload.expression === 'string' ? payload.expression : expression
          const nextZone = typeof payload.timeZone === 'string' ? payload.timeZone : timeZone
          setExpression(nextExpression)
          setTimeZone(nextZone)
          setRuns([])
          setDescription('')
          try {
            setFields(splitCron(nextExpression))
            setError(undefined)
          } catch (cause) {
            setError(cronError(cause))
          }
        }}
      />

      <section className="cron-content">
        <div className="cron-builder">
          <header>
            <span><Sparkles />{t('builder.title')}</span>
            <div className="cron-presets">
              {cronPresets.map((preset) => (
                <button
                  type="button"
                  key={preset.id}
                  onClick={() => calculate(preset.expression)}
                >
                  {t(`preset.${preset.id}`)}
                </button>
              ))}
            </div>
          </header>
          <div className="cron-fields">
            {fieldLabels.map(([key, label, hint]) => (
              <label key={key}>
                <span>{t(label)}</span>
                <input
                  value={fields[key]}
                  spellCheck={false}
                  placeholder={hint}
                  onChange={(event) => updateField(key, event.target.value)}
                />
                <small>{hint}</small>
              </label>
            ))}
          </div>
          <div className="cron-field-tabs" role="tablist">
            {fieldLabels.map(([key, label]) => (
              <button type="button" role="tab" aria-selected={activeField === key} key={key} onClick={() => setActiveField(key)}>{t(label)}</button>
            ))}
          </div>
          <CronFieldEditor key={activeField} field={activeField} value={fields[activeField]} language={descriptionLanguage} onChange={(value) => updateField(activeField, value)} />
          <button className="secondary-button" type="button" onClick={() => setExamplesOpen(true)}>{t('action.examples')}</button>
          <div className={error ? 'cron-description cron-description--error' : 'cron-description'}>
            {error
              ? <><TriangleAlert />{errorText}</>
              : description
                ? <><CheckCircle2 />{description}</>
                : <><Clock3 />{t('description.empty')}</>}
          </div>
        </div>

        <section className="cron-runs">
          <header><CalendarClock />{t('runs.title')} <span>{timeZone}</span></header>
          <ol>
            {runs.map((run, index) => <li key={run}><span>{index + 1}</span><code>{run}</code></li>)}
          </ol>
          {!runs.length && <p>{t('runs.empty')}</p>}
        </section>
      </section>

      <footer className={error ? 'utility-status utility-status--error' : 'utility-status'}>
        <span>{error ? <TriangleAlert /> : <CheckCircle2 />}
          {t(error ? 'status.error' : 'status.ready')}
        </span>
        <code>{session.summary}</code>
      </footer>
      {reportError && <p className="tool-surface-report-error">{t('report.error', { error: reportError })}</p>}
      {examplesOpen && (
        <div className="quick-note-git-backdrop" role="presentation" onMouseDown={(event) => { if (event.target === event.currentTarget) setExamplesOpen(false) }}>
          <section className="cron-examples" role="dialog" aria-modal="true" aria-label={t('action.examples')}>
            <header><strong>{t('action.examples')}</strong><button type="button" onClick={() => setExamplesOpen(false)}>×</button></header>
            <div className="cron-example-list">
              {commonCronExpressions.map((value) => (
                <button type="button" key={value} onClick={() => { updateExpression(value); setExamplesOpen(false) }}>
                  <code>{value}</code>
                  <span>{exampleDescription(value, descriptionLanguage)}</span>
                </button>
              ))}
            </div>
          </section>
        </div>
      )}
    </main>
  )
}

function exampleDescription(expression: string, language: AppLanguage): string {
  try { return describeCron(expression, language) } catch { return '' }
}
