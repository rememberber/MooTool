import { describe, expect, it, vi } from 'vitest'
import { UsageBudgetNotificationService } from '../../electron/main/ai/usageBudgetNotificationService'
import type { AiUsageDashboard } from './contracts/aiUsage'

describe('UsageBudgetNotificationService', () => {
  it('notifies each higher threshold once per local budget period', () => {
    const claimed = new Map<string, number>()
    const notify = vi.fn()
    const service = new UsageBudgetNotificationService({
      repository: {
        claimBudgetNotification: (period, periodKey, threshold) => {
          const key = `${period}:${periodKey}`
          if ((claimed.get(key) ?? 0) >= threshold) return false
          claimed.set(key, threshold)
          return true
        }
      },
      notify
    })

    const dashboard = budgetDashboard('2026-07-18T01:00:00.000Z', 0.81)
    expect(service.evaluate(dashboard, -480)).toEqual([expect.objectContaining({ period: 'daily', periodKey: '2026-07-18', threshold: 80, tokenRatio: 0.81 })])
    expect(service.evaluate(dashboard, -480)).toEqual([])

    dashboard.budgets[0].tokenRatio = 0.51
    expect(service.evaluate(dashboard, -480)).toEqual([])
    dashboard.budgets[0].tokenRatio = 1.01
    expect(service.evaluate(dashboard, -480)).toEqual([expect.objectContaining({ threshold: 100 })])

    const nextDay = budgetDashboard('2026-07-19T01:00:00.000Z', 1.01)
    expect(service.evaluate(nextDay, -480)).toEqual([expect.objectContaining({ periodKey: '2026-07-19', threshold: 100 })])
    expect(notify).toHaveBeenCalledTimes(3)
  })

  it('ignores disabled budgets and contains native notifier failures', () => {
    const service = new UsageBudgetNotificationService({
      repository: { claimBudgetNotification: () => true },
      notify: () => { throw new Error('native notifications unavailable') }
    })
    const enabled = budgetDashboard('2026-07-18T01:00:00.000Z', 0.5)
    expect(service.evaluate(enabled, 0)).toHaveLength(1)
    enabled.budgets[0].budget.enabled = false
    expect(service.evaluate(enabled, 0)).toEqual([])
  })
})

function budgetDashboard(generatedAt: string, tokenRatio: number): AiUsageDashboard {
  const totals = { events: 0, requests: 0, inputTokens: 0, outputTokens: 0, cachedInputTokens: 0, cacheWriteTokens: 0, reasoningTokens: 0, totalTokens: 0, estimatedCosts: [], billedCosts: [] }
  return {
    generatedAt,
    range: { from: generatedAt, to: generatedAt, days: 1 },
    totals,
    trend: [], byModel: [], byClient: [], byProject: [], anomalies: [],
    budgets: [{ budget: { period: 'daily', tokenLimit: 1_000, enabled: true, updatedAt: generatedAt }, usedTokens: Math.round(1_000 * tokenRatio), tokenRatio }]
  }
}
