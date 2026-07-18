import type { AiUsageBudgetPeriod, AiUsageDashboard } from '../../../src/shared/contracts/aiUsage'
import type { AiUsageRepository } from './usageRepository'

export type AiUsageBudgetNotification = {
  period: AiUsageBudgetPeriod
  periodKey: string
  threshold: 50 | 80 | 100
  tokenRatio?: number
  costRatio?: number
}

type UsageBudgetNotificationServiceOptions = {
  repository: Pick<AiUsageRepository, 'claimBudgetNotification'>
  notify: (notification: AiUsageBudgetNotification) => void
}

const thresholds = [100, 80, 50] as const

export class UsageBudgetNotificationService {
  private readonly repository: UsageBudgetNotificationServiceOptions['repository']
  private readonly notify: UsageBudgetNotificationServiceOptions['notify']

  constructor(options: UsageBudgetNotificationServiceOptions) {
    this.repository = options.repository
    this.notify = options.notify
  }

  evaluate(dashboard: AiUsageDashboard, timezoneOffsetMinutes: number): AiUsageBudgetNotification[] {
    const generatedAt = new Date(dashboard.generatedAt)
    const notifications: AiUsageBudgetNotification[] = []
    for (const status of dashboard.budgets) {
      if (!status.budget.enabled) continue
      const maximumRatio = Math.max(status.tokenRatio ?? 0, status.costRatio ?? 0)
      const threshold = thresholds.find((candidate) => maximumRatio >= candidate / 100)
      if (!threshold) continue
      const periodKey = budgetPeriodKey(status.budget.period, generatedAt, timezoneOffsetMinutes)
      if (!this.repository.claimBudgetNotification(status.budget.period, periodKey, threshold)) continue
      const notification: AiUsageBudgetNotification = {
        period: status.budget.period,
        periodKey,
        threshold,
        ...(status.tokenRatio === undefined ? {} : { tokenRatio: status.tokenRatio }),
        ...(status.costRatio === undefined ? {} : { costRatio: status.costRatio })
      }
      notifications.push(notification)
      try { this.notify(notification) } catch { /* Native notification failure must not block Usage data. */ }
    }
    return notifications
  }
}

function budgetPeriodKey(period: AiUsageBudgetPeriod, now: Date, offset: number): string {
  const local = new Date(now.getTime() - offset * 60_000)
  if (period === 'monthly') return local.toISOString().slice(0, 7)
  if (period === 'weekly') {
    const day = local.getUTCDay()
    local.setUTCDate(local.getUTCDate() - (day === 0 ? 6 : day - 1))
  }
  return local.toISOString().slice(0, 10)
}
