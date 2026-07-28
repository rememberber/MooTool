import { describe, expect, it } from 'vitest'
import { createServer } from 'node:net'
import { tmpdir } from 'node:os'
import { SystemService, ipv4ToLong, longToIpv4, normalizeHostsContent, parseEnvironmentContent, parseIpv4Range, parsePortSpec, updateEnvironmentContent } from '../../electron/main/systemService'
import { normalizeDeleteEnvironmentVariableInput, normalizeEnvironmentVariableInput } from '../../electron/main/p5Validation'

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

  it('expands a /24-style IPv4 prefix to usable host addresses', () => {
    const addresses = parseIpv4Range('192.168.10')
    expect(addresses).toHaveLength(254)
    expect(addresses[0]).toBe('192.168.10.1')
    expect(addresses[253]).toBe('192.168.10.254')
    expect(() => parseIpv4Range('192.168.999')).toThrow('INVALID_TARGET')
  })

  it('parses custom port lists and uses known ports by default', () => {
    expect(parsePortSpec('3306,80-82,22')).toEqual([22, 80, 81, 82, 3306])
    expect(parsePortSpec()).toEqual(expect.arrayContaining([22, 3306, 5432, 6379]))
    expect(() => parsePortSpec('0,70000')).toThrow('INVALID_TARGET')
    expect(() => parsePortSpec('1-5000')).toThrow('INVALID_TARGET')
  })

  it('parses and safely updates persistent environment files', () => {
    const source = `# keep this comment
export JAVA_HOME='/old jdk'
OTHER="value"
`
    const updated = updateEnvironmentContent(source, 'JAVA_HOME', "/opt/jdk's", true)
    expect(updated).toBe(`# keep this comment
export JAVA_HOME='/opt/jdk'\\''s'
OTHER="value"
`)
    expect(parseEnvironmentContent(updated)).toEqual([
      { key: 'JAVA_HOME', value: "/opt/jdk's" },
      { key: 'OTHER', value: 'value' }
    ])
    expect(updateEnvironmentContent(updated, 'JAVA_HOME', undefined, true)).toBe('# keep this comment\nOTHER="value"\n')
  })

  it('validates environment mutations at the IPC boundary', () => {
    expect(normalizeEnvironmentVariableInput({ scope: 'user', key: 'JAVA_HOME', value: '/opt/jdk' }))
      .toEqual({ scope: 'user', key: 'JAVA_HOME', value: '/opt/jdk' })
    expect(normalizeDeleteEnvironmentVariableInput({ scope: 'system', key: 'JAVA_HOME' }))
      .toEqual({ scope: 'system', key: 'JAVA_HOME' })
    expect(() => normalizeEnvironmentVariableInput({ scope: 'user', key: 'BAD-NAME', value: 'x' })).toThrow()
    expect(() => normalizeEnvironmentVariableInput({ scope: 'user', key: 'SAFE', value: 'one\ntwo' })).toThrow()
  })

  it('reports an open TCP port from a real local listener', async () => {
    const server = createServer()
    await new Promise<void>((resolve) => server.listen(0, '127.0.0.1', resolve))
    try {
      const address = server.address()
      if (!address || typeof address === 'string') throw new Error('Expected TCP server address')
      const result = await new SystemService(tmpdir()).runNetwork({
        requestId: 'port-scan-test',
        action: 'port-scan',
        target: '127.0.0.1',
        ports: String(address.port),
        timeoutMs: 2_000
      })
      expect(result.errorCode).toBeUndefined()
      expect(result.output).toContain(`${address.port}/tcp open`)
    } finally {
      await new Promise<void>((resolve, reject) => server.close((error) => error ? reject(error) : resolve()))
    }
  })
})
