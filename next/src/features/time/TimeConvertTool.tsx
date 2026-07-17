import { ArrowDown, ArrowUp, Clock3, Copy, Expand, History, X } from 'lucide-react'
import { useEffect, useMemo, useReducer } from 'react'
import { createPortal } from 'react-dom'
import { useToolActivity } from '@/shared/components/ToolActivity'
import { HistoryDialog } from '@/features/history/HistoryDialog'
import { Tooltip } from '@/shared/components/Tooltip'
import { useToast } from '@/shared/feedback/ToastProvider'
import { useI18n } from '@/shared/i18n/I18nProvider'
import {
  commonTimezones,
  formatLocalTime,
  formatTimezoneLabel,
  localToTimestamp,
  quickTimezones,
  timestampToLocal,
  type TimestampUnit
} from './timeTools'

type TimeState = {
  now: number
  timestamp: string
  localTime: string
  unit: TimestampUnit
  zone: string
  historyOpen: boolean
  clockOpen: boolean
}

function updateState(state: TimeState, patch: Partial<TimeState>): TimeState {
  return { ...state, ...patch }
}

export function TimeConvertTool() {
  const { t } = useI18n()
  const toast = useToast()
  const systemZone = Intl.DateTimeFormat().resolvedOptions().timeZone || 'UTC'
  const [state, update] = useReducer(updateState, systemZone, (zone): TimeState => {
    const now = Date.now()
    return {
      now,
      timestamp: String(Math.trunc(now / 1000)),
      localTime: formatLocalTime(now, zone),
      unit: 'second',
      zone,
      historyOpen: false,
      clockOpen: false
    }
  })
  const timezones = useMemo(() => Array.from(new Set([systemZone, ...commonTimezones])), [systemZone])

  useEffect(() => {
    const timer = window.setInterval(() => update({ now: Date.now() }), 1000)
    return () => window.clearInterval(timer)
  }, [])

  async function saveHistory(summary: string, inputText: string, outputText: string): Promise<void> {
    try {
      await window.mootool.saveHistory({ funcType: 'timeConvert', summary, inputText, outputText, extraData: JSON.stringify({ zone: state.zone, unit: state.unit }) })
    } catch {
      // The conversion remains useful even when history persistence fails.
    }
  }

  function convertToLocal(): void {
    try {
      const result = timestampToLocal(state.timestamp, state.unit, state.zone)
      update({ localTime: result.localTime, unit: result.unit })
      const notice = t('time.notice.toLocal', { zone: state.zone })
      toast.success(notice)
      void saveHistory(notice, state.timestamp, result.localTime)
    } catch {
      toast.error(t('time.error.timestamp'))
    }
  }

  function convertToTimestamp(): void {
    try {
      const result = localToTimestamp(state.localTime, state.unit, state.zone)
      update({ timestamp: result })
      const notice = t('time.notice.toTimestamp')
      toast.success(notice)
      void saveHistory(notice, state.localTime, result)
    } catch {
      toast.error(t('time.error.localTime'))
    }
  }

  async function copy(value: string): Promise<void> {
    try {
      await navigator.clipboard.writeText(value)
      toast.success(t('time.notice.copied'))
    } catch {
      toast.error(t('json.notice.copyFailed'))
    }
  }

  const currentTimestamp = String(Math.trunc(state.now / 1000))
  const currentLocalTime = formatLocalTime(state.now, state.zone)
  return (
    <section className="tool-page time-tool">
      <div className="tool-page__header">
        <h1>{t('time.title')}</h1>
        <div className="tool-header-actions">
          <button className="toolbar-button" type="button" onClick={() => update({ historyOpen: true })}><History size={14} />{t('time.history')}</button>
          <button className="toolbar-button" type="button" onClick={() => update({ clockOpen: true })}><Expand size={14} />{t('time.clock')}</button>
          <div className="status-pill"><Clock3 size={14} />{formatTimezoneLabel(state.zone, state.now)}</div>
        </div>
      </div>

      <div className="time-workspace">
        <section className="time-current-band">
          <h2>{t('time.current')}</h2>
          <TimeValue label={t('time.timestamp')} value={currentTimestamp} onCopy={() => { void copy(currentTimestamp) }} />
          <TimeValue label={`${t('time.localTime')} · ${state.zone}`} value={currentLocalTime} onCopy={() => { void copy(currentLocalTime) }} />
        </section>

        <section className="time-zone-band">
          <label htmlFor="time-zone">{t('time.timezone')}</label>
          <select id="time-zone" value={state.zone} onChange={(event) => update({ zone: event.target.value, localTime: formatLocalTime(state.now, event.target.value) })}>
            {timezones.map((zone) => <option value={zone} key={zone}>{formatTimezoneLabel(zone, state.now)}</option>)}
          </select>
          <div className="time-quick-zones">
            {quickTimezones.map((item) => <button className={state.zone === item.zone ? 'time-zone-button time-zone-button--active' : 'time-zone-button'} type="button" key={item.zone} onClick={() => update({ zone: item.zone, localTime: formatLocalTime(state.now, item.zone) })}>{item.label}</button>)}
          </div>
        </section>

        <section className="time-converter">
          <div className="time-field-group">
            <label htmlFor="timestamp-input">{t('time.timestamp')}</label>
            <div className="time-input-row">
              <input id="timestamp-input" value={state.timestamp} inputMode="numeric" onChange={(event) => update({ timestamp: event.target.value })} onKeyDown={(event) => { if (event.key === 'Enter') convertToLocal() }} />
              <select aria-label={t('time.timestamp')} value={state.unit} onChange={(event) => update({ unit: event.target.value as TimestampUnit })}>
                <option value="second">{t('time.unit.second')}</option>
                <option value="millisecond">{t('time.unit.millisecond')}</option>
              </select>
              <Tooltip content={t('time.copy')}><button className="icon-button" type="button" aria-label={t('time.copy')} onClick={() => { void copy(state.timestamp) }}><Copy size={14} /></button></Tooltip>
            </div>
          </div>

          <button className="time-convert-button" type="button" onClick={convertToLocal}><ArrowDown size={15} />{t('time.toLocal')}</button>

          <div className="time-field-group">
            <label htmlFor="local-time-input">{t('time.localTime')} · {state.zone}</label>
            <div className="time-input-row">
              <input id="local-time-input" value={state.localTime} onChange={(event) => update({ localTime: event.target.value })} onKeyDown={(event) => { if (event.key === 'Enter') convertToTimestamp() }} />
              <span className="time-format-hint">{t('time.formatHint')}</span>
              <Tooltip content={t('time.copy')}><button className="icon-button" type="button" aria-label={t('time.copy')} onClick={() => { void copy(state.localTime) }}><Copy size={14} /></button></Tooltip>
            </div>
          </div>

          <button className="time-convert-button" type="button" onClick={convertToTimestamp}><ArrowUp size={15} />{t('time.toTimestamp')}</button>
        </section>
      </div>

      <HistoryDialog
        funcType="timeConvert"
        open={state.historyOpen}
        onClose={() => update({ historyOpen: false })}
        onApply={(value) => {
          update(/^[-+]?\d+$/.test(value.trim())
            ? { timestamp: value, historyOpen: false }
            : { localTime: value, historyOpen: false })
        }}
      />
      {state.clockOpen && <ClockOverlay now={state.now} zone={state.zone} onClose={() => update({ clockOpen: false })} />}
    </section>
  )
}

