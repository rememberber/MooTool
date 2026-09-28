import { describe, expect, it } from 'vitest'
import { buildCron, defaultCronFields, describeCron, nextCronRuns, splitCron } from './cronTools'

describe('Cron tools', () => {
  it('builds and splits Quartz-style expressions', () => {
    expect(buildCron(defaultCronFields)).toBe('0 * * * * ?')
    expect(splitCron('0 15 10 ? * MON-FRI 2027').year).toBe('2027')
  })

  it('calculates upcoming runs with a timezone', () => {
    const runs = nextCronRuns('0 0 9 ? * MON-FRI', 'Asia/Shanghai', 2, new Date('2026-07-17T02:00:00Z'))
    expect(runs).toHaveLength(2)
    expect(runs[0]).toContain('2026-07-20 09:00:00')
  })

  it('filters an optional year field', () => {
    const [run] = nextCronRuns('0 0 0 1 1 ? 2028', 'UTC', 1, new Date('2026-01-01T00:00:00Z'))
    expect(run).toContain('2028-01-01 00:00:00')
  })

  it('describes expressions in the selected application language', () => {
    expect(describeCron('0 0 9 ? * MON-FRI', 'en-US')).toContain('09:00')
    expect(describeCron('0 0 9 ? * MON-FRI', 'zh-CN')).toContain('09:00')
  })
})

describe('Java Cron compatibility', () => {
  const start = new Date('2026-07-01T00:00:00Z')
  it('uses Quartz weekday numbers for plain, range, last and nth weekdays', () => {
    for (const week of ['6', '6L', '6#3', 'MON-FRI']) {
      const runs = nextCronRuns(`0 15 10 ? * ${week}`, 'UTC', 2, start)
      expect(runs).toHaveLength(2)
      if (week !== 'MON-FRI') runs.forEach(run => expect(new Date(run.slice(0, 10)).getUTCDay()).toBe(5))
    }
    expect(describeCron('0 15 10 ? * 6', 'en-US')).toContain('Friday')
  })
  it('supports Linux five fields and macros with Unix weekday numbering', () => {
    expect(nextCronRuns('15 10 * * 5', 'UTC', 1, start)[0]).toContain('2026-07-03 10:15:00')
    expect(nextCronRuns('@monthly', 'UTC', 1, start)[0]).toContain('2026-08-01 00:00:00')
    expect(describeCron('15 10 * * 5', 'en-US')).toContain('Friday')
  })
  it('handles nearest weekdays without crossing month boundaries', () => {
    expect(nextCronRuns('0 0 9 1W * ?', 'UTC', 1, new Date('2026-08-01T00:00:00Z'))[0]).toContain('2026-08-03 09:00:00')
    expect(nextCronRuns('0 0 9 31W * ?', 'UTC', 1, new Date('2026-05-01T00:00:00Z'))[0]).toContain('2026-05-29 09:00:00')
    expect(nextCronRuns('0 0 9 LW * ?', 'UTC', 1, new Date('2026-05-01T00:00:00Z'))[0]).toContain('2026-05-29 09:00:00')
  })
  it('jumps directly to future years and returns partial or exhausted schedules', () => {
    expect(nextCronRuns('* * * * * ? 2199', 'UTC', 1, start)[0]).toContain('2199-01-01 00:00:00')
    expect(nextCronRuns('0 0 0 31 12 ? 2026', 'UTC', 10, start)).toHaveLength(1)
    expect(nextCronRuns('0 0 0 1 1 ? 2005', 'UTC', 10, start)).toEqual([])
    expect(nextCronRuns('0 0 0 1 1 ? 2028/2', 'UTC', 2, start)[1]).toContain('2030-01-01')
  })
  it('uses the selected timezone for year boundaries', () => {
    expect(nextCronRuns('0 0 0 1 1 ? 2027', 'Asia/Tokyo', 1, start)[0]).toContain('2027-01-01 00:00:00')
  })
  it('rejects invalid Quartz combinations and invalid years or timezones', () => {
    for (const expression of ['0 0 0 * * *', '0 0 0 ? * ?', '0 0 0 ? * 0', '0 0 0 * * ? 2027/0', '0 0 0 32W * ?']) {
      expect(() => nextCronRuns(expression, 'UTC', 1, start)).toThrow()
    }
    expect(() => nextCronRuns('0 0 0 * * ?', 'invalid', 1, start)).toThrow()
  })
})

it('converts Unix weekday ranges without changing their meaning', () => {
  const fields = splitCron('15 10 * * 1-7')
  expect(fields.day).toBe('?')
  expect(fields.week).toBe('1,2,3,4,5,6,7')
  expect(nextCronRuns(buildCron(fields), 'UTC', 3, new Date('2026-07-01T00:00:00Z')))
    .toEqual(nextCronRuns('15 10 * * 1-7', 'UTC', 3, new Date('2026-07-01T00:00:00Z')))
  expect(() => splitCron('0 0 1 * 1')).toThrow('OR')
  expect(nextCronRuns('0 0 1 * 1', 'UTC', 2, new Date('2026-07-01T00:00:00Z'))).toHaveLength(2)
})

it('supports last-day offsets and impossible nearest-weekday dates', () => {
  expect(nextCronRuns('0 0 9 L-3 * ?', 'UTC', 1, new Date('2026-02-01T00:00:00Z'))[0]).toContain('2026-02-25 09:00:00')
  expect(nextCronRuns('0 0 9 31W 2 ?', 'UTC', 1, new Date('2026-02-01T00:00:00Z'))).toEqual([])
})
