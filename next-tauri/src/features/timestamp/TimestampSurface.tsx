import {
  ArrowDown,
  ArrowUp,
  CheckCircle2,
  Clipboard,
  Clock3,
  Copy,
  Expand,
  History,
  RefreshCw,
  TriangleAlert,
  X
} from 'lucide-react'
import { useEffect, useMemo, useState } from 'react'
import { createPortal } from 'react-dom'
import { useLocalizedMessages, type LocalizedMessageKey, type MessageValues } from '../../app/localizedMessages'
import { clipboardApi } from '../../platform/api/clipboardApi'
import type { OperationHistory } from '../../platform/contracts/history'
import { contentFingerprint } from '../../shared/fingerprint'
import { useToolSessionReport } from '../toolWebview/useToolSessionReport'
import { HistoryPanel } from '../history/HistoryPanel'
import { useOperationHistory } from '../history/useOperationHistory'
import { parseOperationMetadata, useOperationRestore } from '../history/operationRestore'
import { useSettings } from '../settings/SettingsProvider'
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

export function TimestampSurface({ active = true }: { active?: boolean }) {
  const { t, locale } = useLocalizedMessages(timestampMessages)
  const { settings } = useSettings()
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
  const [historyOpen, setHistoryOpen] = useState(false)
  const [clockOpen, setClockOpen] = useState(false)
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

  useOperationRestore('timestamp', (entry) => {
    const metadata = parseOperationMetadata(entry)
    const nextUnit = metadata.unit === 'millisecond' ? 'millisecond' : 'second'
    const nextZone = typeof metadata.zone === 'string' ? metadata.zone : systemZone
    const direction = metadata.direction === 'toTimestamp' ? 'toTimestamp' : 'toLocal'
    setUnit(nextUnit)
    setZone(nextZone)
    if (direction === 'toLocal') {
      setTimestamp(entry.inputText)
      setLocalTime(entry.outputText)
    } else {
      setLocalTime(entry.inputText)
      setTimestamp(entry.outputText)
    }
    try { setDetails(timestampToLocal(direction === 'toLocal' ? entry.inputText : entry.outputText, nextUnit, nextZone, locale)); setError(undefined) } catch (cause) { setError(timestampError(cause)) }
  })

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
      recordOperation(t('action.toLocal'), `${timestamp} · ${zone} → ${result.localTime}`, 'success', {
        inputText: timestamp, outputText: result.localTime, metadata: { direction: 'toLocal', unit, zone }
      })
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
      recordOperation(t('action.toTimestamp'), `${localTime} · ${zone} → ${result}`, 'success', {
        inputText: localTime, outputText: result, metadata: { direction: 'toTimestamp', unit, zone }
      })
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

  function applyHistory(entry: OperationHistory): void {
    const value = entry.outputText || entry.inputText
    if (/^[-+]?\d+$/.test(value.trim())) setTimestamp(value)
    else setLocalTime(value)
    setHistoryOpen(false)
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
        <h1 className="visually-hidden">{t('title')}</h1>
        <span className="utility-session">{t('session.label')} <code>{sessionId}</code></span>
      </header>

      <section className="time-current">
        <h2>{t('current.heading')}</h2>
        <div className="time-current-values">
          <TimeValue label={t('current.unix')} value={currentSeconds} onCopy={() => void copy('current-seconds', currentSeconds)} copied={copied === 'current-seconds'} />
          <TimeValue label={`${t('current.local')} · ${zone}`} value={currentLocal} onCopy={() => void copy('current-local', currentLocal)} copied={copied === 'current-local'} />
        </div>
        <div className="time-current-actions">
          <button type="button" onClick={() => setHistoryOpen(true)}><History size={14} />{t('action.history')}</button>
          <button type="button" onClick={() => setClockOpen(true)}><Expand size={14} />{t('action.clock')}</button>
          <button type="button" onClick={useNow}><RefreshCw size={14} />{t('action.now')}</button>
          <div className="status-pill"><Clock3 size={14} />{formatTimezoneLabel(zone, now, t('timezone.invalid'))}</div>
        </div>
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
              <select aria-label={t('field.timestamp')} value={unit} onChange={(event) => setUnit(event.target.value as TimestampUnit)}>
                <option value="second">{t('unit.second')}</option>
                <option value="millisecond">{t('unit.millisecond')}</option>
              </select>
              <button className="utility-copy" type="button" aria-label={t('action.copy')} onClick={() => void copy('timestamp', timestamp)}>
                {copied === 'timestamp' ? <Clipboard /> : <Copy />}
              </button>
            </div>
          </label>
          <div className="time-actions">
            <button className="primary-button" type="button" onClick={toLocal}>
              <ArrowDown />{t('action.toLocal')}
            </button>
            <button className="secondary-button" type="button" onClick={toTimestamp}>
              <ArrowUp />{t('action.toTimestamp')}
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
              <span className="time-format-hint">{t('format.hint')}</span>
              <button className="utility-copy" type="button" aria-label={t('action.copy')} onClick={() => void copy('local-time', localTime)}>
                {copied === 'local-time' ? <Clipboard /> : <Copy />}
              </button>
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
      {historyOpen && (
        <HistoryPanel
          limit={settings.data.historyLimit}
          toolId="timestamp"
          onClose={() => setHistoryOpen(false)}
          onRestore={applyHistory}
        />
      )}
      {clockOpen && <ClockOverlay active={active} now={now} zone={zone} onClose={() => setClockOpen(false)} />}
    </main>
  )
}

function ClockOverlay({ active, now, zone, onClose }: { active: boolean; now: number; zone: string; onClose: () => void }) {
  const { t } = useLocalizedMessages(timestampMessages)
  const [date, time] = formatLocalTime(now, zone).split(' ')
  useEffect(() => {
    if (!active) return
    const closeOnEscape = (event: KeyboardEvent) => { if (event.key === 'Escape') onClose() }
    window.addEventListener('keydown', closeOnEscape)
    return () => window.removeEventListener('keydown', closeOnEscape)
  }, [active, onClose])
  if (!active) return null
  return createPortal(
    <div className="clock-overlay">
      <button type="button" aria-label={t('action.closeClock')} onClick={onClose}><X size={18} /></button>
      <span>{zone}</span>
      <strong>{time}</strong>
      <time>{date}</time>
    </div>,
    document.body
  )
}

function TimeValue({ label, value, copied, onCopy }: {
  label: string
  value: string
  copied: boolean
  onCopy(): void
}) {
  const { t } = useLocalizedMessages(timestampMessages)
  return (
    <div className="time-current-value">
      <span>{label}</span>
      <strong>{value}</strong>
      <button className="utility-copy" type="button" aria-label={t('action.copyValue', { label })} title={t('action.copyValue', { label })} onClick={onCopy}>
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