function TimeValue({ label, value, onCopy }: { label: string; value: string; onCopy: () => void }) {
  const { t } = useI18n()
  return (
    <div className="time-current-value">
      <span>{label}</span><strong>{value}</strong>
      <Tooltip content={t('time.copy')}><button className="icon-button" type="button" aria-label={t('time.copy')} onClick={onCopy}><Copy size={14} /></button></Tooltip>
    </div>
  )
}

function ClockOverlay({ now, zone, onClose }: { now: number; zone: string; onClose: () => void }) {
  const toolActive = useToolActivity()
  const { t } = useI18n()
  const value = formatLocalTime(now, zone)
  const [date, time] = value.split(' ')
  useEffect(() => {
    if (!toolActive) return
    const closeOnEscape = (event: KeyboardEvent) => { if (event.key === 'Escape') onClose() }
    window.addEventListener('keydown', closeOnEscape)
    return () => window.removeEventListener('keydown', closeOnEscape)
  }, [onClose, toolActive])
  if (!toolActive) return null
  return createPortal(
    <div className="clock-overlay">
      <button type="button" aria-label={t('time.clock.close')} onClick={onClose}><X size={18} /></button>
      <span>{zone}</span>
      <strong>{time}</strong>
      <time>{date}</time>
    </div>,
    document.body
  )
}
