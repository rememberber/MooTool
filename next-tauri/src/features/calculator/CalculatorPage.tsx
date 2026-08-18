import { ArrowDown, ArrowUp, Equal, History, RotateCcw } from 'lucide-react'
import { useEffect, useState } from 'react'
import { useLocalizedMessages, type LocalizedMessageKey } from '../../app/localizedMessages'
import {
  CalculatorToolError,
  type CalculatorToolErrorCode,
  combination,
  convertBase,
  evaluateExpression,
  greatestCommonDivisor,
  leastCommonMultiple,
  permutation
} from './calculator'
import { calculatorMessages } from './calculatorMessages'

type CalculatorMessageKey = LocalizedMessageKey<typeof calculatorMessages>
export type CalculatorOperation = 'expression' | 'hexToDec' | 'decToHex' | 'decToBin' | 'binToDec' | 'gcd' | 'lcm' | 'permutation' | 'combination'
export interface CalculatorHistoryEntry { operation: CalculatorOperation; input: string; output: string }
export interface CalculatorErrorState { code?: CalculatorToolErrorCode; raw?: string }

export interface CalculatorSessionState {
  expression: string
  result: string
  decimal: string
  hex: string
  binary: string
  gcdValues: string[]
  lcmValues: string[]
  permutationValues: string[]
  combinationValues: string[]
  history: CalculatorHistoryEntry[]
  error: CalculatorErrorState | null
}

