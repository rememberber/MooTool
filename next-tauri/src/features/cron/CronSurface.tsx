import {
  CalendarClock,
  CheckCircle2,
  Clock3,
  Play,
  Sparkles,
  TriangleAlert
} from 'lucide-react'
import { useMemo, useState } from 'react'
import { useLocalizedMessages, type LocalizedMessageKey, type MessageValues } from '../../app/localizedMessages'
import { contentFingerprint } from '../../shared/fingerprint'
import { useSettings } from '../settings/SettingsProvider'
import { useToolSessionReport } from '../toolWebview/useToolSessionReport'
import {
  buildCron,
  cronPresets,
  defaultCronFields,
  describeCron,
  nextCronRuns,
  splitCron,
  CronToolError,
  type CronFields
} from './cronTools'
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
  const zoneOptions = Array.from(new Set([systemZone, ...timezones]))

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
    } catch (cause) {
      setRuns([])
      setDescription('')
      setError(cronError(cause))
    }
  }

  function updateField(key: keyof CronFields, value: string): void {
    const next = { ...fields, [key]: value }
    setFields(next)
    try {
      setExpression(buildCron(next))
      setError(undefined)
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
        <div>
          <span className="eyebrow">TAURI CRON EXPLORER</span>
          <h1>Cron</h1>
        </div>
        <span className="utility-session">{t('session.label')} <code>{sessionId}</code></span>
      </header>

      <section className="cron-expression-row">
        <label htmlFor="cron-expression">Quartz Cron</label>
        <div>
          <input
            id="cron-expression"
            value={expression}
            spellCheck={false}
            onChange={(event) => setExpression(event.target.value)}
            onKeyDown={(event) => event.key === 'Enter' && calculate()}
          />
          <select value={timeZone} onChange={(event) => setTimeZone(event.target.value)}>
            {zoneOptions.map((zone) => <option key={zone} value={zone}>{zone}</option>)}
          </select>
          <button className="primary-button" type="button" onClick={() => calculate()}>
            <Play />{t('action.parse')}
          </button>
        </div>
      </section>

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
    </main>
  )
}
