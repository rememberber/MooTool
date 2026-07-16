import { describe, expect, it } from 'vitest'
import { formatYaml, propertiesToYaml, validateYaml, yamlToProperties } from './configTools'

describe('configuration tools', () => {
  it('converts nested properties and indexed lists to YAML', () => {
    const yaml = propertiesToYaml('server.port=8080\nusers[0].name=Ada\nusers[1].name=Lin')
    expect(yaml).toContain('server:')
    expect(yaml).toContain('users:')
    expect(yaml).toContain('name: Ada')
  })

  it('flattens YAML and joins scalar lists like the Java version', () => {
    expect(yamlToProperties('server:\n  port: 8080\ntags: [a, b]\n')).toBe('server.port=8080\ntags=a,b')
  })

  it('validates and formats YAML', () => {
    expect(validateYaml('a: [1, 2]').valid).toBe(true)
    expect(validateYaml('a: [1,').valid).toBe(false)
    expect(formatYaml('a: {b: 1}')).toContain('a:')
  })
})
