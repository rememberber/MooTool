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
