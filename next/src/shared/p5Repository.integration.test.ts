import { mkdtempSync, rmSync } from 'node:fs'
import { tmpdir } from 'node:os'
import { join } from 'node:path'
import { afterEach, describe, expect, it } from 'vitest'
import { P5Repository } from '../../electron/main/p5Repository'
import type { HttpRequestDraft, HttpResponseResult } from './contracts/network'

const temporaryDirectories: string[] = []

afterEach(() => {
  for (const directory of temporaryDirectories.splice(0)) rmSync(directory, { recursive: true, force: true })
})

function repository(): P5Repository {
  const directory = mkdtempSync(join(tmpdir(), 'mootool-p5-'))
  temporaryDirectories.push(directory)
  return new P5Repository(join(directory, 'MooToolNext.db'))
}

describe('P5Repository', () => {
  it('persists saved HTTP requests and Java-compatible request history', () => {
    const database = repository()
    const request: HttpRequestDraft = {
      name: 'Example API', method: 'POST', url: 'https://example.com/api',
      params: [{ id: 'p', name: 'page', value: '1', enabled: true }],
      headers: [{ id: 'h', name: 'Accept', value: 'application/json', enabled: true }],
      cookies: [], body: '{"hello":"world"}', bodyType: 'application/json'
    }
    const response: HttpResponseResult = { requestId: 'one', ok: true, status: 200, statusText: 'OK', url: request.url, durationMs: 17, body: '{\n  "ok": true\n}', headers: 'content-type: application/json', cookies: '' }
    const saved = database.saveHttpRequest(request, response)
    expect(database.listHttpRequests('Example')).toEqual([expect.objectContaining({ id: saved.id, method: 'POST', responseBody: response.body })])

    database.saveHttpRequest({ ...request, id: saved.id, method: 'PUT', body: 'updated' }, { ...response, body: 'updated response' })
    expect(database.listHttpRequests()[0]).toMatchObject({ method: 'PUT', body: 'updated', responseBody: 'updated response' })

    const history = database.saveHttpHistory({ ...request, id: saved.id }, response)
    expect(database.listHttpHistory()).toEqual([expect.objectContaining({ id: history.id, requestIdValue: saved.id, status: '200 OK', costTime: 17 })])
    database.close()
  })

  it('persists host profiles, translation words and translation history', () => {
    const database = repository()
    const host = database.saveHost({ name: 'Local', content: '127.0.0.1 localhost\n' })
    expect(database.listHosts()).toEqual([host])
    expect(database.saveHost({ id: host.id, name: 'Development', content: '127.0.0.1 dev.local\n' })).toMatchObject({ name: 'Development' })

    const word = database.saveTranslationWord({ sourceText: 'hello', targetText: '你好', sourceLang: 'en', targetLang: 'zh-CN', remark: 'greeting' })
    expect(database.listTranslationWords('greet')).toEqual([word])
    expect(database.saveTranslationWord({ id: word.id, sourceText: 'hello', targetText: '您好', sourceLang: 'en', targetLang: 'zh-CN', remark: 'formal' })).toMatchObject({ targetText: '您好' })

    const history = database.saveTranslationHistory({ sourceText: 'hello', targetText: '你好', sourceLang: 'en', targetLang: 'zh-CN', translatorType: 'google' })
    expect(database.listTranslationHistory()).toEqual([history])
    database.close()
  })
})
