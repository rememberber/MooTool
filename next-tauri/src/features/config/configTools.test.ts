import { describe, expect, it } from 'vitest'
import { ConfigToolError, formatYaml, propertiesToYaml, validateYaml, yamlToProperties } from './configTools'

describe('YAML and Properties conversion', () => {
  it('flattens nested objects and arrays into property paths', () => {
    expect(yamlToProperties('server:\n  port: 8080\ntags: [a, b]\n')).toBe('server.port=8080\ntags=a,b')
    expect(yamlToProperties('users:\n  - name: Ada\n  - name: Lin\n')).toBe('users[0].name=Ada\nusers[1].name=Lin')
  })

  it('restores nested paths, escapes, Unicode and continuation lines', () => {
    const yaml = propertiesToYaml(
      'server.port=8080\nmessage=hello\\nworld\nname=\\u725b\nlong=first\\\n second\nitems[0]=a\nitems[1]=b'
    )
    expect(yaml).toContain('port: "8080"')
    expect(yaml).toContain('message: |-\n    hello\n    world')
    expect(yaml).toContain('name: 牛')
    expect(yaml).toContain('long: first second')
    expect(yaml).toContain('- a\n    - b')
  })

  it('formats and validates YAML with actionable parser output', () => {
    expect(formatYaml('root: {name: MooTool, enabled: true}')).toContain('name: MooTool')
    expect(validateYaml('root: [')).toMatchObject({ valid: false })
    expect(validateYaml('root: ok')).toEqual({ valid: true, message: '' })
  })

  it('rejects prototype-polluting property paths', () => {
    expect(() => propertiesToYaml('__proto__.polluted=true')).toThrowError(
      expect.objectContaining<Partial<ConfigToolError>>({ code: 'unsafeKey' })
    )
  })
})
