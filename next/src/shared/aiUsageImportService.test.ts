import { mkdtemp, rm, writeFile } from 'node:fs/promises'
import { tmpdir } from 'node:os'
import { join } from 'node:path'
import { afterEach, describe, expect, it } from 'vitest'
import { AiUsageRepository } from '../../electron/main/ai/usageRepository'
import { UsageImportService } from '../../electron/main/ai/usageImportService'

const temporaryDirectories: string[] = []
const now = new Date('2026-07-18T12:00:00.000Z')

afterEach(async () => {
  await Promise.all(temporaryDirectories.splice(0).map((directory) => rm(directory, { recursive: true, force: true })))
})

describe('UsageImportService', () => {
  it('previews Codex cumulative token metadata and imports only the final total without retaining messages', async () => {
    const fixture = await createFixture()
    const path = join(fixture.directory, 'codex-session.jsonl')
    await writeFile(path, [
      { timestamp: '2026-07-18T08:00:00.000Z', type: 'session_meta', payload: { id: 'codex-session', cwd: '/project/codex' } },
      { timestamp: '2026-07-18T08:00:01.000Z', type: 'turn_context', payload: { model: 'gpt-test', user_message: 'SECRET PROMPT MUST NOT PERSIST' } },
      { timestamp: '2026-07-18T08:00:02.000Z', type: 'event_msg', payload: { type: 'token_count', info: { total_token_usage: { input_tokens: 100, cached_input_tokens: 40, output_tokens: 20, reasoning_output_tokens: 5 } } } },
      { timestamp: '2026-07-18T08:00:03.000Z', type: 'event_msg', payload: { type: 'token_count', info: { total_token_usage: { input_tokens: 180, cached_input_tokens: 60, output_tokens: 30, reasoning_output_tokens: 8 } } } }
    ].map((value) => JSON.stringify(value)).join('\n'))

    const preview = await fixture.service.preview({ paths: [path] })

    expect(preview).toMatchObject({ events: 1, uniqueEvents: 1, duplicates: 0, fields: expect.arrayContaining(['inputTokens', 'cachedInputTokens', 'reasoningTokens']) })
    expect(JSON.stringify(preview)).not.toContain('SECRET PROMPT')
    const result = await fixture.service.apply(preview.planId, -480)
    expect(result).toMatchObject({ imported: 1, updated: 0, unchanged: 0 })
    expect(result.dashboard.totals).toMatchObject({ inputTokens: 180, outputTokens: 30, cachedInputTokens: 60, reasoningTokens: 8 })

    const secondPreview = await fixture.service.preview({ paths: [path] })
    expect(secondPreview.duplicates).toBe(1)
    expect(await fixture.service.apply(secondPreview.planId, -480)).toMatchObject({ imported: 0, updated: 0, unchanged: 1 })
    fixture.repository.close()
  })

  it('sums Claude assistant usage while keeping cache reads and writes distinct', async () => {
    const fixture = await createFixture()
    const path = join(fixture.directory, 'claude-session.jsonl')
    await writeFile(path, [
      { type: 'assistant', sessionId: 'claude-session', cwd: '/project/claude', timestamp: '2026-07-18T09:00:00.000Z', message: { model: 'claude-test', content: 'SECRET RESPONSE', usage: { input_tokens: 10, output_tokens: 20, cache_read_input_tokens: 30, cache_creation_input_tokens: 40 } } },
      { type: 'assistant', sessionId: 'claude-session', cwd: '/project/claude', timestamp: '2026-07-18T09:01:00.000Z', message: { model: 'claude-test', usage: { input_tokens: 5, output_tokens: 8, cache_read_input_tokens: 9, cache_creation_input_tokens: 7 } } }
    ].map((value) => JSON.stringify(value)).join('\n'))

    const preview = await fixture.service.preview({ paths: [path] })
    const result = await fixture.service.apply(preview.planId, -480)

    expect(result.dashboard.totals).toMatchObject({ inputTokens: 15, outputTokens: 28, cachedInputTokens: 39, cacheWriteTokens: 47, requests: 2 })
    expect(JSON.stringify(preview)).not.toContain('SECRET RESPONSE')
    fixture.repository.close()
  })

  it('imports normalized events while preserving the difference between estimated and billed costs', async () => {
    const fixture = await createFixture()
    const path = join(fixture.directory, 'normalized.json')
    await writeFile(path, JSON.stringify({ events: [{
      source: 'providerApi', provider: 'openai', clientId: 'api', model: 'gpt-test', startedAt: '2026-07-18T10:00:00.000Z',
      inputTokens: 100, outputTokens: 20, estimatedCost: { currency: 'USD', micros: 120000 }, billedCost: { currency: 'USD', micros: 100000 }, sourceFingerprint: 'provider-event-1'
    }] }))

    const preview = await fixture.service.preview({ paths: [path] })
    const result = await fixture.service.apply(preview.planId, -480)

    expect(result.dashboard.totals.estimatedCosts).toEqual([{ currency: 'USD', micros: 120_000 }])
    expect(result.dashboard.totals.billedCosts).toEqual([{ currency: 'USD', micros: 100_000 }])
    fixture.repository.close()
  })

  it('rejects an apply when the source changes after preview', async () => {
    const fixture = await createFixture()
    const path = join(fixture.directory, 'usage.json')
    await writeFile(path, JSON.stringify([{ model: 'model-a', startedAt: '2026-07-18T10:00:00.000Z', inputTokens: 1, outputTokens: 2 }]))
    const preview = await fixture.service.preview({ paths: [path] })
    await writeFile(path, JSON.stringify([{ model: 'model-b', startedAt: '2026-07-18T10:00:00.000Z', inputTokens: 10, outputTokens: 20 }]))

    await expect(fixture.service.apply(preview.planId, -480)).rejects.toThrow('source changed')
    expect(fixture.repository.dashboard({ rangeDays: 30, timezoneOffsetMinutes: -480 }).totals.events).toBe(0)
    fixture.repository.close()
  })
})

async function createFixture(): Promise<{ directory: string; repository: AiUsageRepository; service: UsageImportService }> {
  const directory = await mkdtemp(join(tmpdir(), 'mootool-usage-import-'))
  temporaryDirectories.push(directory)
  const repository = new AiUsageRepository(join(directory, 'usage.db'), () => now)
  return { directory, repository, service: new UsageImportService({ repository, clock: () => now }) }
}
