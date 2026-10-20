import { describe, expect, it } from 'vitest'
import { formatOpenPorts, parseIpv4Range, parsePortSpec } from './networkCommand'

describe('network command specs', () => {
  it('expands a three-octet prefix to .1 through .254', () => {
    const hosts = parseIpv4Range('192.168.10')
    expect(hosts[0]).toBe('192.168.10.1')
    expect(hosts.at(-1)).toBe('192.168.10.254')
    expect(hosts).toHaveLength(254)
    expect(() => parseIpv4Range('192.168.10.1')).toThrow('INVALID_TARGET')
  })

  it('uses the common port list when the spec is blank and keeps custom order rules', () => {
    expect(parsePortSpec('')).toContain(22)
    expect(parsePortSpec('80, 22, 443')).toEqual([22, 80, 443])
    expect(parsePortSpec('1-3')).toEqual([1, 2, 3])
    expect(() => parsePortSpec('80-1')).toThrow('INVALID_TARGET')
  })

  it('names open ports the same way as Electron', () => {
    expect(formatOpenPorts('127.0.0.1', [22, 80], [22])).toBe('Open TCP ports on 127.0.0.1: 1 / 2\n\n22/tcp open ssh')
  })
})
