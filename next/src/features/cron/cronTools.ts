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

const weekNames = ['SUN', 'MON', 'TUE', 'WED', 'THU', 'FRI', 'SAT']
const monthNames = ['JAN', 'FEB', 'MAR', 'APR', 'MAY', 'JUN', 'JUL', 'AUG', 'SEP', 'OCT', 'NOV', 'DEC']

function readFields(expression: string): CronFields {
  const parts = expand(expression).toUpperCase().split(/\s+/)
  if (parts.length === 5) parts.unshift('0')
  if (parts.length !== 6 && parts.length !== 7) throw new Error('Cron requires Linux 5 fields or Quartz 6/7 fields')
  return { second: parts[0], minute: parts[1], hour: parts[2], day: parts[3], month: parts[4], week: parts[5], year: parts[6] ?? '' }
}

export function splitCron(expression: string): CronFields {
  const expanded = expand(expression)
  const fields = readFields(expanded)
  fields.month = fields.month.replace(/JAN|FEB|MAR|APR|MAY|JUN|JUL|AUG|SEP|OCT|NOV|DEC/g, month => String(monthNames.indexOf(month) + 1))
  if (expanded.split(/\s+/).length === 5) {
    // Unix combines restricted day and weekday with OR, which Quartz cannot represent.
    if (fields.day !== '*' && fields.week !== '*') throw new Error('Unix day/weekday OR cannot be converted to a single Quartz expression')
    if (fields.week === '*') fields.week = '?'
    else {
      fields.day = '?'
      fields.week = [...new Set(CronExpressionParser.parse(expanded).fields.dayOfWeek.values.map(day => Number(day) % 7 + 1))].sort((a, b) => a - b).join(',')
    }
  } else {
    fields.week = fields.week.replace(/SUN|MON|TUE|WED|THU|FRI|SAT/g, day => String(weekNames.indexOf(day) + 1))
  }
  return fields
}

const aliases: Record<string, string> = {
  '@yearly': '0 0 1 1 *', '@annually': '0 0 1 1 *', '@monthly': '0 0 1 * *',
  '@weekly': '0 0 * * 0', '@daily': '0 0 * * *', '@midnight': '0 0 * * *', '@hourly': '0 * * * *'
}

function expand(expression: string): string {
  const value = expression.trim()
  return aliases[value.toLowerCase()] ?? value
}

