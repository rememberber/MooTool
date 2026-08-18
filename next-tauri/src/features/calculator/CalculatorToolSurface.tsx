import { useCallback, useRef, useState } from 'react'
import { useLocalizedMessages } from '../../app/localizedMessages'
import { toolWebviewApis } from '../../platform/api/toolWebviewApi'
import { CalculatorPage, type CalculatorSessionState } from './CalculatorPage'
import { describeCalculatorState } from './calculatorSession'
import { calculatorMessages } from './calculatorMessages'

export function CalculatorToolSurface() {
  const { locale, t } = useLocalizedMessages(calculatorMessages)
  const sessionId = useRef(crypto.randomUUID())
  const revision = useRef(0)
  const [reportError, setReportError] = useState('')

  const reportState = useCallback((state: CalculatorSessionState) => {
    revision.current += 1
    const description = describeCalculatorState(state, locale)
    void toolWebviewApis.calculator.report({
      sessionId: sessionId.current,
      stateRevision: revision.current,
      ...description
    }).then(() => setReportError('')).catch((error: unknown) => {
      setReportError(error instanceof Error ? error.message : String(error))
    })
  }, [locale])

  return (
    <main className="tool-surface-scroll">
      <CalculatorPage onStateChange={reportState} />
      {reportError && (
        <p className="tool-surface-report-error" role="alert">
          {t('report.error', { error: reportError })}
        </p>
      )}
    </main>
  )
}
