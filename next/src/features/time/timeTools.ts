import { DateTime } from 'luxon'

export type TimestampUnit = 'second' | 'millisecond'

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
  { label: '-5', zone: 'America/New_York' },
  { label: '-8', zone: 'America/Los_Angeles' },
  { label: '+1', zone: 'Europe/Paris' },
  { label: '+3', zone: 'Europe/Moscow' }
] as const

export function timestampToLocal(input: string, unit: TimestampUnit, zone: string): { localTime: string; unit: TimestampUnit; milliseconds: number } {
  const normalized = input.trim()
  if (!/^-?\d+$/.test(normalized)) throw new Error('invalid-timestamp')
  const detectedUnit = normalized.replace('-', '').length >= 13 ? 'millisecond' : unit
  const value = Number(normalized)
  const milliseconds = detectedUnit === 'second' ? value * 1000 : value
  if (!Number.isFinite(milliseconds)) throw new Error('invalid-timestamp')
  const dateTime = DateTime.fromMillis(milliseconds, { zone })
  if (!dateTime.isValid) throw new Error('invalid-timestamp')
  return { localTime: dateTime.toFormat('yyyy-MM-dd HH:mm:ss'), unit: detectedUnit, milliseconds }
}

export function localToTimestamp(input: string, unit: TimestampUnit, zone: string): string {
  const dateTime = DateTime.fromFormat(input.trim(), 'yyyy-MM-dd HH:mm:ss', { zone, setZone: true, locale: 'en-US' })
  if (!dateTime.isValid || dateTime.toFormat('yyyy-MM-dd HH:mm:ss') !== input.trim()) throw new Error('invalid-local-time')
  const milliseconds = dateTime.toMillis()
  return String(unit === 'second' ? Math.trunc(milliseconds / 1000) : milliseconds)
}

export function formatLocalTime(milliseconds: number, zone: string): string {
  return DateTime.fromMillis(milliseconds, { zone }).toFormat('yyyy-MM-dd HH:mm:ss')
}

export function formatTimezoneLabel(zone: string, now = Date.now()): string {
  const dateTime = DateTime.fromMillis(now, { zone })
  return `${zone} (GMT${dateTime.toFormat('ZZ')})`
}
