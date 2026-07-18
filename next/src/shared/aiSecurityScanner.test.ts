import { describe, expect, it } from 'vitest'
import { redactSensitiveContent, scanSensitiveContent } from '../../electron/main/ai/securityScanner'

describe('AI security scanner', () => {
  it('reports secret categories and line numbers without returning matched values', () => {
    const secret = 'sk-this_is_a_test_token_value_123456'
    const content = `model = "gpt-5"\napi_key = "${secret}"\n-----BEGIN PRIVATE KEY-----\n`

    const findings = scanSensitiveContent(content)

    expect(findings).toEqual(expect.arrayContaining([
      expect.objectContaining({ kind: 'credentialAssignment', line: 2 }),
      expect.objectContaining({ kind: 'knownTokenFormat', line: 2, severity: 'blocking' }),
      expect.objectContaining({ kind: 'privateKey', line: 3, severity: 'blocking' })
    ]))
    expect(JSON.stringify(findings)).not.toContain(secret)
  })

  it('accepts environment references and explicit placeholders', () => {
    expect(scanSensitiveContent(`api_key = "\${OPENAI_API_KEY}"\ntoken: env:GITHUB_TOKEN\npassword = <from-safe-storage>\n`)).toEqual([])
  })

  it('redacts assignments, known token formats, and private-key blocks before producing a diff', () => {
    const secret = 'sk-this_is_a_test_token_value_123456'
    const redacted = redactSensitiveContent(`api_key = "${secret}"\n-----BEGIN PRIVATE KEY-----\nprivate material\n-----END PRIVATE KEY-----\nkeep = true`)

    expect(redacted).not.toContain(secret)
    expect(redacted).not.toContain('private material')
    expect(redacted).toContain('[REDACTED]')
    expect(redacted).toContain('[REDACTED PRIVATE KEY]')
    expect(redacted).toContain('keep = true')
  })
})
