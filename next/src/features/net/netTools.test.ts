import { describe, expect, it } from 'vitest'
import { ipv4ToLong, longToIpv4 } from './netTools'

describe('network conversions', () => {
  it('converts IPv4 and unsigned integer values', () => {
    expect(ipv4ToLong('127.0.0.1')).toBe(2130706433)
    expect(longToIpv4('2130706433')).toBe('127.0.0.1')
  })
})