export function CalculatorPage({ onStateChange }: {
  onStateChange?(state: CalculatorSessionState): void
}) {
  const { t } = useLocalizedMessages(calculatorMessages)
  const [expression, setExpression] = useState('2 * (3 + 4)')
  const [result, setResult] = useState('14')
  const [decimal, setDecimal] = useState('255')
  const [hex, setHex] = useState('ff')
  const [binary, setBinary] = useState('11111111')
  const [gcdValues, setGcdValues] = useState(['54', '24'])
  const [lcmValues, setLcmValues] = useState(['54', '24'])
  const [permutationValues, setPermutationValues] = useState(['5', '2'])
  const [combinationValues, setCombinationValues] = useState(['5', '2'])
  const [history, setHistory] = useState<CalculatorHistoryEntry[]>([
    { operation: 'expression', input: '2 * (3 + 4)', output: '14' }
  ])
  const [error, setError] = useState<CalculatorErrorState | null>(null)
  const errorText = error ? error.code ? t(`error.${error.code}`) : error.raw || t('error.unknown') : ''

  useEffect(() => {
    onStateChange?.({
      expression,
      result,
      decimal,
      hex,
      binary,
      gcdValues,
      lcmValues,
      permutationValues,
      combinationValues,
      history,
      error
    })
  }, [
    binary,
    combinationValues,
    decimal,
    error,
    expression,
    gcdValues,
    hex,
    history,
    lcmValues,
    onStateChange,
    permutationValues,
    result
  ])

  function run(operationId: CalculatorOperation, input: string, operation: () => string): void {
    try {
      const output = operation()
      setResult(output)
      setError(null)
      setHistory((current) => [{ operation: operationId, input, output }, ...current].slice(0, 12))
    } catch (cause) {
      setError(cause instanceof CalculatorToolError
        ? { code: cause.code }
        : { raw: cause instanceof Error ? cause.message : t('error.unknown') })
    }
  }

  function evaluate(): void {
    run('expression', expression, () => evaluateExpression(expression))
  }

  return (
    <section className="tool-page">
      <header className="tool-header">
        <div>
          <span className="eyebrow">DAILY TOOL</span>
          <h1>{t('title')}</h1>
        </div>
        <button className="secondary-button" type="button" onClick={() => setHistory([])}>
          <RotateCcw />{t('action.clearHistory')}
        </button>
      </header>

      <div className="calculator-layout">
        <div className="calculator-controls">
          <section className="tool-card calculator-expression">
            <h2>{t('arithmetic.title')}</h2>
            <div className="expression-row">
              <label className="sr-only" htmlFor="calculator-expression">{t('expression.label')}</label>
              <input
                id="calculator-expression"
                value={expression}
                onChange={(event) => setExpression(event.target.value)}
                onKeyDown={(event) => { if (event.key === 'Enter') evaluate() }}
              />
              <button className="primary-button" type="button" onClick={evaluate}>
                <Equal />{t('action.calculate')}
              </button>
            </div>
            {error && <p className="error-message" role="alert">{errorText}</p>}
          </section>

          <section className="tool-card">
            <h2>{t('base.title')}</h2>
            <div className="base-grid">
              <BaseInput label={t('base.hex')} value={hex} onChange={setHex} />
              <div className="conversion-actions">
                <button type="button" onClick={() => run('hexToDec', hex, () => {
                  const value = convertBase(hex, 16, 10)
                  setDecimal(value)
                  return value
                })}><ArrowDown />HEX → DEC</button>
                <button type="button" onClick={() => run('decToHex', decimal, () => {
                  const value = convertBase(decimal, 10, 16)
                  setHex(value)
                  return value
                })}><ArrowUp />DEC → HEX</button>
              </div>
              <BaseInput label={t('base.decimal')} value={decimal} onChange={setDecimal} />
              <div className="conversion-actions">
                <button type="button" onClick={() => run('decToBin', decimal, () => {
                  const value = convertBase(decimal, 10, 2)
                  setBinary(value)
                  return value
                })}><ArrowDown />DEC → BIN</button>
                <button type="button" onClick={() => run('binToDec', binary, () => {
                  const value = convertBase(binary, 2, 10)
                  setDecimal(value)
                  return value
                })}><ArrowUp />BIN → DEC</button>
              </div>
              <BaseInput label={t('base.binary')} value={binary} onChange={setBinary} />
            </div>
          </section>

          <IntegerOperation
            title={t('integer.gcd')}
            values={gcdValues}
            onChange={setGcdValues}
            action={t('integer.calculateGcd')}
            onRun={() => run('gcd', gcdValues.join(', '), () => greatestCommonDivisor(gcdValues[0], gcdValues[1]))}
          />
          <IntegerOperation
            title={t('integer.lcm')}
            values={lcmValues}
            onChange={setLcmValues}
            action={t('integer.calculateLcm')}
            onRun={() => run('lcm', lcmValues.join(', '), () => leastCommonMultiple(lcmValues[0], lcmValues[1]))}
          />
          <IntegerOperation
            title={t('integer.permutation')}
            labels={['n', 'm']}
            values={permutationValues}
            onChange={setPermutationValues}
            action="A(n,m)"
            onRun={() => run('permutation', permutationValues.join(', '), () => permutation(permutationValues[0], permutationValues[1]))}
          />
          <IntegerOperation
            title={t('integer.combination')}
            labels={['n', 'm']}
            values={combinationValues}
            onChange={setCombinationValues}
            action="C(n,m)"
            onRun={() => run('combination', combinationValues.join(', '), () => combination(combinationValues[0], combinationValues[1]))}
          />
        </div>

        <aside className="calculator-output">
          <div className="result-panel">
            <span>{t('result.title')}</span>
            <output>{result}</output>
          </div>
          <div className="history-panel">
            <h2><History />{t('history.title')}</h2>
            {history.length === 0
              ? <p className="empty-state">{t('history.empty')}</p>
              : history.map((item, index) => (
                  <p key={`${index}-${item.operation}-${item.input}`}>
                    {t('history.entry', { label: t(`history.${item.operation}`), input: item.input, output: item.output })}
                  </p>
                ))}
          </div>
        </aside>
      </div>
    </section>
  )
}

function BaseInput({ label, value, onChange }: {
  label: string
  value: string
  onChange(value: string): void
}) {
  return (
    <label className="base-input">
      <span>{label}</span>
      <input value={value} onChange={(event) => onChange(event.target.value)} />
    </label>
  )
}

function IntegerOperation({ title, labels, values, onChange, action, onRun }: {
  title: string
  labels?: [string, string]
  values: string[]
  onChange(values: string[]): void
  action: string
  onRun(): void
}) {
  const { t } = useLocalizedMessages(calculatorMessages)
  const resolvedLabels = labels ?? [t('integer.value1'), t('integer.value2')]
  return (
    <section className="tool-card operation-card">
      <h2>{title}</h2>
      <div className="operation-row">
        {values.map((value, index) => (
          <label key={resolvedLabels[index]}>
            <span>{resolvedLabels[index]}</span>
            <input
              value={value}
              onChange={(event) => onChange(values.map((current, itemIndex) => itemIndex === index ? event.target.value : current))}
            />
          </label>
        ))}
        <button className="secondary-button" type="button" onClick={onRun}>{action}</button>
      </div>
    </section>
  )
}
