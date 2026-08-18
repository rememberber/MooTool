import { describe, expect, it } from 'vitest'
import {
  buildCron,
  defaultCronFields,
  describeCron,
  nextCronRuns,
  splitCron
} from './cronTools'

describe('Cron tools', () => {
  it('builds and splits Quartz expressions with an optional year', () => {
    expect(buildCron(defaultCronFields)).toBe('0 0 9 ? * MON-FRI')
    expect(splitCron('0 30 8 ? * MON-FRI 2027')).toEqual({
      second: '0',
      minute: '30',
      hour: '8',
      day: '?',
      month: '*',
      week: 'MON-FRI',
      year: '2027'
    })
    expect(() => splitCron('* * *')).toThrow('CRON_TOOL_fieldCount')
  })

  it('calculates deterministic future runs in the requested timezone', () => {
    expect(nextCronRuns(
      '0 0 9 ? * MON-FRI',
      'Asia/Shanghai',
      3,
      new Date('2026-07-31T01:00:00.000Z')
    )).toEqual([
      '2026-08-03 09:00:00 GMT+8',
      '2026-08-04 09:00:00 GMT+8',
      '2026-08-05 09:00:00 GMT+8'
    ])
  })

  it('supports year ranges and localized descriptions', () => {
    expect(nextCronRuns(
      '0 0 0 1 1 ? 2028',
      'UTC',
      1,
      new Date('2026-01-01T00:00:00Z')
    )[0]).toContain('2028-01-01')
    expect(describeCron('0 0 9 ? * MON-FRI', 'en-US')).toContain('09:00')
    expect(describeCron('0 0 9 ? * MON-FRI', 'zh-CN')).toBeTruthy()
  })
})
