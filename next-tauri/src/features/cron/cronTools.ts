import { CronExpressionParser } from 'cron-parser'
import cronstrue from 'cronstrue'
import 'cronstrue/locales/en'
import 'cronstrue/locales/ja'
import 'cronstrue/locales/zh_CN'
import { DateTime } from 'luxon'
import type { AppLanguage } from '../../platform/contracts/settings'

export interface CronFields {
  second: string
  minute: string
  hour: string
  day: string
  month: string
  week: string
  year: string
}
export type CronToolErrorCode = 'requiredFields' | 'fieldCount' | 'runCount' | 'invalidZone' | 'insufficientRuns' | 'lastOffset'

export class CronToolError extends Error {
  constructor(readonly code: CronToolErrorCode, readonly values?: Record<string, string>) {
    super(`CRON_TOOL_${code}`)
    this.name = 'CronToolError'
  }
}

export const defaultCronFields: CronFields = {
  second: '0',
  minute: '0',
  hour: '9',
  day: '?',
  month: '*',
  week: 'MON-FRI',
  year: ''
}

export const cronPresets = [
  { id: 'minute', expression: '0 * * * * ?' },
  { id: 'hour', expression: '0 0 * * * ?' },
  { id: 'day', expression: '0 0 0 * * ?' },
  { id: 'weekdays', expression: '0 0 9 ? * MON-FRI' },
  { id: 'month', expression: '0 0 0 1 * ?' }
] as const

export function buildCron(fields: CronFields): string {
  const required = [
    fields.second,
    fields.minute,
    fields.hour,
    fields.day,
    fields.month,
    fields.week
  ]
  if (required.some((value) => !value.trim())) throw new CronToolError('requiredFields')
  return [...required, fields.year.trim()].filter(Boolean).join(' ')
}

export function splitCron(expression: string): CronFields {
  const parts = expression.trim().split(/\s+/)
  if (parts.length !== 6 && parts.length !== 7) throw new CronToolError('fieldCount')
  return {
    second: parts[0],
    minute: parts[1],
    hour: parts[2],
    day: parts[3],
    month: parts[4],
    week: parts[5],
    year: parts[6] ?? ''
  }
}

export function nextCronRuns(
  expression: string,
  timeZone: string,
  count = 10,
  currentDate = new Date()
): string[] {
  if (!Number.isInteger(count) || count < 1 || count > 50) {
    throw new CronToolError('runCount')
  }
  const fields = splitCron(expression)
  if (/^(\d{1,2})W$/.test(fields.day) || fields.day === 'LW' || /^L-\d{1,2}$/.test(fields.day)) {
    return nextNearestWeekdayRuns(fields, timeZone, count, currentDate)
  }
  const normalized = [
    fields.second,
    fields.minute,
    fields.hour,
    fields.day === '?' ? '*' : fields.day,
    fields.month,
    fields.week === '?' ? '*' : normalizeWeek(fields.week)
  ].join(' ')
  const interval = CronExpressionParser.parse(normalized, {
    currentDate,
    tz: timeZone,
    strict: false
  })
  const runs: string[] = []
  let guard = 0
  while (runs.length < count && guard < 100_000) {
    guard += 1
    const date = interval.next().toDate()
    if (matchesYear(date.getFullYear(), fields.year)) {
      const display = DateTime.fromJSDate(date).setZone(timeZone)
      if (!display.isValid) throw new CronToolError('invalidZone', { zone: timeZone })
      runs.push(display.toFormat('yyyy-MM-dd HH:mm:ss ZZZZ'))
    }
  }
  if (runs.length < count) throw new CronToolError('insufficientRuns')
  return runs
}

export function describeCron(expression: string, language: AppLanguage): string {
  const value = expression.trim()
  const day = value.split(/\s+/)[3]
  const lastOffset = day ? /^L-(\d{1,2})$/.exec(day) : null
  if (lastOffset && Number(lastOffset[1]) > 30) throw new CronToolError('lastOffset')
  const locale = language === 'zh-CN' ? 'zh_CN' : language === 'ja-JP' ? 'ja' : 'en'
  return cronstrue.toString(value, {
    locale,
    dayOfWeekStartIndexZero: value.split(/\s+/).length === 5,
    use24HourTimeFormat: true,
    throwExceptionOnParseError: true
  })
}

