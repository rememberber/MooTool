import { describe, expect, it } from 'vitest'
import { parseCurlCommand, toCurlCommand } from './httpTools'

describe('HTTP cURL tools', () => {
  it('parses method, headers, cookies and JSON body', () => {
    const request = parseCurlCommand(`curl 'https://example.com/api?q=1' -X POST -H 'Content-Type: application/json' -H 'X-Test: yes' -b 'sid=abc; mode=dark' --data-raw '{"ok":true}'`)
    expect(request).toMatchObject({ method: 'POST', url: 'https://example.com/api?q=1', bodyType: 'application/json', body: '{"ok":true}' })
    expect(request.headers).toHaveLength(2)
    expect(request.cookies.map((item) => item.name)).toEqual(['sid', 'mode'])
  })

  it('round trips the important request fields', () => {
    const source = parseCurlCommand(`curl https://example.com -H 'Accept: application/json' --data-raw 'hello world'`)
    const parsed = parseCurlCommand(toCurlCommand(source))
    expect(parsed).toMatchObject({ method: 'POST', url: 'https://example.com', body: 'hello world' })
    expect(parsed.headers[0]).toMatchObject({ name: 'Accept', value: 'application/json' })
  })
})
