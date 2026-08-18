import { describe, expect, it } from 'vitest'
import { analyzeIpv4Cidr, integerToIpv4 } from './networkTools'

describe('IPv4 and CIDR tools', () => {
  it('calculates a regular subnet', () => {
    expect(analyzeIpv4Cidr('192.168.1.10/24')).toMatchObject({
      address: '192.168.1.10',
      prefix: 24,
      netmask: '255.255.255.0',
      wildcard: '0.0.0.255',
      network: '192.168.1.0',
      broadcast: '192.168.1.255',
      firstHost: '192.168.1.1',
      lastHost: '192.168.1.254',
      totalAddresses: 256,
      usableHosts: 254,
      category: 'private'
    })
  })

  it('supports point-to-point and host prefixes', () => {
    expect(analyzeIpv4Cidr('10.0.0.0/31').usableHosts).toBe(2)
    expect(analyzeIpv4Cidr('127.0.0.1').category).toBe('loopback')
  })

  it('converts unsigned integers and rejects invalid values', () => {
    expect(integerToIpv4('3232235777')).toBe('192.168.1.1')
    expect(() => integerToIpv4('4294967296')).toThrow('NETWORK_TOOL_integerRange')
    expect(() => analyzeIpv4Cidr('256.1.1.1/24')).toThrow('NETWORK_TOOL_octetRange')
  })
})
