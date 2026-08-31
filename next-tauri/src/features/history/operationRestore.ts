import { useEffect, useRef } from 'react'
import type { OperationHistory } from '../../platform/contracts/history'

const RESTORE_EVENT = 'mootool-operation-restore'
let pending: OperationHistory | undefined

export function queueOperationRestore(entry: OperationHistory): void {
  pending = entry
  window.dispatchEvent(new CustomEvent<OperationHistory>(RESTORE_EVENT, { detail: entry }))
}

export function useOperationRestore(toolId: string, restore: (entry: OperationHistory) => void): void {
  const restoreRef = useRef(restore)
  restoreRef.current = restore
  useEffect(() => {
    const apply = (entry: OperationHistory | undefined) => {
      if (!entry || entry.toolId !== toolId || (!entry.inputText && !entry.outputText && entry.metadataJson === '{}')) return
      restoreRef.current(entry)
      if (pending?.id === entry.id) pending = undefined
    }
    const listener = (event: Event) => apply((event as CustomEvent<OperationHistory>).detail)
    window.addEventListener(RESTORE_EVENT, listener)
    apply(pending)
    return () => window.removeEventListener(RESTORE_EVENT, listener)
  }, [toolId])
}

export function parseOperationMetadata(entry: OperationHistory): Record<string, unknown> {
  try {
    const value = JSON.parse(entry.metadataJson) as unknown
    return value && typeof value === 'object' && !Array.isArray(value) ? value as Record<string, unknown> : {}
  } catch {
    return {}
  }
}
