import { useCallback } from 'react'
import { historyApi } from '../../platform/api/historyApi'
import type { OperationHistoryPayload, OperationStatus } from '../../platform/contracts/history'
import { useSettings } from '../settings/SettingsProvider'

export function useOperationHistory(toolId: string) {
  const { settings } = useSettings()
  return useCallback((action: string, summary: string, status: OperationStatus = 'success', payload: OperationHistoryPayload = {}) => {
    void historyApi.record({
      id: crypto.randomUUID(),
      toolId,
      action,
      summary,
      status,
      inputText: truncateUtf8(payload.inputText),
      outputText: truncateUtf8(payload.outputText),
      metadataJson: serializeMetadata(payload.metadata),
      createdAt: Date.now()
    }, settings.data.historyLimit).catch(() => undefined)
  }, [settings.data.historyLimit, toolId])
}

function truncateUtf8(value = '', maxBytes = 512 * 1024): string {
  const encoded = new TextEncoder().encode(value)
  if (encoded.length <= maxBytes) return value
  let end = maxBytes
  while (end > 0 && (encoded[end] & 0xc0) === 0x80) end -= 1
  return new TextDecoder().decode(encoded.slice(0, end))
}

function serializeMetadata(metadata: Record<string, unknown> | undefined): string {
  try {
    const value = JSON.stringify(metadata ?? {})
    return new TextEncoder().encode(value).length <= 64 * 1024 ? value : '{}'
  } catch {
    return '{}'
  }
}
