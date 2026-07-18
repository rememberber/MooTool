import { mkdtemp, rm } from 'node:fs/promises'
import { tmpdir } from 'node:os'
import { join } from 'node:path'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { OpenAiUsageSyncService } from '../../electron/main/ai/openAiUsageSyncService'
import { AiUsageRepository } from '../../electron/main/ai/usageRepository'

const temporaryDirectories: string[] = []
const now = new Date('2026-07-18T12:00:00.000Z')

afterEach(async () => {
  await Promise.all(temporaryDirectories.splice(0).map((directory) => rm(directory, { recursive: true, force: true })))
})

describe('OpenAiUsageSyncService', () => {
  it('paginates Usage and Costs, imports authoritative amounts, and never returns the Admin key', async () => {
    const repository = await createRepository()
    const adminKey = 'sk-admin-secret-must-not-cross-service-boundary'
    const requests: Array<{ url: URL; authorization: string }> = []
    const fetcher = vi.fn(async (input: string | URL | Request, init?: RequestInit) => {
      const url = new URL(String(input))
      requests.push({ url, authorization: new Headers(init?.headers).get('authorization') ?? '' })
      if (url.pathname.endsWith('/usage/completions')) {
        if (!url.searchParams.has('page')) return jsonResponse({ data: [{ start_time: 1784246400, end_time: 1784332800, results: [{ input_tokens: 100, output_tokens: 25, input_cached_tokens: 40, num_model_requests: 2, project_id: 'proj-a', model: 'gpt-test' }] }], has_more: true, next_page: 'usage-next' })
        return jsonResponse({ data: [{ start_time: 1784332800, end_time: 1784419200, results: [{ input_tokens: 200, output_tokens: 50, num_model_requests: 3, project_id: 'proj-a', model: 'gpt-test' }] }], has_more: false, next_page: null })
      }
      return jsonResponse({ data: [{ start_time: 1784332800, end_time: 1784419200, results: [{ amount: { value: 1.25, currency: 'usd' }, line_item: 'Text models', project_id: 'proj-a' }] }], has_more: false, next_page: null })
    })
    const service = new OpenAiUsageSyncService({ repository, credentialProvider: () => adminKey, fetcher, clock: () => now })

    const first = await service.sync({ provider: 'openai', rangeDays: 7, timezoneOffsetMinutes: -480 })
    expect(first).toMatchObject({ provider: 'openai', imported: 3, updated: 0, unchanged: 0, usageEvents: 2, costEvents: 1 })
    expect(first.dashboard.totals).toMatchObject({ inputTokens: 300, outputTokens: 75, cachedInputTokens: 40, requests: 5, billedCosts: [{ currency: 'USD', micros: 1_250_000 }] })
    expect(JSON.stringify(first)).not.toContain(adminKey)
    expect(requests).toHaveLength(3)
    expect(requests.every((request) => request.authorization === `Bearer ${adminKey}`)).toBe(true)
    expect(requests[0].url.searchParams.getAll('group_by')).toEqual(['model', 'project_id'])
    expect(requests.some((request) => request.url.searchParams.get('page') === 'usage-next')).toBe(true)
    expect(requests.find((request) => request.url.pathname.endsWith('/organization/costs'))?.url.searchParams.getAll('group_by')).toEqual(['project_id', 'line_item'])

    const second = await service.sync({ provider: 'openai', rangeDays: 7, timezoneOffsetMinutes: -480 })
    expect(second).toMatchObject({ imported: 0, updated: 0, unchanged: 3 })
    repository.close()
  })

  it('requires a configured credential and does not surface remote error bodies', async () => {
    const repository = await createRepository()
    const missing = new OpenAiUsageSyncService({ repository, credentialProvider: () => '', fetcher: vi.fn() })
    await expect(missing.sync({ provider: 'openai', rangeDays: 7, timezoneOffsetMinutes: 0 })).rejects.toThrow('not configured')

    const denied = new OpenAiUsageSyncService({ repository, credentialProvider: () => 'secret-key', fetcher: async () => new Response('remote secret response', { status: 401 }) })
    await expect(denied.sync({ provider: 'openai', rangeDays: 7, timezoneOffsetMinutes: 0 })).rejects.toThrow('HTTP 401')
    await expect(denied.sync({ provider: 'openai', rangeDays: 7, timezoneOffsetMinutes: 0 })).rejects.not.toThrow('remote secret response')
    repository.close()
  })
})

async function createRepository(): Promise<AiUsageRepository> {
  const directory = await mkdtemp(join(tmpdir(), 'mootool-openai-usage-'))
  temporaryDirectories.push(directory)
  return new AiUsageRepository(join(directory, 'usage.db'), () => now)
}

function jsonResponse(value: unknown): Response {
  return new Response(JSON.stringify(value), { status: 200, headers: { 'content-type': 'application/json' } })
}
