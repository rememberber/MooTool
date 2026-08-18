import { useCallback } from 'react'
import { historyApi } from '../../platform/api/historyApi'
import type { OperationStatus } from '../../platform/contracts/history'
import { useSettings } from '../settings/SettingsProvider'

export function useOperationHistory(toolId: string) {
  const { settings } = useSettings()
  return useCallback((action: string, summary: string, status: OperationStatus = 'success') => {
    void historyApi.record({
      id: crypto.randomUUID(),
      toolId,
      action,
      summary,
      status,
      createdAt: Date.now()
    }, settings.data.historyLimit).catch(() => undefined)
  }, [settings.data.historyLimit, toolId])
}
