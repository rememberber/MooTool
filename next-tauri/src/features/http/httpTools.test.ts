import { describe, expect, it } from 'vitest'
import { buildCurl, HttpToolError, parseCurl } from './httpTools'

describe('HTTP curl tools', () => {
  it('parses method, headers and body', () => {
    const request = parseCurl(`curl -X POST 'https://example.com/api' -H 'Content-Type: application/json' --data-raw '{"ok":true}'`)
    expect(request).toMatchObject({ method: 'POST', url: 'https://example.com/api', body: '{"ok":true}' })
    expect(request.headers[0]).toMatchObject({ name: 'Content-Type', value: 'application/json', enabled: true })
  })
  it('builds a safely quoted curl command', () => {
    expect(buildCurl(parseCurl("curl -X GET 'https://example.com/a?q=1'"))).toBe("curl -X GET 'https://example.com/a?q=1'")
  })
  it('rejects malformed commands', () => {
    expect(() => parseCurl('wget https://example.com')).toThrow(HttpToolError)
    expect(() => parseCurl("curl 'https://example.com")).toThrow('HTTP_TOOL_curlUnclosed')
  })
})
