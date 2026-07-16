import { createServer } from 'node:http'
import { afterAll, beforeAll, describe, expect, it } from 'vitest'
import { NetworkService, buildRequestUrl, splitTranslationText } from '../../electron/main/networkService'
import type { HttpRequestDraft } from './contracts/network'

let baseUrl = ''
const server = createServer((request, response) => {
  const chunks: Buffer[] = []
  request.on('data', (chunk: Buffer) => chunks.push(chunk))
  request.on('end', () => {
    response.setHeader('content-type', 'application/json')
    response.setHeader('set-cookie', ['session=abc; Path=/', 'mode=test; Path=/'])
    response.end(JSON.stringify({ method: request.method, url: request.url, header: request.headers['x-mootool'], body: Buffer.concat(chunks).toString('utf8') }))
  })
})

beforeAll(async () => {
  await new Promise<void>((resolve) => server.listen(0, '127.0.0.1', resolve))
  const address = server.address()
  if (!address || typeof address === 'string') throw new Error('Test server failed to bind')
  baseUrl = `http://127.0.0.1:${address.port}`
})

afterAll(async () => {
  await new Promise<void>((resolve, reject) => server.close((error) => error ? reject(error) : resolve()))
})

describe('NetworkService', () => {
  it('builds query params and sends a request through the main-process service', async () => {
    const request: HttpRequestDraft = {
      name: 'Local request', method: 'GET', url: `${baseUrl}/inspect`, body: '', bodyType: 'application/json',
      params: [{ id: 'p1', name: 'query', value: 'Moo Tool', enabled: true }],
      headers: [{ id: 'h1', name: 'X-MooTool', value: 'Next', enabled: true }],
      cookies: [{ id: 'c1', name: 'client', value: 'desktop', enabled: true, domain: '', path: '/', expires: '' }]
    }
    const result = await new NetworkService().sendHttp({ requestId: 'local-get', request, timeoutMs: 3_000 }, { enabled: false, host: '', port: '', username: '', password: '' })

    expect(result.ok).toBe(true)
    expect(result.status).toBe(200)
    expect(JSON.parse(result.body)).toMatchObject({ method: 'GET', url: '/inspect?query=Moo+Tool', header: 'Next' })
    expect(result.cookies).toContain('session=abc')
  })

  it('uses form params for a body when no explicit body is present', async () => {
    const request: HttpRequestDraft = {
      name: 'Form request', method: 'POST', url: `${baseUrl}/form`, body: '', bodyType: 'application/json',
      params: [{ id: 'p1', name: 'name', value: 'Moo Tool', enabled: true }], headers: [], cookies: []
    }
    const result = await new NetworkService().sendHttp({ requestId: 'local-post', request, timeoutMs: 3_000 }, { enabled: false, host: '', port: '', username: '', password: '' })
    expect(JSON.parse(result.body).body).toBe('name=Moo+Tool')
  })
})

describe('network helpers', () => {
  it('normalizes schemes and excludes disabled params', () => {
    expect(buildRequestUrl('example.com/path', 'GET', [
      { id: '1', name: 'a', value: '1', enabled: true },
      { id: '2', name: 'b', value: '2', enabled: false }
    ])).toBe('http://example.com/path?a=1')
  })

  it('splits long translation text without losing content', () => {
    const text = `${'a'.repeat(8)}\n${'b'.repeat(8)}`
    expect(splitTranslationText(text, 10).join('')).toBe(text)
    expect(splitTranslationText(text, 10).every((chunk) => chunk.length <= 10)).toBe(true)
  })
})