export const commonCronExpressions = [
  '0 0 2 1 * ? *',
  '0 15 10 ? * MON-FRI',
  '0 15 10 ? * 6L 2002-2006',
  '0 0 10,14,16 * * ?',
  '0 0/30 9-17 * * ?',
  '0 0 12 ? * WED',
  '0 0 12 * * ?',
  '0 15 10 ? * *',
  '0 15 10 * * ?',
  '0 15 10 * * ? *',
  '0 15 10 * * ? 2005',
  '0 * 14 * * ?',
  '0 0/5 14 * * ?',
  '0 0/5 14,18 * * ?',
  '0 0-5 14 * * ?',
  '0 10,44 14 ? 3 WED',
  '0 15 10 15 * ?',
  '0 15 10 L * ?',
  '0 15 10 ? * 6L',
  '0 15 10 ? * 6L 2002-2005',
  '0 15 10 ? * 6#3',
  '0 15 10 * * ? 2017'
] as const

function nextNearestWeekdayRuns(
  fields: CronFields,
  timeZone: string,
  count: number,
  currentDate: Date
): string[] {
  const now = DateTime.fromJSDate(currentDate, { zone: timeZone })
  if (!now.isValid) throw new CronToolError('invalidZone', { zone: timeZone })
  const lastOffsetMatch = /^L-(\d{1,2})$/.exec(fields.day)
  const lastOffset = lastOffsetMatch ? Number(lastOffsetMatch[1]) : null
  if (lastOffset !== null && lastOffset > 30) throw new CronToolError('lastOffset')
  const requested = fields.day === 'LW' || lastOffset !== null ? null : Number(/^(\d{1,2})W$/.exec(fields.day)?.[1])
  if (requested !== null && (requested < 1 || requested > 31)) throw new CronToolError('insufficientRuns')
  const allowedMonths = CronExpressionParser.parse(`0 0 0 1 ${fields.month} *`, { currentDate }).fields.month.values
  const runs: string[] = []
  for (let year = Math.max(1970, now.year); year <= 2199 && runs.length < count; year += 1) {
    if (!matchesYear(year, fields.year)) continue
    for (let month = 1; month <= 12 && runs.length < count; month += 1) {
      if (!allowedMonths.some((allowed) => allowed === month)) continue
      const start = DateTime.fromObject({ year, month, day: 1 }, { zone: timeZone })
      const end = start.endOf('month')
      if (!start.isValid || end.toMillis() <= now.toMillis()) continue
      const days = start.daysInMonth
      const day = lastOffset === null
        ? nearestWeekday(start, requested ?? days ?? 0)
        : days === undefined || days - lastOffset < 1 ? null : days - lastOffset
      if (!day) continue
      const normalized = [
        fields.second,
        fields.minute,
        fields.hour,
        String(day),
        fields.month,
        fields.week === '?' ? '*' : normalizeWeek(fields.week)
      ].join(' ')
      const interval = CronExpressionParser.parse(normalized, {
        currentDate: new Date(Math.max(now.toMillis(), start.toMillis() - 1)),
        endDate: end.toJSDate(),
        tz: timeZone
      })
      while (runs.length < count && interval.hasNext()) {
        const display = DateTime.fromJSDate(interval.next().toDate()).setZone(timeZone)
        if (!display.isValid) throw new CronToolError('invalidZone', { zone: timeZone })
        runs.push(display.toFormat('yyyy-MM-dd HH:mm:ss ZZZZ'))
      }
    }
  }
  return runs
}

function nearestWeekday(start: DateTime, day: number): number | null {
  const days = start.daysInMonth
  if (days === undefined || day < 1 || day > days) return null
  let resolved = day
  const weekday = start.set({ day }).weekday
  if (weekday === 6) resolved += resolved === 1 ? 2 : -1
  if (weekday === 7) resolved += resolved === days ? -2 : 1
  return resolved
}

function normalizeWeek(value: string): string {
  const days = ['SUN', 'MON', 'TUE', 'WED', 'THU', 'FRI', 'SAT']
  return value.replace(/SUN|MON|TUE|WED|THU|FRI|SAT/gi, (day) => (
    String(days.indexOf(day.toUpperCase()))
  ))
}

function matchesYear(year: number, expression: string): boolean {
  if (!expression || expression === '*') return true
  return expression.split(',').some((part) => {
    const step = /^(\*|\d{4}-\d{4})\/(\d+)$/.exec(part)
    if (step) {
      const [start, end] = step[1] === '*' ? [1970, 2199] : step[1].split('-').map(Number)
      return year >= start && year <= end && (year - start) % Number(step[2]) === 0
    }
    const range = /^(\d{4})-(\d{4})$/.exec(part)
    if (range) return year >= Number(range[1]) && year <= Number(range[2])
    return Number(part) === year
  })
}
