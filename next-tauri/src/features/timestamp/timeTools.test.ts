import { describe, expect, it } from 'vitest'
import {
  formatTimezoneLabel,
  localToTimestamp,
  timestampToLocal
} from './timeTools'

describe('time conversion tools', () => {
  it('converts Unix epoch into the selected timezone', () => {
    expect(timestampToLocal('0', 'second', 'Asia/Shanghai')).toMatchObject({
      localTime: '1970-01-01 08:00:00',
      unit: 'second',
      milliseconds: 0,
      offset: 'GMT+08:00'
    })
  })

  it('auto-detects millisecond timestamps and converts back', () => {
    expect(timestampToLocal('1704067200000', 'second', 'UTC')).toMatchObject({
      localTime: '2024-01-01 00:00:00',
      unit: 'millisecond'
    })
    expect(localToTimestamp('2024-01-01 00:00:00', 'millisecond', 'UTC'))
      .toBe('1704067200000')
  })

  it('rejects malformed and nonexistent local times', () => {
    expect(() => timestampToLocal('hello', 'second', 'UTC')).toThrow('TIME_TOOL_timestampInteger')
    expect(() => localToTimestamp('2024-02-30 10:00:00', 'second', 'UTC')).toThrow('TIME_TOOL_localFormat')
  })

  it('includes the current daylight-aware offset in timezone labels', () => {
    expect(formatTimezoneLabel('Asia/Shanghai', Date.UTC(2026, 0, 1))).toBe(
      'Asia/Shanghai (GMT+08:00)'
    )
  })
})
