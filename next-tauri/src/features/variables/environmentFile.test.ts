import { describe, expect, it } from 'vitest'
import { formatEnvironment, parseEnvironmentContent, updateEnvironmentContent } from './environmentFile'

describe('persistent environment files', () => {
  it('parses and safely updates assignments the same way as Electron', () => {
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
    expect(() => updateEnvironmentContent('', 'BAD-NAME', 'x', true)).toThrow('Invalid environment variable name')
    expect(() => updateEnvironmentContent('', 'SAFE', 'one\ntwo', true)).toThrow('Invalid environment variable value')
  })

  it('exports the four Electron sections in order', () => {
    expect(formatEnvironment({
      user: [{ key: 'A', value: '1' }],
      system: [],
      environment: [{ key: 'PATH', value: '/bin' }],
      runtime: [{ key: 'process.platform', value: 'darwin' }]
    })).toBe([
      '------------Persistent user environment---------------',
      'A=1',
      '',
      '------------Persistent system environment---------------',
      '',
      '------------Current process environment---------------',
      'PATH=/bin',
      '',
      '------------Electron runtime---------------',
      'process.platform=darwin'
    ].join('\n'))
  })
})
