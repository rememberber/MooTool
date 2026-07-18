import { mkdtemp, rm } from 'node:fs/promises'
import { tmpdir } from 'node:os'
import { join } from 'node:path'
import { afterEach, describe, expect, it } from 'vitest'
import { UsageExportService } from '../../electron/main/ai/usageExportService'
import { AiUsageRepository } from '../../electron/main/ai/usageRepository'

const temporaryDirectories: string[] = []
const now = new Date('2026-07-18T12:00:00.000Z')

afterEach(async () => {
  await Promise.all(temporaryDirectories.splice(0).map((directory) => rm(directory, { recursive: true, force: true })))
})

describe('UsageExportService', () => {
  it('exports normalized JSON metadata without prompt or response fields', async () => {
    const repository = await fixture()
    repository.import([{
      source: 'localRuntime', provider: 'ollama', clientId: 'codex', projectId: '/project,one', modelRuntimeId: 'ollama',
      localModelDigest: 'sha256:model', sessionId: 'session-1', model: 'qwen3:8b', startedAt: '2026-07-18T10:00:00.000Z',
      inputTokens: 120, outputTokens: 30, reasoningTokens: 5, requestCount: 1, sourceFingerprint: 'export-fixture'
    }])
    const service = new UsageExportService(repository)

    const document = service.create({ rangeDays: 7, timezoneOffsetMinutes: -480, format: 'json' })

    expect(document).toMatchObject({ format: 'json', extension: 'json', events: 1 })
    const parsed = JSON.parse(document.content) as { events: Array<Record<string, unknown>> }
    expect(parsed.events[0]).toMatchObject({ provider: 'ollama', modelRuntimeId: 'ollama', inputTokens: 120, outputTokens: 30 })
    expect(document.content).not.toMatch(/"(?:prompt|response|toolArguments)"/i)
    repository.close()
  })

  it('escapes CSV metadata values and preserves separate token columns', async () => {
    const repository = await fixture()
    repository.import([{
      source: 'import', provider: 'openai', clientId: 'codex', projectId: '/project,"quoted"', model: 'gpt-test',
      startedAt: '2026-07-18T10:00:00.000Z', inputTokens: 100, outputTokens: 20, cachedInputTokens: 40,
      cacheWriteTokens: 10, reasoningTokens: 3, sourceFingerprint: 'csv-fixture'
    }])

    const document = new UsageExportService(repository).create({ rangeDays: 1, timezoneOffsetMinutes: -480, format: 'csv' })

    expect(document.content).toContain('inputTokens,outputTokens,cachedInputTokens,cacheWriteTokens,reasoningTokens')
    expect(document.content).toContain('"/project,""quoted"""')
    expect(document.events).toBe(1)
    repository.close()
  })
})

async function fixture(): Promise<AiUsageRepository> {
  const directory = await mkdtemp(join(tmpdir(), 'mootool-usage-export-'))
  temporaryDirectories.push(directory)
  return new AiUsageRepository(join(directory, 'usage.db'), () => now)
}
