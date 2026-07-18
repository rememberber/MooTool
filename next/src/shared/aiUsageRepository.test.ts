import { mkdtemp, rm } from 'node:fs/promises'
import { tmpdir } from 'node:os'
import { join } from 'node:path'
import { afterEach, describe, expect, it } from 'vitest'
import { AiUsageRepository } from '../../electron/main/ai/usageRepository'
import type { AiUsageEventInput } from './contracts/aiUsage'

const temporaryDirectories: string[] = []
const now = new Date('2026-07-18T12:00:00.000Z')

afterEach(async () => {
  await Promise.all(temporaryDirectories.splice(0).map((directory) => rm(directory, { recursive: true, force: true })))
})

describe('AiUsageRepository', () => {
  it('imports idempotently and updates a stable source instead of double counting cumulative logs', async () => {
    const repository = await createRepository()
    const event = usageEvent({ inputTokens: 100, outputTokens: 20 })

    expect(repository.import([event])).toEqual({ imported: 1, updated: 0, unchanged: 0 })
    expect(repository.import([event])).toEqual({ imported: 0, updated: 0, unchanged: 1 })
    expect(repository.import([{ ...event, inputTokens: 140, outputTokens: 30 }])).toEqual({ imported: 0, updated: 1, unchanged: 0 })

    const dashboard = repository.dashboard({ rangeDays: 7, timezoneOffsetMinutes: -480 })
    expect(dashboard.totals).toMatchObject({ events: 1, inputTokens: 140, outputTokens: 30, totalTokens: 170 })
    repository.close()
  })

  it('keeps billed and estimated costs separate and aggregates by day, model, client, and project', async () => {
    const repository = await createRepository()
    repository.import([
      usageEvent({ sourceFingerprint: 'event-a', startedAt: '2026-07-17T23:30:00.000Z', estimatedCost: { currency: 'USD', micros: 100_000 } }),
      usageEvent({ sourceFingerprint: 'event-b', startedAt: '2026-07-18T01:00:00.000Z', model: 'claude-test', clientId: 'claudeCode', projectId: '/project/b', billedCost: { currency: 'USD', micros: 90_000 } })
    ])

    const dashboard = repository.dashboard({ rangeDays: 2, timezoneOffsetMinutes: -480 })

    expect(dashboard.totals.estimatedCosts).toEqual([{ currency: 'USD', micros: 100_000 }])
    expect(dashboard.totals.billedCosts).toEqual([{ currency: 'USD', micros: 90_000 }])
    expect(dashboard.byModel.map((item) => item.key)).toEqual(expect.arrayContaining(['gpt-test', 'claude-test']))
    expect(dashboard.byClient.map((item) => item.key)).toEqual(expect.arrayContaining(['codex', 'claudeCode']))
    expect(dashboard.byProject.map((item) => item.key)).toEqual(expect.arrayContaining(['/project/a', '/project/b']))
    expect(dashboard.trend).toHaveLength(2)
    repository.close()
  })

  it('evaluates token and financial budgets independently', async () => {
    const repository = await createRepository()
    repository.import([usageEvent({ inputTokens: 800, outputTokens: 200, billedCost: { currency: 'USD', micros: 750_000 } })])
    repository.saveBudget({ period: 'daily', tokenLimit: 2_000, costLimit: { currency: 'USD', micros: 1_000_000 }, enabled: true })

    const status = repository.dashboard({ rangeDays: 1, timezoneOffsetMinutes: -480 }).budgets[0]

    expect(status).toMatchObject({ usedTokens: 1_000, tokenRatio: 0.5, usedCost: { currency: 'USD', micros: 750_000 }, costRatio: 0.75 })
    repository.close()
  })

  it('claims only increasing budget thresholds within the same period', async () => {
    const repository = await createRepository()
    expect(repository.claimBudgetNotification('daily', '2026-07-18', 50)).toBe(true)
    expect(repository.claimBudgetNotification('daily', '2026-07-18', 50)).toBe(false)
    expect(repository.claimBudgetNotification('daily', '2026-07-18', 80)).toBe(true)
    expect(repository.claimBudgetNotification('daily', '2026-07-18', 50)).toBe(false)
    expect(repository.claimBudgetNotification('daily', '2026-07-19', 50)).toBe(true)
    repository.close()
  })

  it('flags growth anomalies only after at least three active baseline days', async () => {
    const repository = await createRepository()
    repository.import([
      usageEvent({ sourceFingerprint: 'baseline-1', startedAt: '2026-07-15T10:00:00.000Z', inputTokens: 900, outputTokens: 100 }),
      usageEvent({ sourceFingerprint: 'baseline-2', startedAt: '2026-07-16T10:00:00.000Z', inputTokens: 900, outputTokens: 100 }),
      usageEvent({ sourceFingerprint: 'baseline-3', startedAt: '2026-07-17T10:00:00.000Z', inputTokens: 900, outputTokens: 100 }),
      usageEvent({ sourceFingerprint: 'spike', startedAt: '2026-07-18T10:00:00.000Z', inputTokens: 9_000, outputTokens: 1_000 })
    ])

    expect(repository.dashboard({ rangeDays: 7, timezoneOffsetMinutes: 0 }).anomalies).toEqual([{
      date: '2026-07-18', totalTokens: 10_000, baselineAverageTokens: 1_000, ratio: 10
    }])
    repository.close()
  })

  it('clears imported statistics without touching source files', async () => {
    const repository = await createRepository()
    repository.import([usageEvent()])

    expect(repository.clear()).toBe(1)
    expect(repository.dashboard({ rangeDays: 30, timezoneOffsetMinutes: 0 }).totals.events).toBe(0)
    repository.close()
  })
})

async function createRepository(): Promise<AiUsageRepository> {
  const directory = await mkdtemp(join(tmpdir(), 'mootool-usage-repository-'))
  temporaryDirectories.push(directory)
  return new AiUsageRepository(join(directory, 'usage.db'), () => now)
}

function usageEvent(overrides: Partial<AiUsageEventInput> = {}): AiUsageEventInput {
  return {
    source: 'localLog',
    provider: 'openai',
    clientId: 'codex',
    projectId: '/project/a',
    sessionId: 'session-a',
    model: 'gpt-test',
    startedAt: '2026-07-18T10:00:00.000Z',
    inputTokens: 100,
    outputTokens: 20,
    cachedInputTokens: 40,
    requestCount: 1,
    sourceFingerprint: 'event-default',
    ...overrides
  }
}
