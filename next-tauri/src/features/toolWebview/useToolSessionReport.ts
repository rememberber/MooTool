import { useEffect, useRef, useState } from 'react'
import { toolWebviewApis } from '../../platform/api/toolWebviewApi'
import type { ManagedToolId } from '../../platform/contracts/toolWebview'

export function useToolSessionReport(
  toolId: ManagedToolId,
  digest: string,
  summary: string
): { sessionId: string; reportError: string } {
  const sessionId = useRef(crypto.randomUUID())
  const revision = useRef(0)
  const [reportError, setReportError] = useState('')

  useEffect(() => {
    revision.current += 1
    void toolWebviewApis[toolId].report({
      sessionId: sessionId.current,
      stateRevision: revision.current,
      stateDigest: digest,
      stateSummary: summary
    }).then(() => setReportError('')).catch((cause: unknown) => {
      setReportError(cause instanceof Error ? cause.message : String(cause))
    })
  }, [digest, summary, toolId])

  return { sessionId: sessionId.current, reportError }
}
