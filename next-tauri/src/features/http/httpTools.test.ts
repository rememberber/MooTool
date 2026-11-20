import { describe, expect, it } from 'vitest'
import { buildCurl, emptyHttpRequest, entry, formatHttpBody, httpFailureCode, HttpToolError, parseCurl, prepareHttpExchange } from './httpTools'

describe('HTTP curl tools', () => {
  it('parses method, headers and body', () => {
    const request = parseCurl(`curl -X POST 'https://example.com/api' -H 'Content-Type: application/json' --data-raw '{"ok":true}'`)
    expect(request).toMatchObject({ name: 'Imported cURL', method: 'POST', url: 'https://example.com/api', body: '{"ok":true}' })
    expect(request.headers[0]).toMatchObject({ name: 'Content-Type', value: 'application/json', enabled: true })
  })
  it('builds a safely quoted curl command', () => {
    expect(buildCurl(parseCurl("curl -X GET 'https://example.com/a?q=1'"))).toBe("curl -X GET 'https://example.com/a?q=1'")
  })
  it('copies the URL as entered and leaves query parameters out of the curl command', () => {
    const request = emptyHttpRequest()
    request.url = 'https://example.com/api'
    request.params = [entry('q', 'moo')]
    expect(buildCurl(request)).toBe("curl -X GET 'https://example.com/api'")
  })

  it('formats JSON, XML, HTML, JavaScript and plain text like Electron', async () => {
    await expect(formatHttpBody('{"a":1}', 'application/json')).resolves.toBe('{\n  "a": 1\n}')
    await expect(formatHttpBody('<root><name>MooTool</name></root>', 'application/xml')).resolves.toContain('<name>MooTool</name>')
    await expect(formatHttpBody('<main><p>MooTool</p></main>', 'text/html')).resolves.toContain('<p>MooTool</p>')
    await expect(formatHttpBody('const value=1', 'application/javascript')).resolves.toContain('const value = 1')
    await expect(formatHttpBody('hello   \nworld  ', 'text/plain')).resolves.toBe('hello\nworld')
  })

  it('adds query parameters only for GET, HEAD and OPTIONS, and form-encodes them for other methods', () => {
    const get = emptyHttpRequest()
    get.url = 'example.com/api'
    get.params = [entry('q', 'moo tool')]
    get.body = 'ignored'
    expect(prepareHttpExchange(get)).toEqual({ url: 'http://example.com/api?q=moo+tool' })
    const post = emptyHttpRequest()
    post.method = 'POST'
    post.url = 'https://example.com/api'
    post.params = [entry('q', '1')]
    expect(prepareHttpExchange(post)).toEqual({
      url: 'https://example.com/api',
      body: 'q=1',
      contentType: 'application/x-www-form-urlencoded'
    })
  })

  it('classifies failed requests with Electron error codes', () => {
    expect(httpFailureCode('The operation was aborted: TIMEOUT', true)).toBe('TIMEOUT')
    expect(httpFailureCode('The operation was aborted', true)).toBe('ABORTED')
    expect(httpFailureCode('RESPONSE_TOO_LARGE')).toBe('RESPONSE_TOO_LARGE')
    expect(httpFailureCode('invalid HTTP URL')).toBe('INVALID_REQUEST')
    expect(httpFailureCode('connection refused')).toBe('NETWORK')
  })

  it('rejects malformed commands', () => {
    expect(() => parseCurl('wget https://example.com')).toThrow(HttpToolError)
    expect(() => parseCurl("curl 'https://example.com")).toThrow('HTTP_TOOL_curlUnclosed')
  })
})
