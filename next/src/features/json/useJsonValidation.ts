import { useEffect, useMemo, useState } from 'react'
import type { Language, MessageKey } from '@/shared/i18n/messages'
import { runTextTask, isTaskCancelled } from '@/shared/workers/runTextTask'
import { validateJson, type JsonStatus } from './jsonTools'

type Translate = (key: MessageKey, params?: Record<string, string>) => string
const workerThreshold = 50_000

export function useJsonValidation(input: string, language: Language, t: Translate): JsonStatus {
  const [completed, setCompleted] = useState<{ input: string; language: Language; status: JsonStatus } | null>(null)
  const smallResult = useMemo(() => input.length < workerThreshold ? validateJson(input, t) : null, [input, t])
  useEffect(() => {
    if (input.length < workerThreshold) return
    const controller = new AbortController()
    const timer = setTimeout(() => {
      void runTextTask({ kind: 'validateJson', input, language }, controller.signal).then(status => {
        if (!controller.signal.aborted) setCompleted({ input, language, status })
      }).catch(error => {
        if (!isTaskCancelled(error) && !controller.signal.aborted) {
          setCompleted({ input, language, status: { kind: 'error', message: String(error.message ?? error) } })
        }
      })
    }, 150)
    return () => { clearTimeout(timer); controller.abort() }
  }, [input, language])
  return smallResult ?? (completed?.input === input && completed.language === language ? completed.status : { kind: 'idle', message: t('common.processing') })
}
