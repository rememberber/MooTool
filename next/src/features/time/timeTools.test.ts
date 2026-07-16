import { describe, expect, it } from 'vitest'
import { formatTimezoneLabel, localToTimestamp, timestampToLocal } from './timeTools'

describe('timeTools', () => {
  it('converts seconds and milliseconds into an IANA timezone', () => {
    expect(timestampToLocal('0', 'second', 'Asia/Shanghai').localTime).toBe('1970-01-01 08:00:00')
    expect(timestampToLocal('1704067200000', 'second', 'UTC')).toMatchObject({ localTime: '2024-01-01 00:00:00', unit: 'millisecond' })
  })

  it('converts local time back to the selected timestamp unit', () => {
    expect(localToTimestamp('1970-01-01 08:00:00', 'second', 'Asia/Shanghai')).toBe('0')
    expect(localToTimestamp('2024-01-01 00:00:00', 'millisecond', 'UTC')).toBe('1704067200000')
  })

  it('rejects invalid dates and formats timezone labels', () => {
    expect(() => localToTimestamp('2024-02-31 00:00:00', 'second', 'UTC')).toThrow('invalid-local-time')
    expect(() => timestampToLocal('hello', 'second', 'UTC')).toThrow('invalid-timestamp')
    expect(formatTimezoneLabel('UTC', 0)).toBe('UTC (GMT+00:00)')
  })
})
