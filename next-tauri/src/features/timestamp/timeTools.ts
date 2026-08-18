import { DateTime } from 'luxon'

export type TimestampUnit = 'second' | 'millisecond'
export type TimeToolErrorCode = 'timestampInteger' | 'safeRange' | 'invalidTimestamp' | 'localFormat' | 'invalidZone'

export class TimeToolError extends Error {
  constructor(readonly code: TimeToolErrorCode, readonly values?: Record<string, string>) {
    super(`TIME_TOOL_${code}`)
    this.name = 'TimeToolError'
  }
}

export const commonTimezones = [
  'UTC',
  'Asia/Shanghai',
  'Asia/Tokyo',
  'Asia/Seoul',
  'Asia/Singapore',
  'Asia/Hong_Kong',
  'Asia/Kolkata',
  'Asia/Dubai',
  'Europe/London',
  'Europe/Paris',
  'Europe/Berlin',
  'Europe/Moscow',
  'America/New_York',
  'America/Chicago',
  'America/Denver',
  'America/Los_Angeles',
  'Australia/Sydney',
  'Pacific/Auckland'
] as const

export const quickTimezones = [
  { label: 'UTC', zone: 'UTC' },
  { label: '+8', zone: 'Asia/Shanghai' },
  { label: '+9', zone: 'Asia/Tokyo' },
  { label: '-5/-4', zone: 'America/New_York' },
  { label: '-8/-7', zone: 'America/Los_Angeles' },
  { label: '+1/+2', zone: 'Europe/Paris' }
] as const

export interface TimeConversion {
  localTime: string
  unit: TimestampUnit
  milliseconds: number
  iso: string
  rfc2822: string
  weekday: string
  offset: string
}

export function timestampToLocal(
  input: string,
  unit: TimestampUnit,
  zone: string,
  locale = 'en-US'
): TimeConversion {
  const normalized = input.trim()
  if (!/^-?\d+$/.test(normalized)) throw new TimeToolError('timestampInteger')
  const detectedUnit = normalized.replace('-', '').length >= 13 ? 'millisecond' : unit
  const value = Number(normalized)
  const milliseconds = detectedUnit === 'second' ? value * 1000 : value
  if (!Number.isSafeInteger(milliseconds)) throw new TimeToolError('safeRange')
  const dateTime = DateTime.fromMillis(milliseconds, { zone, locale })
  if (!dateTime.isValid) throw new TimeToolError('invalidTimestamp', { detail: dateTime.invalidExplanation ?? '' })
  return describeDateTime(dateTime, detectedUnit, milliseconds)
}

export function localToTimestamp(input: string, unit: TimestampUnit, zone: string): string {
  const normalized = input.trim()
  const dateTime = DateTime.fromFormat(normalized, 'yyyy-MM-dd HH:mm:ss', {
    zone,
    setZone: true,
    locale: 'en-US'
  })
  if (!dateTime.isValid || dateTime.toFormat('yyyy-MM-dd HH:mm:ss') !== normalized) {
    throw new TimeToolError('localFormat')
  }
  const milliseconds = dateTime.toMillis()
  return String(unit === 'second' ? Math.trunc(milliseconds / 1000) : milliseconds)
}

export function formatLocalTime(milliseconds: number, zone: string): string {
  const value = DateTime.fromMillis(milliseconds, { zone })
  if (!value.isValid) throw new TimeToolError('invalidZone', { zone })
  return value.toFormat('yyyy-MM-dd HH:mm:ss')
}

export function formatTimezoneLabel(zone: string, now = Date.now(), invalidLabel = 'invalid'): string {
  const dateTime = DateTime.fromMillis(now, { zone })
  if (!dateTime.isValid) return `${zone} (${invalidLabel})`
  return `${zone} (GMT${dateTime.toFormat('ZZ')})`
}

function describeDateTime(
  dateTime: DateTime,
  unit: TimestampUnit,
  milliseconds: number
): TimeConversion {
  return {
    localTime: dateTime.toFormat('yyyy-MM-dd HH:mm:ss'),
    unit,
    milliseconds,
    iso: dateTime.toISO({ suppressMilliseconds: false }) ?? '',
    rfc2822: dateTime.toRFC2822() ?? '',
    weekday: dateTime.toFormat('cccc'),
    offset: `GMT${dateTime.toFormat('ZZ')}`
  }
}
