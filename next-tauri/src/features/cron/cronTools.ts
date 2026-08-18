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
export type CronToolErrorCode = 'requiredFields' | 'fieldCount' | 'runCount' | 'invalidZone' | 'insufficientRuns'

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
  const locale = language === 'zh-CN' ? 'zh_CN' : language === 'ja-JP' ? 'ja' : 'en'
  return cronstrue.toString(expression, {
    locale,
    use24HourTimeFormat: true,
    throwExceptionOnParseError: true
  })
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