function quartzWeek(value: string): string {
  const named = value.toUpperCase().replace(/SUN|MON|TUE|WED|THU|FRI|SAT/g, day => String(['SUN', 'MON', 'TUE', 'WED', 'THU', 'FRI', 'SAT'].indexOf(day) + 1))
  if (!/^(?:[1-7](?:-[1-7])?(?:\/[1-7])?|[1-7]L|[1-7]#[1-5]|\*(?:\/[1-7])?)(?:,(?:[1-7](?:-[1-7])?))*$/.test(named)) throw new Error('Invalid Quartz weekday')
  return named.split(',').map(part => {
    const [base, nth] = part.split('#')
    const [range, step] = base.split('/')
    const converted = range.replace(/\d+/g, number => {
      const day = Number(number)
      if (day < 1 || day > 7) throw new Error('Quartz weekday must be 1–7 (Sunday–Saturday)')
      return String(day - 1)
    })
    return converted + (step ? '/' + step : '') + (nth ? '#' + nth : '')
  }).join(',')
}

function years(value: string): number[] {
  if (!value || value === '*') return Array.from({ length: 230 }, (_, i) => 1970 + i)
  const result = new Set<number>()
  for (const part of value.split(',')) {
    const match = /^(\*|\d{4})(?:-(\d{4}))?(?:\/(\d+))?$/.exec(part)
    if (!match) throw new Error('Invalid year field')
    const start = match[1] === '*' ? 1970 : Number(match[1])
    const end = match[2] ? Number(match[2]) : match[1] === '*' || match[3] ? 2199 : start
    const step = Number(match[3] ?? 1)
    if (start < 1970 || end > 2199 || start > end || step < 1) throw new Error('Invalid year range (1970–2199)')
    for (let year = start; year <= end; year += step) result.add(year)
  }
  return [...result].sort((a, b) => a - b)
}

function parseSchedule(expression: string) {
  const expanded = expand(expression)
  const unix = expanded.split(/\s+/).length === 5
  const fields = readFields(expanded)
  if (!unix && ((fields.day === '?') === (fields.week === '?'))) throw new Error('Quartz requires ? in either day or weekday')
  const workingDay = /^(\d{1,2})W$/.exec(fields.day)
  if (workingDay && (+workingDay[1] < 1 || +workingDay[1] > 31)) throw new Error('Invalid nearest weekday')
  const lastOffset = /^L-(\d{1,2})$/.exec(fields.day)
  if (lastOffset && +lastOffset[1] > 30) throw new Error('Invalid last-day offset')
  const special = Boolean(workingDay || fields.day === 'LW' || lastOffset)
  const normalized = [fields.second, fields.minute, fields.hour, special || fields.day === '?' ? '*' : fields.day,
    fields.month, fields.week === '?' ? '*' : unix ? fields.week : quartzWeek(fields.week)].join(' ')
  // Validate even when the requested year is already in the past.
  const parsed = CronExpressionParser.parse(normalized)
  return { fields, normalized, special, workingDay, lastOffset, allowedMonths: parsed.fields.month.values, allowedYears: years(fields.year) }
}

export function nextCronRuns(expression: string, timeZone: string, count = 10, currentDate = new Date()): string[] {
  if (!Number.isInteger(count) || count < 0 || count > 100) throw new Error('Run count must be between 0 and 100')
  const now = DateTime.fromJSDate(currentDate, { zone: timeZone })
  if (!now.isValid) throw new Error('Invalid date or timezone')
  const schedule = parseSchedule(expression)
  const runs: string[] = []
  for (const year of schedule.allowedYears) {
    if (year < now.year || runs.length >= count) continue
    // Work month by month so W and optional years never require scanning individual seconds.
    for (let month = 1; month <= 12 && runs.length < count; month++) {
      if (!schedule.allowedMonths.some(allowed => allowed === month)) continue
      const start = DateTime.fromObject({ year, month, day: 1 }, { zone: timeZone })
      const end = start.endOf('month')
      if (end.toMillis() <= now.toMillis()) continue
      let normalized = schedule.normalized
      if (schedule.special) {
        let day = schedule.workingDay ? Number(schedule.workingDay[1]) : start.daysInMonth!
        if (schedule.lastOffset) day -= Number(schedule.lastOffset[1])
        if (day < 1 || day > start.daysInMonth!) continue
        if (!schedule.lastOffset) {
          const weekday = start.set({ day }).weekday
          if (weekday === 6) day += day === 1 ? 2 : -1
          if (weekday === 7) day += day === start.daysInMonth ? -2 : 1
        }
        const parts = normalized.split(' '); parts[3] = String(day); normalized = parts.join(' ')
      }
      const interval = CronExpressionParser.parse(normalized, {
        currentDate: new Date(Math.max(now.toMillis(), start.toMillis() - 1)), endDate: end.toJSDate(), tz: timeZone
      })
      while (runs.length < count && interval.hasNext()) {
        runs.push(DateTime.fromJSDate(interval.next().toDate(), { zone: timeZone }).toFormat('yyyy-MM-dd HH:mm:ss ZZZZ'))
      }
    }
  }
  return runs
}

export function describeCron(expression: string, language: 'zh-CN' | 'en-US' | 'ja-JP'): string {
  parseSchedule(expression)
  const expanded = expand(expression)
  const locale = language === 'zh-CN' ? 'zh_CN' : language === 'ja-JP' ? 'ja' : 'en'
  return cronstrue.toString(expanded, {
    locale, dayOfWeekStartIndexZero: expanded.split(/\s+/).length === 5,
    use24HourTimeFormat: true, throwExceptionOnParseError: true
  })
}

export const commonCronExpressions = [
  "0 0 2 1 * ? *",
  "0 15 10 ? * MON-FRI",
  "0 15 10 ? * 6L 2002-2006",
  "0 0 10,14,16 * * ?",
  "0 0/30 9-17 * * ?",
  "0 0 12 ? * WED",
  "0 0 12 * * ?",
  "0 15 10 ? * *",
  "0 15 10 * * ?",
  "0 15 10 * * ? *",
  "0 15 10 * * ? 2005",
  "0 * 14 * * ?",
  "0 0/5 14 * * ?",
  "0 0/5 14,18 * * ?",
  "0 0-5 14 * * ?",
  "0 10,44 14 ? 3 WED",
  "0 15 10 15 * ?",
  "0 15 10 L * ?",
  "0 15 10 ? * 6L",
  "0 15 10 ? * 6L 2002-2005",
  "0 15 10 ? * 6#3",
  "0 15 10 * * ? 2017"
] as const
