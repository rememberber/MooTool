import { CronExpressionParser } from 'cron-parser'
import cronstrue from 'cronstrue'
import 'cronstrue/locales/en'
import 'cronstrue/locales/ja'
import 'cronstrue/locales/zh_CN'
import { DateTime } from 'luxon'

export type CronFields = { second: string; minute: string; hour: string; day: string; month: string; week: string; year: string }

export const defaultCronFields: CronFields = { second: '0', minute: '*', hour: '*', day: '*', month: '*', week: '?', year: '' }

export const cronPresets = [
  { id: 'minute', expression: '0 * * * * ?' },
  { id: 'hour', expression: '0 0 * * * ?' },
  { id: 'day', expression: '0 0 0 * * ?' },
  { id: 'weekdays', expression: '0 0 9 ? * MON-FRI' }
] as const

export function buildCron(fields: CronFields): string {
  const values = [fields.second, fields.minute, fields.hour, fields.day, fields.month, fields.week]
  if (values.some((value) => !value.trim())) throw new Error('All Cron fields are required')
  return [...values, fields.year.trim()].filter(Boolean).join(' ')
}

export function splitCron(expression: string): CronFields {
  const parts = expression.trim().split(/\s+/)
  if (parts.length !== 6 && parts.length !== 7) throw new Error('Cron requires 6 or 7 fields')
  return { second: parts[0], minute: parts[1], hour: parts[2], day: parts[3], month: parts[4], week: parts[5], year: parts[6] ?? '' }
}

export function nextCronRuns(expression: string, timeZone: string, count = 10, currentDate?: Date): string[] {
  const fields = splitCron(expression)
  const normalized = [fields.second, fields.minute, fields.hour, fields.day === '?' ? '*' : fields.day, fields.month, fields.week === '?' ? '*' : normalizeWeek(fields.week)].join(' ')
  const interval = CronExpressionParser.parse(normalized, { currentDate, tz: timeZone, strict: false })
  const runs: string[] = []
  let guard = 0
  while (runs.length < count && guard < 100_000) {
    guard += 1
    const date = interval.next().toDate()
    if (matchesYear(date.getFullYear(), fields.year)) runs.push(DateTime.fromJSDate(date).setZone(timeZone).toFormat('yyyy-MM-dd HH:mm:ss ZZZZ'))
  }
  if (runs.length < count) throw new Error('No matching run time in the supported year range')
  return runs
}

export function describeCron(expression: string, language: 'zh-CN' | 'en-US' | 'ja-JP'): string {
  const locale = language === 'zh-CN' ? 'zh_CN' : language === 'ja-JP' ? 'ja' : 'en'
  return cronstrue.toString(expression, {
    locale,
    use24HourTimeFormat: true,
    throwExceptionOnParseError: true
  })
}

function normalizeWeek(value: string): string {
  return value.replace(/SUN|MON|TUE|WED|THU|FRI|SAT/gi, (day) => String(['SUN', 'MON', 'TUE', 'WED', 'THU', 'FRI', 'SAT'].indexOf(day.toUpperCase())))
}

function matchesYear(year: number, expression: string): boolean {
  if (!expression || expression === '*') return true
  return expression.split(',').some((part) => {
    const stepMatch = /^(\*|\d{4}-\d{4})\/(\d+)$/.exec(part)
    if (stepMatch) {
      const [start, end] = stepMatch[1] === '*' ? [1970, 2199] : stepMatch[1].split('-').map(Number)
      return year >= start && year <= end && (year - start) % Number(stepMatch[2]) === 0
    }
    const range = /^(\d{4})-(\d{4})$/.exec(part)
    if (range) return year >= Number(range[1]) && year <= Number(range[2])
    return Number(part) === year
  })
}
