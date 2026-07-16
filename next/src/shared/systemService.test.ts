import { describe, expect, it } from 'vitest'
import { ipv4ToLong, longToIpv4, normalizeHostsContent } from '../../electron/main/systemService'

describe('system helpers', () => {
  it('converts IPv4 and unsigned long values in both directions', () => {
    expect(ipv4ToLong('192.168.1.1')).toBe(3232235777)
    expect(longToIpv4(3232235777)).toBe('192.168.1.1')
    expect(() => ipv4ToLong('300.1.1.1')).toThrow('Invalid IPv4')
    expect(() => longToIpv4(0x1_0000_0000)).toThrow('Invalid IPv4')
  })

  it('normalizes hosts line endings and final newline', () => {
    expect(normalizeHostsContent('127.0.0.1 localhost\r\n\r\n')).toBe('127.0.0.1 localhost\n')
    expect(() => normalizeHostsContent('bad\0value')).toThrow('Invalid hosts')
  })
})
