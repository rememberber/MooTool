import type { CalculatorSessionState } from './CalculatorPage'
import { createLocalizedTranslator } from '../../app/localizedMessages'
import type { AppLanguage } from '../../platform/contracts/settings'
import { calculatorMessages } from './calculatorMessages'

export interface CalculatorStateDescription {
  stateDigest: string
  stateSummary: string
}

export function describeCalculatorState(
  state: CalculatorSessionState,
  locale: AppLanguage = 'en-US'
): CalculatorStateDescription {
  const { t } = createLocalizedTranslator(calculatorMessages, locale)
  return {
    stateDigest: JSON.stringify(state),
    stateSummary: t('session.summary', { expression: state.expression, result: state.result, count: state.history.length })
  }
}
