import {
  ArrowDown,
  ArrowUp,
  CheckCircle2,
  Clipboard,
  Clock3,
  Copy,
  RefreshCw,
  TriangleAlert
} from 'lucide-react'
import { useEffect, useMemo, useState } from 'react'
import { useLocalizedMessages, type LocalizedMessageKey, type MessageValues } from '../../app/localizedMessages'
import { clipboardApi } from '../../platform/api/clipboardApi'
import { contentFingerprint } from '../../shared/fingerprint'
import { useToolSessionReport } from '../toolWebview/useToolSessionReport'
import { useOperationHistory } from '../history/useOperationHistory'
import {
  commonTimezones,
  formatLocalTime,
  formatTimezoneLabel,
  localToTimestamp,
  quickTimezones,
  timestampToLocal,
  TimeToolError,
  type TimeConversion,
  type TimestampUnit
} from './timeTools'
import { timestampMessages } from './timestampMessages'

type TimestampMessageKey = LocalizedMessageKey<typeof timestampMessages>
type TimestampNotice = { key: TimestampMessageKey; values?: MessageValues } | { raw: string }

export function TimestampSurface() {
  const { t, locale } = useLocalizedMessages(timestampMessages)
  const systemZone = Intl.DateTimeFormat().resolvedOptions().timeZone || 'UTC'
  const initialNow = Date.now()
  const [now, setNow] = useState(initialNow)
  const [timestamp, setTimestamp] = useState(String(Math.trunc(initialNow / 1000)))
  const [localTime, setLocalTime] = useState(formatLocalTime(initialNow, systemZone))
  const [unit, setUnit] = useState<TimestampUnit>('second')
  const [zone, setZone] = useState(systemZone)
  const [details, setDetails] = useState<TimeConversion>(() => (
    timestampToLocal(String(Math.trunc(initialNow / 1000)), 'second', systemZone, locale)
  ))
  const [notice, setNotice] = useState<TimestampNotice>({ key: 'notice.ready' })
  const [error, setError] = useState<TimestampNotice>()
  const [copied, setCopied] = useState('')
  const noticeText = localizeTimestampNotice(notice, t)
  const errorText = error ? localizeTimestampNotice(error, t) : ''
  const zoneOptions = useMemo(() => Array.from(new Set([systemZone, ...commonTimezones])), [systemZone])
  const session = useMemo(() => ({
    digest: JSON.stringify({
      timestampLength: timestamp.length,
      timestampHash: contentFingerprint(timestamp),
      localHash: contentFingerprint(localTime),
      unit,
      zone,
      valid: !error
    }),
    summary: `${t(unit === 'second' ? 'unit.second' : 'unit.millisecond')} · ${zone}`
  }), [error, localTime, t, timestamp, unit, zone])
  const { sessionId, reportError } = useToolSessionReport(
    'timestamp',
    session.digest,
    session.summary
  )
  const recordOperation = useOperationHistory('timestamp')

  useEffect(() => {
    const timer = window.setInterval(() => setNow(Date.now()), 1000)
    return () => window.clearInterval(timer)
  }, [])

  function toLocal(): void {
    try {
      const result = timestampToLocal(timestamp, unit, zone, locale)
      setDetails(result)
      setLocalTime(result.localTime)
      setUnit(result.unit)
      setNotice({ key: 'notice.toLocal', values: { zone } })
      setError(undefined)
      recordOperation(t('action.toLocal'), `${timestamp} · ${zone} → ${result.localTime}`, 'success')
    } catch (cause) {
      setError(timestampError(cause))
    }
  }

  function toTimestamp(): void {
    try {
      const result = localToTimestamp(localTime, unit, zone)
      const nextDetails = timestampToLocal(result, unit, zone, locale)
      setTimestamp(result)
      setDetails(nextDetails)
      setNotice({ key: 'notice.toTimestamp', values: { unit: t(unit === 'second' ? 'unit.second' : 'unit.millisecond') } })
      setError(undefined)
      recordOperation(t('action.toTimestamp'), `${localTime} · ${zone} → ${result}`, 'success')
    } catch (cause) {
      setError(timestampError(cause))
    }
  }

  function selectZone(nextZone: string): void {
    setZone(nextZone)
    try {
      const result = timestampToLocal(timestamp, unit, nextZone, locale)
      setLocalTime(result.localTime)
      setDetails(result)
      setError(undefined)
    } catch (cause) {
      setError(timestampError(cause))
    }
  }

  async function copy(label: string, value: string): Promise<void> {
    try {
      await clipboardApi.writeText(value)
      setCopied(label)
      window.setTimeout(() => setCopied(''), 1200)
    } catch {
      setError({ key: 'error.clipboard' })
    }
  }

  function useNow(): void {
    const value = Date.now()
    const nextTimestamp = unit === 'second' ? String(Math.trunc(value / 1000)) : String(value)
    setTimestamp(nextTimestamp)
    const result = timestampToLocal(nextTimestamp, unit, zone, locale)
    setLocalTime(result.localTime)
    setDetails(result)
    setNotice({ key: 'notice.now' })
    setError(undefined)
  }

  const currentSeconds = String(Math.trunc(now / 1000))
  const currentLocal = formatLocalTime(now, zone)

  function timestampError(cause: unknown): TimestampNotice {
    return cause instanceof TimeToolError
      ? { key: `error.${cause.code}`, values: cause.values }
      : { raw: cause instanceof Error ? cause.message : String(cause) }
  }

  return (
    <main className="utility-workbench time-workbench">
      <header className="utility-header">
        <div>
          <span className="eyebrow">TAURI TIME CONVERTER</span>
          <h1>{t('title')}</h1>
        </div>
        <span className="utility-session">{t('session.label')} <code>{sessionId}</code></span>
      </header>

      <section className="time-current">
        <div>
          <Clock3 />
          <span>{t('current.title', { zone: formatTimezoneLabel(zone, now, t('timezone.invalid')) })}</span>
        </div>
        <TimeValue label={t('current.unix')} value={currentSeconds} onCopy={() => void copy('current-seconds', currentSeconds)} copied={copied === 'current-seconds'} />
        <TimeValue label={t('current.local')} value={currentLocal} onCopy={() => void copy('current-local', currentLocal)} copied={copied === 'current-local'} />
      </section>

      <section className="time-content">
        <div className="time-zone-row">
          <label>
            {t('field.timezone')}
            <select value={zone} onChange={(event) => selectZone(event.target.value)}>
              {zoneOptions.map((item) => (
                <option value={item} key={item}>{formatTimezoneLabel(item, now, t('timezone.invalid'))}</option>
              ))}
            </select>
          </label>
          <div>
            {quickTimezones.map((item) => (
              <button
                className={zone === item.zone ? 'time-zone-chip time-zone-chip--active' : 'time-zone-chip'}
                type="button"
                key={item.zone}
                onClick={() => selectZone(item.zone)}
              >
                {item.label}
              </button>
            ))}
          </div>
        </div>

        <div className="time-converter-grid">
          <label className="time-input-card">
            <span>{t('field.timestamp')}</span>
            <div>
              <input
                value={timestamp}
                inputMode="numeric"
                onChange={(event) => setTimestamp(event.target.value)}
                onKeyDown={(event) => event.key === 'Enter' && toLocal()}
              />
              <select value={unit} onChange={(event) => setUnit(event.target.value as TimestampUnit)}>
                <option value="second">{t('unit.second')}</option>
                <option value="millisecond">{t('unit.millisecond')}</option>
              </select>
            </div>
          </label>
          <div className="time-actions">
            <button className="primary-button" type="button" onClick={toLocal}>
              <ArrowDown />{t('action.toLocal')}
            </button>
            <button className="secondary-button" type="button" onClick={toTimestamp}>
              <ArrowUp />{t('action.toTimestamp')}
            </button>
            <button className="secondary-button" type="button" onClick={useNow}>
              <RefreshCw />{t('action.now')}
            </button>
          </div>
          <label className="time-input-card">
            <span>{t('field.local', { zone })}</span>
            <div>
              <input
                value={localTime}
                onChange={(event) => setLocalTime(event.target.value)}
                onKeyDown={(event) => event.key === 'Enter' && toTimestamp()}
              />
              <code>YYYY-MM-DD HH:mm:ss</code>
            </div>
          </label>
        </div>

        <dl className="time-details">
          <Detail label="ISO 8601" value={details.iso} />
          <Detail label="RFC 2822" value={details.rfc2822} />
          <Detail label={t('detail.weekday')} value={details.weekday} />
          <Detail label={t('detail.offset')} value={details.offset} />
        </dl>
      </section>

      <footer className={error ? 'utility-status utility-status--error' : 'utility-status'}>
        <span>{error ? <TriangleAlert /> : <CheckCircle2 />}{errorText || noticeText}</span>
        <span>{t('footer.capabilities')}</span>
        <code>{session.summary}</code>
      </footer>
      {reportError && <p className="tool-surface-report-error">{t('report.error', { error: reportError })}</p>}
    </main>
  )
}

function TimeValue({ label, value, copied, onCopy }: {
  label: string
  value: string
  copied: boolean
  onCopy(): void
}) {
  return (
    <div className="time-current-value">
      <span>{label}</span>
      <strong>{value}</strong>
      <button className="utility-copy" type="button" onClick={onCopy}>
        {copied ? <Clipboard /> : <Copy />}
      </button>
    </div>
  )
}

function Detail({ label, value }: { label: string; value: string }) {
  return <div><dt>{label}</dt><dd>{value || '—'}</dd></div>
}

function localizeTimestampNotice(notice: TimestampNotice, t: (key: TimestampMessageKey, values?: MessageValues) => string): string {
  return 'raw' in notice ? notice.raw : t(notice.key, notice.values)
}
