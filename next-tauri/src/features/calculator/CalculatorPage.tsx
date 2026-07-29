import { ArrowDown, ArrowUp, Equal, History, RotateCcw } from 'lucide-react'
import { useState } from 'react'
import {
  combination,
  convertBase,
  evaluateExpression,
  greatestCommonDivisor,
  leastCommonMultiple,
  permutation
} from './calculator'

export function CalculatorPage() {
  const [expression, setExpression] = useState('2 * (3 + 4)')
  const [result, setResult] = useState('14')
  const [decimal, setDecimal] = useState('255')
  const [hex, setHex] = useState('ff')
  const [binary, setBinary] = useState('11111111')
  const [gcdValues, setGcdValues] = useState(['54', '24'])
  const [lcmValues, setLcmValues] = useState(['54', '24'])
  const [permutationValues, setPermutationValues] = useState(['5', '2'])
  const [combinationValues, setCombinationValues] = useState(['5', '2'])
  const [history, setHistory] = useState(['表达式: 2 * (3 + 4) = 14'])
  const [error, setError] = useState('')

  function run(label: string, input: string, operation: () => string): void {
    try {
      const output = operation()
      setResult(output)
      setError('')
      setHistory((current) => [`${label}: ${input} = ${output}`, ...current].slice(0, 12))
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : '计算失败')
    }
  }

  function evaluate(): void {
    run('表达式', expression, () => evaluateExpression(expression))
  }

  return (
    <section className="tool-page">
      <header className="tool-header">
        <div>
          <span className="eyebrow">DAILY TOOL</span>
          <h1>计算器</h1>
        </div>
        <button className="secondary-button" type="button" onClick={() => setHistory([])}>
          <RotateCcw />清空记录
        </button>
      </header>

      <div className="calculator-layout">
        <div className="calculator-controls">
          <section className="tool-card calculator-expression">
            <h2>四则运算</h2>
            <div className="expression-row">
              <label className="sr-only" htmlFor="calculator-expression">表达式</label>
              <input
                id="calculator-expression"
                value={expression}
                onChange={(event) => setExpression(event.target.value)}
                onKeyDown={(event) => { if (event.key === 'Enter') evaluate() }}
              />
              <button className="primary-button" type="button" onClick={evaluate}>
                <Equal />计算
              </button>
            </div>
            {error && <p className="error-message" role="alert">{error}</p>}
          </section>

          <section className="tool-card">
            <h2>进制转换</h2>
            <div className="base-grid">
              <BaseInput label="十六进制" value={hex} onChange={setHex} />
              <div className="conversion-actions">
                <button type="button" onClick={() => run('HEX → DEC', hex, () => {
                  const value = convertBase(hex, 16, 10)
                  setDecimal(value)
                  return value
                })}><ArrowDown />HEX → DEC</button>
                <button type="button" onClick={() => run('DEC → HEX', decimal, () => {
                  const value = convertBase(decimal, 10, 16)
                  setHex(value)
                  return value
                })}><ArrowUp />DEC → HEX</button>
              </div>
              <BaseInput label="十进制" value={decimal} onChange={setDecimal} />
              <div className="conversion-actions">
                <button type="button" onClick={() => run('DEC → BIN', decimal, () => {
                  const value = convertBase(decimal, 10, 2)
                  setBinary(value)
                  return value
                })}><ArrowDown />DEC → BIN</button>
                <button type="button" onClick={() => run('BIN → DEC', binary, () => {
                  const value = convertBase(binary, 2, 10)
                  setDecimal(value)
                  return value
                })}><ArrowUp />BIN → DEC</button>
              </div>
              <BaseInput label="二进制" value={binary} onChange={setBinary} />
            </div>
          </section>

          <IntegerOperation
            title="最大公约数"
            values={gcdValues}
            onChange={setGcdValues}
            action="计算 GCD"
            onRun={() => run('GCD', gcdValues.join(', '), () => greatestCommonDivisor(gcdValues[0], gcdValues[1]))}
          />
          <IntegerOperation
            title="最小公倍数"
            values={lcmValues}
            onChange={setLcmValues}
            action="计算 LCM"
            onRun={() => run('LCM', lcmValues.join(', '), () => leastCommonMultiple(lcmValues[0], lcmValues[1]))}
          />
          <IntegerOperation
            title="排列 A(n,m)"
            labels={['n', 'm']}
            values={permutationValues}
            onChange={setPermutationValues}
            action="A(n,m)"
            onRun={() => run('A(n,m)', permutationValues.join(', '), () => permutation(permutationValues[0], permutationValues[1]))}
          />
          <IntegerOperation
            title="组合 C(n,m)"
            labels={['n', 'm']}
            values={combinationValues}
            onChange={setCombinationValues}
            action="C(n,m)"
            onRun={() => run('C(n,m)', combinationValues.join(', '), () => combination(combinationValues[0], combinationValues[1]))}
          />
        </div>

        <aside className="calculator-output">
          <div className="result-panel">
            <span>计算结果</span>
            <output>{result}</output>
          </div>
          <div className="history-panel">
            <h2><History />计算记录</h2>
            {history.length === 0
              ? <p className="empty-state">暂无计算记录</p>
              : history.map((item, index) => <p key={`${index}-${item}`}>{item}</p>)}
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

function IntegerOperation({ title, labels = ['数值 1', '数值 2'], values, onChange, action, onRun }: {
  title: string
  labels?: [string, string]
  values: string[]
  onChange(values: string[]): void
  action: string
  onRun(): void
}) {
  return (
    <section className="tool-card operation-card">
      <h2>{title}</h2>
      <div className="operation-row">
        {values.map((value, index) => (
          <label key={labels[index]}>
            <span>{labels[index]}</span>
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
